package com.rapports.moteur.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RateLimitingFilterTest {

    private RateLimitingFilter filter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
        ReflectionTestUtils.setField(filter, "enabled", true);
        ReflectionTestUtils.setField(filter, "capacity", 2); // 2 tokens max pour le test
        ReflectionTestUtils.setField(filter, "refillTokens", 2);
        ReflectionTestUtils.setField(filter, "refillDurationSeconds", 60);
        filter.resetCache();

        filterChain = mock(FilterChain.class);
    }

    @Test
    @DisplayName("shouldNotFilter retourne true pour les endpoints ordinaires hors génération")
    void testShouldNotFilterRegularPaths() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/templates");
        assertThat(filter.shouldNotFilter(request)).isTrue();

        MockHttpServletRequest loginRequest = new MockHttpServletRequest("POST", "/api/auth/login");
        assertThat(filter.shouldNotFilter(loginRequest)).isTrue();
    }

    @Test
    @DisplayName("shouldNotFilter retourne false pour /api/generate et /api/batches")
    void testShouldFilterGeneratePaths() {
        MockHttpServletRequest generateReq = new MockHttpServletRequest("POST", "/api/generate/sync");
        assertThat(filter.shouldNotFilter(generateReq)).isFalse();

        MockHttpServletRequest batchReq = new MockHttpServletRequest("POST", "/api/batches");
        assertThat(filter.shouldNotFilter(batchReq)).isFalse();
    }

    @Test
    @DisplayName("Consommation sous le quota : la requête passe et ajoute les en-têtes X-RateLimit")
    void testUnderQuotaPasses() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/generate/sync");
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("2");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("1");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Dépassement du quota : retourne HTTP 429 Too Many Requests")
    void testExceededQuotaReturns429() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/generate/sync");
        request.addHeader("X-Entreprise-Code", "TENANT_TEST");

        // Consomme 2 tokens
        filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);
        filter.doFilterInternal(request, new MockHttpServletResponse(), filterChain);

        // 3e requête : quota dépassé
        MockHttpServletResponse response3 = new MockHttpServletResponse();
        filter.doFilterInternal(request, response3, filterChain);

        assertThat(response3.getStatus()).isEqualTo(429);
        assertThat(response3.getHeader("Retry-After")).isEqualTo("60");
        assertThat(response3.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response3.getContentAsString()).contains("Too Many Requests");
        assertThat(response3.getContentAsString()).contains("Quota de générations atteint");
    }
}

