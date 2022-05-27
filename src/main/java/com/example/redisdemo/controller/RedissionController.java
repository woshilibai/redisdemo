package com.example.redisdemo.controller;

import org.redisson.Redisson;
import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RSemaphore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @Description: todo
 * @Author tianwl
 * @Company 安徽中科美络信息技术有限公司
 * @Email tianwl@izkml.com
 * @Date 2022/5/27 10:10
 */
@RestController
@RequestMapping("/redission")
public class RedissionController {

    @Resource
    private Redisson redisson;

    /**
     * redission 常见的分布式锁
     */
    public void redissionDoc() throws InterruptedException {
        //  1、普通可重入锁
        RLock rLock = redisson.getLock("generalLock");
        //  加锁操作，获取失败不停自动重试，具有watch dog 自动续期机制，默认30s
        rLock.lock();
        //  加锁操作，获取失败自动重试10s，具有watch dog机制
        rLock.tryLock(10, TimeUnit.SECONDS);
        //  获取失败不停自动重试，获取到锁后30s后自动释放锁，没有watch dog 自动续期机制
        rLock.lock(30, TimeUnit.SECONDS);
        //  获取失败自动重试10s，获取到后30s后自动释放锁，没有watch dog 自动续期机制
        rLock.tryLock(10, 30 ,TimeUnit.SECONDS);

        //  2、公平锁
        RLock rFairLock = redisson.getFairLock("fairLock");

        //  3、读写锁，读-读操作并发
        RReadWriteLock rReadWriteLock = redisson.getReadWriteLock("readWriteLock");
        //  读锁
        RLock readLock = rReadWriteLock.readLock();
        //  写锁
        RLock writeLock = rReadWriteLock.writeLock();

        //  4、信号量
        RSemaphore rSemaphore = redisson.getSemaphore("resource");
        rSemaphore.acquire();
        //  do...
        rSemaphore.release();

        //  5、闭锁
        RCountDownLatch rCountDownLatch = redisson.getCountDownLatch("latch");
        rCountDownLatch.await();
        //  do...
        rCountDownLatch.countDown();
    }

}
