package com.boot.gugi.token.auth;

import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
public class NaverUserInfo implements OAuth2UserInfo {

    private Map<String, Object> attributes;

    @Override
    public String getProvider() {
        return "naver";
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("id");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    public Integer getGender() {
        String genderValue = (String) attributes.get("gender");
        if ("M".equalsIgnoreCase(genderValue)) {
            return 1;
        } else if ("F".equalsIgnoreCase(genderValue)) {
            return 2;
        }
        return null;
    }

    @Override
    public Integer getBirthyear() {
        String birthyearValue = (String) attributes.get("birthyear");

        if (birthyearValue != null) {
            try {
                return Integer.valueOf(birthyearValue);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}