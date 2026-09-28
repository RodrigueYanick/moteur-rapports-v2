package com.rapports.moteur.security.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service de chiffrement symétrique AES-256-GCM pour la protection au repos
 * des identifiants (mots de passe BDD, tokens d'API REST).
 */
@Service
@Slf4j
public class AesCryptoService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final int IV_LENGTH = 12; // 12 bytes pour GCM

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesCryptoService(@Value("${app.encryption.secret:RapportsSecretKeyForAES256GCMEncryption2026!}") String masterSecret) {
        try {
            // Dérivation d'une clé de 256 bits (32 octets) à partir du secret maître via SHA-256
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha.digest(masterSecret.getBytes(StandardCharsets.UTF_8));
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'initialiser le service de chiffrement AES-256", e);
        }
    }

    /**
     * Chiffre un texte en clair avec AES-256-GCM et un IV aléatoire de 12 octets.
     *
     * @param plainText Texte à chiffrer
     * @return Chaîne chiffrée encodée en Base64 [IV + Ciphertext + Tag]
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Assemblage [IV (12 octets) + Ciphertext + Tag]
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Erreur lors du chiffrement des données : {}", e.getMessage());
            throw new RuntimeException("Échec du chiffrement du secret", e);
        }
    }

    /**
     * Déchiffre une chaîne encodée en Base64 produite par {@link #encrypt(String)}.
     *
     * @param cipherTextBase64 Chaîne chiffrée en Base64
     * @return Texte déchiffré en clair
     */
    public String decrypt(String cipherTextBase64) {
        if (cipherTextBase64 == null || cipherTextBase64.isEmpty()) {
            return null;
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(cipherTextBase64);
            if (decoded.length < IV_LENGTH) {
                throw new IllegalArgumentException("Payload chiffré invalide (longueur insuffisante)");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Erreur lors du déchiffrement des données : {}", e.getMessage());
            throw new RuntimeException("Échec du déchiffrement du secret", e);
        }
    }
}
