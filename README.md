# Emergency Matching Backend

MSA 기반 응급환자 병원 수용 매칭 백엔드 프로젝트입니다.

구급대원이 환자 정보를 입력해 응급 요청을 생성하면, 서버가 환자 위치 기준으로 근처 병원을 조회하고 후보 병원들에게 요청을 전달합니다. 병원이 수락하면 첫 번째 수락 병원을 최종 수용 병원으로 확정하는 구조를 목표로 합니다.

## Project Structure

```text
emergency-matching-backend
├── api-gateway
├── auth-service
├── hospital-service
├── emergency-service
└── notification-service
```

## Services

| Service | Role |
| --- | --- |
| api-gateway | 외부 요청 진입점 및 서비스 라우팅 |
| auth-service | 사용자 인증, 인가, 계정 관리 |
| hospital-service | 병원 등록, 조회, 수용 가능 여부 관리, 위치 기반 병원 검색 |
| emergency-service | 응급 요청 생성, 후보 병원 조회, 병원 응답 상태 관리 |
| notification-service | 응급 요청 및 병원 응답 관련 실시간 알림 처리 |

## Tech Stack

- Java 17
- Spring Boot 3.x
- Spring Cloud
- Gradle Groovy DSL
- PostgreSQL
- RabbitMQ

## Build

```bash
./gradlew clean build
```

## Run

```bash
./gradlew :api-gateway:bootRun
./gradlew :auth-service:bootRun
./gradlew :hospital-service:bootRun
./gradlew :emergency-service:bootRun
./gradlew :notification-service:bootRun
```

Default ports:

| Service | Port |
| --- | --- |
| api-gateway | 8080 |
| auth-service | 8081 |
| hospital-service | 8082 |
| emergency-service | 8083 |
| notification-service | 8084 |
