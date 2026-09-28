package com.rapports.moteur.security.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AesCryptoServiceTest {

    private AesCryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new AesCryptoService("TestMasterSecretKeyForUnitTestingPurposesOnly2026!");
    }

    @Test
    @DisplayName("Chiffrement et déchiffrement réussi d'un texte simple")
    void testEncryptDecryptSuccess() {
        String original = "MonMotDePasseSecret123!";
        String encrypted = cryptoService.encrypt(original);

        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = cryptoService.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    @DisplayName("Deux chiffrements du même texte produisent des ciphertexts différents (IV aléatoire)")
    void testUniqueIvPerEncryption() {
        String original = "TokenBearerAbc123";
        String enc1 = cryptoService.encrypt(original);
        String enc2 = cryptoService.encrypt(original);

        assertNotEquals(enc1, enc2, "Deux chiffrements doivent générer des résultats distincts grâce au vecteur d'initialisation (IV)");
        assertEquals(original, cryptoService.decrypt(enc1));
        assertEquals(original, cryptoService.decrypt(enc2));
    }

    @Test
    @DisplayName("Gestion des valeurs nulles et vides")
    void testNullAndEmptyValues() {
        assertNull(cryptoService.encrypt(null));
        assertNull(cryptoService.encrypt(""));
        assertNull(cryptoService.decrypt(null));
        assertNull(cryptoService.decrypt(""));
    }

    @Test
    @DisplayName("Échec du déchiffrement si le payload est corrompu ou altéré")
    void testTamperedPayloadFails() {
        String original = "DonnéesConfidentielles";
        String encrypted = cryptoService.encrypt(original);

        // Altération du dernier caractère
        char lastChar = encrypted.charAt(encrypted.length() - 1);
        char alteredChar = (lastChar == 'A') ? 'B' : 'A';
        String corrupted = encrypted.substring(0, encrypted.length() - 1) + alteredChar;

        assertThrows(RuntimeException.class, () -> cryptoService.decrypt(corrupted));
    }
}
