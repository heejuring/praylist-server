/*
 * 파일 목적: Swagger UI(/swagger-ui.html)에 표시될 API 문서의 제목·설명·인증 방식을 정의한다.
 *
 * 이 파일이 존재하는 이유:
 *  - 기획서 08 "코드 규칙"에 따라 API 문서는 별도 위키가 아니라 코드(@Operation 애노테이션)에서
 *    자동 생성한다. 이 설정은 그 문서의 표지에 해당한다.
 *  - JWT 인증 방식을 여기 등록해 두면 Swagger UI에서 "Authorize" 버튼으로 토큰을 넣고
 *    보호된 API를 바로 눌러 볼 수 있다. (Sprint 1부터 실제로 사용)
 */
package com.praylist.server.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * springdoc-openapi가 읽어 가는 OpenAPI 최상위 메타데이터 설정.
 */
@Configuration
class OpenApiConfig {
    /**
     * API 문서의 제목·버전과 "bearerAuth"라는 이름의 JWT 보안 스킴을 등록한다.
     *
     * 왜 `addSecurityItem`을 전역으로 거는가:
     *  - 이 서버의 API는 헬스체크를 빼면 전부 로그인이 필요하다. 전역으로 걸어 두면
     *    컨트롤러마다 반복해서 적지 않아도 된다. 공개 API는 개별적으로 예외 표시한다.
     *
     * @return springdoc이 사용할 [OpenAPI] 객체.
     */
    @Bean
    fun praylistOpenApi(): OpenAPI {
        val schemeName = "bearerAuth"
        return OpenAPI()
            .info(
                Info()
                    .title("Praylist API")
                    .description(
                        "기도제목 기록·공유 앱 프레이리스트의 백엔드 API. " +
                            "모든 엔드포인트는 헬스체크를 제외하고 JWT Bearer 토큰이 필요하다.",
                    ).version("v1"),
            ).components(
                Components().addSecuritySchemes(
                    schemeName,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
            ).addSecurityItem(SecurityRequirement().addList(schemeName))
    }
}
