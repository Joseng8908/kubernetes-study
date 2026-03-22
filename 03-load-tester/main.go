package main

import (
	"flag"
	"fmt"
	"net/http"
	"sync"
	"time"
)

func main() {
	// 1. 설정값 입력 받기 (기본값 설정)
	url := flag.String("url", "", "대상 URL (예: http://192.168.49.2:30080/api/fast)")
	vus := flag.Int("c", 10, "동시 접속자 수 (Concurrency)")
	total := flag.Int("n", 100, "총 요청 횟수 (Number of requests)")
	flag.Parse()

	if *url == "" {
		fmt.Println("사용법: go run main.go -url=http://[IP]:[PORT]/api/fast -n=100 -c=10")
		return
	}

	fmt.Printf("테스트 시작: %s\n", *url)
	fmt.Printf("설정: 총 요청 %d개, 동시 접속 %d명\n", *total, *vus)
	fmt.Println("--------------------------------------------------")

	start := time.Now()
	var wg sync.WaitGroup
	results := make(chan time.Duration, *total)
	semaphore := make(chan struct{}, *vus) // 동시성 제어용 채널

	for i := 0; i < *total; i++ {
		wg.Add(1)
		go func(id int) {
			defer wg.Done()

			semaphore <- struct{}{} // 빈 슬롯 확인 (동시성 제한)
			defer func() { <-semaphore }()

			reqStart := time.Now()
			resp, err := http.Get(*url)
			if err != nil {
				fmt.Printf("[%d] 에러: %v\n", id, err)
				return
			}
			resp.Body.Close()
			results <- time.Since(reqStart)
		}(i)
	}

	wg.Wait()
	close(results)
	duration := time.Since(start)

	// 2. 결과 계산
	var totalTime time.Duration
	count := 0
	for t := range results {
		totalTime += t
		count++
	}

	avg := time.Duration(0)
	if count > 0 {
		avg = totalTime / time.Duration(count)
	}

	// 3. 리포트 출력
	fmt.Println("--------------------------------------------------")
	fmt.Printf(" 테스트 완료!\n")
	fmt.Printf(" 전체 소요 시간: %v\n", duration)
	fmt.Printf(" 전체 응답 시간: %v\n", avg)
	fmt.Printf(" 초당 처리량 (TPS): %.2f req/s\n", float64(count)/duration.Seconds())
}
