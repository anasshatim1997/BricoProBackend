package com.bricopro.verification.ocr;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
@Slf4j
public class TesseractCinOcrService {

    @Value("${app.ocr.tessdata-path:/usr/share/tesseract-ocr/5/tessdata}")
    private String tessdataPath;

    @Value("${app.ocr.language:fra}")
    private String language;

    public String extractText(byte[] imageBytes) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (original == null) {
                throw new IllegalArgumentException("Unreadable image file");
            }
            BufferedImage processed = preprocess(original);

            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessdataPath);
            tesseract.setLanguage(language);
            tesseract.setPageSegMode(3);

            return tesseract.doOCR(processed);
        } catch (TesseractException e) {
            log.error("Tesseract OCR failed: {}", e.getMessage());
            throw new RuntimeException("OCR processing failed");
        } catch (IOException e) {
            log.error("Image read failed: {}", e.getMessage());
            throw new RuntimeException("Could not read uploaded image");
        }
    }

    private BufferedImage preprocess(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        BufferedImage gray = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        gray.getGraphics().drawImage(source, 0, 0, null);

        int targetWidth = 1600;
        if (width >= targetWidth) {
            return gray;
        }
        double scale = (double) targetWidth / width;
        int newHeight = (int) (height * scale);
        BufferedImage resized = new BufferedImage(targetWidth, newHeight, BufferedImage.TYPE_BYTE_GRAY);
        resized.getGraphics().drawImage(gray, 0, 0, targetWidth, newHeight, null);
        return resized;
    }
}