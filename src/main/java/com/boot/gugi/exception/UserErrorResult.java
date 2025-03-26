package com.boot.gugi.exception;

import com.boot.gugi.base.dto.ErrorReasonDTO;
import com.boot.gugi.base.status.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorResult implements BaseErrorCode {

    NOT_FOUND_USER(HttpStatus.NOT_FOUND, "404", "NOT_FOUND_USER", "존재하지 않는 유저입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String errorCode;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .isSuccess(false)
                .code(code)
                .errorCode(errorCode)
                .message(message)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .isSuccess(false)
                .httpStatus(httpStatus)
                .code(code)
                .errorCode(errorCode)
                .message(message)
                .build();
    }
}