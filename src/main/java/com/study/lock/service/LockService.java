package com.study.lock.service;

import com.study.lock.util.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author yuboliang
 * @date 2019/3/21
 */
@Service
@Slf4j
public class LockService {

    public void tryLock() {
        String lockName = "user:lock:123";
        DistributedLock distributedLock = new DistributedLock(lockName);

        try {
            boolean lock = distributedLock.lock();
            if (lock) {
                // TODO doBusiness
            }

        } finally {
            distributedLock.unLock();
        }
    }


}
