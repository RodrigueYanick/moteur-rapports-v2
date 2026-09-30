package com.rapports.moteur.dto.dtoPdf;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PdfProtectionOptions {
    private String motDePasseUtilisateur;
    private String motDePasseProprietaire;
    @Builder.Default
    private boolean autoriserImpression = true;
    @Builder.Default
    private boolean autoriserCopie = false;
    @Builder.Default
    private boolean autoriserModification = false;
    @Builder.Default
    private int tailleCleBits = 256;

    public boolean isProtectionRequise() {
        return (motDePasseUtilisateur != null && !motDePasseUtilisateur.trim().isEmpty())
                || (motDePasseProprietaire != null && !motDePasseProprietaire.trim().isEmpty());
    }
}