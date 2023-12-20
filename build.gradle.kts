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
