package com.info.demo.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface IndexService {
    public void indexObject(JsonNode inputData, String parentId, String parentType, String routing);

    public void deleteEachObject(JsonNode inputData);

}
