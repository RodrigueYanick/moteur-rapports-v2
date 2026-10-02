package com.rapports.moteur.controller;

import com.rapports.moteur.dto.ApiResponse;
import com.rapports.moteur.dto.dtoAi.*;
import com.rapports.moteur.service.ai.AiClientManager;
import com.rapports.moteur.service.ai.AiMockDataService;
import com.rapports.moteur.service.ai.AiTemplateBuilderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "Assistant IA", description = "Endpoints d'assistance IA pour la génération de modèles et de données de test")
public class AiAssistantController {

    private final AiTemplateBuilderService aiTemplateBuilderService;
    private final AiMockDataService aiMockDataService;
    private final AiClientManager aiClientManager;

    @PostMapping("/generate-template")
    @Operation(summary = "Générer un modèle de document par prompt IA",
               description = "Conçoit un modèle de document complet (pages, blocs, mise en page, variables) à partir d'une description textuelle en langage naturel")
    public ResponseEntity<ApiResponse<AiTemplateGenerationResponse>> generateTemplate(
            @Valid @RequestBody AiTemplatePromptRequest request) {
        log.info("Requête de génération de template par IA reçue : {}", request.getPrompt());
        AiTemplateGenerationResponse response = aiTemplateBuilderService.generateTemplate(request);
        return ResponseEntity.ok(ApiResponse.success("Modèle généré avec succès par l'IA", response));
    }

    @PostMapping("/mock-data")
    @Operation(summary = "Générer un jeu de données de test intelligent",
               description = "Génère des données de test réalistes et mathématiquement cohérentes pour un modèle ou un schéma de variables")
    public ResponseEntity<ApiResponse<AiMockDataResponse>> generateMockData(
            @RequestBody AiMockDataRequest request) {
        log.info("Requête de génération de mock data IA reçue");
        AiMockDataResponse response = aiMockDataService.generateMockData(request);
        return ResponseEntity.ok(ApiResponse.success("Données de test générées avec succès", response));
    }

    @GetMapping("/status")
    @Operation(summary = "Consulter le statut du service IA",
               description = "Retourne l'état d'activation et de disponibilité du service IA")
    public ResponseEntity<ApiResponse<AiStatusResponse>> getStatus() {
        AiStatusResponse status = AiStatusResponse.builder()
                .enabled(aiClientManager.isAiEnabled())
                .available(aiClientManager.isGeminiAvailable())
                .provider(aiClientManager.getCurrentProvider())
                .model(aiClientManager.getModelName())
                .build();
        return ResponseEntity.ok(ApiResponse.success("Statut IA récupéré", status));
    }
}
