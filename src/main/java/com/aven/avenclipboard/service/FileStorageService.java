package com.aven.avenclipboard.service;

import com.aven.avenclipboard.model.FileDownloadInfo;
import com.aven.avenclipboard.model.FileMetadataView;
import com.aven.avenclipboard.model.TransferFile;
import com.aven.avenclipboard.repository.TransferFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class FileStorageService {

    private static final long SINGLE_FILE_ID = 1L;
    private static final String STORED_FILE_NAME = "shared-file.dat";
    private static final String BACKUP_FILE_NAME = "shared-file.dat.bak";
    private static final String DEFAULT_CONTENT_TYPE = MediaType.APPLICATION_OCTET_STREAM_VALUE;

    private final TransferFileRepository transferFileRepository;
    private final long maxFileSizeBytes;
    private final Path storageDirectory;

    public FileStorageService(TransferFileRepository transferFileRepository,
                              @Value("${spring.datasource.url}") String datasourceUrl,
                              @Value("${clipboard.file.max-size:100MB}") String configuredMaxFileSize,
                              @Value("${clipboard.file.storage-dir:}") String configuredStorageDir) {
        this.transferFileRepository = transferFileRepository;
        this.maxFileSizeBytes = DataSize.parse(configuredMaxFileSize).toBytes();
        this.storageDirectory = resolveStorageDirectory(datasourceUrl, configuredStorageDir);
    }

    public FileMetadataView store(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "上传文件不能为空");
        }
        if (multipartFile.getSize() > maxFileSizeBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "文件超过大小限制");
        }

        try {
            Files.createDirectories(storageDirectory);
            Path tempFilePath = Files.createTempFile(storageDirectory, "shared-file-", ".upload");
            Path storedFilePath = getStoredFilePath();
            Path backupFilePath = storageDirectory.resolve(BACKUP_FILE_NAME);
            boolean backupCreated = false;

            try {
                DigestResult uploadDigest = writeMultipartToPath(multipartFile, tempFilePath);
                if (uploadDigest.sizeBytes == 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "上传文件不能为空");
                }

                if (Files.exists(storedFilePath)) {
                    Files.move(storedFilePath, backupFilePath, StandardCopyOption.REPLACE_EXISTING);
                    backupCreated = true;
                }

                moveFile(tempFilePath, storedFilePath);

                DigestResult persistedDigest = digestFile(storedFilePath);
                if (persistedDigest.sizeBytes != uploadDigest.sizeBytes
                        || !persistedDigest.sha256.equals(uploadDigest.sha256)) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "文件写入校验失败");
                }

                TransferFile transferFile = transferFileRepository.findById(SINGLE_FILE_ID)
                        .orElseGet(() -> new TransferFile(SINGLE_FILE_ID));
                transferFile.setOriginalName(resolveOriginalName(multipartFile.getOriginalFilename()));
                transferFile.setStoredName(STORED_FILE_NAME);
                transferFile.setContentType(resolveContentType(multipartFile.getContentType(), storedFilePath));
                transferFile.setSizeBytes(persistedDigest.sizeBytes);
                transferFile.setSha256(persistedDigest.sha256);
                transferFile.setUploadedAt(LocalDateTime.now());
                transferFileRepository.save(transferFile);

                if (backupCreated) {
                    Files.deleteIfExists(backupFilePath);
                }

                return FileMetadataView.from(transferFile);
            } catch (IOException | RuntimeException ex) {
                Files.deleteIfExists(tempFilePath);
                restoreBackupIfNeeded(storedFilePath, backupFilePath, backupCreated);
                throw ex;
            } finally {
                Files.deleteIfExists(tempFilePath);
            }
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "文件保存失败", ex);
        }
    }

    public Optional<FileMetadataView> getMetadata() {
        Optional<TransferFile> metadata = transferFileRepository.findById(SINGLE_FILE_ID);
        if (metadata.isEmpty()) {
            return Optional.empty();
        }

        Path storedFilePath = getStoredFilePath(metadata.get());
        if (!Files.exists(storedFilePath)) {
            transferFileRepository.deleteById(SINGLE_FILE_ID);
            return Optional.empty();
        }

        return Optional.of(FileMetadataView.from(metadata.get()));
    }

    public FileDownloadInfo prepareDownload() {
        TransferFile transferFile = transferFileRepository.findById(SINGLE_FILE_ID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "暂无可下载文件"));

        Path storedFilePath = getStoredFilePath(transferFile);
        if (!Files.exists(storedFilePath)) {
            transferFileRepository.deleteById(SINGLE_FILE_ID);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文件不存在，请重新上传");
        }

        try {
            DigestResult digestResult = digestFile(storedFilePath);
            if (digestResult.sizeBytes != transferFile.getSizeBytes()
                    || !digestResult.sha256.equals(transferFile.getSha256())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "文件校验失败，请重新上传");
            }

            return new FileDownloadInfo(
                    new UrlResource(storedFilePath.toUri()),
                    transferFile.getOriginalName(),
                    resolveContentType(transferFile.getContentType(), storedFilePath),
                    digestResult.sizeBytes
            );
        } catch (MalformedURLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "下载文件路径异常", ex);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "读取文件失败", ex);
        }
    }

    public boolean remove() {
        Optional<TransferFile> metadata = transferFileRepository.findById(SINGLE_FILE_ID);
        Path storedFilePath = metadata.map(this::getStoredFilePath).orElseGet(this::getStoredFilePath);
        Path backupFilePath = storageDirectory.resolve(BACKUP_FILE_NAME);

        try {
            boolean fileRemoved = Files.deleteIfExists(storedFilePath);
            boolean backupRemoved = Files.deleteIfExists(backupFilePath);

            if (metadata.isPresent()) {
                transferFileRepository.deleteById(SINGLE_FILE_ID);
            }

            return metadata.isPresent() || fileRemoved || backupRemoved;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "文件移除失败", ex);
        }
    }

    private DigestResult writeMultipartToPath(MultipartFile multipartFile, Path destinationPath) throws IOException {
        try (InputStream inputStream = multipartFile.getInputStream()) {
            MessageDigest messageDigest = createDigest();
            byte[] buffer = new byte[8192];
            long totalBytes = 0;
            int bytesRead;

            try (var outputStream = Files.newOutputStream(destinationPath)) {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    totalBytes += bytesRead;
                    if (totalBytes > maxFileSizeBytes) {
                        throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "文件超过大小限制");
                    }
                    outputStream.write(buffer, 0, bytesRead);
                    messageDigest.update(buffer, 0, bytesRead);
                }
            }

            return new DigestResult(totalBytes, HexFormat.of().formatHex(messageDigest.digest()));
        }
    }

    private DigestResult digestFile(Path filePath) throws IOException {
        MessageDigest messageDigest = createDigest();
        byte[] buffer = new byte[8192];
        long totalBytes = 0;

        try (InputStream inputStream = Files.newInputStream(filePath)) {
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytes += bytesRead;
                messageDigest.update(buffer, 0, bytesRead);
            }
        }

        return new DigestResult(totalBytes, HexFormat.of().formatHex(messageDigest.digest()));
    }

    private void restoreBackupIfNeeded(Path storedFilePath, Path backupFilePath, boolean backupCreated) throws IOException {
        Files.deleteIfExists(storedFilePath);
        if (backupCreated && Files.exists(backupFilePath)) {
            moveFile(backupFilePath, storedFilePath);
        }
    }

    private void moveFile(Path sourcePath, Path targetPath) throws IOException {
        try {
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path getStoredFilePath() {
        return storageDirectory.resolve(STORED_FILE_NAME);
    }

    private Path getStoredFilePath(TransferFile transferFile) {
        return storageDirectory.resolve(transferFile.getStoredName());
    }

    private String resolveOriginalName(String originalFilename) {
        String candidateName = StringUtils.cleanPath(
                StringUtils.hasText(originalFilename) ? originalFilename : "clipboard-file.dat"
        );
        String fileNameOnly = Paths.get(candidateName).getFileName().toString().trim();
        return fileNameOnly.isEmpty() ? "clipboard-file.dat" : fileNameOnly;
    }

    private String resolveContentType(String contentType, Path filePath) {
        if (StringUtils.hasText(contentType)) {
            return contentType;
        }
        try {
            String detectedContentType = Files.probeContentType(filePath);
            return StringUtils.hasText(detectedContentType) ? detectedContentType : DEFAULT_CONTENT_TYPE;
        } catch (IOException ex) {
            return DEFAULT_CONTENT_TYPE;
        }
    }

    private Path resolveStorageDirectory(String datasourceUrl, String configuredStorageDir) {
        if (StringUtils.hasText(configuredStorageDir)) {
            return Paths.get(configuredStorageDir).toAbsolutePath().normalize();
        }

        String sqlitePathText = datasourceUrl;
        if (sqlitePathText.startsWith("jdbc:sqlite:")) {
            sqlitePathText = sqlitePathText.substring("jdbc:sqlite:".length());
        }

        Path sqlitePath = Paths.get(sqlitePathText).toAbsolutePath().normalize();
        Path parentPath = sqlitePath.getParent();
        return parentPath == null ? Paths.get(".").toAbsolutePath().normalize() : parentPath;
    }

    private MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static class DigestResult {
        private final long sizeBytes;
        private final String sha256;

        private DigestResult(long sizeBytes, String sha256) {
            this.sizeBytes = sizeBytes;
            this.sha256 = sha256;
        }
    }
}
