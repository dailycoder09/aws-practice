package com.microservices.product.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler
 * 
 * Centralized exception handling for the entire application
 * Uses RFC 7807 Problem Details for HTTP APIs
 * 
 * @author Microservices Team
 * @version 1.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle Product Not Found Exception
     * 
     * @param ex ProductNotFoundException
     * @param request WebRequest
     * @return ProblemDetail with 404 status
     */
    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleProductNotFoundException(
            ProductNotFoundException ex,
            WebRequest request) {
        
        log.error("Product not found: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            ex.getMessage()
        );
        
        problemDetail.setTitle("Product Not Found");
        problemDetail.setType(URI.create("https://api.example.com/errors/product-not-found"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));
        
        return problemDetail;
    }

    /**
     * Handle Duplicate SKU Exception
     * 
     * @param ex DuplicateSkuException
     * @param request WebRequest
     * @return ProblemDetail with 409 status
     */
    @ExceptionHandler(DuplicateSkuException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleDuplicateSkuException(
            DuplicateSkuException ex,
            WebRequest request) {
        
        log.error("Duplicate SKU: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.CONFLICT,
            ex.getMessage()
        );
        
        problemDetail.setTitle("Duplicate SKU Code");
        problemDetail.setType(URI.create("https://api.example.com/errors/duplicate-sku"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));
        
        return problemDetail;
    }

    /**
     * Handle Validation Exceptions
     * 
     * @param ex MethodArgumentNotValidException
     * @param request WebRequest
     * @return ProblemDetail with validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        log.error("Validation failed: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed for one or more fields"
        );
        
        problemDetail.setTitle("Validation Error");
        problemDetail.setType(URI.create("https://api.example.com/errors/validation"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));
        problemDetail.setProperty("errors", errors);
        
        return problemDetail;
    }

    /**
     * Handle Illegal Argument Exception
     * 
     * @param ex IllegalArgumentException
     * @param request WebRequest
     * @return ProblemDetail with 400 status
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {
        
        log.error("Illegal argument: {}", ex.getMessage());
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            ex.getMessage()
        );
        
        problemDetail.setTitle("Invalid Request");
        problemDetail.setType(URI.create("https://api.example.com/errors/invalid-request"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));
        
        return problemDetail;
    }

    /**
     * Handle requests to unmapped URLs
     * Spring's default resource-handler fallback raises this for any path that
     * matches no controller mapping and no static resource - without this handler
     * it falls through to the generic 500 handler below, which is wrong: a
     * nonexistent route is a 404, not a server error.
     *
     * @param ex NoResourceFoundException
     * @param request WebRequest
     * @return ProblemDetail with 404 status
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNoResourceFoundException(
            NoResourceFoundException ex,
            WebRequest request) {

        log.debug("No resource found: {}", ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND,
            "The requested resource was not found"
        );

        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(URI.create("https://api.example.com/errors/resource-not-found"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));

        return problemDetail;
    }

    /**
     * Handle all other exceptions
     * 
     * @param ex Exception
     * @param request WebRequest
     * @return ProblemDetail with 500 status
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGlobalException(
            Exception ex,
            WebRequest request) {
        
        log.error("Unexpected error occurred: ", ex);
        
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please try again later."
        );
        
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://api.example.com/errors/internal-server-error"));
        problemDetail.setProperty("timestamp", LocalDateTime.now());
        problemDetail.setProperty("path", request.getDescription(false).replace("uri=", ""));
        
        return problemDetail;
    }
}
