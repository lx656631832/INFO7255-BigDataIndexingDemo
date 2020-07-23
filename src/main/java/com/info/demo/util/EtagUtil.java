package com.info.demo.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

@Service
public class EtagUtil {

    public String getEtag(JsonNode jsonNode){
        String encoded = null;

        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(jsonNode.toString().getBytes(StandardCharsets.UTF_8));
            encoded = Base64.getEncoder().encodeToString(hash);

        }catch (NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        return "\""+encoded+"\"";
    }

    public boolean verifyEtag(JsonNode json, List<String> etags){
        if(etags.isEmpty()){
            return false;
        }
        String encoded = getEtag(json);
        //System.out.println("verify used");
        return etags.contains(encoded);
    }
}
