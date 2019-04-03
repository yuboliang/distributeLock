package com.study.lock.web;

import com.study.lock.service.LockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yuboliang
 * @date 2019/3/21
 */
@RestController
public class LockController {

    @Autowired
    private LockService lockService;

    @RequestMapping("/lock")
    public void lock(){
        lockService.tryLock();
    }

}
