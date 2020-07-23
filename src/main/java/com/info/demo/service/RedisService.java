package com.info.demo.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Set;

public interface RedisService {
    public String getValue(final String key);

    public void postValue(final String key, final String value);

    public void traverseInput(JsonNode inputData);

    public void populateNestedData(JsonNode parent, Set<String> childIdSet);

    public boolean deleteValue(final String key);

    public String getHash(String internalID);

}
