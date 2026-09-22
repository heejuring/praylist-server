/*
 * 파일 목적: Praylist API 서버의 진입점(main 함수)과 Spring Boot 부트스트랩 클래스.
 *
 * 이 파일이 존재하는 이유:
 *  - Spring Boot는 @SpringBootApplication이 붙은 클래스의 패키지(com.praylist.server)를
 *    기준으로 하위 패키지의 모든 컴포넌트(@Service, @RestController 등)를 자동 탐색한다.
 *    따라서 모든 서버 코드는 반드시 이 패키지 아래에 두어야 한다.
 *  - 기획서 07 "기술 스택 & 아키텍처" 참조: Kotlin + Spring Boot 단일 서버, DB는 PostgreSQL.
 */
package com.praylist.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Praylist API 서버 애플리케이션.
 *
 * 붙어 있는 애노테이션의 의미:
 *  - [SpringBootApplication]: 컴포넌트 스캔 + 자동 설정 + 설정 클래스 선언을 한 번에 켠다.
 *  - [EnableScheduling]: 기획서 06 "서버 이벤트·스케줄러"의 새벽 4시 정리 작업(오래된 알림 삭제,
 *    탈퇴 유예 만료 계정 삭제 등)을 `@Scheduled`로 돌리기 위해 미리 켜 둔다.
 *  - [EnableAsync]: 알림 발송(FCM 호출)은 요청 스레드를 붙잡지 않도록 `@Async`로 처리한다.
 *    Sprint 4에서 NotificationService가 이 기능을 사용한다.
 *
 * 왜 Kotlin의 `class`가 아닌 `open`이 필요 없는가:
 *  - Gradle의 `kotlin("plugin.spring")` 플러그인이 Spring 애노테이션이 붙은 클래스를
 *    자동으로 open 처리해 주므로 별도 키워드가 필요 없다.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
class PraylistServerApplication

/**
 * JVM 진입점. Spring Boot 컨텍스트를 띄우고 내장 Tomcat을 시작한다.
 *
 * @param args 커맨드라인 인자. `--spring.profiles.active=local`처럼 프로필을 넘길 때 사용한다.
 */
fun main(args: Array<String>) {
    runApplication<PraylistServerApplication>(*args)
}
