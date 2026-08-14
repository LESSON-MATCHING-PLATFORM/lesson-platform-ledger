# lesson-platform-ledger

결제 거래 사실을 append-only 원장으로 기록하는 Ledger 서비스입니다.

현재 프로젝트는 서비스 책임과 API 계약을 정의하기 위한 초기 기준선입니다. 결제·환불·취소 거래의 원장 기록, 멱등성 보장, 거래 조회, 보정 거래를 핵심 범위로 둡니다.

## 기술 스택

- Java 21
- Spring Boot 4.1
- Spring MVC
- Spring Data JDBC
- H2 for local tests
- MySQL connector for deployed environments

## 실행

```bash
./gradlew bootRun
```

## 테스트

```bash
./gradlew test
```

## 현재 범위

- Ledger 거래 기록 API
- 거래 및 계정별 원장 조회
- idempotency key 기반 중복 요청 방지
- 기존 원장을 수정하지 않는 reversal 처리

API와 DB 스키마는 Ledger MVP 계약이 확정된 뒤 구현합니다.
