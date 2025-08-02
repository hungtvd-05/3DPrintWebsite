package com.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class TempFileCleanupJob {
    @Value("${file.upload.img-path:src/main/resources/static/tmp}")
    private String imgPath;

    private static final long CLEANUP_DELAY = 6 * 3600000;

    @Scheduled(fixedRate = 3600000) // Chạy mỗi giờ
    public void cleanupTempFiles() {
        cleanupDirectoryImg();
        cleanupDirectoryStl();
    }

    private void cleanupDirectoryImg() {
        String tempDir = imgPath + File.separator + "img";

        try {
            if (!Files.exists(Paths.get(tempDir))) {
                return;
            }

            try (var stream = Files.list(Paths.get(tempDir))) {
                stream.filter(Files::isRegularFile)
                        .filter(this::isFileOldEnoughByNanoTime)
                        .forEach(this::safeDeleteImg);
            }

        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    private void cleanupDirectoryStl() {
        String tempDir = imgPath + File.separator + "stl";

        try {
            if (!Files.exists(Paths.get(tempDir))) {
                return;
            }

            try (var stream = Files.list(Paths.get(tempDir))) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".stl"))
                        .filter(this::isFileOldEnoughByNanoTime)
                        .forEach(this::safeDeleteStl);
            }

        } catch (IOException e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }

    private boolean isFileOldEnoughByNanoTime(Path path) {
        try {
            String fileName = path.getFileName().toString();

            // Extract nanoTime từ tên file
            long uploadNanoTime = extractNanoTimeFromFileName(fileName);

            if (uploadNanoTime == -1) {
                // Fallback về lastModified nếu không parse được
                long fileAge = System.currentTimeMillis() - Files.getLastModifiedTime(path).toMillis();
                return fileAge > CLEANUP_DELAY;
            }

            // Convert nanoTime thành milliseconds và tính tuổi file
            long uploadTimeMillis = uploadNanoTime / 1_000_000; // nano -> milli
            long currentTimeMillis = System.currentTimeMillis();
            long fileAge = currentTimeMillis - uploadTimeMillis;

            return fileAge > CLEANUP_DELAY;

        } catch (Exception e) {
            return false;
        }
    }

    private long extractNanoTimeFromFileName(String fileName) {
        try {
            // Từ tên file format: "timestamp_originalname.ext"
            // Hoặc từ existing format trong code: "img_uuid_nanotime.ext"

            if (fileName.contains("_")) {
                String[] parts = fileName.split("_");

                // Case 1: Format "timestamp_originalname.ext"
                if (parts.length >= 2) {
                    try {
                        return Long.parseLong(parts[0]);
                    } catch (NumberFormatException e) {
                        // Case 2: Format "img_uuid_nanotime.ext"
                        if (parts.length >= 3) {
                            String lastPart = parts[parts.length - 1];
                            // Remove extension
                            String nanoTimeStr = lastPart.substring(0, lastPart.lastIndexOf("."));
                            return Long.parseLong(nanoTimeStr);
                        }
                    }
                }
            }

            return -1; // Không parse được
        } catch (Exception e) {
            return -1;
        }
    }

    private void safeDeleteImg(Path path) {
        try {
            Files.deleteIfExists(path);
            System.out.println("Deleted temp file: " + path.getFileName());
        } catch (IOException e) {
            System.err.println("Failed to delete temp file: " + path.getFileName() + " - " + e.getMessage());
        }
    }

    private void safeDeleteStl(Path stlPath) {

        try {

            String stlFileName = stlPath.getFileName().toString();

            String previewFileName = stlFileName + "_preview.png";
            Path previewPath = stlPath.getParent().resolve(previewFileName);
            if (Files.exists(stlPath)) {
                System.out.println("Attempting to delete STL file: " + stlFileName);
                Files.deleteIfExists(stlPath);
                Files.deleteIfExists(previewPath);
            }

        } catch (IOException e) {
            System.err.println("Failed to delete temp file: " + stlPath.getFileName() + " - " + e.getMessage());
        }
    }
}
