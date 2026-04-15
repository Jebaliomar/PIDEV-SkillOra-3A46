package tn.esprit.services;

import tn.esprit.entities.Certificate;
import tn.esprit.tools.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class CertificateService {

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
