-- ============================================================================
-- V1__init_users.sql
-- 목적: 사용자 테이블과 JWT 재발급 토큰 테이블을 만든다. (기획서 06 "DB 설계" users · refresh_tokens)
--
-- 이 마이그레이션이 첫 번째인 이유:
--   모든 다른 테이블(prayers, friendships, ...)이 users.id를 참조한다. 기반이 먼저 있어야 한다.
--   기도제목·친구 등은 Sprint 1~3에서 V2, V3... 로 순서대로 추가한다.
--
-- 규칙:
--   - Flyway는 한 번 적용된 파일을 다시 실행하지 않는다. 이미 커밋된 마이그레이션은 절대 수정하지 말고
--     새 번호의 파일을 추가한다.
--   - 모든 테이블·컬럼에 COMMENT ON 을 남긴다(기획서 08 코드 규칙). DB 도구에서 바로 의미를 볼 수 있다.
--   - 기본 키는 UUID. 순번(serial)은 "다음 사용자 ID가 뭔지" 추측 가능해 URL에 노출하기 나쁘다.
-- ============================================================================

-- gen_random_uuid()는 PostgreSQL 13+ 내장이라 확장 설치가 필요 없다.

CREATE TABLE users (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    google_sub        VARCHAR(255) NOT NULL,
    email             VARCHAR(320) NOT NULL,
    handle            VARCHAR(30)  NOT NULL,
    display_name      VARCHAR(50)  NOT NULL,
    avatar_url        TEXT,
    reminder_enabled  BOOLEAN      NOT NULL DEFAULT TRUE,
    reminder_time     TIME         NOT NULL DEFAULT '21:00',
    notif_prefs       JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,

    -- 같은 Google 계정으로 두 번 가입할 수 없다.
    CONSTRAINT uq_users_google_sub UNIQUE (google_sub),
    -- @아이디는 앱 전체에서 유일. 대소문자 구분 없이 유일해야 하므로 아래 함수 인덱스로 보강한다.
    CONSTRAINT uq_users_handle UNIQUE (handle),
    -- 아이디 형식: 영문 소문자·숫자·밑줄, 3~30자. 앱과 서버 검증이 뚫려도 DB가 마지막 방어선.
    CONSTRAINT ck_users_handle_format CHECK (handle ~ '^[a-z0-9_]{3,30}$')
);

COMMENT ON TABLE  users                  IS '앱 사용자. Google 로그인으로만 생성된다. 비밀번호는 저장하지 않는다.';
COMMENT ON COLUMN users.id               IS '내부 식별자(UUID). API 응답과 다른 테이블의 FK에 사용.';
COMMENT ON COLUMN users.google_sub       IS 'Google ID 토큰의 sub 클레임. Google이 보장하는 불변 계정 ID. 이메일은 바뀔 수 있어 이것으로 매칭한다.';
COMMENT ON COLUMN users.email            IS 'Google 계정 이메일. 표시·연락용. 로그인 매칭에는 쓰지 않는다.';
COMMENT ON COLUMN users.handle           IS '@아이디. 친구 검색에 사용. 소문자 영숫자·밑줄 3~30자, 전체 유일.';
COMMENT ON COLUMN users.display_name     IS '화면에 보이는 닉네임. 중복 허용.';
COMMENT ON COLUMN users.avatar_url       IS '프로필 사진 URL. 첫 로그인 시 Google 프로필 사진으로 채운다. 없을 수 있음.';
COMMENT ON COLUMN users.reminder_enabled IS '데일리 기도 리마인더 on/off. 기본 켬. (기획서 05 알림 설계)';
COMMENT ON COLUMN users.reminder_time    IS '리마인더 시각(사용자 기기 로컬 시각 기준). 기본 21:00. 실제 알림은 기기가 예약하고 서버는 값만 보관.';
COMMENT ON COLUMN users.notif_prefs      IS '알림 종류별 on/off. 예: {"prayed":true,"answered":true,"comment":true,"friend":false}. 키가 없으면 켠 것으로 간주.';
COMMENT ON COLUMN users.created_at       IS '가입 시각(UTC).';
COMMENT ON COLUMN users.updated_at       IS '마지막 수정 시각(UTC). 애플리케이션이 갱신한다.';
COMMENT ON COLUMN users.deleted_at       IS '탈퇴 요청 시각. NULL이면 활성. 값이 있으면 30일 유예 후 스케줄러가 완전 삭제한다. (기획서 11 데이터·탈퇴)';

-- 아이디 검색은 대소문자 무시로 동작해야 한다("Jinsung"으로 검색해도 "jinsung"이 나와야 함).
CREATE UNIQUE INDEX uq_users_handle_lower ON users (lower(handle));

-- 탈퇴 유예 계정 정리 스케줄러가 매일 조회하는 조건.
CREATE INDEX ix_users_deleted_at ON users (deleted_at) WHERE deleted_at IS NOT NULL;


CREATE TABLE refresh_tokens (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash   CHAR(64)     NOT NULL,
    device_info  VARCHAR(200),
    expires_at   TIMESTAMPTZ  NOT NULL,
    revoked_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uq_refresh_tokens_hash UNIQUE (token_hash)
);

COMMENT ON TABLE  refresh_tokens             IS 'JWT 재발급(refresh) 토큰. access 토큰(30분)이 만료되면 이걸로 새 토큰을 받는다. (기획서 07 인증)';
COMMENT ON COLUMN refresh_tokens.user_id     IS '토큰 주인. 사용자가 완전 삭제되면 토큰도 함께 삭제(CASCADE).';
COMMENT ON COLUMN refresh_tokens.token_hash  IS '토큰 원문의 SHA-256 해시(16진수 64자). 원문은 저장하지 않아 DB가 유출돼도 토큰을 재사용할 수 없다.';
COMMENT ON COLUMN refresh_tokens.device_info IS '발급 기기 설명(예: "Galaxy S24 · Android 15"). 사용자가 로그인 기기 목록을 볼 때 표시.';
COMMENT ON COLUMN refresh_tokens.expires_at  IS '만료 시각. 발급 후 30일. 지나면 재로그인 필요.';
COMMENT ON COLUMN refresh_tokens.revoked_at  IS '폐기 시각. 로그아웃하거나 재발급으로 교체되면 채운다. NULL이면 유효.';
COMMENT ON COLUMN refresh_tokens.created_at  IS '발급 시각.';

-- 사용자별 유효 토큰 조회(로그아웃 전체 처리, 기기 목록)에 사용.
CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);
-- 만료 토큰 정리 스케줄러용.
CREATE INDEX ix_refresh_tokens_expires_at ON refresh_tokens (expires_at);
