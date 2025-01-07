package com.boot.gugi.exception;

import com.boot.gugi.base.dto.ReasonDTO;
import com.boot.gugi.base.status.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserSuccessResult implements BaseCode {
    CREATED_DEFINE_USER(HttpStatus.CREATED, "201", "유저 등록에 성공했습니다."),
    GET_DEFINE_USER(HttpStatus.OK, "200", "유저 조회에 성공했습니다."),
    MODIFY_DEFINE_USER(HttpStatus.OK, "200", "유저정보 수정에 성공했습니다."),
    UPDATE_MATE_REQUEST(HttpStatus.OK, "200", "신청 상태를 성공적으로 업데이트했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ReasonDTO getReason() {
        return ReasonDTO.builder()
                .isSuccess(true)
                .code(code)
                .message(message)
                .build();
    }

    @Override
    public ReasonDTO getReasonHttpStatus() {
        return ReasonDTO.builder()
                .isSuccess(true)
                .httpStatus(httpStatus)
                .code(code)
                .message(message)
                .build();
    }
}