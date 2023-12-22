import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.github.detekt.gradle.DetektKotlinCompilerPlugin
import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    alias(libs.plugins.agp) apply false
    alias(libs.plugins.kgp) apply false

    alias(libs.plugins.ksp) apply false

    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.wire) apply false

    alias(libs.plugins.detekt)

    id("jvm-ecosystem")
    alias(libs.plugins.versions)
}

allprojects {
    with(pluginManager) {
        apply(DetektPlugin::class)
        apply(DetektKotlinCompilerPlugin::class)
    }

    extensions.configure<DetektExtension> {
        parallel = true
        enableCompilerPlugin = true
        autoCorrect = true
    }

    val libs = rootProject.libs
    dependencies {
        detektPlugins(libs.detekt.formatting)
        detektPlugins(libs.detekt.compose)
    }
}

tasks.withType<DependencyUpdatesTask> {
    rejectVersionIf {
        !isStable(candidate.version) && isStable(currentVersion)
    }
}

fun isStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    return stableKeyword || regex.matches(version)
}
