package com.rapports.moteur.entity;

public enum GenerationStatus {
    PENDING, //le rapport est en attente de génération
    RUNNING, //le rapport est en cours de génération
    COMPLETED,// le rapport a été généré avec succès
    FAILED //le rapport n'a pas pu être généré
}
