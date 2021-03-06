package com.study.lock.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.types.Expiration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author yuboliang
 * @date 2019/3/21
 */
@Slf4j
public class DistributedLock {

    private RedisTemplate redisTemplate;

    /**
     * 锁名称（redisKey）
     */
    private String lockName;

    /**
     * 锁值（redisValue）
     */
    private String lockValue;

    /**
     * 过期时间
     */
    private Integer expireTime;

    /**
     * 是否获取到锁标志
     */
    private boolean getLock;

    /**
     * 解锁的lua脚本
     */
    public static final String UNLOCK_LUA;

    static {
        StringBuilder sb = new StringBuilder();
        sb.append("if redis.call('get', KEYS[1]) == ARGV[1] ");
        sb.append("then ");
        sb.append("    return redis.call('del',KEYS[1]) ");
        sb.append("else ");
        sb.append("    return 0 ");
        sb.append("end ");
        UNLOCK_LUA = sb.toString();
    }

    public DistributedLock(String lockName, Integer expireTime) {
        this.lockName = lockName;
        this.expireTime = expireTime;
        redisTemplate = SpringContextUtil.getBean("redisTemplate");
    }

    /**
     * 获取分布式锁
     * @return
     */
    public boolean lock() {
        lockValue = UUID.randomUUID().toString();
        log.info("尝试获取分布式锁，key：{}，value:{}", lockName, lockValue);
        getLock = (boolean) redisTemplate.execute((RedisCallback) redisConnection ->
                redisConnection.set(redisTemplate.getKeySerializer().serialize(lockName),
                        redisTemplate.getValueSerializer().serialize(lockValue),
                        Expiration.seconds(expireTime), RedisStringCommands.SetOption.ifAbsent()));
        log.info("获取分布式锁结果:{}", getLock);

        return getLock;
    }

    /**
     * 获取分布式锁并阻塞
     * @return
     */
    public boolean lockAndBlocking() {
        while (true) {
            boolean lock = lock();
            if (lock) {
                break;
            }

            try {
                Thread.sleep(1000 * 5);
            } catch (InterruptedException ignore) {
                log.warn("获取分布式锁休眠被中断", ignore);
            }
        }

        return getLock;
    }

    /**
     * 获取分布式锁并等待
     * @param timeOut 等待超时，单位：秒
     * @return
     */
    public boolean lockAndWait(long timeOut) {
        long lockTime = System.currentTimeMillis() / 1000;
        while (System.currentTimeMillis() / 1000 - lockTime < timeOut) {
            boolean lock = lock();
            if (lock) {
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
                log.warn("获取分布式锁休眠被中断", ignore);
            }
        }

        return getLock;

    }

    /**
     * 解锁
     * @return
     */
    public boolean unLock() {
        if (!getLock) {
            return false;
        }

        DefaultRedisScript redisScript = new DefaultRedisScript(UNLOCK_LUA);
        redisScript.setResultType(Long.class);

        List<Object> keyList = new ArrayList<>();
        keyList.add(lockName);
        Object values = lockValue;

        Long unLockValue = (Long) redisTemplate.execute(redisScript, keyList, values);
        if (unLockValue == 0) {
            log.info("解锁失败，lockName:{}", lockName);
        }
        log.info("解锁lockName:{}成功", lockName);

        return true;
    }
}
