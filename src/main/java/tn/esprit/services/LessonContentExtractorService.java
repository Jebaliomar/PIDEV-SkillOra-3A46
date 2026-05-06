package tn.esprit.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jsoup.Jsoup;
import tn.esprit.entities.Lesson;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class LessonContentExtractorService {

    private static final long MAX_PDF_BYTES = 25_000_000L;
    private static final int MAX_SUMMARY_INPUT_CHARS = 18_000;

    public String extractForSummary(Lesson lesson) throws Exception {
        if (lesson == null) {
            throw new IllegalStateException("No lesson selected.");
        }

        String type = lesson.getType() == null ? "" : lesson.getType().trim().toLowerCase();
        String text = switch (type) {
            case "text" -> extractTextLessonContent(lesson);
            case "pdf" -> extractPdfLessonContent(lesson);
            default -> throw new IllegalStateException("AI summary is currently available for text and PDF lessons only.");
        };

        if (text.isBlank()) {
            throw new IllegalStateException("No readable content found in this lesson.");
        }

        return text.length() > MAX_SUMMARY_INPUT_CHARS ? text.substring(0, MAX_SUMMARY_INPUT_CHARS) : text;
    }

    private String extractTextLessonContent(Lesson lesson) {
        String raw = lesson.getContent() == null ? "" : lesson.getContent();
        if (raw.isBlank()) {
            throw new IllegalStateException("This text lesson is empty.");
        }

        return normalizeWhitespace(Jsoup.parse(raw).text());
    }

    private String extractPdfLessonContent(Lesson lesson) throws Exception {
        Path pdfPath = resolveFile(lesson.getFilePath());
        long size = Files.size(pdfPath);
        if (size > MAX_PDF_BYTES) {
            throw new IllegalStateException("PDF is too large to summarize.");
        }

        String text;
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            text = normalizeWhitespace(new PDFTextStripper().getText(document));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to parse this PDF file.");
        }

        if (text.isBlank()) {
            throw new IllegalStateException("Could not extract readable text from this PDF. Scanned/image PDFs are not supported.");
        }
        return text;
    }

    private Path resolveFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalStateException("No PDF file found for this lesson.");
        }

        Path path = Path.of(filePath);
        if (!path.isAbsolute()) {
            path = Path.of("").toAbsolutePath().resolve(filePath);
        }

        File file = path.normalize().toFile();
        if (!file.exists() || !file.isFile()) {
            throw new IllegalStateException("PDF file is missing on this computer.");
        }
        return file.toPath();
    }

    private String normalizeWhitespace(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }
}
