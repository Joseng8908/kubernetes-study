# kubernetes-study

## introduction
- 멋쟁이 사자처럼 14기 밴엔드 세션 자료입니다.
- 쿠버네티스 세션 관련 자료입니다. 느리게 설계된 서버(/slow)와, 빠르게 설계된 서버(/fast) 이 두개가 같은 쿠버네티스 환경에서 어떻게 돌아가는지
golang으로 작성된 트래픽 부하 툴을 이용하여 확인하는 프로젝트입니다. -> golang말고 JavaScript로 작성하는 k6라는 툴을 한번 이용해볼 예정입니다!

## features
1. 01-app/: 자바 소스 파일과, 도커 빌드 파일이 있습니다.
2. 02-k8s-manifests: 쿠버네티스 설정 파일입니다, deployment와 service정의 파일이 있습니다.
3. 03-load-tester: 트래픽 발생 소스가 있습니다.

## instruction
### 0. 사전 준비 (Prerequisites)

- 컴퓨터에 다음이 설치되어 있어야 합니다: Docker Desktop, Minikube, kubectl, Go, JDK 17, k6

### 1. 프로젝트 복사 및 환경 설정
~~~bash
# 1. 저장소 클론
git clone https://github.com/your-repo/k8s-performance-lab.git
cd k8s-performance-lab

# 2. Minikube 시작
minikube start

# 3. [매우 중요] 현재 터미널을 Minikube 도커 환경에 연결
# 이 명령어를 입력해야 docker build 시 이미지가 쿠버네티스 안으로 바로 들어갑니다.
eval $(minikube docker-env)
~~~

### 2. Spring Boot 서버 빌드 및 이미지 생성

- 소스를 빌드하고 이미지화하는 단계입니다.

~~~bash
cd 01-app

# 1. 실행 파일(JAR) 만들기
./gradlew clean build -x test

# 2. 도커 이미지 굽기
docker build -t k8s-demo-app:latest .

# 3. 확인 (k8s-demo-app이 목록에 있어야 함)
docker images
~~~

### 3. Kubernetes에 서버 배포

- 작성된 YAML 설정을 클러스터에 적용합니다.

~~~bash
cd ../02-k8s-manifests

# 1. Deployment와 Service 배포
kubectl apply -f 01-deployment.yaml
kubectl apply -f 02-service.yaml

# 2. 서버가 뜰 때까지 대기 (Running 상태 확인)
kubectl get pods -w
~~~

### 4. 접속 주소 확인 및 테스트

- Minikube 터미널에서 서버 주소를 따냅니다.

~~~Bash
# 외부 접속 URL 확인
minikube service k8s-demo-service --url

# (출력 예시: http://127.0.0.1:30080)
# 위 주소를 복사해서 다른 터미널에서 테스트
curl http://127.0.0.1:30080/api/fast
~~~

### 5. [메인 실습] Go 부하 테스트

- 이제 화면을 분할하여 한쪽에는 로그를, 한쪽에는 부하 툴을 띄웁니다.

- **실험 A: 확장 성공 (Scale-Out)**

1. 로그 감시:`kubectl logs -f -l app=k8s-demo --max-log-requests=10`

2. 부하 발생:

~~~Bash
cd ../03-load-tester
go run main.go -url=http://<URL>/api/fast -n 100 -c 10
~~~
3. 서버 늘리기: `kubectl scale deployment k8s-demo-app --replicas=5`

- **실험 B: 반전 실패 (The Bottleneck)**

1. 로그 감시: (위와 동일)

2. 부하 발생 (/slow 경로):

~~~Bash
go run main.go -url=http://<URL>/api/slow -n 50 -c 10
~~~
- 관찰: 서버를 10개로 늘려도 여전히 응답이 3초씩 걸리고 전체 완료 속도가 개선되지 않음을 확인합니다.

### 5-1. [메인 실습 ] k6버전 부하 테스트

- k6 다운로드
~~~bash
brew install k6 #mac
choco install k6 #windows
~~~

1. /fast 경로 테스트
~~~bash
# minikube에서 따온 URL을 환경변수로 넣어서 실행
k6 run -e TARGET_URL=http://<MINIKUBE_IP>:<PORT> script.js
~~~

- http_req_duration 확인
- `kubectl scale deployment k8s-demo-app --replicas=5` 실행 후 안정화되는지 확인

2. /slow 경로 테스트
~~~bash
# URL 끝에 /api/slow가 가도록 실행 (스크립트 수정 혹은 환경변수 활용)
k6 run -e TARGET_URL=http://<MINIKUBE_IP>:<PORT>/api/slow script.js
~~~

- http_req_duration 확인
- `kubectl scale deployment k8s-demo-app --replicas=5` 실행 후 안정화되는지 확인

3. 결과 분석
- `http_req_duration`: 요청부터 응답까지 걸린 시간
- `http_reqs`: 초당 처리량, 서버가 얼마나 많은 요청을 버텼는지 보여줌
- `checks`: 성공률, 서버가 터졌다면 여기서 x가 뜸

### 터미널 배치 가이드
Terminal 1 (왼쪽 상단): watch -n 1 kubectl get pods (Pod 수 변화 관찰)

Terminal 2 (오른쪽 상단): kubectl logs -f -l app=k8s-demo (서버 내부 동작 관찰)

Terminal 3 (하단): go run main.go ... (공격 및 결과 확인)4. 모니터링 꿀팁 (CLI 전용)

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
- Go(트래픽 부하 실험용)
- k6(트래픽 부하 실험용)
