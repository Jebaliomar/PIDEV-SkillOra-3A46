package tn.esprit.services;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import tn.esprit.entities.Certificate;
import tn.esprit.entities.Course;
import tn.esprit.entities.Enrollment;
import tn.esprit.entities.User;
import tn.esprit.tools.MyConnection;
import tn.esprit.tools.QrGen;

import java.awt.Color;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class CertificateService {

    private static final DateTimeFormatter CERTIFICATE_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final String DEFAULT_WEB_BASE_URL = "http://127.0.0.1:8000";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Connection connection;

    public CertificateService() {
        this.connection = MyConnection.getInstance().getConnection();
    }

    public void add(Certificate certificate) throws SQLException {
        String sql = "INSERT INTO `certificate` (`certificate_code`, `issued_at`, `pdf_path`, `student_name`, `course_title`, `enrollment_id`) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, certificate.getCertificateCode());
            setTimestamp(preparedStatement, 2, certificate.getIssuedAt());
            preparedStatement.setString(3, certificate.getPdfPath());
            preparedStatement.setString(4, certificate.getStudentName());
            preparedStatement.setString(5, certificate.getCourseTitle());
            setInteger(preparedStatement, 6, certificate.getEnrollmentId());
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    certificate.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public List<Certificate> getAll() throws SQLException {
        String sql = "SELECT * FROM `certificate`";
        List<Certificate> certificates = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                certificates.add(mapResultSetToCertificate(resultSet));
            }
        }

        return certificates;
    }

    public Certificate getById(int id) throws SQLException {
        String sql = "SELECT * FROM `certificate` WHERE `id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToCertificate(resultSet);
                }
            }
        }

        return null;
    }

    public Certificate findOneByEnrollment(int enrollmentId) throws SQLException {
        String sql = "SELECT * FROM `certificate` WHERE `enrollment_id` = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, enrollmentId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToCertificate(resultSet);
                }
            }
        }

        return null;
    }

    public Certificate findOneByCode(String certificateCode) throws SQLException {
        String sql = "SELECT * FROM `certificate` WHERE `certificate_code` = ? LIMIT 1";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, certificateCode);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToCertificate(resultSet);
                }
            }
        }

        return null;
    }

    public Certificate issueIfEligible(Enrollment enrollment, User user, Course course, int totalLessons) throws Exception {
        if (enrollment == null || enrollment.getId() == null || totalLessons <= 0) {
            return null;
        }

        Certificate existing = findOneByEnrollment(enrollment.getId());
        if (existing != null) {
            ensurePdfGenerated(existing);
            return existing;
        }

        int progress = enrollment.getProgressPercent() == null ? 0 : enrollment.getProgressPercent();
        if (progress < 100) {
            return null;
        }

        Certificate certificate = new Certificate();
        certificate.setCertificateCode(generateUniqueCode());
        certificate.setIssuedAt(java.time.LocalDateTime.now());
        certificate.setStudentName(resolveStudentName(user));
        certificate.setCourseTitle(course == null || course.getTitle() == null || course.getTitle().isBlank() ? "SkillORA Course" : course.getTitle());
        certificate.setEnrollmentId(enrollment.getId());
        add(certificate);

        Path pdfPath = generatePdf(certificate);
        certificate.setPdfPath(pdfPath.toString());
        update(certificate);
        return certificate;
    }

    public Path ensurePdfGenerated(Certificate certificate) throws Exception {
        if (certificate == null) {
            return null;
        }
        Path pdfPath = generatePdf(certificate);
        certificate.setPdfPath(pdfPath.toString());
        update(certificate);
        return pdfPath;
    }

    public String buildVerificationUrl(Certificate certificate) {
        String baseUrl = loadWebBaseUrl();
        return baseUrl + "/certificates/verify/" + certificate.getCertificateCode();
    }

    public boolean update(Certificate certificate) throws SQLException {
        String sql = "UPDATE `certificate` SET `certificate_code` = ?, `issued_at` = ?, `pdf_path` = ?, "
                + "`student_name` = ?, `course_title` = ?, `enrollment_id` = ? WHERE `id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, certificate.getCertificateCode());
            setTimestamp(preparedStatement, 2, certificate.getIssuedAt());
            preparedStatement.setString(3, certificate.getPdfPath());
            preparedStatement.setString(4, certificate.getStudentName());
            preparedStatement.setString(5, certificate.getCourseTitle());
            setInteger(preparedStatement, 6, certificate.getEnrollmentId());
            preparedStatement.setInt(7, certificate.getId());
            return preparedStatement.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM `certificate` WHERE `id` = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            return preparedStatement.executeUpdate() > 0;
        }
    }

    private Path generatePdf(Certificate certificate) throws Exception {
        Files.createDirectories(Path.of("uploads", "certificates"));
        Path pdfPath = Path.of("uploads", "certificates",
                "certificate-" + certificate.getCertificateCode().toLowerCase().replaceAll("[^a-z0-9-]", "-") + ".pdf")
                .toAbsolutePath()
                .normalize();

        String verificationUrl = buildVerificationUrl(certificate);
        Document document = new Document(PageSize.A4.rotate(), 0, 0, 0, 0);
        PdfWriter writer = PdfWriter.getInstance(document, Files.newOutputStream(pdfPath));
        document.open();

        Rectangle page = document.getPageSize();
        PdfContentByte canvas = writer.getDirectContent();
        BaseFont regular = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
        BaseFont bold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.EMBEDDED);
        BaseFont mono = BaseFont.createFont(BaseFont.COURIER_BOLD, BaseFont.WINANSI, BaseFont.EMBEDDED);

        drawFilledRect(canvas, 0, 0, page.getWidth(), page.getHeight(), new Color(241, 245, 249));
        drawStrokeRect(canvas, 14, 14, page.getWidth() - 28, page.getHeight() - 28, new Color(29, 78, 216), 6);
        drawStrokeRect(canvas, 30, 30, page.getWidth() - 60, page.getHeight() - 60, new Color(147, 197, 253), 2);

        float centerX = page.getWidth() / 2f;
        drawCenteredText(canvas, bold, "SkillORA", 32, centerX, 502, new Color(29, 78, 216));
        drawCenteredText(canvas, bold, "CERTIFICATE OF COMPLETION", 12, centerX, 474, new Color(30, 64, 175), 2.2f);
        drawCenteredText(canvas, bold, "SkillORA Certificate", 42, centerX, 421, new Color(30, 58, 138));
        drawCenteredText(canvas, regular, "This certifies that", 20, centerX, 378, new Color(51, 65, 85));
        drawCenteredText(canvas, bold, nullSafe(certificate.getStudentName(), "SkillORA Learner"), 36, centerX, 331, new Color(29, 78, 216));
        drawCenteredText(canvas, regular, "has successfully completed", 19, centerX, 292, new Color(51, 65, 85));
        drawCenteredText(canvas, bold, fitText(nullSafe(certificate.getCourseTitle(), "SkillORA Course"), 58), 27, centerX, 251, new Color(15, 23, 42));
        drawCenteredText(canvas, regular, "Issued on " + (certificate.getIssuedAt() == null ? "" : certificate.getIssuedAt().format(CERTIFICATE_DATE_FORMAT)),
                15, centerX, 218, new Color(71, 85, 105));

        canvas.setColorStroke(new Color(191, 219, 254));
        canvas.setLineWidth(1);
        canvas.moveTo(52, 174);
        canvas.lineTo(page.getWidth() - 52, 174);
        canvas.stroke();

        drawText(canvas, regular, "Certificate Code", 13, 60, 143, new Color(100, 116, 139));
        drawText(canvas, mono, certificate.getCertificateCode(), 20, 60, 116, new Color(30, 58, 138));
        drawText(canvas, regular, "Verification URL: " + fitText(verificationUrl, 88), 11, 60, 91, new Color(71, 85, 105));

        Image qrImage = Image.getInstance(QrGen.makePngBytes(verificationUrl, 180));
        qrImage.scaleAbsolute(116, 116);
        qrImage.setAbsolutePosition(page.getWidth() - 186, 66);
        writer.getDirectContent().addImage(qrImage);
        drawCenteredText(canvas, regular, "Scan to verify", 11, page.getWidth() - 128, 48, new Color(100, 116, 139));

        document.close();

        return pdfPath;
    }

    private void drawFilledRect(PdfContentByte canvas, float x, float y, float width, float height, Color color) {
        canvas.saveState();
        canvas.setColorFill(color);
        canvas.rectangle(x, y, width, height);
        canvas.fill();
        canvas.restoreState();
    }

    private void drawStrokeRect(PdfContentByte canvas, float x, float y, float width, float height, Color color, float lineWidth) {
        canvas.saveState();
        canvas.setColorStroke(color);
        canvas.setLineWidth(lineWidth);
        canvas.rectangle(x, y, width, height);
        canvas.stroke();
        canvas.restoreState();
    }

    private void drawText(PdfContentByte canvas, BaseFont font, String text, float size, float x, float y, Color color) {
        canvas.saveState();
        canvas.beginText();
        canvas.setFontAndSize(font, size);
        canvas.setColorFill(color);
        canvas.setTextMatrix(x, y);
        canvas.showText(text == null ? "" : text);
        canvas.endText();
        canvas.restoreState();
    }

    private void drawCenteredText(PdfContentByte canvas, BaseFont font, String text, float size, float x, float y, Color color) {
        drawCenteredText(canvas, font, text, size, x, y, color, 0);
    }

    private void drawCenteredText(PdfContentByte canvas, BaseFont font, String text, float size, float x, float y, Color color, float characterSpacing) {
        canvas.saveState();
        canvas.beginText();
        canvas.setFontAndSize(font, size);
        canvas.setCharacterSpacing(characterSpacing);
        canvas.setColorFill(color);
        canvas.showTextAligned(Element.ALIGN_CENTER, text == null ? "" : text, x, y, 0);
        canvas.endText();
        canvas.restoreState();
    }

    private String fitText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private PdfPCell metaCell(String label, String value) {
        Paragraph labelParagraph = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(100, 116, 139)));
        Paragraph valueParagraph = new Paragraph(nullSafe(value, "-"), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(15, 23, 42)));
        valueParagraph.setSpacingBefore(6);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(10);
        cell.addElement(labelParagraph);
        cell.addElement(valueParagraph);
        return cell;
    }

    private String generateUniqueCode() throws SQLException {
        String code;
        do {
            byte[] bytes = new byte[6];
            RANDOM.nextBytes(bytes);
            StringBuilder builder = new StringBuilder("SKH-");
            for (byte value : bytes) {
                builder.append(String.format("%02X", value));
            }
            code = builder.toString();
        } while (findOneByCode(code) != null);
        return code;
    }

    private String resolveStudentName(User user) {
        if (user == null) {
            return "SkillORA Learner";
        }

        String fullName = (nullSafe(user.getFirstName(), "") + " " + nullSafe(user.getLastName(), "")).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        return "SkillORA Learner";
    }

    private String loadWebBaseUrl() {
        Properties properties = new Properties();
        try (InputStream inputStream = CertificateService.class.getResourceAsStream("/app.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (Exception ignored) {
            // Optional config; fall back to the local Symfony development URL.
        }

        Path localProperties = Path.of("app.properties");
        if (Files.exists(localProperties)) {
            try (InputStream inputStream = new FileInputStream(localProperties.toFile())) {
                properties.load(inputStream);
            } catch (Exception ignored) {
                // Optional local override.
            }
        }

        String baseUrl = properties.getProperty("skillora.web.baseUrl", DEFAULT_WEB_BASE_URL).trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl.isBlank() ? DEFAULT_WEB_BASE_URL : baseUrl;
    }

    private String nullSafe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Certificate mapResultSetToCertificate(ResultSet resultSet) throws SQLException {
        Certificate certificate = new Certificate();
        certificate.setId(resultSet.getInt("id"));
        certificate.setCertificateCode(resultSet.getString("certificate_code"));
        certificate.setIssuedAt(getTimestamp(resultSet, "issued_at"));
        certificate.setPdfPath(resultSet.getString("pdf_path"));
        certificate.setStudentName(resultSet.getString("student_name"));
        certificate.setCourseTitle(resultSet.getString("course_title"));
        certificate.setEnrollmentId(getInteger(resultSet, "enrollment_id"));
        return certificate;
    }

    private void setInteger(PreparedStatement preparedStatement, int index, Integer value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, java.sql.Types.INTEGER);
        } else {
            preparedStatement.setInt(index, value);
        }
    }

    private Integer getInteger(ResultSet resultSet, String column) throws SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private void setTimestamp(PreparedStatement preparedStatement, int index, java.time.LocalDateTime value) throws SQLException {
        if (value == null) {
            preparedStatement.setNull(index, java.sql.Types.TIMESTAMP);
        } else {
            preparedStatement.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    private java.time.LocalDateTime getTimestamp(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
