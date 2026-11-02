plugins {
    id("com.android.application")
}

// Local verification only: -PapiUrl=http://127.0.0.1:8765/graphdata points the app at a mock server.
val apiUrl = (findProperty("apiUrl") as String?)
    ?: "https://production.dataviz.cnn.io/index/fearandgreed/graphdata"

android {
    namespace = "app.feargreed"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.feargreed"
        minSdk = 26
        targetSdk = 37
        versionCode = 6
        versionName = "0.5.0"
        buildConfigField("String", "API_URL", "\"$apiUrl\"")
        manifestPlaceholders["cleartext"] = apiUrl.startsWith("http://").toString()
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
