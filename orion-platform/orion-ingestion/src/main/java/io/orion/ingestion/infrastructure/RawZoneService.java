package io.orion.ingestion.infrastructure;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class RawZoneService {
    private static final Logger logger = Logger.getLogger(RawZoneService.class.getName());
    private final Path rawZonePath;

    public RawZoneService() {
        this.rawZonePath = Paths.get("raw-zone");
        try {
            Files.createDirectories(rawZonePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create raw zone directory", e);
        }
    }

    public String storeFile(MultipartFile file) {
        String extension = getFileExtension(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString() + extension;
        Path targetLocation = rawZonePath.resolve(fileName);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Stored file in raw zone: " + targetLocation);
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file in raw zone", e);
        }
    }

    public File getFile(String fileName) {
        return rawZonePath.resolve(fileName).toFile();
    }

    public void deleteFile(String fileName) {
        try {
            Files.deleteIfExists(rawZonePath.resolve(fileName));
        } catch (IOException e) {
            logger.warning("Failed to delete file: " + fileName);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
