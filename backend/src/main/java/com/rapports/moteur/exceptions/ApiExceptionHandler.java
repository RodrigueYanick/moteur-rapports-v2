package com.rapports.moteur.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.rapports.moteur.dto.ApiError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<?> handleNotFound(TemplateNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<?> handleValidation(ValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", ex.getMessage(), "errors", ex.getErrors()));
    }

    /**
     * Filet de sécurité : toute IllegalStateException/IllegalArgumentException non prévue
     * remonte désormais comme une erreur 400/422 lisible, plutôt qu'un 500 générique.
     */
    @ExceptionHandler({ IllegalStateException.class, IllegalArgumentException.class })
    public ResponseEntity<ProblemDetail> handleIllegalState(RuntimeException ex) {
        log.warn("Erreur métier non catégorisée : {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage() != null ? ex.getMessage() : "Erreur de traitement"
        );
        problem.setTitle("Erreur de traitement");
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    /**
     * Dernier filet : toute exception vraiment inattendue devient un ProblemDetail 500
     * propre plutôt qu'une page d'erreur Spring Boot brute avec stacktrace exposée.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Erreur interne non gérée", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur interne est survenue."
        );
        problem.setTitle("Erreur interne");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        ApiError error = ApiError.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message("Le corps de la requête est invalide ou n'est pas un objet JSON valide : " + ex.getMessage())
            .path("")
            .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ApiError> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(err -> err.getField() + " : " + err.getDefaultMessage())
            .collect(Collectors.toList());

        ApiError apiError = ApiError.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())   // 400
            .error("Bad Request")
            .message(String.join(" ; ", errors))
            .path("")   // vous pouvez récupérer le path si disponible
            .build();

        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ApiError error = ApiError.builder()
            .timestamp(LocalDateTime.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message("Paramètre invalide : " + ex.getName())
            .path("") // récupérez le path si possible
            .build();
        return ResponseEntity.badRequest().body(error);
    }
}