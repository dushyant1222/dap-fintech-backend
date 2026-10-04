package com.dapfintech.exception.handler;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.dapfintech.common.response.ApiResponse;
import com.dapfintech.exception.custom.CustomException;


@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ApiResponse<Object>> handleCustomException(CustomException ex){

		ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	@ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
	public ResponseEntity<ApiResponse<Object>> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
		ApiResponse<Object> response = ApiResponse.builder()
				.success(false)
				.message("Endpoint not found: " + ex.getResourcePath())
				.data(null)
				.build();
		return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Object>> handleAllExceptions(Exception ex){
		System.err.println("=== UNHANDLED EXCEPTION CAUGHT IN GLOBAL EXCEPTION HANDLER ===");
		ex.printStackTrace();
		ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage() != null ? ex.getMessage() : "Internal server error: " + ex.getClass().getSimpleName())
                .data(null)
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
