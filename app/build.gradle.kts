plugins {
    id("com.android.application")
}

// Local verification only: -PapiBase=http://10.0.2.2:8765 points the app at a mock server.
val apiBase = (findProperty("apiBase") as String?) ?: "https://api.oanor.com/stockfeargreed-api"

android {
    namespace = "app.feargreed"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.feargreed"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "API_BASE", "\"$apiBase\"")
        manifestPlaceholders["cleartext"] = apiBase.startsWith("http://").toString()
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.work:work-runtime:2.11.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
