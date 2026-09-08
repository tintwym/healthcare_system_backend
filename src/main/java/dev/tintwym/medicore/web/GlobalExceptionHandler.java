package dev.tintwym.medicore.web;

import dev.tintwym.medicore.security.ApiException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String, String>> handle(ApiException ex) {
    return ResponseEntity.status(ex.getStatus()).body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Map<String, String>> handleBadEnum(IllegalArgumentException ex) {
    return ResponseEntity.status(400).body(Map.of("error", ex.getMessage() == null ? "Bad request" : ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleOther(Exception ex) {
    return ResponseEntity.status(500).body(Map.of("error", "Internal server error"));
  }
}
