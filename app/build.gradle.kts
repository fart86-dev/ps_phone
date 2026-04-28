plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

fun loadEnv(fileName: String) {
  val envFile = rootProject.file(fileName)
  if (envFile.exists()) {
    envFile.readLines().forEach { line ->
      if (line.isNotEmpty() && !line.startsWith("#")) {
        val parts = line.split("=", limit = 2)
        if (parts.size == 2) {
          val key = parts[0].trim()
          val value = parts[1].trim()
          System.setProperty(key, value)
        }
      }
    }
  }
}

// google-services.json이 있을 때만 Google Services 플러그인 적용
if (file("google-services.json").exists()) {
  apply(plugin = "com.google.gms.google-services")
}

android {
  namespace = "com.modooshuttle.ps_phone"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.modooshuttle.ps_phone"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    val isBuildingProd = gradle.startParameter.taskNames.any {
      it.contains("Prod", ignoreCase = true)
    }
    val envFileName = if (isBuildingProd) ".env.production" else ".env.development"
    loadEnv(envFileName)

    val notionApiToken = System.getProperty("NOTION_API_TOKEN") ?: project.findProperty("NOTION_API_TOKEN") as? String ?: ""
    val notionDatabaseId = System.getProperty("NOTION_DATABASE_ID") ?: project.findProperty("NOTION_DATABASE_ID") as? String ?: ""
    val awsAccessKeyId = System.getProperty("AWS_ACCESS_KEY_ID") ?: project.findProperty("AWS_ACCESS_KEY_ID") as? String ?: ""
    val awsSecretAccessKey = System.getProperty("AWS_SECRET_ACCESS_KEY") ?: project.findProperty("AWS_SECRET_ACCESS_KEY") as? String ?: ""
    val awsS3BucketName = System.getProperty("AWS_S3_BUCKET_NAME") ?: project.findProperty("AWS_S3_BUCKET_NAME") as? String ?: ""
    val awsS3Region = System.getProperty("AWS_S3_REGION") ?: project.findProperty("AWS_S3_REGION") as? String ?: ""

    buildConfigField("String", "NOTION_API_TOKEN", "\"$notionApiToken\"")
    buildConfigField("String", "NOTION_DATABASE_ID", "\"$notionDatabaseId\"")
    buildConfigField("String", "AWS_ACCESS_KEY_ID", "\"$awsAccessKeyId\"")
    buildConfigField("String", "AWS_SECRET_ACCESS_KEY", "\"$awsSecretAccessKey\"")
    buildConfigField("String", "AWS_S3_BUCKET_NAME", "\"$awsS3BucketName\"")
    buildConfigField("String", "AWS_S3_REGION", "\"$awsS3Region\"")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  flavorDimensions.add("environment")
  productFlavors {
    create("dev") {
      dimension = "environment"
      applicationIdSuffix = ".dev"
    }
    create("prod") {
      dimension = "environment"
      applicationIdSuffix = ".prod"
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions {
    jvmTarget = "11"
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
}

dependencies {

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.fragment)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Firebase
  implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
  implementation("com.google.firebase:firebase-messaging")

  // Network
  implementation("com.squareup.okhttp3:okhttp:4.11.0")

  // JSON
  implementation("org.json:json:20231013")

  // Coroutines
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

  // AWS SDK for Android
  implementation("com.amazonaws:aws-android-sdk-core:2.52.0")
  implementation("com.amazonaws:aws-android-sdk-s3:2.52.0")
}