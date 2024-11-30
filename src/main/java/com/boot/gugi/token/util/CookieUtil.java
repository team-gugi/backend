package com.boot.gugi.token.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CookieUtil {
    private final JwtUtil jwtUtil;
    private final String REFRESH_COOKIE_NAME = "refresh_token";
    private final String ACCESS_COOKIE_NAME = "access_token";

    public ResponseCookie createRefreshCookie(UUID userId, long expirationMillis) {
        String cookieValue = jwtUtil.generateRefreshToken(userId, expirationMillis);

        return ResponseCookie.from(REFRESH_COOKIE_NAME, cookieValue)
                .path("/")
                .sameSite("None")
                .httpOnly(true)
                .secure(true)
                .maxAge((int) expirationMillis / 1000)
                .build();
    }

    public ResponseCookie createAccessCookie(UUID userId, long expirationMillis) {
        String cookieValue = jwtUtil.generateAccessToken(userId, expirationMillis);

        return ResponseCookie.from(ACCESS_COOKIE_NAME, cookieValue)
                .path("/")
                .sameSite("None")
                .httpOnly(true)
                .secure(true)
                .maxAge((int) expirationMillis / 1000)
                .build();
    }

    public Cookie getRefreshCookie(HttpServletRequest request) {
        return getCookie(request, REFRESH_COOKIE_NAME);
    }

    public Cookie getAccessCookie(HttpServletRequest request) {
        return getCookie(request, ACCESS_COOKIE_NAME);
    }

    private Cookie getCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie;
                }
            }
        }
        return null;
    }

    public Cookie deleteCookie(String cookieName) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        return cookie;
    }
}
