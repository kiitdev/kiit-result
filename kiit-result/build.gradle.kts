plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
    id("signing")
}

kotlin {
    jvm()

    androidTarget {
        publishLibraryVariants("release")
    }

    js(IR) {
        browser()
        nodejs()
        binaries.library()
        generateTypeScriptDefinitions()
    }

    listOf(iosArm64(), iosSimulatorArm64(), iosX64()).forEach {
        it.binaries.framework {
            baseName = "KiitResult"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // api, not implementation — kiit-result's public API (Result.status, Outcome<T>'s Err
            // type param, etc.) directly exposes kiit-codes types, so consumers need them
            // transitively on their own compile classpath.
            api("dev.kiit:kiit-codes:0.2.1")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "kiit.result"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

// Single source of truth for the published version — also read by the release workflow via
// the printVersion task below, so the git tag and GitHub release always match what's published.
val libraryVersion = "0.1.0"

/**
 * Store the following in ~/.gradle/gradle.properties
 *
 * signingInMemoryKeyPassword=
 * signingInMemoryKey=
 * signing.gnupg.keyName=
 * signing.gnupg.passphrase=
 *
 * Maven local: ~/.m2/repository/dev/kiit/kiit-result/
 */
mavenPublishing {
    publishToMavenCentral(automaticRelease = true)

    coordinates(
        groupId = "dev.kiit",
        artifactId = "kiit-result",
        version = libraryVersion,
    )
    pom {
        name = "kiit-result"
        description = "Result<T, E> / Outcome monad for typed success and failure, built on kiit-codes"
        url = "https://kiit.dev"
        licenses {
            license {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
            }
        }
        developers {
            developer {
                id = "codehelix"
                name = "CodeHelix"
                url = "https://kiit.dev"
            }
        }
        scm {
            url = "https://github.com/slatekit/kiit-result"
            connection = "scm:git:git://github.com/slatekit/kiit-result.git"
            developerConnection = "scm:git:ssh://git@github.com/slatekit/kiit-result.git"
        }
    }
}

detekt {
    config.setFrom("$projectDir/detekt.yml")
    buildUponDefaultConfig = true
    source.setFrom(
        "src/commonMain/kotlin",
    )
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

// Read by the release workflow (`./gradlew :kiit-result:printVersion -q`) to derive the git tag
// and GitHub release name from the same version published to Maven Central.
tasks.register("printVersion") {
    doLast { println(libraryVersion) }
}
