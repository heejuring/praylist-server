# ADR-0001: BaaS(Supabase) 대신 자체 Spring Boot 서버를 운영한다

- 상태: 채택 (2026-09-11)
- 관련: 기획서 07 "기술 스택 & 아키텍처"

## 배경

프레이리스트는 1인 개발 프로젝트이자 안드로이드·백엔드 취업 포트폴리오다.
초안(v1.0~v1.1)에서는 Supabase(BaaS)로 서버 없이 만들 계획이었다.
BaaS는 일정이 약 4주 짧고 운영 부담이 없지만, "REST API 설계·인증·배포·운영을
직접 해 봤는가"라는 질문에 답할 수 없다.

## 결정

Kotlin + Spring Boot 4 서버를 직접 만들고, PostgreSQL과 함께 Docker Compose로
무료 VM(Oracle Cloud Always Free, 대체안 Koyeb/Render)에 배포한다.
앱은 DB에 직접 접근하지 않고 이 서버의 API만 호출한다.

## 대안과 기각 이유

| 대안 | 기각 이유 |
|---|---|
| Supabase (BaaS) | 백엔드 역량을 보여줄 수 없음. 특정 서비스 종속. |
| Node.js (Express/Nest) 서버 | 앱(Kotlin)과 언어가 달라 학습 부담 2배. 국내 백엔드 채용은 Spring 비중이 큼. |
| Firebase | Supabase와 같은 이유 + NoSQL이라 관계(친구·함께 기도 연결) 표현이 불편. |

## 결과

- 장점: 앱·서버 모두 Kotlin, 면접에서 설명 가능한 전체 구조, iOS(v2)에서 서버 재사용.
- 비용: 일정 +4주(18주), 운영 책임(백업·모니터링)을 직접 진다. 무료 VM 확보 실패 리스크는
  기획서 11에 대응책을 적어 두었다.
