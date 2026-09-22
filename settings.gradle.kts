/*
 * 파일 목적: Gradle 빌드의 루트 설정. 프로젝트 이름과 플러그인을 어디서 받아올지 정한다.
 * 단일 모듈 프로젝트이므로 include()는 없다.
 */
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "praylist-server"
