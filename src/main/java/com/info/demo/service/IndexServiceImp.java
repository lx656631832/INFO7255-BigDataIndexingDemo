package com.info.demo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.Iterator;
@Scope("prototype")
@Service
public class IndexServiceImp implements IndexService {

    @Autowired
    private KafkaService kafkaProducer;
    @Override
    public void indexObject(JsonNode inputData, String parentId, String parentType, String routing) {

        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();
        ObjectNode joinNode = objectMapper.createObjectNode();

        System.out.println("parent type : " + parentType);

        if (parentType != null && parentType.equals("planservice")
                && inputData.get("objectType").textValue().equals("membercostshare")) {
            joinNode.put("name", "planservice_membercostshare");
        } else {
            joinNode.put("name", inputData.get("objectType").asText());
        }

        if (parentId != null) {
            joinNode.put("parent", parentId);
        }

        node.set("plan_service", joinNode);
        node.put("routing", routing);

        inputData.fields().forEachRemaining(entry -> {
            if (!entry.getValue().isArray() && !entry.getValue().isContainerNode()) {
                node.set(entry.getKey(), entry.getValue());
            }
        });

        kafkaProducer.publish(node.toString(), "index");

        inputData.fields().forEachRemaining(entry -> {
            if (entry.getValue().isArray()) {
                Iterator<JsonNode> iterator = entry.getValue().iterator();
                while (iterator.hasNext()) {
                    JsonNode child = iterator.next();
                    indexObject(child,
                            inputData.get("objectId").textValue(),
                            inputData.get("objectType").textValue(),
                            routing);
                }
            } else if (entry.getValue().isContainerNode()) {
                indexObject(entry.getValue(),
                        inputData.get("objectId").textValue(),
                        inputData.get("objectType").textValue(),
                        routing);
            }
        });
    }


    @Override
    public void deleteEachObject(JsonNode inputData) {
        inputData.fields().forEachRemaining(entry -> {
            if (entry.getKey().equals("objectId")) {
                kafkaProducer.publish(entry.getValue().textValue(), "delete");
            }
        });
    }
}
