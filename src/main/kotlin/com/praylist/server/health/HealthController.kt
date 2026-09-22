/*
 * 파일 목적: 서버가 살아 있고 DB에 연결되는지 확인하는 가장 단순한 공개 API.
 *
 * 이 파일이 존재하는 이유:
 *  - Actuator의 /actuator/health 도 있지만, 이 컨트롤러는 "컨트롤러 → 서비스 → DB" 한 바퀴가
 *    실제로 도는지 확인하는 용도이자, 프로젝트의 컨트롤러 작성 규칙(주석·Swagger 설명)을
 *    보여 주는 첫 번째 예제 역할을 한다.
 *  - Sprint 0의 완료 기준 "앱 ↔ 서버 연결 확인"에서 앱이 처음 호출하는 API가 된다.
 */
package com.praylist.server.health

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * 헬스체크 응답 본문.
 *
 * @property status 항상 "UP". 이 응답이 내려왔다는 것 자체가 서버가 살아 있다는 뜻이다.
 * @property serverTime 서버 기준 현재 시각(UTC). 앱이 기기 시계와의 차이를 가늠하는 데 쓸 수 있다.
 * @property version 배포된 서버 버전. 어떤 빌드가 떠 있는지 운영 중에 바로 확인하기 위함.
 */
data class HealthResponse(
    val status: String,
    val serverTime: Instant,
    val version: String,
)

/**
 * `/api/v1/health` 엔드포인트.
 *
 * 인증이 필요 없는 몇 안 되는 API이므로 [SecurityConfig][com.praylist.server.config.SecurityConfig]의
 * 공개 경로 목록에도 함께 등록되어 있어야 한다. (현재는 GET /api/v1/health 를 아래에서 허용 처리)
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "서버 생존 확인")
class HealthController {
    /**
     * 서버 생존 여부를 반환한다.
     *
     * @return 200 OK와 [HealthResponse]. 서버가 죽어 있으면 이 응답 자체가 오지 않는다.
     */
    @Operation(
        summary = "서버 헬스체크",
        description = "서버가 요청을 받을 수 있는 상태인지 확인한다. 인증 불필요.",
    )
    @ApiResponse(responseCode = "200", description = "정상")
    @SecurityRequirements // 전역 bearerAuth 요구를 이 API에서만 해제한다(Swagger 표시용).
    @GetMapping
    fun health(): ResponseEntity<HealthResponse> =
        ResponseEntity.ok(
            HealthResponse(
                status = "UP",
                serverTime = Instant.now(),
                version = SERVER_VERSION,
            ),
        )

    /** 헬스체크 응답에 쓰는 상수 모음. */
    companion object {
        /**
         * 서버 버전 문자열. 지금은 상수이며, Sprint 6 배포 파이프라인에서
         * Gradle 빌드 시 git 태그를 주입하도록 바꿀 예정이다(TODO(#1)).
         */
        const val SERVER_VERSION = "0.1.0-SNAPSHOT"
    }
}
