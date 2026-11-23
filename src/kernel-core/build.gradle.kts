plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    testFixturesApi(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
