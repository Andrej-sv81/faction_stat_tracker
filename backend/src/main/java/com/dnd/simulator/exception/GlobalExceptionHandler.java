package com.dnd.simulator.exception;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<?> unauthorized(AuthenticationException exception) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(Map.of("error", exception.getMessage()));
  }

  @ExceptionHandler(DuplicateResourceException.class)
  ResponseEntity<?> conflict(DuplicateResourceException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(Map.of("error", exception.getMessage()));
  }

  @ExceptionHandler(TooManyRequestsException.class)
  ResponseEntity<?> tooManyRequests(TooManyRequestsException exception) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header(
            "Retry-After",
            String.valueOf(exception.getRetryAfterSeconds()))
        .body(Map.of("error", exception.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<?> bad(IllegalArgumentException exception) {
    return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
  }

  @ExceptionHandler(NoSuchElementException.class)
  ResponseEntity<?> missing(NoSuchElementException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", exception.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> validation(MethodArgumentNotValidException exception) {
    return ResponseEntity.badRequest()
        .body(
            Map.of(
                "error",
                "Validation failed",
                "fields",
                exception.getBindingResult().getFieldErrors().stream()
                    .collect(
                        java.util.stream.Collectors.toMap(
                            fieldError -> fieldError.getField(),
                            fieldError -> fieldError.getDefaultMessage(),
                            (firstMessage, secondMessage) -> firstMessage))));
  }
}
