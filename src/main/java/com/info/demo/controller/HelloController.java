package com.info.demo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.info.demo.constant.CommonConstants;
import com.info.demo.service.RedisService;
import com.info.demo.service.TokenService;
import com.info.demo.util.EtagUtil;
import com.info.demo.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


@RestController
@RequestMapping("/")
public class HelloController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Autowired
    private RedisService redisService;

    @Autowired
    private EtagUtil etagUtil;

    @Autowired
    private TokenService authorizeService;

    @GetMapping("/{object}/{key}")
    public ResponseEntity<String> getValue(@PathVariable String object,
                                           @PathVariable String key,
                                           @RequestHeader HttpHeaders requestHeader) {
        //System.out.println("Get method is running");
        logger.info("getValue(String object : " + object + " key : " + key + " - Start");

        if(!requestHeader.containsKey("Authorization")){
            return new ResponseEntity<>("{\"message\":\"Not Authorized\"}",HttpStatus.UNAUTHORIZED);
        }
        if(!authorizeService.authorizeToken(requestHeader)){
            return new ResponseEntity<>("{\"message\":\"Invalid Token\"}",HttpStatus.UNAUTHORIZED);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String internalID = CommonConstants.ID + object + "_" + key;
        String value = redisService.getValue(internalID);

        if (value == null) {
            return new ResponseEntity<String>("{\"message\": \"There is no data found\" }", HttpStatus.NOT_FOUND);
        }else{
            JsonNode node = JsonUtil.nodeFromString(value);
            redisService.populateNestedData(node, null);
            value = node.toString();

            String etag = etagUtil.getEtag(node);
            //System.out.println(etag);
            if(!etagUtil.verifyEtag(node, requestHeader.getIfNoneMatch())){
                headers.setETag(etag);
            }else{
                headers.setETag(etag);
                return new ResponseEntity<>(value,headers,HttpStatus.NOT_MODIFIED);
            }
        }
        logger.info("getValue(String object : " + object + " key : " + key + " - End");

        return ResponseEntity.ok().body(value);
    }


    @PostMapping("/{object}")
    public ResponseEntity<String> postObject(@PathVariable String object,
                                             HttpEntity<String> input,
                                             @RequestHeader HttpHeaders requestHeader)  {

        logger.info("postValue(String object : " + object + " input : " + input.getBody() + " - Start");

        if(!requestHeader.containsKey("Authorization")){
            return new ResponseEntity<>("{\"message\":\"Not Authorized\"}",HttpStatus.UNAUTHORIZED);
        }
        if(!authorizeService.authorizeToken(requestHeader)){
            return new ResponseEntity<>("{\"message\":\"Invalid Token\"}",HttpStatus.UNAUTHORIZED);
        }


        String planId = "";
        //loadSchema();
        JsonNode rootNode = JsonUtil.validateAgainstSchema(input.getBody());
        if (rootNode != null) {
            String objectId = rootNode.get("objectId").textValue();
            planId = CommonConstants.ID + rootNode.get("objectType").textValue() + "_" + objectId;

            String etag = etagUtil.getEtag(rootNode);
            HttpHeaders responseHeader = new HttpHeaders();
            responseHeader.setETag(etag);

            if (redisService.getValue(planId) != null) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(" {\"message\": \"A object already exisits with the id: " + planId + "\" }");
            }

            redisService.traverseInput(rootNode);
            redisService.postValue(planId, rootNode.toString());

            return ResponseEntity.status(HttpStatus.CREATED).headers(responseHeader).body(" {\"message\": \"Created data with key: " + planId + "\" }");

        } else {
            logger.info("postValue(String object : " + object + " input : " + input.getBody() + " - End");

            return new ResponseEntity<>(" {\"message\": \"validation failed with the input data\" }",HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping(value = "/{object}/{objectId}")
    public ResponseEntity<String> deleteValue(@PathVariable("object") String object,
                                              @PathVariable("objectId") String objectId,
                                              @RequestHeader HttpHeaders requestHeader) {

        if(!requestHeader.containsKey("Authorization")){
            return new ResponseEntity<>("{\"message\":\"Not Authorized\"}",HttpStatus.UNAUTHORIZED);
        }
        if(!authorizeService.authorizeToken(requestHeader)){
            return new ResponseEntity<>("{\"message\":\"Invalid Token\"}",HttpStatus.UNAUTHORIZED);
        }

        String internalID = CommonConstants.ID + object + "_" + objectId;
        String masterObject = redisService.getValue(internalID);
        Set<String> childIdSet = new HashSet<String>();
        childIdSet.add(internalID);



        redisService.populateNestedData(JsonUtil.nodeFromString(masterObject), childIdSet);
        boolean deleteSuccess = false;

        for (String id : childIdSet) {
            deleteSuccess = redisService.deleteValue(id);
        }

        if (deleteSuccess)
            return new ResponseEntity<>(" {\"message\": \"Deleted\" }", HttpStatus.OK);

        return new ResponseEntity<>(" {\"message\": \"There is nothing to delete\" }", HttpStatus.NOT_FOUND);
    }



    @RequestMapping(value = "/{object}/{key}", method = RequestMethod.PUT)
    public ResponseEntity<String> putValue(@PathVariable String object, HttpEntity<String> input,
                                           @PathVariable String key,
                                           @RequestHeader HttpHeaders requestHeader) {

        logger.info("postValue(String object : " + object + " input : " + input.getBody() + " - Start");

        String internalID = CommonConstants.ID + object + "_" + key;
        String masterObject = redisService.getValue(internalID);

        if (masterObject == null) {
            return new ResponseEntity<String>("{\"message\": \"No Data Found\" }", HttpStatus.NOT_FOUND);
        }

        Set<String> childIdSet = new HashSet<String>();
        childIdSet.add(internalID);
        redisService.populateNestedData(JsonUtil.nodeFromString(masterObject), childIdSet);
        boolean deleteSuccess = false;

        for (String id : childIdSet) {
            deleteSuccess = redisService.deleteValue(id);
        }

        if (deleteSuccess) {
            String planId = "";
            JsonNode rootNode = JsonUtil.validateAgainstSchema(input.getBody());
            if (null != rootNode) {
                String objectId = rootNode.get("objectId").textValue();
                planId = CommonConstants.ID + rootNode.get("objectType").textValue() + "_" + objectId;

                if (redisService.getValue(planId) != null) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body(" {\"message\": \"A resource already exisits with the id: " + planId + "\" }");
                }

                redisService.traverseInput(rootNode);
                redisService.postValue(planId, rootNode.toString());
            } else {
                return ResponseEntity.ok().body(" {\"message\": \"Error validating the input data\" }");
            }

            logger.info("postValue(String object : " + object + " input : " + input.getBody() + " - End");

            return ResponseEntity.ok().body(" {\"message\": \"Updated data with key: " + planId + "\" }");
        }

        return ResponseEntity.ok().body(" {\"message\": \"Error updating the object }");
    }

    @RequestMapping(value = "/{object}/{key}", method = RequestMethod.PATCH)
    public ResponseEntity<String> patchValue(@PathVariable String object, @PathVariable String key,
                                             HttpEntity<String> input,
                                             @RequestHeader HttpHeaders requestHeader) {

        logger.info("patchValue(String object : " + object + "String objectId : " + key + " input : " + input.getBody() + " - Start");

        String internalID = CommonConstants.ID + object + "_" + key;
        String value = redisService.getValue(internalID);

        if (value == null) {
            return new ResponseEntity<String>("{\"message\": \"No Data Found\" }", HttpStatus.NOT_FOUND);
        }

        try {
            //Get the old node from redis using the object Id
            JsonNode oldNode = JsonUtil.nodeFromString(value);
            redisService.populateNestedData(oldNode, null);
            value = oldNode.toString();

            //Construct the new node from the input body
            String inputData = input.getBody();
            JsonNode newNode = JsonUtil.nodeFromString(inputData);

            ArrayNode planServicesNew = (ArrayNode) newNode.get("linkedPlanServices");
            Set<JsonNode> planServicesSet = new HashSet<>();
            Set<String> objectIds = new HashSet<String>();

            planServicesNew.addAll((ArrayNode) oldNode.get("linkedPlanServices"));

            for (JsonNode node : planServicesNew) {
                Iterator<Map.Entry<String, JsonNode>> sitr = node.fields();
                while (sitr.hasNext()) {
                    Map.Entry<String, JsonNode> val = sitr.next();
                    if (val.getKey().equals("objectId")) {
                        if (!objectIds.contains(val.getValue().toString())) {
                            planServicesSet.add(node);
                            objectIds.add(val.getValue().toString());
                        }
                    }
                }
            }

            planServicesNew.removeAll();

            if (!planServicesSet.isEmpty())
                planServicesSet.forEach(s -> {
                    planServicesNew.add(s);
                });

            redisService.traverseInput(newNode);
            redisService.postValue(internalID, newNode.toString());

        } catch (Exception e) {
            logger.error(e.getMessage());
            return new ResponseEntity<>(" {\"message\": \"Invalid Data\" }", HttpStatus.BAD_REQUEST);
        }

        logger.info("patchValue(String object : " + object + "String objectId : " + key + " input : " + input.getBody() + " - End");

        return ResponseEntity.ok().body(" {\"message\": \"Updated data with key: " + internalID + "\" }");
    }

}
