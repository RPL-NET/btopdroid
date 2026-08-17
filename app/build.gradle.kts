plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "ca.rplnet.btopwidget"
    compileSdk = 34

    defaultConfig {
        applicationId = "ca.rplnet.btopwidget"
        minSdk = 26
        targetSdk = 34
        versionCode = 22
        versionName = "0.15.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // renomme l'APK de sortie pour inclure la version et le build type,
    // au lieu du generique "app-debug.apk" par defaut
    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                output.outputFileName = "btopdroid-${variant.versionName}-${variant.buildType.name}.apk"
            }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.activity:activity-ktx:1.9.0")
}
