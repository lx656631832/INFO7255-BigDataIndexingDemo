package com.info.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.info.demo.constant.CommonConstants;
import com.info.demo.service.RedisService;
import com.info.demo.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/")
public class HelloController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RedisService redisService;

    @GetMapping("/{object}/{key}")
    public ResponseEntity<String> getValue(@PathVariable String object,
                                           @PathVariable String key){
        logger.info("getValue(String object : " + object + " key : " + key + " - Start");

        String internalID = CommonConstants.ID + object + "_" + key;
        String value = redisService.getValue(internalID);

        if (value == null) {
            return new ResponseEntity<String>("{\"message\": \"No Data Found\" }", HttpStatus.NOT_FOUND);
        }else{
            JsonNode node = JsonUtil.nodeFromString(value);
            redisService.populateNestedData(node, null);
            value = node.toString();
        }

        logger.info("getValue(String object : " + object + " key : " + key + " - End");

        return ResponseEntity.ok().body(value);
    }


    @PostMapping("/{object}")
    public ResponseEntity<String> postObject(@PathVariable String object, HttpEntity<String> input){
        logger.info("postValue(String object : " + object + " input : " + input.getBody() + " - Start");

        String planId = "";
        JsonNode rootNode = JsonUtil.validateAgainstSchema(input.getBody());
        if (rootNode != null) {
            String objectId = rootNode.get("objectId").textValue();
            planId = CommonConstants.ID + rootNode.get("objectType").textValue() + "_" + objectId;

            if (redisService.getValue(planId) != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(" {\"message\": \"A resource already exisits with the id: " + planId + "\" }");
            }

            redisService.traverseInput(rootNode);
            redisService.postValue(planId, rootNode.toString());

            logger.info("postValue(String object : " + object + " input : " + input.getBody() + " - End");

            return ResponseEntity.ok().body(" {\"message\": \"Created data with key: " + planId + "\" }");

        } else {
            return ResponseEntity.ok().body(" {\"message\": \"Error validating the input data\" }");
        }
    }

    @DeleteMapping(value = "/{object}/{objectId}")
    public ResponseEntity<String> deleteValue(@PathVariable("object") String object,
                                              @PathVariable("objectId") String objectId) {

        logger.info("deleteValue(String object : " + object + " objectId : " + objectId + " - Start");

        String internalID = CommonConstants.ID + object + "_" + objectId;
        String masterObject = redisService.getValue(internalID);
        Set<String> childIdSet = new HashSet<String>();
        childIdSet.add(internalID);
        redisService.populateNestedData(JsonUtil.nodeFromString(masterObject), childIdSet);
        boolean deleteSuccess = false;

        for (String id : childIdSet) {
            deleteSuccess = redisService.deleteValue(id);
        }

        logger.info("deleteValue(String object : " + object + " objectId : " + objectId + " - End");
        if (deleteSuccess)
            return new ResponseEntity<>(" {\"message\": \"Deleted\" }", HttpStatus.OK);

        return new ResponseEntity<>(" {\"message\": \"There is nothing to delete\" }", HttpStatus.NOT_FOUND);
    }
















}
