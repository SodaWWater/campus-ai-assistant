package com.liminghan.campusai.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Component
public class FileTextExtractor {

    /**
     * 根据文件类型提取文本内容
     */
    public String extract(byte[] fileBytes, String fileType) throws IOException {
        return switch (fileType.toLowerCase()) {
            case "pdf" -> extractPdf(fileBytes);
            case "docx", "doc" -> extractWord(fileBytes);
            case "md" -> new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8);
            default -> new String(fileBytes, java.nio.charset.StandardCharsets.UTF_8); // txt
        };
    }

    private String extractPdf(byte[] fileBytes) throws IOException {
        try (var pdf = Loader.loadPDF(new RandomAccessReadBuffer(fileBytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(pdf);
        }
    }

    private String extractWord(byte[] fileBytes) throws IOException {
        try (var doc = new XWPFDocument(new ByteArrayInputStream(fileBytes))) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
            return extractor.getText();
        }
    }
}
