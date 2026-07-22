package com.rapports.moteur.entity;

public enum TemplateStatus {
    DRAFT, // Le template est en cours de création et n'est pas encore publié
    PUBLISHED, // Le template est publié et peut être utilisé pour générer des rapports
    ACTIVE, // Le template est actif et peut être utilisé pour générer des rapports
    ARCHIVED // Le template est archivé et ne peut plus être utilisé
}
