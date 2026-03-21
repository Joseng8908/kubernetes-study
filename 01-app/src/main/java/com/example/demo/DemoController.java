package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.net.InetAddress;

@RestController
@RequestMapping("/api")
public class DemoController {
    private static final Logger log = LoggerFactory.getLogger(DemoController.class);

    @GetMapping("/fast")
    public String fast() throws Exception {
        String host = InetAddress.getLocalHost().getHostName();
        log.info("🚀 FAST 요청 처리 중... Host: {}", host);
        return "🚀 Fast from " + host + "\n";
    }

    @GetMapping("/slow")
    public String slow() throws Exception {
        String host = InetAddress.getLocalHost().getHostName();
        log.info("🐢 SLOW 요청 시작 (3초 대기)... Host: {}", host);
        Thread.sleep(3000); // 병목 시뮬레이션
        return "🐢 Slow from " + host + "\n";
    }
}
