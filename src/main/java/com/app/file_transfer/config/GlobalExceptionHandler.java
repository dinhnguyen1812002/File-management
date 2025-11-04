package com.app.file_transfer.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle AsyncRequestNotUsableException - occurs when client disconnects during streaming
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public ResponseEntity<Map<String, String>> handleAsyncRequestNotUsable(
            AsyncRequestNotUsableException ex, HttpServletRequest request) {
        
        // Log the incident for monitoring but don't treat as error
        String requestUri = request.getRequestURI();
        System.out.println("Client disconnected during async request: " + requestUri);
        
        // Return empty response since client is gone anyway
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * Handle AsyncRequestTimeoutException - occurs when request times out
     */
    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public ResponseEntity<Map<String, String>> handleAsyncRequestTimeout(
            AsyncRequestTimeoutException ex, HttpServletRequest request) {
        
        String requestUri = request.getRequestURI();
        System.out.println("Async request timeout: " + requestUri);
        
        Map<String, String> error = new HashMap<>();
        error.put("error", "Request timeout");
        error.put("message", "The request took too long to process");
        
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(error);
    }

    /**
     * Handle IOException during streaming - common when client disconnects
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> handleIOException(
            IOException ex, HttpServletRequest request) {
        
        String errorMessage = ex.getMessage();
        String requestUri = request.getRequestURI();
        
        // Check if it's a client disconnect during streaming
        if (errorMessage != null && (
            errorMessage.contains("Connection reset by peer") ||
            errorMessage.contains("Broken pipe") ||
            errorMessage.contains("connection was aborted") ||
            errorMessage.contains("ClientAbortException") ||
            errorMessage.contains("An established connection was aborted"))) {
            
            System.out.println("Client disconnected during streaming: " + requestUri);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        
        // For other IO errors, log and return appropriate error
        System.err.println("IO Error on " + requestUri + ": " + errorMessage);
        
        Map<String, String> error = new HashMap<>();
        error.put("error", "IO Error");
        error.put("message", "An error occurred while processing the request");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handle general exceptions during file operations
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {
        
        String errorMessage = ex.getMessage();
        String requestUri = request.getRequestURI();
        
        // Check if it's related to streaming/file operations
        if (requestUri.contains("/stream/") || requestUri.contains("/preview/") || requestUri.contains("/download/")) {
            
            // Check for client disconnect patterns
            if (errorMessage != null && (
                errorMessage.contains("Connection reset") ||
                errorMessage.contains("connection was aborted") ||
                errorMessage.contains("Broken pipe"))) {
                
                System.out.println("Client disconnected during file operation: " + requestUri);
                return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
            }
        }
        
        // Log other runtime exceptions
        System.err.println("Runtime error on " + requestUri + ": " + errorMessage);
        
        Map<String, String> error = new HashMap<>();
        error.put("error", "Runtime Error");
        error.put("message", "An unexpected error occurred");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
