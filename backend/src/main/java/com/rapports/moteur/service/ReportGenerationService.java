package com.rapports.moteur.service;

import com.rapports.moteur.dto.dtoGeneration.GenerationResponse;
import com.rapports.moteur.entity.GenerationStatus;
import com.rapports.moteur.entity.ReportGeneration;
import com.rapports.moteur.repository.ReportGenerationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ReportGenerationService {
    private final ReportGenerationRepository repository;

    public ReportGenerationService(ReportGenerationRepository repository) {
        this.repository = repository;
    }

    // public List<GenerationResponse> findAll() {
    //     return repository.findAll().stream().map(this::toDto).toList();
    // }

    // public GenerationResponse generate(GenerateRequest request) {
    //     ReportGeneration generation = new ReportGeneration();
    //     // generation.setTemplateId(request.templateId());
    //     generation.setTitle(request.title());
    //     generation.setFormat(request.format());
    //     generation.setStatus(GenerationStatus.COMPLETED);
    //     return toDto(repository.save(generation));
    // }

    // public GenerationResponse findById(UUID id) {
    //     return toDto(repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Generation not found")));
    // }

    // private GenerationResponse toDto(ReportGeneration generation) {
    //     return new GenerationResponse(
    //             // generation.getId(),
    //             // generation.getTemplateId(),
    //             generation.getTitle(),
    //             generation.getFormat(),
    //             generation.getStatus(),
    //             generation.getCreatedAt(),
    //             generation.getUpdatedAt()
    //     );
    // }
}
