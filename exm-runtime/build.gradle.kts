plugins { id("com.android.library"); id("org.jetbrains.kotlin.android") }

android { namespace = "studio.exmera.exm"; compileSdk = 35
    defaultConfig { minSdk = 26 }
}

dependencies { implementation("androidx.core:core-ktx:1.15.0") }
