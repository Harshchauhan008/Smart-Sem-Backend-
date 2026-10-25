package com.smartsem.smartsem.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

@Service
public class DocumentTextExtractionService {

    public String extractText(Path filePath)
            throws IOException, TesseractException {

        try {
            return doExtractText(filePath);
        } catch (Throwable e) {
            // Shows the real OCR error in the Render logs.
            System.err.println("OCR failed for " + filePath + ": " + e);
            e.printStackTrace();
            throw e;
        }
    }

    private String doExtractText(Path filePath)
            throws IOException, TesseractException {

        String fileName = filePath.getFileName()
                .toString()
                .toLowerCase(Locale.ROOT);

        String extractedText;

        if (fileName.endsWith(".pdf")) {
            extractedText = extractPdfText(filePath);
        } else if (fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg")
                || fileName.endsWith(".png")) {
            extractedText = extractImageText(filePath);
        } else {
            throw new IllegalArgumentException(
                    "Only PDF, JPG and PNG files are supported.");
        }

        if (extractedText == null || extractedText.isBlank()) {
            throw new IllegalArgumentException(
                    "No text was found. The document may be scanned or blank.");
        }

        return extractedText.trim();
    }

    private String extractPdfText(Path filePath) throws IOException {
        try (var document = Loader.loadPDF(filePath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractImageText(Path filePath)
            throws IOException, TesseractException {

        BufferedImage original = ImageIO.read(filePath.toFile());

        if (original == null) {
            throw new IOException("Could not read the uploaded image.");
        }

        // Enlarge small images to improve OCR recognition.
        int targetWidth = Math.max(original.getWidth(), 2400);
        double scale = (double) targetWidth / original.getWidth();
        int targetHeight = (int) Math.round(
                original.getHeight() * scale);

        BufferedImage resized = new BufferedImage(
                targetWidth,
                targetHeight,
                BufferedImage.TYPE_BYTE_GRAY);

        Graphics2D graphics = resized.createGraphics();

        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        graphics.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        graphics.drawImage(
                original, 0, 0, targetWidth, targetHeight, null);

        graphics.dispose();

        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(resolveTessdataPath());
        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(11);

        try {
            return tesseract.doOCR(resized);
        } catch (LinkageError e) {
            // UnsatisfiedLinkError / NoClassDefFoundError: native Tesseract library missing
            throw new IllegalStateException(
                    "Text extraction is unavailable right now. Please try again later.", e);
        }
    }

    private String resolveTessdataPath() {
        String fromEnv = System.getenv("TESSDATA_PREFIX");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String windowsPath = "C:/Program Files/Tesseract-OCR/tessdata";
        if (new File(windowsPath).exists()) {
            return windowsPath; // local development on Windows
        }
        return "/usr/share/tesseract-ocr/5/tessdata"; // Linux / Docker
    }
}