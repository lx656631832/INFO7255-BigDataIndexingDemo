package com.info.demo.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

@Scope("prototype")
@Service
public class TokenServiceImp implements TokenService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static String CLIENT_ID = "365972582441-cm9i0drjfkmi3f8hcqn4umirgkgcn9vh.apps.googleusercontent.com";

    @Override
    public boolean authorizeToken(@RequestHeader HttpHeaders headers) {
        String token = headers.getFirst("Authorization");
        if(token == null || token == ""){
            return false;
        }
        return verify(token.substring(7));
    }

    @Override
    public boolean verify(String idTokenString) {
        logger.info("Token: {}", idTokenString);

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), JacksonFactory.getDefaultInstance()
        ).setAudience(Arrays.asList(CLIENT_ID)).build();
        try{
            GoogleIdToken idToken = verifier.verify(idTokenString);
            logger.info(idToken.toString());
            if(idToken != null){
                logger.info("Valid ID token");
                return true;
            }else{
                logger.info("Invalid ID token");
                return false;
            }
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } catch (Exception e){
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        return false;
    }
}
