# kubernetes-study

## introduction
- 멋쟁이 사자처럼 14기 밴엔드 세션 자료입니다.
- 쿠버네티스 세션 관련 자료입니다. 느리게 설계된 서버(/slow)와, 빠르게 설계된 서버(/fast) 이 두개가 같은 쿠버네티스 환경에서 어떻게 돌아가는지
golang으로 작성된 트래픽 부하 툴을 이용하여 확인하는 프로젝트입니다.

## features
1. 01-app/: 자바 소스 파일과, 도커 빌드 파일이 있습니다.
- 사용 방법
- 이미지 빌드 및 푸시
```
cd 01-app
./gradlew bootBuildImage --imageName="your-docker-id"/k8s-demo-app
docker push "your-docker-id/k8s-demo-app"
```
2. 02-k8s-manifests: 쿠버네티스 설정 파일입니다, deployment와 service정의 파일이 있습니다.
- 서버 띄우기
```
cd ../02-k8s-manifests
kubectl apply -f -01-deployment.yaml
kubectl apply -f -02-service.yaml
```

- 서비스 접속 주소 확인
```
kubectl get svc k8s-demo-service
```

- 정상 확인(curl사용)
```
curl http://"service-ip"/api/fast
```

3. 03-load-tester: 트래픽 발생 소스가 있습니다.
- 사용 방법
- `main.go`에 트래픽을 몰리게 할 서버의 주소를 적고 아래의 명령어로 트래픽 부하를 시키면 됩니다.
```
cd ../03-load-tester
go run main.go
```

- 실시간 모니터링(다른 터미널 켜서)
```
watch kubectl get pods
```

- 수동 확장 실험(Replica 늘리기)
```
kubectl scale deployment k8s-demo-app --replicas=5
```

- 추가 꿀 명령어
1. pod 변화 관찰
```
watch -n 1 "kubectl get pods | grep k8s-demo"
```

2. 실시간 로그
```
kubectl logs -f -l app=k8s-demo-app --max-log-requests=10
```

3. 리소스 사용량(Metric Server 설치 시 사용 가능?)
```
kubectl top pods
```

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
