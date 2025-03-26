package com.boot.gugi.base.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@Builder
public class ErrorReasonDTO {
    private HttpStatus httpStatus;
    private final boolean isSuccess;
    private final String code;
    private final String errorCode;
    private final String message;
}