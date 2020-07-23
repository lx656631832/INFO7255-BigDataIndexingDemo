package com.info.demo.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import javax.servlet.Filter;

@Configuration
public class RedisConfiguration {
    @Bean
    public JedisConnectionFactory redisConnectionFactory() {
        //RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6379);
        return new JedisConnectionFactory();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        final RedisTemplate<String, Object> template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(redisConnectionFactory());
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericToStringSerializer<Object>(Object.class));
        template.setValueSerializer(new GenericToStringSerializer<Object>(Object.class));
        return template;
    }

    //demo2
//    @Bean(name = "etagFilter")
//    public javax.servlet.Filter shallowEtagHeaderFilter() {
//        return new ShallowEtagHeaderFilter();
//    }
//
//    @Bean
//    public FilterRegistrationBean<javax.servlet.Filter> ftreg() {
//        final FilterRegistrationBean<Filter> fr = new FilterRegistrationBean<>();
//        fr.setFilter(shallowEtagHeaderFilter());
//        fr.addUrlPatterns("/*");
//        fr.setName("etagFilter");
//        fr.setOrder(1);
//        return fr;
//    }
    //demo2

}
