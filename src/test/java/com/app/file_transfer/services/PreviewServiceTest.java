package com.app.file_transfer.services;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PreviewServiceTest {

    @Test
    public void testEncodeFilenameForHeader() {
        // Test normal ASCII filename
        String normalFilename = "document.pdf";
        String encoded = PreviewService.encodeFilenameForHeader(normalFilename);
        assertEquals("document.pdf", encoded);

        // Test filename with spaces
        String filenameWithSpaces = "my document.pdf";
        String encodedSpaces = PreviewService.encodeFilenameForHeader(filenameWithSpaces);
        assertEquals("my%20document.pdf", encodedSpaces);

        // Test filename with Unicode characters (Vietnamese)
        String unicodeFilename = "Tài liệu Đặc biệt.pdf";
        String encodedUnicode = PreviewService.encodeFilenameForHeader(unicodeFilename);
        assertNotNull(encodedUnicode);
        assertFalse(encodedUnicode.contains("Đ")); // Should be encoded
        assertTrue(encodedUnicode.contains("%")); // Should contain percent encoding

        // Test filename with special characters
        String specialFilename = "file@#$%^&*().txt";
        String encodedSpecial = PreviewService.encodeFilenameForHeader(specialFilename);
        assertNotNull(encodedSpecial);
        // Should handle special characters properly

        // Test empty filename
        String emptyFilename = "";
        String encodedEmpty = PreviewService.encodeFilenameForHeader(emptyFilename);
        assertEquals("", encodedEmpty);

        // Test null filename (should not throw exception)
        assertDoesNotThrow(() -> {
            PreviewService.encodeFilenameForHeader(null);
        });
    }

    @Test
    public void testVietnameseCharacters() {
        // Test various Vietnamese characters
        String[] vietnameseFilenames = {
            "Báo cáo.docx",
            "Đề tài nghiên cứu.pdf", 
            "Hướng dẫn sử dụng.txt",
            "Tài liệu tham khảo.xlsx",
            "Ảnh chụp màn hình.png"
        };

        for (String filename : vietnameseFilenames) {
            String encoded = PreviewService.encodeFilenameForHeader(filename);
            assertNotNull(encoded, "Encoded filename should not be null for: " + filename);
            assertFalse(encoded.isEmpty(), "Encoded filename should not be empty for: " + filename);
            
            // Should not contain raw Unicode characters that cause the error
            assertFalse(encoded.matches(".*[\\u0100-\\uFFFF].*"), 
                "Encoded filename should not contain raw Unicode characters: " + encoded);
        }
    }
}
