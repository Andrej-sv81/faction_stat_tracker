package com.dnd.simulator.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class GlobalExceptionHandlerTest {
  @Test
  void mapsOptimisticLockFailureToConflict() {
    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    ResponseEntity<?> response =
        handler.optimisticConflict(
            new ObjectOptimisticLockingFailureException("A resource changed concurrently", null));

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    assertEquals(
        Map.of("error", "This resource changed in another request. Reload and retry."),
        response.getBody());
  }
}
