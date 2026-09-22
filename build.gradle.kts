/*
 * 파일 목적: praylist-server의 빌드 정의. 어떤 라이브러리를 쓰는지, 어떤 규칙(주석·스타일)으로
 * 검사하는지가 전부 여기에 있다. 라이브러리를 추가할 때는 반드시 "왜"를 한 줄 주석으로 남긴다.
 *
 * 버전 선택 근거 (2026-09-22 기준):
 *  - Spring Boot 4.1.1: 현재 안정 릴리스. 기획서에는 3.x로 적혀 있으나 착수 시점의 최신 안정판을 쓴다.
 *  - Kotlin 2.3.21: Spring Boot 4.1.1이 관리(BOM)하는 Kotlin 버전과 맞춘다. 다르게 잡으면 경고가 난다.
 *  - JDK 21: LTS. 기획서 07 참조.
 */
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.21"
    // Spring 애노테이션(@Service 등)이 붙은 클래스를 자동 open 처리. 없으면 프록시 생성이 실패한다.
    kotlin("plugin.spring") version "2.3.21"
    // JPA 엔티티에 기본 생성자를 자동 생성. Hibernate가 리플렉션으로 객체를 만들 때 필요하다.
    kotlin("plugin.jpa") version "2.3.21"
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    // 정적 분석. 기획서 08의 "모든 public 심볼 KDoc 필수" 규칙을 CI에서 강제하는 도구.
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    // 코드 포맷 검사. 스타일 논쟁을 없애고 diff를 깨끗하게 유지한다.
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

group = "com.praylist"
version = "0.1.0-SNAPSHOT"
description = "Praylist API server"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
        // JSR-305 애노테이션을 엄격히 해석해 Java 라이브러리의 null 가능성을 Kotlin 타입에 반영한다.
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // ---- 웹 · 직렬화 -------------------------------------------------------------
    implementation("org.springframework.boot:spring-boot-starter-webmvc") // REST 컨트롤러 + 내장 Tomcat
    implementation("org.springframework.boot:spring-boot-starter-validation") // @Valid 요청 검증
    implementation("tools.jackson.module:jackson-module-kotlin") // Kotlin data class ↔ JSON (Jackson 3)

    // ---- 보안 -----------------------------------------------------------------------
    implementation("org.springframework.boot:spring-boot-starter-security") // 필터 체인. JWT는 Sprint 1에서 추가

    // ---- 데이터 -----------------------------------------------------------------------
    implementation("org.springframework.boot:spring-boot-starter-data-jpa") // JPA/Hibernate 리포지토리
    implementation("org.springframework.boot:spring-boot-starter-flyway") // 스키마 마이그레이션 자동 적용
    implementation("org.flywaydb:flyway-database-postgresql") // Flyway 10+는 DB별 모듈이 별도로 필요
    runtimeOnly("org.postgresql:postgresql") // JDBC 드라이버

    // ---- 운영 · 문서 -----------------------------------------------------------------
    implementation("org.springframework.boot:spring-boot-starter-actuator") // /actuator/health 등 운영 엔드포인트
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1") // Swagger UI 자동 생성 (Boot 4용 3.x)

    // ---- 개발 편의 --------------------------------------------------------------------
    developmentOnly("org.springframework.boot:spring-boot-devtools") // 코드 변경 시 자동 재시작

    // ---- 테스트 -----------------------------------------------------------------------
    testImplementation("org.springframework.boot:spring-boot-starter-test") // JUnit 5 + AssertJ + Spring Test
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test") // MockMvc
    testImplementation("org.springframework.boot:spring-boot-starter-security-test") // 인증 상태 흉내
    testImplementation("org.springframework.boot:spring-boot-testcontainers") // @ServiceConnection
    testImplementation("org.testcontainers:testcontainers-postgresql") // 진짜 PostgreSQL 컨테이너로 통합 테스트
    testImplementation("org.testcontainers:testcontainers-junit-jupiter") // @Testcontainers/@Container 로 컨테이너 수명 자동 관리
    testImplementation("com.ninja-squad:springmockk:5.0.1") // Kotlin 친화 목 라이브러리(MockK)의 Spring 통합
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// ---- 정적 분석 설정 ----------------------------------------------------------------
// detekt 1.23.x는 Kotlin 2.0.21로 컴파일되어 있어, 프로젝트의 Kotlin 2.3.21과 같은 클래스패스에서 돌면
// "not supported" 오류로 실패한다. detekt 공식 문서의 안내대로 detekt 전용 설정(classpath)에서만
// Kotlin을 2.0.21로 고정한다. 프로젝트 코드 컴파일에는 영향이 없다.
configurations.matching { it.name == "detekt" }.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion("2.0.21")
        }
    }
}

detekt {
    // 기본 규칙 위에 프로젝트 규칙(주석 필수 등)을 덮어쓴다. 파일은 config/detekt/detekt.yml.
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    // 생성 코드·테스트는 검사 대상에서 뺀다. 테스트는 주석 규칙을 강제하지 않는다(가독성 우선).
    source.setFrom(files("src/main/kotlin"))
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "21"
    reports {
        html.required = true // build/reports/detekt/detekt.html 에서 사람이 읽는 리포트
        sarif.required = true // GitHub Actions 코드 스캔 탭에 올리기 위한 형식
    }
}

ktlint {
    android = false
    // 기존 .editorconfig의 규칙을 따른다. 파일 상단 라이선스 헤더 검사는 하지 않는다.
    filter {
        exclude("**/generated/**")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    // Testcontainers가 Docker를 찾지 못하면 테스트가 통째로 실패한다.
    // 로컬에서 Docker Desktop이 켜져 있어야 한다는 사실을 README에 적어 두었다.
}

// `./gradlew check` 한 번으로 스타일 · 정적 분석 · 테스트가 모두 돌도록 묶는다. CI가 이 태스크를 호출한다.
tasks.named("check") {
    dependsOn("detekt", "ktlintCheck")
}
