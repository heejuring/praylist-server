/*
 * 파일 목적: 서버가 실제 PostgreSQL 위에서 정상 기동하는지 확인하는 가장 기본적인 통합 테스트.
 *
 * 확인하는 것:
 *  1. Spring 컨텍스트가 뜬다 (빈 설정 오류 없음).
 *  2. Flyway 마이그레이션이 빈 DB에 성공적으로 적용된다 (SQL 문법 오류 없음).
 *  3. Hibernate의 ddl-auto=validate 가 통과한다 (엔티티 ↔ 테이블 불일치 없음).
 *  4. 공개 헬스체크 API가 인증 없이 200을 준다.
 *
 * 실행 전제: Docker가 떠 있어야 한다. Testcontainers가 postgres:16 컨테이너를 자동으로 띄우고 내린다.
 */
package com.praylist.server

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PraylistServerApplicationTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `컨텍스트가 뜨고 Flyway 마이그레이션이 적용된다`() {
        // 이 메서드 본문이 실행됐다는 것 자체가 컨텍스트 기동 + 마이그레이션 + 스키마 검증 성공을 뜻한다.
        assertThat(postgres.isRunning).isTrue()
    }

    @Test
    fun `헬스체크는 인증 없이 200과 UP을 돌려준다`() {
        mockMvc
            .get("/api/v1/health")
            .andExpect {
                status { isOk() }
                jsonPath("$.status") { value("UP") }
            }
    }

    @Test
    fun `보호된 경로는 인증 없이 401을 돌려준다`() {
        // 아직 어떤 보호 API도 없지만, 존재하지 않는 경로라도 인증이 먼저 걸려야 한다(404보다 401).
        mockMvc
            .get("/api/v1/prayers")
            .andExpect { status { isUnauthorized() } }
    }

    companion object {
        /**
         * 테스트용 PostgreSQL 컨테이너. [ServiceConnection]이 붙어 있으면 Spring Boot가
         * 컨테이너의 접속 정보를 자동으로 datasource 설정에 주입하므로 application.yml을 건드릴 필요가 없다.
         * 운영과 같은 16 버전을 써서 "로컬에서는 됐는데 운영에서 안 되는" SQL 차이를 없앤다.
         */
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }
}
