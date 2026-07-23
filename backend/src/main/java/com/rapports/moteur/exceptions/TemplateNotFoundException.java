package com.rapports.moteur.exceptions;

import java.util.UUID;

public class TemplateNotFoundException extends RuntimeException {

    public TemplateNotFoundException(String message){ 
        super(message); 
    }
    public TemplateNotFoundException(UUID id) {
        super("Template non trouvé avec l'id : " + id);
    }

}
