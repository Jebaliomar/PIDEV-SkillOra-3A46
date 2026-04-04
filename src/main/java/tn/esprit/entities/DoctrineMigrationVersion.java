package tn.esprit.entities;

import java.time.LocalDateTime;

public class DoctrineMigrationVersion {

    private String version;
    private LocalDateTime executedAt;
    private Integer executionTime;

    public DoctrineMigrationVersion() {
    }

    public DoctrineMigrationVersion(String version, LocalDateTime executedAt, Integer executionTime) {
        this.version = version;
        this.executedAt = executedAt;
        this.executionTime = executionTime;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public Integer getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(Integer executionTime) {
        this.executionTime = executionTime;
    }

    @Override
    public String toString() {
        return "DoctrineMigrationVersion{" +
                "version=" + version + ", " + "executedAt=" + executedAt + ", " + "executionTime=" + executionTime +
                "}";
    }
}
