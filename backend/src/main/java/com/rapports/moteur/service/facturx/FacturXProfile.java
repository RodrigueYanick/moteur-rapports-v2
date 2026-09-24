package com.rapports.moteur.service.facturx;

import lombok.Getter;

@Getter
public enum FacturXProfile {
    MINIMUM("urn:factur-x.eu:1p0:minimum", "MINIMUM"),
    BASIC("urn:factur-x.eu:1p0:basic", "BASIC"),
    EN16931("urn:cen.eu:en16931:2017#compliant#urn:factur-x.eu:1p0:en16931", "EN16931");

    private final String urn;
    private final String conformanceLevel;

    FacturXProfile(String urn, String conformanceLevel) {
        this.urn = urn;
        this.conformanceLevel = conformanceLevel;
    }

    public static FacturXProfile fromString(String val) {
        if (val == null || val.isBlank()) {
            return BASIC;
        }
        for (FacturXProfile p : values()) {
            if (p.name().equalsIgnoreCase(val) || p.conformanceLevel.equalsIgnoreCase(val)) {
                return p;
            }
        }
        return BASIC;
    }
}

