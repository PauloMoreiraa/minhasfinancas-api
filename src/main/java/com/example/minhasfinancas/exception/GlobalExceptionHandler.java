package com.example.minhasfinancas.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

// Anotação que indica que esta classe irá tratar exceções de forma global em todos os controladores
@ControllerAdvice
public class GlobalExceptionHandler {

  // Método que trata IllegalArgumentException
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException ex) {
    // Retorna uma resposta com status 400 (BAD REQUEST) e a mensagem de erro da exceção
    return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ex.getMessage());
  }

  // Método que trata MaxUploadSizeExceededException
  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<String> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
    // Mensagem personalizada para o erro de tamanho máximo de upload
    String message = "O tamanho máximo do arquivo foi excedido! O tamanho máximo permitido é de 10MB.";
    // Retorna uma resposta com status 413 (PAYLOAD TOO LARGE) e a mensagem de erro
    return ResponseEntity
            .status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(message);
  }

}
