package com.rapports.moteur.service.storage;

import com.rapports.moteur.config.AppProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalStorageService localStorageService;
    private AppProperties appProperties;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        appProperties.getStorage().setLocalPath(tempDir.toString());
        localStorageService = new LocalStorageService(appProperties);
    }

    @Test
    @DisplayName("Doit stocker et relire un fichier avec une clé multi-tenant")
    void shouldStoreAndLoadFileWithMultiTenantKey() {
        String key = "entreprises/ENT-001/reports/test-generation.pdf";
        byte[] content = "%PDF-1.4 Mock PDF Content".getBytes();

        String storedKey = localStorageService.storeFile(key, content, "application/pdf");
        assertThat(storedKey).isEqualTo("entreprises/ENT-001/reports/test-generation.pdf");

        assertThat(localStorageService.fileExists(key)).isTrue();

        byte[] loaded = localStorageService.loadFile(key);
        assertThat(loaded).isEqualTo(content);
    }

    @Test
    @DisplayName("Doit relire un fichier via son chemin absolu disque (rétrocompatibilité)")
    void shouldLoadFileViaLegacyAbsolutePath() throws IOException {
        Path legacyFile = tempDir.resolve("legacy-report.pdf");
        byte[] legacyContent = "Legacy PDF Content".getBytes();
        Files.write(legacyFile, legacyContent);

        byte[] loaded = localStorageService.loadFile(legacyFile.toAbsolutePath().toString());
        assertThat(loaded).isEqualTo(legacyContent);
    }

    @Test
    @DisplayName("Doit supprimer un fichier existant")
    void shouldDeleteExistingFile() {
        String key = "public/reports/to-delete.pdf";
        byte[] content = "PDF Content to delete".getBytes();

        localStorageService.storeFile(key, content, "application/pdf");
        assertThat(localStorageService.fileExists(key)).isTrue();

        localStorageService.deleteFile(key);
        assertThat(localStorageService.fileExists(key)).isFalse();
    }

    @Test
    @DisplayName("Doit lever une exception si le fichier est introuvable")
    void shouldThrowExceptionWhenFileNotFound() {
        assertThatThrownBy(() -> localStorageService.loadFile("inexistant/chemin/fichier.pdf"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fichier introuvable");
    }

    @Test
    @DisplayName("Doit normaliser les clés avec antislashs Windows")
    void shouldNormalizeWindowsBackslashes() {
        String key = "entreprises\\ENT-002\\reports\\subfolder\\report.pdf";
        byte[] content = "Content with backslashes".getBytes();

        String storedKey = localStorageService.storeFile(key, content, "application/pdf");
        assertThat(storedKey).isEqualTo("entreprises/ENT-002/reports/subfolder/report.pdf");

        byte[] loaded = localStorageService.loadFile(storedKey);
        assertThat(loaded).isEqualTo(content);
    }

    @Test
    @DisplayName("S3StorageService doit relire un fichier local en fallback s'il existe sur le disque")
    void s3ShouldFallbackToLocalDiskIfPresent() throws IOException {
        AppProperties s3Props = new AppProperties();
        s3Props.getStorage().setProvider("s3");
        s3Props.getStorage().setLocalPath(tempDir.toString());
        s3Props.getStorage().getS3().setAutoCreateBucket(false);

        S3StorageService s3Service = new S3StorageService(s3Props);
        s3Service.init();

        Path localFile = tempDir.resolve("legacy-s3-test.pdf");
        byte[] expected = "Fallback local content".getBytes();
        Files.write(localFile, expected);

        byte[] loaded = s3Service.loadFile(localFile.toAbsolutePath().toString());
        assertThat(loaded).isEqualTo(expected);
    }
}
