/*
 * 파일 목적: HTTP 보안 규칙(어떤 URL을 로그인 없이 열어 둘지)을 한곳에 모아 정의한다.
 *
 * 이 파일이 존재하는 이유:
 *  - Spring Security를 의존성에 넣는 순간 모든 URL이 기본으로 잠긴다.
 *    헬스체크와 API 문서(Swagger)만은 로그인 없이 열려 있어야 배포 모니터링과 개발이 가능하다.
 *  - Sprint 1에서 JWT 필터가 이 설정에 추가된다. (기획서 07 "인증" 행 참조)
 */
package com.praylist.server.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint

/**
 * 서버 전체의 보안 필터 체인을 구성한다.
 *
 * 현재(Sprint 0) 정책:
 *  - 아래 [PUBLIC_PATHS]는 인증 없이 허용한다.
 *  - 그 외 모든 요청은 인증을 요구한다. 아직 로그인 기능이 없으므로 401이 돌아온다.
 *  - CSRF 보호는 끈다. 이 서버는 브라우저 세션 쿠키가 아니라 모바일 앱이 보내는
 *    JWT(Authorization 헤더)로 인증하므로 CSRF 공격 표면 자체가 없다.
 *  - 세션은 만들지 않는다(STATELESS). 매 요청을 토큰만으로 판단해야 서버를 여러 대로
 *    늘려도 문제가 없고, 메모리도 아낀다.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {
    /**
     * 보안 필터 체인 빈.
     *
     * @param http Spring Security가 주입하는 빌더. 여기에 규칙을 체이닝해 완성한다.
     * @return 완성된 필터 체인. Spring이 서블릿 필터로 등록한다.
     */
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(HttpMethod.GET, *PUBLIC_PATHS)
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }
            // 기본 로그인 폼·HTTP Basic은 모바일 API에 필요 없으므로 끈다.
            // 켜 두면 401 대신 로그인 페이지 HTML이 내려가 앱이 혼란스러워진다.
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            // 위 두 가지를 끄면 Spring Security는 "어떻게 인증하라고 안내할지" 몰라 기본으로 403을 준다.
            // API 서버의 계약은 "인증 없음 = 401, 권한 없음 = 403"이므로 진입점을 401로 명시한다.
            // 앱은 401을 받으면 토큰 재발급을 시도하고, 403이면 재시도하지 않는다(Sprint 1 인터셉터).
            .exceptionHandling { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) }
        return http.build()
    }

    /** 보안 규칙에서 쓰는 상수 모음. 테스트에서도 같은 목록을 참조할 수 있도록 public으로 둔다. */
    companion object {
        /**
         * 인증 없이 GET을 허용하는 경로 목록.
         *
         *  - `/api/v1/health`: 앱이 서버 연결을 확인하는 자체 헬스체크(HealthController).
         *  - `/actuator/health`: 배포 서버·Uptime Kuma가 살아 있는지 확인하는 용도.
         *  - `/v3/api-docs/` 이하, `/swagger-ui/` 이하: springdoc이 만드는 API 문서.
         *    운영 환경에서는 application-prod.yml에서 springdoc 자체를 끈다.
         */
        val PUBLIC_PATHS =
            arrayOf(
                "/api/v1/health",
                "/actuator/health",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
            )
    }
}
