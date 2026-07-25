package com.rapports.moteur.dto;

import java.time.LocalDateTime;

public record ApiResponse<T>(String message, T data, String timestamp) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(message, data, LocalDateTime.now().toString());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(message, null, LocalDateTime.now().toString());
    }
}
