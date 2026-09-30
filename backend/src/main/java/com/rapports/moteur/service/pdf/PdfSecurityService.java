package com.rapports.moteur.service.pdf;

import com.rapports.moteur.dto.dtoPdf.PdfProtectionOptions;
import com.rapports.moteur.exceptions.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
public class PdfSecurityService {

    public byte[] protectPdf(byte[] pdfBytes, PdfProtectionOptions options) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new ValidationException(List.of("Le flux PDF fourni pour le chiffrement est vide"));
        }
        if (options == null || !options.isProtectionRequise()) {
            return pdfBytes;
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            AccessPermission accessPermission = new AccessPermission();
            accessPermission.setCanPrint(options.isAutoriserImpression());
            accessPermission.setCanExtractContent(options.isAutoriserCopie());
            accessPermission.setCanModify(options.isAutoriserModification());
            accessPermission.setCanModifyAnnotations(options.isAutoriserModification());
            accessPermission.setCanFillInForm(true);

            String ownerPassword = options.getMotDePasseProprietaire() != null ? options.getMotDePasseProprietaire() : "";
            String userPassword = options.getMotDePasseUtilisateur() != null ? options.getMotDePasseUtilisateur() : "";
            int keyLength = options.getTailleCleBits() == 128 ? 128 : 256;

            StandardProtectionPolicy protectionPolicy = new StandardProtectionPolicy(ownerPassword, userPassword, accessPermission);
            protectionPolicy.setEncryptionKeyLength(keyLength);
            protectionPolicy.setPreferAES(true);

            document.protect(protectionPolicy);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new ValidationException(List.of("Ã‰chec du chiffrement du document PDF : " + e.getMessage()));
        }
    }
}