package com.rapports.moteur.service.pdf;

import com.rapports.moteur.dto.dtoPdf.PdfProtectionOptions;
import com.rapports.moteur.exceptions.ValidationException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfSecurityServiceTest {

    private PdfSecurityService securityService;
    private byte[] samplePdfBytes;

    @BeforeEach
    void setUp() throws IOException {
        securityService = new PdfSecurityService();

        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            samplePdfBytes = baos.toByteArray();
        }
    }

    @Test
    @DisplayName("Protection rÃ©ussie avec mot de passe utilisateur (AES-256)")
    void shouldProtectPdfWithUserPassword() throws IOException {
        PdfProtectionOptions options = PdfProtectionOptions.builder()
                .motDePasseUtilisateur("SecretUser2026!")
                .motDePasseProprietaire("AdminMaster2026!")
                .autoriserImpression(true)
                .autoriserCopie(false)
                .autoriserModification(false)
                .tailleCleBits(256)
                .build();

        byte[] protectedPdf = securityService.protectPdf(samplePdfBytes, options);

        assertThat(protectedPdf).isNotNull();
        assertThat(protectedPdf.length).isGreaterThan(0);

        assertThatThrownBy(() -> Loader.loadPDF(protectedPdf))
                .isInstanceOf(InvalidPasswordException.class);

        try (PDDocument opened = Loader.loadPDF(protectedPdf, "SecretUser2026!")) {
            assertThat(opened.isEncrypted()).isTrue();
            assertThat(opened.getCurrentAccessPermission().canPrint()).isTrue();
            assertThat(opened.getCurrentAccessPermission().canExtractContent()).isFalse();
            assertThat(opened.getCurrentAccessPermission().canModify()).isFalse();
        }
    }

    @Test
    @DisplayName("Retourne le PDF intact si aucune protection n'est demandÃ©e")
    void shouldReturnOriginalPdfWhenNoProtectionRequested() {
        PdfProtectionOptions options = PdfProtectionOptions.builder()
                .motDePasseUtilisateur("")
                .motDePasseProprietaire(null)
                .build();

        byte[] result = securityService.protectPdf(samplePdfBytes, options);
        assertThat(result).isEqualTo(samplePdfBytes);
    }

    @Test
    @DisplayName("Rejette un flux PDF vide")
    void shouldThrowValidationExceptionWhenPdfBytesEmpty() {
        PdfProtectionOptions options = PdfProtectionOptions.builder()
                .motDePasseUtilisateur("pwd")
                .build();

        assertThatThrownBy(() -> securityService.protectPdf(new byte[0], options))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("vide");
    }
}