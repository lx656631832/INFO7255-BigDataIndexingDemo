package com.info.demo;

import com.info.demo.util.JsonUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement()
public class DemoApplication {

    public static void main(String[] args) {
        JsonUtil.loadSchema();
        SpringApplication.run(DemoApplication.class, args);

    }

}
