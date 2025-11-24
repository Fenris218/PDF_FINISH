package model.BO;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;

import utils.Utils;

public class CombineDocx {

    /**
     * Combine nhiều file DOCX thành 1 file duy nhất
     * @param docFilePaths danh sách đường dẫn file DOCX (ít nhất 1 file)
     * @param output đường dẫn file DOCX đầu ra
     * @param progressCallback callback để báo tiến độ (nullable)
     * @return true nếu combine thành công, false nếu có lỗi
     */
    public static boolean combineFiles(List<String> docFilePaths, String output, ProgressCallback progressCallback) {
        if (docFilePaths == null || docFilePaths.isEmpty()) {
            System.err.println("No DOCX files to combine.");
            return false;
        }

        // WARNING: For very large number of files (>50), use batch combining
        if (docFilePaths.size() > 50) {
            System.out.println("Using BATCH mode for " + docFilePaths.size() + " files (threshold: 50)");
            return combineInBatches(docFilePaths, output, progressCallback);
        }

        XWPFDocument firstDocument = null;

        try {
            // Validate all input files exist
            for (String filePath : docFilePaths) {
                File file = new File(filePath);
                if (!file.exists()) {
                    System.err.println("File does not exist: " + filePath);
                    return false;
                }
                if (file.length() == 0) {
                    System.err.println("File is empty: " + filePath);
                    return false;
                }
            }

            System.out.println("Starting to combine " + docFilePaths.size() + " DOCX files...");
            long startTime = System.currentTimeMillis();

            // Load first document
            try (FileInputStream fis = new FileInputStream(docFilePaths.get(0))) {
                firstDocument = new XWPFDocument(fis);
            }

            if (progressCallback != null) {
                progressCallback.onProgress(81, "Combined 1/" + docFilePaths.size() + " files");
            }

            // Merge remaining documents
            for (int i = 1; i < docFilePaths.size(); i++) {
                XWPFDocument documentMerge = null;
                try {
                    try (FileInputStream fis = new FileInputStream(docFilePaths.get(i))) {
                        documentMerge = new XWPFDocument(fis);
                    }

                    // Copy all body elements from the merge document to the first document
                    CTBody body = documentMerge.getDocument().getBody();
                    appendBody(firstDocument, documentMerge);

                    // Progress update
                    if (progressCallback != null && i % 10 == 0) {
                        int progress = 81 + (int)((i / (double)docFilePaths.size()) * 18); // 81-99%
                        progressCallback.onProgress(progress, "Combined " + i + "/" + docFilePaths.size() + " files");
                    }

                    // Log progress for large files
                    if (i % 50 == 0) {
                        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
                        System.out.println("Progress: " + i + "/" + docFilePaths.size() + " files combined (" + elapsed + "s)");
                    }

                } finally {
                    // Close merged document to free resources
                    if (documentMerge != null) {
                        documentMerge.close();
                    }
                }
            }

            // Save combined document
            System.out.println("Saving combined document...");
            try (FileOutputStream fos = new FileOutputStream(output)) {
                firstDocument.write(fos);
            }

            // Verify output file
            File outputFile = new File(output);
            if (!outputFile.exists() || outputFile.length() == 0) {
                System.err.println("Output file was not created or is empty: " + output);
                return false;
            }

            long totalTime = (System.currentTimeMillis() - startTime) / 1000;
            System.out.println("Combine done: " + output + " (size: " + outputFile.length() + " bytes, time: " + totalTime + "s)");
            return true;

        } catch (OutOfMemoryError e) {
            System.err.println("OUT OF MEMORY while combining files! Try increasing heap size (-Xmx) or use batch combining");
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.err.println("Error combining files: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            // Close first document
            if (firstDocument != null) {
                try {
                    firstDocument.close();
                } catch (Exception e) {
                    System.err.println("Error closing document: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Combine files in batches to avoid memory issues
     */
    private static boolean combineInBatches(List<String> docFilePaths, String output, ProgressCallback progressCallback) {
        System.out.println("Using BATCH combining for " + docFilePaths.size() + " files");

        final int BATCH_SIZE = 30; // Combine 30 files at a time
        final int MAX_PARALLEL_BATCHES = 2; // Process 2 batches in parallel
        List<String> batchOutputs = new ArrayList<>();

        try {
            ExecutorService batchExecutor = Executors.newFixedThreadPool(MAX_PARALLEL_BATCHES);
            List<Future<String>> batchFutures = new ArrayList<>();

            // Step 1: Combine in batches (PARALLEL)
            for (int i = 0; i < docFilePaths.size(); i += BATCH_SIZE) {
                final int batchIndex = i / BATCH_SIZE;
                final int start = i;
                final int end = Math.min(i + BATCH_SIZE, docFilePaths.size());

                Callable<String> batchTask = () -> {
                    List<String> batch = docFilePaths.subList(start, end);
                    String batchOutput = output.replace(".docx", "_batch_" + batchIndex + ".docx");
                    System.out.println("Combining batch " + (batchIndex + 1) + ": files " + start + " to " + (end-1));

                    if (!combineFiles(batch, batchOutput, null)) {
                        throw new Exception("Batch " + batchIndex + " failed");
                    }

                    return batchOutput;
                };

                batchFutures.add(batchExecutor.submit(batchTask));
            }

            // Wait for all batches to complete
            for (Future<String> future : batchFutures) {
                try {
                    String batchOutput = future.get(30, TimeUnit.MINUTES);
                    batchOutputs.add(batchOutput);

                    if (progressCallback != null) {
                        int progress = 81 + (int)((batchOutputs.size() / (double)batchFutures.size()) * 18);
                        progressCallback.onProgress(progress, "Batch " + batchOutputs.size() + "/" + batchFutures.size() + " completed");
                    }
                } catch (TimeoutException e) {
                    System.err.println("Batch combining timeout");
                    return false;
                } catch (ExecutionException e) {
                    System.err.println("Batch failed: " + e.getCause().getMessage());
                    return false;
                }
            }

            batchExecutor.shutdown();

            // Step 2: Combine all batch outputs
            System.out.println("Combining " + batchOutputs.size() + " batch files into final output...");
            boolean result = combineFiles(batchOutputs, output, null);

            // Step 3: Clean up batch files
            for (String batchFile : batchOutputs) {
                try {
                    new File(batchFile).delete();
                } catch (Exception e) {
                    System.err.println("Failed to delete batch file: " + batchFile);
                }
            }

            return result;

        } catch (Exception e) {
            System.err.println("Error in batch combining: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Overload without progress callback for backward compatibility
     */
    public static boolean combineFiles(List<String> docFilePaths, String output) {
        return combineFiles(docFilePaths, output, null);
    }
    
    /**
     * Helper method to append body elements from one document to another
     */
    private static void appendBody(XWPFDocument dest, XWPFDocument src) throws Exception {
        for (IBodyElement bodyElement : src.getBodyElements()) {
            if (bodyElement instanceof XWPFParagraph) {
                XWPFParagraph srcParagraph = (XWPFParagraph) bodyElement;
                XWPFParagraph destParagraph = dest.createParagraph();
                
                // Copy paragraph properties
                destParagraph.getCTP().setPPr(srcParagraph.getCTP().getPPr());
                
                // Copy runs
                for (XWPFRun srcRun : srcParagraph.getRuns()) {
                    XWPFRun destRun = destParagraph.createRun();
                    // Copy run properties and text
                    destRun.getCTR().setRPr(srcRun.getCTR().getRPr());
                    destRun.setText(srcRun.getText(0));
                }
            } else if (bodyElement instanceof XWPFTable) {
                XWPFTable srcTable = (XWPFTable) bodyElement;
                // For tables, we need to copy the CTTbl directly
                dest.getDocument().getBody().addNewTbl().set(srcTable.getCTTbl());
            }
        }
    }

    /**
     * Progress callback interface
     */
    public interface ProgressCallback {
        void onProgress(int percentage, String message);
    }

    /**
     * Xóa các file tạm (docx và pdf tương ứng)
     * @param temporalFiles danh sách đường dẫn file docx tạm đã tạo
     */
    public static void deleteTemporalFiles(List<String> temporalFiles) {
        if (temporalFiles == null || temporalFiles.isEmpty()) {
            return;
        }

        int deletedCount = 0;
        for (String filePath : temporalFiles) {
            try {
                // Xóa file DOCX
                File docxFile = new File(filePath);
                if (docxFile.exists() && docxFile.delete()) {
                    deletedCount++;
                    System.out.println("Deleted: " + filePath);
                }

                // Xóa file PDF tương ứng nếu tồn tại
                String pdfPath = filePath.replaceAll("(?i)\\.docx$", ".pdf");
                File pdfFile = new File(pdfPath);
                if (pdfFile.exists() && pdfFile.delete()) {
                    deletedCount++;
                    System.out.println("Deleted: " + pdfPath);
                }
            } catch (Exception e) {
                System.err.println("Failed to delete file: " + filePath + " - " + e.getMessage());
            }
        }

        System.out.println("Deleted " + deletedCount + " temporary files");
    }
}