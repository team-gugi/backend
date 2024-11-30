package com.boot.gugi.token.auth;

import lombok.AllArgsConstructor;

import java.util.Map;

@AllArgsConstructor
public class KakaoUserInfo implements OAuth2UserInfo{

    private Map<String, Object> attributes;

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getProviderId() { return attributes.get("id").toString(); }

    @Override
    public String getName() {
        return (String) ((Map) attributes.get("kakao_account")).get("name");
    }

    @Override
    public String getEmail() {
        return (String) ((Map) attributes.get("kakao_account")).get("email");
    }

    public Integer getGender() {
        String genderValue = (String) ((Map) attributes.get("kakao_account")).get("gender");
        if ("MALE".equalsIgnoreCase(genderValue)) {
            return 1;
        } else if ("FEMALE".equalsIgnoreCase(genderValue)) {
            return 2;
        }
        return null;
    }

    @Override
    public Integer getBirthyear() {
        String birthyearValue = (String) ((Map) attributes.get("kakao_account")).get("birthyear");

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