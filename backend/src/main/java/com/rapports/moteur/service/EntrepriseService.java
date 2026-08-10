package com.rapports.moteur.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EntrepriseService {

    private final HttpServletRequest request;

    public String getCurrentCodeEntreprise() {
        String code = request.getHeader("X-Entreprise-Code");
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le header X-Entreprise-Code est obligatoire");
        }
        return code;
    }
}