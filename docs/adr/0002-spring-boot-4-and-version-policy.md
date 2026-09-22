# ADR-0002: Spring Boot 4.1과 버전 고정 정책

- 상태: 채택 (2026-09-22)
- 관련: build.gradle.kts

## 배경

기획서(2026-09-11)에는 "Spring Boot 3.x"로 적었지만, 착수일(2026-09-22) 기준 안정판은 4.1.1이다.
Boot 4는 스타터 이름이 일부 바뀌었고(`starter-web` → `starter-webmvc`, 테스트 스타터 분리)
Jackson 3(`tools.jackson`)을 쓴다.

## 결정

- Spring Boot **4.1.1** 로 시작한다. 새 프로젝트가 곧 구버전이 될 이유가 없다.
- Kotlin은 Boot BOM이 관리하는 **2.3.21** 에 맞춘다. 임의로 올리면 컴파일러 경고와 호환 문제가 생긴다.
- JDK **21** (LTS). Gradle **9.7.1**.
- 라이브러리 버전은 가능한 한 Boot BOM에 맡기고, BOM 밖 라이브러리(springdoc, springmockk, detekt 등)만 명시한다.
- 버전 업그레이드는 스프린트 시작일에만 하고, 스프린트 중간에는 하지 않는다.

## 결과

공식 프로젝트 생성기(start.spring.io)가 착수 당일 장애였기 때문에 빌드 파일을 직접 작성했다.
그 덕에 모든 의존성에 "왜 필요한지" 주석이 붙어 있다.
