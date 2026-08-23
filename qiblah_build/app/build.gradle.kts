plugins {
    id("com.android.application")
}

android {
    namespace = "com.trewx.qiblah"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.trewx.qiblah"
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "4.1"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
