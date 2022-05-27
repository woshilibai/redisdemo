package com.example.redisdemo.config;

import org.redisson.Redisson;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import redis.clients.jedis.HostAndPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Description: todo
 * @Author tianwl
 * @Company 安徽中科美络信息技术有限公司
 * @Email tianwl@izkml.com
 * @Date 2022/5/26 16:05
 */
@Configuration
public class RedisConfig {

    @Value("${spring.redis.cluster.nodes}")
    private String clusterNodes;
    @Value("${spring.redis.timeout}")
    private int timeout;
    @Value("${spring.redis.pool.max-idle}")
    private int maxIdle;
    @Value("${spring.redis.pool.max-wait}")
    private long maxWaitMillis;
    @Value("${spring.redis.commandTimeout}")
    private int commandTimeout;
    @Value("${spring.redis.password}")
    private String redisPassword;

    /**
     * spring-data-redis集成 redis集群方式
     * @param redisConnectionFactory
     * @return
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 自定义的string序列化器和fastjson序列化器
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // jackson 序列化器
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();

        // kv 序列化
        redisTemplate.setKeySerializer(stringRedisSerializer);
        redisTemplate.setValueSerializer(jsonRedisSerializer);

        // hash 序列化
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        redisTemplate.setHashValueSerializer(jsonRedisSerializer);

        redisTemplate.afterPropertiesSet();

        return redisTemplate;
    }


    /**
     * jedis集成 redis集群方式
     * @return
     */
    @Bean
    public JedisCluster jedisCluster() {
        String[] cNodes = clusterNodes.split(",");
        Set<HostAndPort> nodes = new HashSet<>();
        //分割出集群节点
        for (String node : cNodes) {
            String[] hp = node.split(":");
            nodes.add(new HostAndPort(hp[0], Integer.parseInt(hp[1])));
        }
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(maxIdle);
        jedisPoolConfig.setMaxWaitMillis(maxWaitMillis);
        //创建集群对象
        return new JedisCluster(nodes, 5000, 5000, 10, redisPassword, jedisPoolConfig);
    }


    /**
     * redis://host:port
     */
    private static final String REDIS_ADDRESS = "redis://%s:%s";

    /**
     * 集群模式-添加redisson的bean
     * @return
     */
    @Bean
    public Redisson redisson() {
        //redisson版本是3.5，集群的ip前面要加上“redis://”，不然会报错，3.2版本可不加
        List<String> clusterNodes = new ArrayList<>();
        clusterNodes.add("redis://10.5.4.232:6380");
        clusterNodes.add("redis://10.5.4.233:6380");
        clusterNodes.add("redis://10.5.4.234:6380");
        clusterNodes.add("redis://10.5.4.232:6381");
        clusterNodes.add("redis://10.5.4.233:6381");
        clusterNodes.add("redis://10.5.4.234:6381");
        Config config = new Config();
        ClusterServersConfig clusterServersConfig = config.useClusterServers()
                .addNodeAddress(clusterNodes.toArray(new String[clusterNodes.size()]));
        clusterServersConfig.setPassword(redisPassword);//设置密码，如果没有密码，则注释这一行，否则启动会报错
        //  获取redission client
        return (Redisson) Redisson.create(config);
    }

//    /**
//     * Redisson单机模式
//     * @return
//     */
//    @Bean
//    public Redisson RedissonConfig(){
//        Config config = new Config();
//        config.useSingleServer().setAddress("redis://localhost:6379").setDatabase(redisConfigProperties.getDatabase());
//        config.useSingleServer().setAddress(String.format(REDIS_ADDRESS, redisConfigProperties.getHost(), redisConfigProperties.getPort()))
//                .setDatabase(redisConfigProperties.getDatabase())
//                .setPassword(redisConfigProperties.getPassword());
//        return (Redisson) Redisson.create(config);
//    }

    //  java客户端jedis、lettuce、redisson的区别
    //  1、jedis并发环境下非线程安全，需配合连接池使用，保证一个线程使用一个jedis连接，避免共享jedis连接
    //  2、lettue线程安全，基于nettty，支持异步，性能好，netty 是一个多线程、事件驱动的 I/O 框架
    //  3、redisson提供基于redis的分布式锁，分布式操作
//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
//        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
//        redisTemplate.setKeySerializer(new StringRedisSerializer());
//        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
//        redisTemplate.setConnectionFactory(connectionFactory);
//        return redisTemplate;
//    }
}
