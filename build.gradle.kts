plugins {
	java
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.test"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

val jjwtVersion = "0.12.6"
val mapstructVersion = "1.6.3"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.flywaydb:flyway-mysql")

	implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0")

	implementation("org.mapstruct:mapstruct:$mapstructVersion")
	annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

	developmentOnly("org.springframework.boot:spring-boot-devtools")

	runtimeOnly("com.mysql:mysql-connector-j")
	runtimeOnly("com.google.cloud.sql:mysql-socket-factory-connector-j-8:1.30.0")
	runtimeOnly("com.h2database:h2")

	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
	testAnnotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
