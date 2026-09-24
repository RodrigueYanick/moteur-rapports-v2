package com.rapports.moteur.service.storage;

import com.rapports.moteur.config.AppProperties;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3StorageService implements FileStorageService {

    private final AppProperties appProperties;
    private MinioClient minioClient;
    private String bucketName;

    public S3StorageService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void init() {
        AppProperties.S3 s3Props = appProperties.getStorage() != null && appProperties.getStorage().getS3() != null
                ? appProperties.getStorage().getS3()
                : new AppProperties.S3();
        this.bucketName = s3Props.getBucket();

        log.info("Initialisation de S3StorageService avec endpoint: {}, bucket: {}, region: {}",
                s3Props.getEndpoint(), bucketName, s3Props.getRegion());

        this.minioClient = MinioClient.builder()
                .endpoint(s3Props.getEndpoint())
                .credentials(s3Props.getAccessKey(), s3Props.getSecretKey())
                .region(s3Props.getRegion())
                .build();

        if (s3Props.isAutoCreateBucket()) {
            ensureBucketExists();
        }
    }

    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!exists) {
                log.info("Création du bucket S3/MinIO '{}'...", bucketName);
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("Bucket S3/MinIO '{}' créé avec succès.", bucketName);
            } else {
                log.info("Bucket S3/MinIO '{}' existant vérifié.", bucketName);
            }
        } catch (Exception e) {
            log.warn("Impossible de vérifier ou créer le bucket S3 '{}' au démarrage. " +
                    "Vérifiez que MinIO/S3 est démarré et joignable : {}", bucketName, e.getMessage());
        }
    }

    @Override
    public String storeFile(String key, byte[] content, String contentType) {
        String cleanKey = normalizeKey(key);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(cleanKey)
                            .stream(bais, content.length, -1)
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .build()
            );
            log.debug("Fichier stocké dans S3/MinIO : bucket={}, key={}", bucketName, cleanKey);
            return cleanKey;
        } catch (Exception e) {
            log.error("Erreur lors de l'upload vers S3/MinIO : bucket={}, key={}", bucketName, cleanKey, e);
            throw new IllegalStateException("Erreur lors du stockage S3 du fichier : " + cleanKey, e);
        }
    }

    @Override
    public byte[] loadFile(String keyOrPath) {
        if (keyOrPath == null || keyOrPath.isBlank()) {
            throw new IllegalStateException("Clé de stockage S3 vide ou null");
        }
        String cleanKey = normalizeKey(keyOrPath);

        // 1. Tenter le chargement depuis S3/MinIO
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(cleanKey)
                        .build()
        )) {
            return stream.readAllBytes();
        } catch (Exception s3Ex) {
            log.debug("Objet non trouvé sur S3 ({}), vérification du fallback disque local...", cleanKey);

            // 2. Fallback disque local (pour rétrocompatibilité avec les rapports générés avant la bascule S3)
            try {
                Path direct = Paths.get(keyOrPath);
                if (Files.exists(direct) && !Files.isDirectory(direct)) {
                    return Files.readAllBytes(direct);
                }
                String localPath = appProperties.getStorage() != null ? appProperties.getStorage().getLocalPath() : appProperties.getStoragePath();
                Path localResolved = Paths.get(localPath).resolve(cleanKey);
                if (Files.exists(localResolved) && !Files.isDirectory(localResolved)) {
                    return Files.readAllBytes(localResolved);
                }
            } catch (Exception ignored) {
            }

            log.error("Fichier introuvable sur S3/MinIO ({}) ni en local", cleanKey, s3Ex);
            throw new IllegalStateException("Fichier introuvable dans le stockage S3 : " + cleanKey, s3Ex);
        }
    }

    @Override
    public boolean fileExists(String keyOrPath) {
        if (keyOrPath == null || keyOrPath.isBlank()) {
            return false;
        }
        String cleanKey = normalizeKey(keyOrPath);
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(cleanKey)
                            .build()
            );
            return true;
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                try {
                    Path direct = Paths.get(keyOrPath);
                    if (Files.exists(direct)) return true;
                    String localPath = appProperties.getStorage() != null ? appProperties.getStorage().getLocalPath() : appProperties.getStoragePath();
                    Path localResolved = Paths.get(localPath).resolve(cleanKey);
                    return Files.exists(localResolved);
                } catch (Exception ignored) {
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void deleteFile(String keyOrPath) {
        if (keyOrPath == null || keyOrPath.isBlank()) {
            return;
        }
        String cleanKey = normalizeKey(keyOrPath);
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(cleanKey)
                            .build()
            );
            log.debug("Fichier supprimé de S3/MinIO : bucket={}, key={}", bucketName, cleanKey);
        } catch (Exception e) {
            log.warn("Impossible de supprimer l'objet S3 : bucket={}, key={}", bucketName, cleanKey, e);
        }
    }

    private String normalizeKey(String key) {
        String clean = key.replace('\\', '/');
        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        return clean;
    }
}

