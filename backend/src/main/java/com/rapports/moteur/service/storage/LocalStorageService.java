package com.rapports.moteur.service.storage;

import com.rapports.moteur.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements FileStorageService {

    private final Path rootPath;

    public LocalStorageService(AppProperties appProperties) {
        String configuredPath = appProperties.getStorage() != null && appProperties.getStorage().getLocalPath() != null
                ? appProperties.getStorage().getLocalPath()
                : appProperties.getStoragePath();
        this.rootPath = Paths.get(configuredPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootPath);
            log.info("LocalStorageService initialisé sur le répertoire : {}", this.rootPath);
        } catch (IOException e) {
            log.error("Impossible de créer le répertoire racine de stockage local : {}", this.rootPath, e);
        }
    }

    @Override
    public String storeFile(String key, byte[] content, String contentType) {
        try {
            Path targetFile = resolveSafe(key);
            if (targetFile.getParent() != null) {
                Files.createDirectories(targetFile.getParent());
            }
            Files.write(targetFile, content);
            log.debug("Fichier enregistré en local : {}", targetFile);
            return normalizeKey(key);
        } catch (IOException e) {
            log.error("Erreur lors de l'enregistrement local du fichier : {}", key, e);
            throw new IllegalStateException("Impossible d'enregistrer le fichier en local : " + key, e);
        }
    }

    @Override
    public byte[] loadFile(String keyOrPath) {
        if (keyOrPath == null || keyOrPath.isBlank()) {
            throw new IllegalStateException("Chemin de fichier null ou vide");
        }

        try {
            // 1. Essai direct (si c'est déjà un chemin absolu ou relatif direct existant)
            Path direct = Paths.get(keyOrPath);
            if (Files.exists(direct) && !Files.isDirectory(direct)) {
                return Files.readAllBytes(direct);
            }

            // 2. Essai résolu par rapport au rootPath
            Path resolved = resolveSafe(keyOrPath);
            if (Files.exists(resolved) && !Files.isDirectory(resolved)) {
                return Files.readAllBytes(resolved);
            }

            throw new IllegalStateException("Fichier introuvable sur le disque local : " + keyOrPath);
        } catch (IOException e) {
            log.error("Erreur de lecture du fichier : {}", keyOrPath, e);
            throw new IllegalStateException("Erreur lors de la lecture du fichier : " + keyOrPath, e);
        }
    }

    @Override
    public boolean fileExists(String keyOrPath) {
        if (keyOrPath == null || keyOrPath.isBlank()) {
            return false;
        }
        try {
            Path direct = Paths.get(keyOrPath);
            if (Files.exists(direct) && !Files.isDirectory(direct)) {
                return true;
            }
            Path resolved = resolveSafe(keyOrPath);
            return Files.exists(resolved) && !Files.isDirectory(resolved);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void deleteFile(String keyOrPath) {
        if (keyOrPath == null || keyOrPath.isBlank()) {
            return;
        }
        try {
            Path direct = Paths.get(keyOrPath);
            if (Files.exists(direct) && !Files.isDirectory(direct)) {
                Files.delete(direct);
                return;
            }
            Path resolved = resolveSafe(keyOrPath);
            if (Files.exists(resolved) && !Files.isDirectory(resolved)) {
                Files.delete(resolved);
            }
        } catch (IOException e) {
            log.warn("Impossible de supprimer le fichier local : {}", keyOrPath, e);
        }
    }

    private Path resolveSafe(String key) {
        String cleanKey = key.replace('\\', '/');
        while (cleanKey.startsWith("/")) {
            cleanKey = cleanKey.substring(1);
        }
        return rootPath.resolve(cleanKey).normalize();
    }

    private String normalizeKey(String key) {
        String cleanKey = key.replace('\\', '/');
        while (cleanKey.startsWith("/")) {
            cleanKey = cleanKey.substring(1);
        }
        return cleanKey;
    }
}

