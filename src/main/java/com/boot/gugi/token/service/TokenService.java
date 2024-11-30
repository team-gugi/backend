package com.boot.gugi.token.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

@Service
public interface TokenService {
    void reissueAccessToken(HttpServletRequest request, HttpServletResponse response);
}