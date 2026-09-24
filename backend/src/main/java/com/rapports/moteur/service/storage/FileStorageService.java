package com.rapports.moteur.service.storage;

/**
 * Interface d'abstraction pour le stockage des fichiers générés (PDF, assets, etc.).
 * Permet de basculer de manière transparente entre le stockage local (disque)
 * et le stockage objet Cloud / S3 (MinIO, AWS S3, etc.).
 */
public interface FileStorageService {

    /**
     * Enregistre un fichier dans le stockage.
     *
     * @param key         la clé relative ou chemin de destination (ex: "entreprises/ENT-001/reports/abc.pdf")
     * @param content     le contenu binaire du fichier
     * @param contentType le type MIME du fichier (ex: "application/pdf")
     * @return la clé ou chemin de stockage persisté
     */
    String storeFile(String key, byte[] content, String contentType);

    /**
     * Charge le contenu binaire d'un fichier depuis le stockage.
     *
     * @param keyOrPath la clé relative ou chemin complet du fichier
     * @return le contenu binaire du fichier
     */
    byte[] loadFile(String keyOrPath);

    /**
     * Vérifie si un fichier existe dans le stockage.
     *
     * @param keyOrPath la clé relative ou chemin complet du fichier
     * @return true si le fichier existe, false sinon
     */
    boolean fileExists(String keyOrPath);

    /**
     * Supprime un fichier du stockage s'il existe.
     *
     * @param keyOrPath la clé relative ou chemin complet du fichier
     */
    void deleteFile(String keyOrPath);
}

