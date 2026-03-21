# kubernetes-study

## introduction
- 멋쟁이 사자처럼 14기 밴엔드 세션 자료입니다.
- 쿠버네티스 세션 관련 자료입니다. 느리게 설계된 서버(/slow)와, 빠르게 설계된 서버(/fast) 이 두개가 같은 쿠버네티스 환경에서 어떻게 돌아가는지
golang으로 작성된 트래픽 부하 툴을 이용하여 확인하는 프로젝트입니다.

## features
1. 01-app/: 자바 소스 파일과, 도커 빌드 파일이 있습니다.
2. 02-k8s-manifests: 쿠버네티스 설정 파일입니다, deployment와 service정의 파일이 있습니다.
3. 03-load-tester: 트래픽 발생 소스가 있습니다.

## instruction
Step 1: Minikube 시작 및 환경 설정

가장 먼저 클러스터를 띄우고, 로컬 도커 이미지를 인식할 수 있게 연결합니다.

# 클러스터 생성
~~~
minikube start
~~~
# [중요] 현재 터미널의 도커 환경을 Minikube 내부로 전환 (푸시 생략용)
~~~
eval $(minikube docker-env)
~~~
Step 2: Spring Boot 서버 빌드 (CLI)
01-app 폴더로 이동하여 소스 코드를 빌드하고 이미지를 만듭니다.
~~~
cd 01-app
~~~
# Gradle로 JAR 파일 빌드 (JDK가 설치되어 있어야 함)
~~~
./gradlew build -x test
~~~
# 도커 이미지 생성 (Minikube 내부 저장소에 바로 저장됨)
~~~
docker build -t k8s-demo-app:latest .
~~~
Step 3: Kubernetes 배포

YAML 파일이 있는 폴더로 이동하여 배포 명령을 날립니다.

~~~
cd ../02-k8s-manifests
~~~

# Deployment & Service 실행
~~~
kubectl apply -f 01-deployment.yaml
kubectl apply -f 02-service.yaml
~~~

# 배포 상태 확인 (Running이 뜰 때까지 대기)
~~~
kubectl get pods -w
~~~

Step 4: 접속 주소 따기

Minikube는 로컬 IP가 다르므로 아래 명령어로 접속 URL을 추출합니다.

~~~
# 서비스 URL 확인 (이 주소를 복사해두세요)
minikube service k8s-demo-service --url
~~~

3. Go 부하 테스트 실행 (CLI 전용)

03-load-tester/main.go 수정 버전:
~~~
Go
package main

import (
	"flag"
	"fmt"
	"net/http"
	"sync"
	"time"
)

func main() {
	// CLI 인자로 URL과 횟수를 받게 설정
	url := flag.String("url", "", "Target URL (e.g. http://192.168.x.x:30080/api/fast)")
	count := flag.Int("n", 100, "Total number of requests")
	concurrency := flag.Int("c", 10, "Number of concurrent workers")
	flag.Parse()

	if *url == "" {
		fmt.Println(" URL을 입력해주세요. 사용법: go run main.go -url=http://...")
		return
	}

	fmt.Printf(" 부하 테스트 시작! [%s] 요청: %d개, 동시성: %d\n", *url, *count, *concurrency)
	
	start := time.Now()
	var wg sync.WaitGroup
	ch := make(chan struct{}, *concurrency)

	for i := 0; i < *count; i++ {
		wg.Add(1)
		ch <- struct{}{}
		go func(id int) {
			defer wg.Done()
			defer func() { <-ch }()
			resp, err := http.Get(*url)
			if err != nil {
				return
			}
			resp.Body.Close()
		}(i)
	}
	wg.Wait()
	fmt.Printf("\n 소요 시간: %v\n", time.Since(start))
}
~~~
실행 방법:

~~~
cd 03-load-tester
~~~

# 정상 요청 (/fast)
~~~
go run main.go -url=http://<MINIKUBE_IP>:<PORT>/api/fast -n 200 -c 20
~~~

# 반전 실험 (/slow)
~~~
go run main.go -url=http://<MINIKUBE_IP>:<PORT>/api/slow -n 50 -c 10
~~~

4. 모니터링 꿀팁 (CLI 전용)
실습 중에 학생들이 화면을 쪼개서(Split 터미널) 띄워놓게 하면 좋은 명령어입니다.

왼쪽 화면: watch -n 1 kubectl get pods (Pod가 생성/삭제되는 실시간 모습)

오른쪽 화면: kubectl logs -f -l app=k8s-demo --max-log-requests=10 (서버 로그)

하단 화면: go run main.go ... (부하 발생)

## directory structure
```
k8s-performance-lab/
├── 01-app/                 # [1단계] Spring Boot 소스 및 Docker 빌드
│   ├── src/                # Java 소스 (Controller 포함)
│   ├── build.gradle        # 의존성 설정
│   ├── Dockerfile          # 컨테이너 이미지 빌드 정의
│   └── gradlew             # 빌드 스크립트
│
├── 02-k8s-manifests/       # [2~3단계] Kubernetes 설정 파일 (YAML)
│   ├── 01-deployment.yaml  # 앱 배포 정의 (Replica 설정)
│   ├── 02-service.yaml     # 로드밸런서/서비스 정의
│   └── 03-hpa.yaml         # (심화) 자동 확장(HPA) 설정
│
├── 03-load-tester/         # [4단계] Go 기반 트래픽 부하 툴
│   ├── main.go             # 트래픽 발생기 소스
│   └── go.mod              # Go 모듈 설정
│
└── scripts/                # 실습 편의를 위한 쉘 스크립트
    ├── run-fast.sh         # /fast 경로 부하 테스트 실행 스크립트
    └── run-slow.sh         # /slow 경로 부하 테스트 실행 스크립트

```

## requirements 
- Docker Desktop(or Docker)
- Minikube(로컬 쿠버네티스 클러스터 실행용)
- kubectl(쿠버네티스 조작하는 cli 명령어)
- Go
