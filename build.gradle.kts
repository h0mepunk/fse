// Top-level build file where you can add configuration options common to all sub-projects/modules.
val localProperties = java.util.Properties()
rootProject.file("local.properties").takeIf { it.exists() }?.reader(Charsets.UTF_8)?.use { localProperties.load(it) }
val fatsecretClientId = localProperties.getProperty("fatsecret_client_id", "")
val fatsecretClientSecret = localProperties.getProperty("fatsecret_client_secret", "")
extra["fatsecretClientId"] = fatsecretClientId
extra["fatsecretClientSecret"] = fatsecretClientSecret

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}