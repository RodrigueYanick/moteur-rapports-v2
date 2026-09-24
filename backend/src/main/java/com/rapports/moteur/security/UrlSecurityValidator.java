package com.rapports.moteur.security;

import com.rapports.moteur.exceptions.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Component
public class UrlSecurityValidator {

    private static final Set<String> FORBIDDEN_HOSTS = Set.of(
            "localhost",
            "127.0.0.1",
            "::1",
            "169.254.169.254",
            "metadata.google.internal",
            "postgres",
            "minio",
            "backend",
            "frontend"
    );

    /**
     * Valide qu'une URL cible est sûre et ne tente pas une attaque SSRF
     * (Server-Side Request Forgery) vers le réseau interne, localhost ou les métadonnées cloud.
     *
     * @param urlString URL à valider
     * @throws ValidationException si l'URL est invalide ou cible une adresse interdite
     */
    public void validateSafeUrl(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            throw new ValidationException("L'URL ne peut pas être vide");
        }

        URI uri;
        try {
            uri = URI.create(urlString.trim());
        } catch (Exception e) {
            throw new ValidationException("Format d'URL invalide : " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new ValidationException("Protocole non autorisé : seuls HTTP et HTTPS sont permis");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new ValidationException("L'hôte de l'URL est invalide");
        }

        String lowerHost = host.toLowerCase(Locale.ROOT);
        if (FORBIDDEN_HOSTS.contains(lowerHost)
                || lowerHost.endsWith(".local")
                || lowerHost.endsWith(".internal")) {
            log.warn("Tentative SSRF bloquée vers l'hôte interdit : {}", host);
            throw new ValidationException("Accès aux hôtes locaux ou internes interdit (SSRF)");
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (addr.isLoopbackAddress()
                        || addr.isSiteLocalAddress() // RFC 1918 : 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
                        || addr.isLinkLocalAddress() // 169.254.0.0/16, fe80::/10
                        || addr.isAnyLocalAddress()
                        || addr.getHostAddress().equals("169.254.169.254")
                        || addr.getHostAddress().startsWith("127.")) {
                    log.warn("Tentative SSRF bloquée vers l'adresse IP privée/réservée : {} (hôte: {})", addr.getHostAddress(), host);
                    throw new ValidationException("Accès à une adresse IP privée, locale ou réservée interdit (SSRF)");
                }
            }
        } catch (UnknownHostException e) {
            log.warn("Impossible de résoudre l'hôte DNS pour l'URL : {}", host);
            throw new ValidationException("Impossible de résoudre l'hôte DNS : " + host);
        }
    }
}

