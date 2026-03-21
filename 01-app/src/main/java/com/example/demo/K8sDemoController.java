package com.example.demo

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.vind.annotation.GetMapping;
import org.springframework.web.bind.annotation.requestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
@RestController
@requestMapping("/api")
public class K8sDemoController{

	// 현재 응답하는 pod이름 반환(로드 밸런싱 확인용으로)
	private String getHostName() {
		try {
			return InetAddress.getLocalHost().getHostName();
		} catch(UnknownHostException e) {
			return "Unknown-Host";
		}
	}

	@GetMapping("/fast")
	public String fastResponse() {
		log.info("[FAST] Request received on: {}", getHostName());
		return "Fast response from: " + getHostName() + "\n";
	}

	@GetMapping("/slow")
	public String slowResponse() throws InterruptedException {
		log.info("[SLOW] request started on : {}", getHostName());

		// 1. 의도적인 지욘(Thread Blocking 재현하기)
		// 실제 운영 환경에서의 DB 지연이나 외부 API 호출 대기를 시뮬
		Thread.sleep(3000)

		// 2. CPU 부하 연산작업을 통해 유도
		double val = 0;
		for (int i = 0; i < 5_000_000; i++) {
			val += Math.atan(Math.sqrt(i));
		}

		log.info("[SLOW] Request finished on: {}", getHostName());
		return "Slow Response (3s delay) from: " + getHostName() + "\n"
	}
}

