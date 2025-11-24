package com.fileconverter.util;

import com.fileconverter.bean.ConversionTask;
import com.fileconverter.bo.ConversionTaskBO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.util.Units;

import java.io.*;
import java.util.regex.Pattern;

public class PdfToDocxConverter {
    private ConversionTaskBO conversionTaskBO = new ConversionTaskBO();

    public boolean convertPdfToDocx(ConversionTask task, String inputPath, String outputPath) {
        try {
            // Update task status to PROCESSING
            conversionTaskBO.updateTaskStatus(task.getTaskId(), ConversionTask.ConversionStatus.PROCESSING, 25);

            // Simulate conversion process with progress updates
            if (!simulateConversion(task, inputPath, outputPath)) {
                return false;
            }

            // Task completion will be handled by ConversionWorker after uploading output file
            // Just mark conversion step as 95% complete - worker will set 100% with URL
            conversionTaskBO.updateTaskStatus(task.getTaskId(), ConversionTask.ConversionStatus.PROCESSING, 95);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            conversionTaskBO.updateTaskFailed(task.getTaskId(), e.getMessage());
            return false;
        }
    }

    private boolean simulateConversion(ConversionTask task, String inputPath, String outputPath) throws Exception {
        File inputFile = new File(inputPath);
        if (!inputFile.exists()) {
            throw new Exception("Input file not found: " + inputPath);
        }

        PDDocument pdfDocument = null;
        XWPFDocument docxDocument = null;

        try {
            // Step 1: Load PDF document
            conversionTaskBO.updateTaskStatus(task.getTaskId(), ConversionTask.ConversionStatus.PROCESSING, 20);
            pdfDocument = PDDocument.load(inputFile);
            int totalPages = pdfDocument.getNumberOfPages();
            
            // Step 2: Extract text from PDF
            conversionTaskBO.updateTaskStatus(task.getTaskId(), ConversionTask.ConversionStatus.PROCESSING, 40);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String pdfText = stripper.getText(pdfDocument);

            // Step 3: Create DOCX document
            conversionTaskBO.updateTaskStatus(task.getTaskId(), ConversionTask.ConversionStatus.PROCESSING, 60);
            docxDocument = new XWPFDocument();
            
            // Set document properties
            docxDocument.getProperties().getCoreProperties().setTitle("Converted from PDF");
            
            // Split text into paragraphs (by double newlines or single newlines)
            String[] paragraphs = pdfText.split("\\r?\\n\\r?\\n|\\r?\\n");
            
            int processedParagraphs = 0;
            for (String paraText : paragraphs) {
                paraText = paraText.trim();
                if (paraText.isEmpty()) {
                    continue;
                }
                
                // Create paragraph in DOCX
                XWPFParagraph paragraph = docxDocument.createParagraph();
                
                // Detect if it's likely a heading (short text, all caps, or ends with colon)
                if (isLikelyHeading(paraText)) {
                    paragraph.setStyle("Heading1");
                    XWPFRun run = paragraph.createRun();
                    run.setText(paraText);
                    run.setBold(true);
                    run.setFontSize(14);
                } else {
                    // Normal paragraph
                    XWPFRun run = paragraph.createRun();
                    run.setText(paraText);
                    run.setFontSize(11);
                    run.setFontFamily("Calibri");
                }
                
                processedParagraphs++;
                
                // Update progress
                if (processedParagraphs % 10 == 0) {
                    int progress = 60 + (int)((processedParagraphs / (double)paragraphs.length) * 20);
                    conversionTaskBO.updateTaskStatus(task.getTaskId(), ConversionTask.ConversionStatus.PROCESSING, 
                        Math.min(progress, 80));
                }
            }

            // Add document metadata
            XWPFParagraph footerPara = docxDocument.createParagraph();
            footerPara.setPageBreak(true);
            XWPFRun footerRun = footerPara.createRun();
            footerRun.setItalic(true);
            footerRun.setFontSize(9);
            footerRun.setColor("808080");
            footerRun.setText("Converted from PDF - Total pages: " + totalPages);

            // Step 4: Save DOCX document
            conversionTaskBO.updateTaskStatus(task.getTaskId(), ConversionTask.ConversionStatus.PROCESSING, 85);
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                docxDocument.write(fos);
            }

            conversionTaskBO.updateTaskStatus(task.getTaskId(), ConversionTask.ConversionStatus.PROCESSING, 95);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            // Clean up resources
            if (pdfDocument != null) {
                try {
                    pdfDocument.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (docxDocument != null) {
                try {
                    docxDocument.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Heuristic to detect if text is likely a heading
     */
    private boolean isLikelyHeading(String text) {
        if (text.length() < 5 || text.length() > 100) {
            return false;
        }
        
        // Check if mostly uppercase
        long upperCount = text.chars().filter(Character::isUpperCase).count();
        long letterCount = text.chars().filter(Character::isLetter).count();
        
        if (letterCount > 0 && (upperCount / (double)letterCount) > 0.7) {
            return true;
        }
        
        // Check if ends with colon
        if (text.endsWith(":")) {
            return true;
        }
        
        // Check if it's a numbered heading (e.g., "1. Introduction")
        if (Pattern.matches("^\\d+\\.?\\s+[A-Z].*", text)) {
            return true;
        }
        
        return false;
    }
}
