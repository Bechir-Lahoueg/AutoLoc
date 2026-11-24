package tn.esprit.autoloc.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tn.esprit.autoloc.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Traduit les exceptions métier en réponses HTTP : 404 (ressource absente) et 400 (règle de gestion). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErreurReponse(int statut, String message, LocalDateTime horodatage) {
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErreurReponse> handleNotFound(ResourceNotFoundException e) {
        return erreur(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErreurReponse> handleBadRequest(IllegalArgumentException e) {
        return erreur(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private static ResponseEntity<ErreurReponse> erreur(HttpStatus statut, String message) {
        ErreurReponse corps = new ErreurReponse(statut.value(), message,
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        return ResponseEntity.status(statut).body(corps);
    }
}
