# praylist-server

기도제목 기록·공유 앱 **프레이리스트(Praylist)** 의 백엔드 API 서버입니다.
Kotlin + Spring Boot 4 + PostgreSQL 16. 안드로이드 앱 저장소는 [praylist-android](https://github.com/heejuring/praylist-android) 입니다.

> 기획서(피그마): https://www.figma.com/design/1I3QmfCbmJYqQJKZqBQEVX
> 설계 결정 기록: [docs/adr](docs/adr)

## 구조 한눈에

```
Android 앱 ──HTTPS/JSON/JWT──▶ Spring Boot API ──JPA──▶ PostgreSQL 16
                                   │  ├─ Flyway: 스키마 마이그레이션 (src/main/resources/db/migration)
                                   │  ├─ Spring Security: JWT 인증 (Sprint 1)
                                   │  └─ Spring Events → FCM 푸시 (Sprint 4)
                                   └─ Cloudflare R2: 사진·영상은 서명 URL로 앱이 직접 업로드 (Sprint 2)
```

패키지는 기능(도메인) 단위로 나눕니다: `health`, `auth`, `user`, `prayer`, `friend`, `comment`, `notification` …
각 패키지 안에 `controller / service / domain(엔티티) / repository / dto` 를 둡니다.

## 로컬 실행

필요한 것: JDK 21, Docker Desktop(실행 중이어야 함). Gradle은 wrapper가 내려받습니다.

```bash
# 1) PostgreSQL 띄우기 (처음 한 번 이미지 다운로드)
docker compose up -d

# 2) 서버 실행 (Flyway가 스키마를 자동 생성)
./gradlew bootRun

# 3) 확인
#   http://localhost:8080/api/v1/health      → {"status":"UP", ...}
#   http://localhost:8080/swagger-ui.html    → API 문서
```

IntelliJ에서는 프로젝트를 열고 `PraylistServerApplication.kt` 의 ▶ 버튼을 누르면 됩니다.

## 검사와 테스트

```bash
./gradlew check        # ktlint + detekt + 테스트 (CI와 동일)
./gradlew test         # 테스트만. Testcontainers가 postgres:16 컨테이너를 자동으로 띄웁니다
./gradlew ktlintFormat # 서식 자동 수정
./gradlew detekt       # 정적 분석만. 리포트: build/reports/detekt/detekt.html
```

## 코드 규칙 (요약 — 전체는 기획서 08)

이 프로젝트는 포트폴리오이므로 **상세 주석이 머지 조건**입니다. detekt가 CI에서 강제합니다.

1. 모든 `public` 클래스·함수·프로퍼티에 KDoc (`/** */`). 역할 · `@param` · `@return` · `@throws`.
2. 파일 상단에 "이 파일이 왜 존재하는지" 한 단락 + 관련 기획서 섹션/화면 번호.
3. 복잡한 분기·날짜 계산·권한 규칙에는 **왜** 그렇게 했는지 인라인 주석. 자명한 코드에 자명한 주석은 금지.
4. 컨트롤러는 `@Operation(summary, description)` 필수 → Swagger가 곧 API 문서.
5. Flyway SQL은 모든 테이블·컬럼에 `COMMENT ON`.
6. `TODO`/`FIXME`는 이슈 번호와 함께: `// TODO(#12): ...`
7. 설계 결정은 `docs/adr/NNNN-제목.md` 로 남깁니다.

## 브랜치

`main`(배포) ← `dev`(통합) ← `feature/이슈번호-설명`. PR 템플릿의 체크리스트를 채운 뒤 머지합니다.

## 환경 변수

| 변수 | 기본값(로컬) | 설명 |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/praylist` | PostgreSQL JDBC URL |
| `DB_USER` | `praylist` | DB 사용자 |
| `DB_PASSWORD` | `praylist-local` | DB 비밀번호 (운영은 반드시 교체) |
| `SERVER_PORT` | `8080` | HTTP 포트 |

Sprint 1부터 `JWT_SECRET`, `GOOGLE_CLIENT_ID` 가 추가됩니다.
