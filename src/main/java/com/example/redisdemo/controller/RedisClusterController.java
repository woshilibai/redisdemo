package com.example.redisdemo.controller;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.JedisCluster;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @Description: redis集群操作演示
 * @Author tianwl
 * @Company 安徽中科美络信息技术有限公司
 * @Email tianwl@izkml.com
 * @Date 2022/5/26 16:07
 */
@RestController
@RequestMapping("/redis")
public class RedisClusterController {

    @Resource
    RedisTemplate redisTemplate;

    @Resource
    JedisCluster jedisCluster;

    //  spring-data-redis集成 redis集群方式
    @GetMapping("/redisTemplate/{key}")
    public String redisTemplate(@PathVariable("key") String key){
        redisTemplate.opsForValue().set(key, "redisTemplate");
        redisTemplate.expire(key, 30, TimeUnit.SECONDS);
        return redisTemplate.opsForValue().get(key).toString();
    }

    //  jedis集成 redis集群方式
    @GetMapping("/jedisCluster/{key}")
    public String jedisCluster(@PathVariable("key") String key){
        jedisCluster.set(key, "jedisCluster");
        jedisCluster.expire(key, 30);
        return jedisCluster.get(key);
    }

}
