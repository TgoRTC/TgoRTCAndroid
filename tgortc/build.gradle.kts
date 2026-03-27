plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

val libVersion = (project.findProperty("version") as? String)
    ?.takeIf { it.isNotBlank() && it != "unspecified" }
    ?: "1.0.1-local"
val libGroupId = "com.tgo.rtc"
val libArtifactId = "tgortc"

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = libGroupId
                artifactId = libArtifactId
                version = libVersion

                pom {
                    name.set("TgoRTC SDK")
                    description.set("A Kotlin SDK for audio and video calling based on LiveKit")
                }
            }
        }
        repositories {
            maven {
                name = "local"
                url = uri("${rootProject.projectDir}/repo")
            }
        }
    }
}

android {
    namespace = "com.tgo.rtc"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    // LiveKit Android SDK
    api("io.livekit:livekit-android:2.24.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
