## 무엇을

<!-- 이 PR이 바꾸는 것을 한두 문장으로 -->

## 왜

<!-- 기획서 섹션/화면 번호(예: 06 DB, S2 홈)와 이슈 번호 -->

Closes #

## 어떻게 확인했나

- [ ] `./gradlew check` 통과 (ktlint · detekt · 테스트)
- [ ] 로컬에서 `bootRun` 후 Swagger로 직접 호출
- [ ] 권한 테스트: 다른 사용자로 접근 시 404/403 확인 (해당 시)

## 코드 규칙 체크 (기획서 08)

- [ ] 새로 만든 public 클래스·함수·프로퍼티에 KDoc이 있다
- [ ] 파일 상단에 "이 파일이 왜 존재하는지" 주석이 있다
- [ ] 컨트롤러에 `@Operation(summary, description)`이 있다
- [ ] Flyway SQL에 `COMMENT ON`이 있다 (해당 시)
- [ ] 설계 결정이 있었다면 `docs/adr/`에 기록했다

## 스크린샷 / 로그

<!-- Swagger 응답, 테스트 결과 등 -->
