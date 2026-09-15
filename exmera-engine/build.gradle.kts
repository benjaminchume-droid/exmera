plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }

android { namespace = "studio.exmera.engine"; compileSdk = 35
    defaultConfig { minSdk = 26; consumerProguardFiles("consumer-rules.pro") }
}

dependencies { implementation(project(":exm-runtime")); implementation("androidx.core:core-ktx:1.15.0") }
