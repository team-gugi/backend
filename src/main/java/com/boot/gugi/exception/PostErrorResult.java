package com.boot.gugi.exception;

import com.boot.gugi.base.dto.ErrorReasonDTO;
import com.boot.gugi.base.status.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostErrorResult implements BaseErrorCode {
    NOT_FOUND_DIARY(HttpStatus.NOT_FOUND, "404", "해당 직관일기 글이 존재하지 않습니다."),
    NOT_FOUND_MATE_POST(HttpStatus.NOT_FOUND, "404", "해당 직관메이트 글이 존재하지 않습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.FORBIDDEN, "403", "사용자가 해당 포스트에 접근권한이 없습니다."),
    FORBIDDEN_OWN_POST(HttpStatus.FORBIDDEN, "403", "본인의 게시글에는 신청할 수 없습니다."),
    ALREADY_APPLIED(HttpStatus.CONFLICT,"409", "이미 신청한 게시물입니다."),
    NOT_FOUND_REQUEST(HttpStatus.NOT_FOUND, "404", "해당 신청 기록이 존재하지 않습니다."),
    ALREADY_RESPONDED(HttpStatus.CONFLICT,"409", "이미 처리된 신청입니다."),
    MAX_MEMBERS_REACHED(HttpStatus.CONFLICT,"409","더 이상 요청을 수락할 수 없습니다. 최대 멤버 수에 도달했습니다."),
    RECRUITMENT_COMPLETED(HttpStatus.CONFLICT, "409", "이미 모집이 완료된 포스트입니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .isSuccess(false)
                .code(code)
                .message(message)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .isSuccess(false)
                .httpStatus(httpStatus)
                .code(code)
                .message(message)
                .build();
    }
}