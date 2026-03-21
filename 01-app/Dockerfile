# 1. 실행 환경 (JRE 17 사용)
FROM eclipse-temurin:17-jre

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. 빌드된 jar 파일 복사 (gradle 빌드 후 생성된 파일명 확인 필요)
COPY build/libs/*.jar app.jar

# 4. 포트 설정
EXPOSE 8080

# 5. 실행 (컨테이너 이름 확인을 위해 hostname 정보 활용)
ENTRYPOINT ["java", "-jar", "app.jar"]
