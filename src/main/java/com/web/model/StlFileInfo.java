package com.web.model;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Embeddable
public class StlFileInfo {
    private LocalDateTime createdAt;
    private String fileSizeFormatted;

    public StlFileInfo(LocalDateTime createdAt, long fileSize) {
        this.createdAt = createdAt;
        this.fileSizeFormatted = getFormattedSize(fileSize);
    }

    private String getFormattedSize(long fileSize) {
        if (fileSize == 0) return "N/A";

        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double sizeOutput = fileSize;

        while (sizeOutput >= 1024 && unitIndex < units.length - 1) {
            sizeOutput /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", sizeOutput, units[unitIndex]);
    }
}
