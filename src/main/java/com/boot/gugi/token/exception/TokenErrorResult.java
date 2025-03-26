package com.boot.gugi.token.exception;

import com.boot.gugi.base.dto.ErrorReasonDTO;
import com.boot.gugi.base.status.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TokenErrorResult implements BaseErrorCode {

    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "401", "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    INVALID_REGISTER_TOKEN(HttpStatus.UNAUTHORIZED, "401", "INVALID_REGISTER_TOKEN", "유효하지 않은 레지스터 토큰입니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "401", "INVALID_ACCESS_TOKEN", "유효하지 않은 액세스 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "401", "INVALID_REFRESH_TOKEN", "유효하지 않은 리프레쉬 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "REFRESH_TOKEN_NOT_FOUND", "해당 유저 ID의 리프레쉬 토큰이 없습니다."),
    ACCESS_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "404", "ACCESS_TOKEN_NOT_FOUND", "해당 유저 ID의 액세스 토큰이 없습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "400", "INVALID_REQUEST", "잘못된 토큰 요청입니다."),
    BLACKLISTED_TOKEN(HttpStatus.FORBIDDEN, "403", "BLACKLISTED_TOKEN", "블랙리스트에 등록된 토큰입니다. 다시 로그인해 주세요."),
    EXPIRED_AUTH_TOKEN(HttpStatus.UNAUTHORIZED, "401", "EXPIRED_AUTH_TOKEN", "소셜 인증 토큰이 만료되었습니다. 다시 로그인해 주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");

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
                .httpStatus(httpStatus)
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