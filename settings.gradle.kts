pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "testng-root"

plugins {
    `gradle-enterprise`
    id("de.fayard.refreshVersions") version "0.10.0"
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlways()
    }
}

// Sorted by name
include(":testng-ant")
include(":testng-api")
include(":testng-asserts")
include(":testng-bom")
include(":testng-collections")
include(":testng-core")
include(":testng-core-api")
include(":testng-test-kit")

enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
