# 실행 환경
FROM eclipse-temurin:17-jre-alpine

# 작업 디렉토리
WORKDIR /app

# 빌드된 파일 복사, gradle빌드 후 생성된 파일명으로
COPY build/lib/*.jar app.jar

# 포트 설정
EXPOSE 8080

# 실행
ENTRYPOINT [ "java", "-jar", "app.jar" ]
