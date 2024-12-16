package com.boot.gugi.token.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public interface TokenService {
    String reissueAccessToken(HttpServletRequest request, HttpServletResponse response);
    UUID getUserIdFromAccessToken(HttpServletRequest request, HttpServletResponse response);
}