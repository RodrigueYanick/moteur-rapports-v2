package com.rapports.moteur.service;

import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EntrepriseService {

    private final HttpServletRequest request;

    public String getCurrentCodeEntreprise() {
        String code = request.getHeader("X-Entreprise-Code");
        if (code == null || code.isBlank()) {
            return null;   // plus d'exception
        }
        return code;
    }
}