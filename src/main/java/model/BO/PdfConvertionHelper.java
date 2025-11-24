package model.BO;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.pdfbox.pdmodel.PDDocument;

import com.fileconverter.util.PdfToDocxConverter;
import com.fileconverter.bean.ConversionTask;

public class PdfConvertionHelper {
    private static final int MAX_PAGES_PER_FILE = 50;
    private static final int MAX_THREAD_POOL_SIZE = 12;
    private static final int RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final long CHUNK_TIMEOUT_MINUTES = 10; // Timeout cho mỗi chunk
    private static final long TOTAL_TIMEOUT_MINUTES = 30; // Timeout tổng cho toàn bộ conversion

    /**
     * Convert PDF to DOCX with automatic cleanup and progress tracking
     */
    public static ConversionResult convertPdfToDoc(String fileInput) {
        return convertPdfToDoc(fileInput, null);
    }

    /**
     * Convert PDF to DOCX with progress callback
     * @param fileInput Path to input PDF file
     * @param progressCallback Callback for progress updates (nullable)
     */
    public static ConversionResult convertPdfToDoc(String fileInput, ProgressCallback progressCallback) {
        List<String> tempFiles = new ArrayList<>();
        List<String> tempDocxFiles = new ArrayList<>(); // Track DOCX files separately

        try {
            // Validate input file
            File f = new File(fileInput);
            if (!f.exists()) {
                return ConversionResult.failure("File does not exist: " + fileInput);
            }

            // Security: Validate file path to prevent path traversal
            String canonicalPath = f.getCanonicalPath();
            if (!canonicalPath.equals(f.getAbsolutePath())) {
                return ConversionResult.failure("Invalid file path detected");
            }

            if (!fileInput.toLowerCase().endsWith(".pdf")) {
                return ConversionResult.failure("File is not a PDF: " + fileInput);
            }

            // Check file size (optional - prevent huge files)
            long fileSizeInMB = f.length() / (1024 * 1024);
            if (fileSizeInMB > 100) { // Limit to 100MB
                return ConversionResult.failure("File too large: " + fileSizeInMB + "MB (max 100MB)");
            }

            String fileOutput = fileInput.replaceAll("(?i)\\.pdf$", ".docx");
            return convertPdfToDoc(fileInput, fileOutput, tempFiles, tempDocxFiles, progressCallback);

        } catch (IOException e) {
            return ConversionResult.failure("IO Error: " + e.getMessage());
        } catch (Exception e) {
            return ConversionResult.failure("Error converting PDF: " + e.getMessage());
        } finally {
            // CLEANUP: Xóa PDF chunks trước
            cleanupTempFiles(tempFiles, "PDF chunks");
            // Xóa DOCX chunks sau khi đã combine xong
            cleanupTempFiles(tempDocxFiles, "DOCX chunks");
        }
    }

    private static ConversionResult convertPdfToDoc(String fileInput, String fileOutput,
                                                    List<String> tempPdfFiles,
                                                    List<String> tempDocxFiles,
                                                    ProgressCallback progressCallback) {
        try {
            if (progressCallback != null) {
                progressCallback.onProgress(0, "Starting conversion...");
            }

            // Step 1: Split PDF
            if (progressCallback != null) {
                progressCallback.onProgress(10, "Splitting PDF into chunks...");
            }

            List<String> chunkPdfPaths = splitPdf(fileInput);
            tempPdfFiles.addAll(chunkPdfPaths);

            if (chunkPdfPaths.isEmpty()) {
                return ConversionResult.failure("Failed to split PDF");
            }

            if (progressCallback != null) {
                progressCallback.onProgress(30, "Split into " + chunkPdfPaths.size() + " chunks");
            }

            // Step 2: Convert chunks to DOCX
            if (progressCallback != null) {
                progressCallback.onProgress(40, "Converting chunks to DOCX...");
            }

            List<String> docxPaths = convertChunkPdfToDocx(chunkPdfPaths, progressCallback);
            tempDocxFiles.addAll(docxPaths); // Track separately for cleanup AFTER combine

            if (docxPaths.isEmpty()) {
                return ConversionResult.failure("Failed to convert PDF chunks to DOCX");
            }

            if (docxPaths.size() != chunkPdfPaths.size()) {
                return ConversionResult.failure("Only " + docxPaths.size() + "/" + chunkPdfPaths.size() + " chunks converted successfully");
            }

            // Step 3: Combine DOCX files
            if (progressCallback != null) {
                progressCallback.onProgress(80, "Combining DOCX files...");
            }

            Collections.sort(docxPaths);

            boolean combined = false;
            try {
                combined = CombineDocx.combineFiles(docxPaths, fileOutput);
            } catch (Exception e) {
                return ConversionResult.failure("Failed to combine DOCX files: " + e.getMessage());
            }

            if (!combined) {
                return ConversionResult.failure("Failed to combine DOCX files");
            }

            // Verify output file was created
            File outputFile = new File(fileOutput);
            if (!outputFile.exists() || outputFile.length() == 0) {
                return ConversionResult.failure("Output file was not created or is empty");
            }

            if (progressCallback != null) {
                progressCallback.onProgress(100, "Conversion completed successfully");
            }

            return ConversionResult.success(fileOutput);

        } catch (Exception e) {
            return ConversionResult.failure("Conversion error: " + e.getMessage());
        }
    }

    /**
     * Split PDF into smaller chunks with proper resource management
     */
    private static List<String> splitPdf(String filePath) {
        List<String> pathOfChunkFiles = new ArrayList<>();
        PDDocument document = null;

        try {
            String fileNameDontHaveExtension = filePath.replaceAll("(?i)\\.pdf$", "").replaceAll(" ", "_");
            document = PDDocument.load(new File(filePath));
            int totalPages = document.getNumberOfPages();

            if (totalPages == 0) {
                System.err.println("PDF has no pages: " + filePath);
                return pathOfChunkFiles;
            }

            int fileIndex = 1;

            for (int start = 0; start < totalPages; start += MAX_PAGES_PER_FILE) {
                int end = Math.min(start + MAX_PAGES_PER_FILE, totalPages);
                PDDocument chunkDocument = null;

                try {
                    chunkDocument = new PDDocument();
                    for (int page = start; page < end; page++) {
                        chunkDocument.addPage(document.getPage(page));
                    }

                    String outputPdf = fileNameDontHaveExtension + "_part_" + String.format("%03d", fileIndex) + ".pdf";
                    chunkDocument.save(outputPdf);
                    pathOfChunkFiles.add(outputPdf);
                    fileIndex++;

                } finally {
                    if (chunkDocument != null) {
                        chunkDocument.close();
                    }
                }
            }

            System.out.println("Split PDF into " + pathOfChunkFiles.size() + " chunks");

        } catch (Exception e) {
            System.err.println("Error splitting PDF: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    System.err.println("Error closing document: " + e.getMessage());
                }
            }
        }

        return pathOfChunkFiles;
    }

    /**
     * Convert PDF chunks to DOCX using thread pool with retry logic and progress tracking
     */
    private static List<String> convertChunkPdfToDocx(List<String> chunkFiles, ProgressCallback progressCallback) {
        ExecutorService executor = Executors.newFixedThreadPool(MAX_THREAD_POOL_SIZE);
        List<Future<String>> futures = new ArrayList<>();
        AtomicInteger completedChunks = new AtomicInteger(0);
        int totalChunks = chunkFiles.size();

        try {
            // Submit all conversion tasks
            for (String filePath : chunkFiles) {
                Callable<String> task = () -> {
                    String result = convertChunkWithRetry(filePath);
                    int completed = completedChunks.incrementAndGet();

                    if (progressCallback != null) {
                        int progress = 40 + (int)((completed / (double)totalChunks) * 40); // 40-80%
                        progressCallback.onProgress(progress, "Converted chunk " + completed + "/" + totalChunks);
                    }

                    return result;
                };
                futures.add(executor.submit(task));
            }

            // Wait for all tasks to complete and collect results
            List<String> docxPaths = new ArrayList<>();
            for (Future<String> future : futures) {
                try {
                    String docxPath = future.get(CHUNK_TIMEOUT_MINUTES, TimeUnit.MINUTES);
                    if (docxPath != null) {
                        docxPaths.add(docxPath);
                    }
                } catch (TimeoutException e) {
                    System.err.println("Conversion timeout for a chunk after " + CHUNK_TIMEOUT_MINUTES + " minutes");
                    future.cancel(true);
                } catch (ExecutionException e) {
                    System.err.println("Conversion failed: " + e.getCause().getMessage());
                }
            }

            System.out.println("Converted " + docxPaths.size() + "/" + chunkFiles.size() + " chunks successfully");
            return docxPaths;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Conversion interrupted: " + e.getMessage());
            return new ArrayList<>();
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(TOTAL_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                    System.err.println("Executor did not terminate in time, forcing shutdown");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Convert a single chunk with retry mechanism
     */
    private static String convertChunkWithRetry(String filePath) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= RETRY_ATTEMPTS; attempt++) {
            try {
                return convertSingleChunk(filePath);
            } catch (Exception e) {
                lastException = e;
                System.err.println("Attempt " + attempt + "/" + RETRY_ATTEMPTS + " failed for " + filePath + ": " + e.getMessage());

                if (attempt < RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        System.err.println("Failed to convert " + filePath + " after " + RETRY_ATTEMPTS + " attempts");
        if (lastException != null) {
            lastException.printStackTrace();
        }
        return null;
    }

    /**
     * Convert a single PDF chunk to DOCX using PDFBox
     */
    private static String convertSingleChunk(String filePath) throws Exception {
        System.out.println("Converting: " + filePath);

        String docFilePath = filePath.replaceAll("(?i)\\.pdf$", ".docx");
        
        // Create a simple conversion task for progress tracking
        ConversionTask task = new ConversionTask(0, "system", filePath);
        task.setOutputFilePath(docFilePath);
        
        // Use the new PdfToDocxConverter
        PdfToDocxConverter converter = new PdfToDocxConverter();
        boolean success = converter.convertPdfToDocx(task, filePath, docFilePath);
        
        if (!success) {
            throw new Exception("Failed to convert " + filePath);
        }

        System.out.println("Converted: " + filePath + " -> " + docFilePath);
        return docFilePath;
    }

    /**
     * Clean up temporary files with descriptive label
     */
    private static void cleanupTempFiles(List<String> tempFiles, String label) {
        if (tempFiles == null || tempFiles.isEmpty()) {
            return;
        }

        int deleted = 0;
        for (String filePath : tempFiles) {
            try {
                Path path = Paths.get(filePath);
                if (Files.exists(path)) {
                    Files.delete(path);
                    deleted++;
                }
            } catch (IOException e) {
                System.err.println("Failed to delete temp file (" + label + "): " + filePath + " - " + e.getMessage());
            }
        }

        System.out.println("Cleaned up " + deleted + "/" + tempFiles.size() + " " + label);
    }

    /**
     * Progress callback interface
     */
    public interface ProgressCallback {
        void onProgress(int percentage, String message);
    }

    /**
     * Result object for conversion operations
     */
    public static class ConversionResult {
        private final boolean success;
        private final String message;
        private final String outputPath;

        private ConversionResult(boolean success, String message, String outputPath) {
            this.success = success;
            this.message = message;
            this.outputPath = outputPath;
        }

        public static ConversionResult success(String outputPath) {
            return new ConversionResult(true, "Conversion successful", outputPath);
        }

        public static ConversionResult failure(String message) {
            return new ConversionResult(false, message, null);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getOutputPath() {
            return outputPath;
        }

        @Override
        public String toString() {
            return success ? "Success: " + outputPath : "Failure: " + message;
        }
    }
}