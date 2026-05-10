package com.account.controller;

import com.account.util.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Global exception handler for all REST controllers.
 * Converts exceptions to unified Result.error() responses.
 *
 * @author Big Data Demo Team - Member 3
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle illegal argument exceptions (e.g., invalid parameters).
     * Returns 400 Bad Request.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("Illegal argument [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(400, "Invalid parameter: " + e.getMessage());
    }

    /**
     * Handle IO exceptions, especially HDFS-related errors.
     * Returns 500 Internal Server Error with HDFS context.
     */
    @ExceptionHandler(IOException.class)
    public Result handleIOException(IOException e, HttpServletRequest request) {
        log.error("HDFS IO error [{}]: {}", request.getRequestURI(), e.getMessage(), e);
        return Result.error(500, "HDFS operation failed: " + e.getMessage());
    }

    /**
     * Handle multipart/file upload exceptions (e.g., file too large).
     * Returns 400 Bad Request.
     */
    @ExceptionHandler(MultipartException.class)
    public Result handleMultipartException(MultipartException e, HttpServletRequest request) {
        log.warn("Multipart upload error [{}]: {}", request.getRequestURI(), e.getMessage());
        return Result.error(400, "File upload failed: " + e.getMessage() + ". File may be too large.");
    }

    /**
     * Handle all other uncaught exceptions.
     * Returns 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public Result handleGenericException(Exception e, HttpServletRequest request) {
        log.error("Unexpected error [{}]: {}", request.getRequestURI(), e.getMessage(), e);
        return Result.error(500, "Internal server error: " + e.getMessage());
    }
}
