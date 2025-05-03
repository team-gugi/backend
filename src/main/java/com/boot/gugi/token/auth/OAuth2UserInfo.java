package com.boot.gugi.token.auth;

public interface OAuth2UserInfo {
    String getProvider();
    String getProviderId();
    String getEmail();

}