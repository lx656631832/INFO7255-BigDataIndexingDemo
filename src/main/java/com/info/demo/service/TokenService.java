package com.info.demo.service;

import org.springframework.http.HttpHeaders;

public interface TokenService {
    public boolean authorizeToken(HttpHeaders headers);

    public  boolean verify(String idTokenString);
}
