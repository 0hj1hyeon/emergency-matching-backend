# Gemini Code Assist Style Guide

## Project Overview

이 프로젝트는 MSA 기반 응급환자 병원 수용 매칭 백엔드입니다.

- Java 17
- Spring Boot 3.x
- Gradle Groovy DSL
- PostgreSQL
- RabbitMQ
- WebSocket

서비스 구성은 다음과 같습니다.

- `api-gateway`: 외부 요청 진입점 및 서비스 라우팅을 담당합니다.
- `auth-service`: 사용자 인증, 인가, 계정 관리를 담당합니다.
- `hospital-service`: 병원 등록, 조회, 수용 가능 여부 관리, 위치 기반 병원 검색을 담당합니다.
- `emergency-service`: 응급 요청 생성, 후보 병원 조회, 병원 응답 상태 관리를 담당합니다.
- `notification-service`: 응급 요청 및 병원 응답 관련 실시간 알림을 담당합니다.

## Review Language

- 모든 코드 리뷰 코멘트는 한국어로 작성합니다.
- 기술 용어는 필요한 경우 영어 원문을 함께 사용합니다.
- 리뷰 코멘트는 간결하고 실행 가능한 제안 중심으로 작성합니다.
- 커밋 메시지, PR 제목, 코드 식별자는 원문을 유지합니다.

## Review Priorities

Gemini Code Assist는 리뷰 시 다음 사항을 우선 확인해 주세요.

- 서비스 책임이 섞이지 않았는지 확인합니다.
- 다른 서비스의 도메인 Entity를 직접 참조하거나 JPA 연관관계로 연결하지 않았는지 확인합니다.
- 다른 서비스 데이터는 ID 기반으로만 참조하는지 확인합니다.
- Controller에서 Entity를 직접 반환하지 않는지 확인합니다.
- 요청 DTO와 응답 DTO가 분리되어 있는지 확인합니다.
- Request DTO에 Bean Validation이 적용되어 있는지 확인합니다.
- 새 기능에 테스트 코드가 함께 추가되었는지 확인합니다.
- 외부 인프라 없이 테스트가 실행 가능한지 확인합니다.

## Code Rules

- Controller는 HTTP 요청/응답 처리에 집중하고, 비즈니스 로직은 Service에 둡니다.
- Entity는 영속성 모델로 사용하고 API 응답에 직접 노출하지 않습니다.
- API 응답은 Response DTO로 변환해서 반환합니다.
- Request DTO에는 `@NotNull`, `@NotBlank`, `@Positive`, `@DecimalMin`, `@DecimalMax` 등 적절한 Validation을 적용합니다.
- 도메인 상태값은 문자열이나 숫자 상수 대신 enum으로 표현합니다.
- 서비스 간 데이터 참조는 `hospitalId`, `emergencyRequestId`, `memberId` 같은 ID 값으로 처리합니다.
- MSA 경계를 넘는 직접 Entity 연관관계 사용을 금지합니다.
- Feign, RabbitMQ, WebSocket 연동 코드는 서비스 경계를 명확히 유지합니다.
- 예외 응답은 일관된 형태로 반환합니다.
- 불필요한 전역 리팩터링이나 unrelated change를 피합니다.

## Test Rules

- 기능 추가 시 테스트 코드를 함께 작성합니다.
- Service 테스트는 Mockito 기반 단위 테스트로 작성합니다.
- Controller 테스트는 MockMvc 기반 테스트로 작성합니다.
- 테스트는 외부 PostgreSQL DB에 직접 의존하지 않도록 작성합니다.
- 테스트는 외부 RabbitMQ 브로커에 직접 의존하지 않도록 작성합니다.
- 테스트는 외부 WebSocket 서버나 실제 네트워크에 직접 의존하지 않도록 작성합니다.
- DTO Validation은 성공 케이스와 실패 케이스를 함께 검증하는 것을 권장합니다.
- GitHub Actions에서 `./gradlew test` 또는 `./gradlew build`가 통과해야 합니다.

## Commit Message Rules

커밋 메시지는 다음 형식을 사용합니다.

```text
type: 한글 설명
```

예시:

```text
feat: 병원 기본 API 구현
test: 병원 기본 API 테스트 추가
chore: Gemini Code Assist 설정 추가
```
