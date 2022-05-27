package com.example.redisdemo.controller;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Description: redission 提供的分布式并发操作
 * @Author tianwl
 * @Company 安徽中科美络信息技术有限公司
 * @Email tianwl@izkml.com
 * @Date 2022/5/27 10:26
 */
@Slf4j
public class RedissionLockDemo {

    @Autowired
    private Redisson redisson;
    @Autowired
    private RedisTemplate redisTemplate;

    //====================RReadWriteLock===========================

    /**
     * 读写锁 总结
     *
     * 读锁又叫共享锁
     * 写锁又叫排他锁（互斥锁）
     * 读 + 读 相当于无锁，并发读，同时加锁成功
     * 写 + 写 阻塞状态
     * 写 + 读 等待写锁释放
     * 读 + 写 等待读锁完，才写，
     */
    public String writeValue() {
        String str = "";
        RReadWriteLock readWriteLock = redisson.getReadWriteLock("readWriteLock");
        RLock rLock = readWriteLock.writeLock();
        try {
            rLock.lock();
            str = UUID.randomUUID().toString();
            redisTemplate.opsForValue().set("uuid", str);
            Thread.sleep(30000);
        } catch (Exception e) {
        } finally {
            rLock.unlock();
        }
        return str;
    }
    /**
     * 读锁
     *
     * @return
     */
    public String readValue() {
        String str = "";
        RReadWriteLock readWriteLock = redisson.getReadWriteLock("readWriteLock");
        RLock rLock = readWriteLock.readLock();
        rLock.lock();
        str = (String) redisTemplate.opsForValue().get("uuid");
        rLock.unlock();
        return str;
    }


    //====================RSemaphore===========================

    /**
     * 信号量
     *
     * @return
     */
    public String acquire() throws InterruptedException {
        //这里是获取信号量的值，这个信号量的name一定要与你初始化的一致
        RSemaphore park = redisson.getSemaphore("park");
        //这里会将信号量里面的值-1，如果为0则一直等待，直到信号量>0
        park.acquire();
        //tryAcquire为非阻塞式等待
        //park.tryAcquire();
        return "ok";
    }
    public String release() throws InterruptedException {
        //这里是获取信号量的值，这个信号量的name一定要与你初始化的一致
        RSemaphore park = redisson.getSemaphore("park");
        //这里会将信号量里面的值+1，也就是释放信号量
        park.release();
        return "ok";
    }


    //====================RCountDownLatch===========================
    /**
     * 闭锁，限流
     *
     * @return
     * @throws InterruptedException
     */
    //锁门
    public String lockdoor() throws InterruptedException {
        RCountDownLatch door = redisson.getCountDownLatch("door");
        //设置一个班级有20个同学
        door.trySetCount(20);
        //需要等到20个同学全部离开，才锁门
        door.await();
        return "锁门了";
    }
    public String leave(Long id) throws InterruptedException {
        RCountDownLatch door = redisson.getCountDownLatch("door");
        //表示一个同学离开
        door.countDown();
        return "" + id + "号同学离开了";
    }


    /**
     * 利用redis实现的分布式锁
     * 1、死锁问题-->加失效时间
     * 2、误删锁问题-->value存放uuid
     * 3、删除操作原子性问题-->lua删除脚本保证原子性
     * 4、加锁操作原子性-->setnx px（setIfAbsent(goodsId, lock, 30, TimeUnit.SECONDS)）
     * 分布式锁
     * 加锁原子操作，
     * 解锁，删除锁也是原子操作 瑕疵是没有自动续命
     *  redis 锁 集群有瑕疵 不能 续命
     * @return
     */
    public String doKill() {
        String lock = UUID.randomUUID().toString();
        String goodsId = "10054";
        boolean flag = redisTemplate.opsForValue().setIfAbsent(goodsId, lock, 30, TimeUnit.SECONDS);
        if (flag) {
            // 获取锁成功
            try {
                Long stock = redisTemplate.opsForValue().decrement("key");
                if (stock > 0) {
                    redisTemplate.opsForValue().increment("key");
                    log.info("扣减库存成功，还剩:" + stock);
                }
                return "库存不足，该商品已抢购完！";
            } catch (Exception e) {
            } finally {
                //  删除key的lua脚本，保证删除操作的原子性
                String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1] then return redis.call(\"del\",KEYS[1]) else return 0 end";
                redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(goodsId), lock);
            }
        }
        return doKill();
    }



    /**
     * 整合 redission
     * @return
     */
    public String doKillDistributed() {
        String goodsId = "10054";
        RLock lock = redisson.getLock("key");
        // 获取锁成功
        try {
            //1 阻塞式等待，默认30秒时间
            //2 自动续期，如果业务超长，续上新的30秒，不用担心过期时间，锁自动删除掉
            //3 枷锁的业务运行完成，就不会给当前的锁自动续期，即使没有手动释放锁也会，30秒自动释放
//            lock.lock(30, TimeUnit.SECONDS); //不会自动续期需要注意
            lock.lock();
            Long stock = redisTemplate.opsForValue().decrement("key");
            if (stock > 0) {
                redisTemplate.opsForValue().increment("key");
                log.info("扣减库存成功，还剩:" + stock);
            }
            return "库存不足，该商品已抢购完！";
        } catch (Exception e) {
        } finally {
            lock.unlock();
        }
        return "fail";
    }


}
