package tn.esprit.entities;

import java.time.LocalDateTime;

public class Certificate {

    private Integer id;
    private String certificateCode;
    private LocalDateTime issuedAt;
    private String pdfPath;
    private String studentName;
    private String courseTitle;
    private Integer enrollmentId;

    public Certificate() {
    }

    public Certificate(Integer id, String certificateCode, LocalDateTime issuedAt, String pdfPath, String studentName, String courseTitle, Integer enrollmentId) {
        this.id = id;
        this.certificateCode = certificateCode;
        this.issuedAt = issuedAt;
        this.pdfPath = pdfPath;
        this.studentName = studentName;
        this.courseTitle = courseTitle;
        this.enrollmentId = enrollmentId;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCertificateCode() {
        return certificateCode;
    }

    public void setCertificateCode(String certificateCode) {
        this.certificateCode = certificateCode;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public Integer getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Integer enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    @Override
    public String toString() {
        return "Certificate{" +
                "id=" + id + ", " + "certificateCode=" + certificateCode + ", " + "issuedAt=" + issuedAt + ", " + "pdfPath=" + pdfPath + ", " + "studentName=" + studentName + ", " + "courseTitle=" + courseTitle + ", " + "enrollmentId=" + enrollmentId +
                "}";
    }
}
