package com.rapports.moteur.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    @Value("${app.rate-limiting.enabled:true}")
    private boolean enabled;

    @Value("${app.rate-limiting.capacity:60}")
    private int capacity;

    @Value("${app.rate-limiting.refill-tokens:60}")
    private int refillTokens;

    @Value("${app.rate-limiting.refill-duration-seconds:60}")
    private int refillDurationSeconds;

    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String path = request.getRequestURI();
        // Le rate limiting s'applique aux routes de génération lourde
        return !path.startsWith("/api/generate") && !path.startsWith("/api/batches");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientKey = resolveClientKey(request);
        Bucket bucket = bucketCache.computeIfAbsent(clientKey, k -> createNewBucket());

        if (bucket.tryConsume(1)) {
            response.setHeader("X-RateLimit-Limit", String.valueOf(capacity));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
        } else {
            log.warn("Rate limit dépassé pour la clé client [{}] sur {}", clientKey, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(refillDurationSeconds));
            response.setHeader("X-RateLimit-Limit", String.valueOf(capacity));
            response.setHeader("X-RateLimit-Remaining", "0");

            String json = """
                    {
                      "status": 429,
                      "error": "Too Many Requests",
                      "message": "Quota de générations atteint. Veuillez patienter avant de relancer une nouvelle demande.",
                      "retryAfterSeconds": %d
                    }
                    """.formatted(refillDurationSeconds);
            response.getWriter().write(json);
        }
    }

    private String resolveClientKey(HttpServletRequest request) {
        String tenant = request.getHeader("X-Entreprise-Code");
        if (tenant != null && !tenant.isBlank()) {
            return "tenant:" + tenant.trim();
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return "ip:" + xForwardedFor.split(",")[0].trim();
        }

        return "ip:" + request.getRemoteAddr();
    }

    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.greedy(refillTokens, Duration.ofSeconds(refillDurationSeconds))
        );
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Réinitialise le cache pour les tests unitaires.
     */
    public void resetCache() {
        bucketCache.clear();
    }
}

