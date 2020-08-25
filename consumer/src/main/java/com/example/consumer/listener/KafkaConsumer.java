package com.example.consumer.listener;

import java.util.concurrent.ExecutionException;

import com.example.consumer.dao.ElasticDao;

import com.example.consumer.util.JsonUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

@Service
public class KafkaConsumer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ElasticDao elasticDao;

    @KafkaListener(topics = "bigdataindexing", groupId = "group_id")
    public void consume(ConsumerRecord<String, String> record) throws ExecutionException, InterruptedException {
        logger.info("Consumed Message - {} ", record);
        System.out.println("Consumed message: Key" + record.key().toString() + " Value : " + record.value().toString());

        // Send Message to elastic search
        if (record.key().equals("index")) {
            try {
                JsonNode rootNode = JsonUtil.getNode(record.value());
                String objectId = rootNode.get("objectId").textValue();
                String routing = rootNode.get("routing").textValue();
                ObjectNode node = (ObjectNode) rootNode;
                node.remove("routing");
                elasticDao.index(objectId, node.toString(), routing);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getMessage());
            }
        } else if (record.key().equals("delete")) {
            elasticDao.delete(record.value());
        }
    }

}

