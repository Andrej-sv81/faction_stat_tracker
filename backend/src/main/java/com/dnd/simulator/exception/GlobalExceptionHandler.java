package com.dnd.simulator.exception;

import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
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
