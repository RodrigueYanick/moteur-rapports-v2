package com.rapports.moteur.dto.dtoPdf;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfSignatureOptions {
    private UUID certificateId;
    private String nomSignataire;
    private String raison;
    private String lieu;
    private String contact;
    @Builder.Default
    private boolean signatureVisible = false;
    @Builder.Default
    private Integer pageNumero = 1;
    private Float positionX;
    private Float positionY;
    private Float largeur;
    private Float hauteur;
}