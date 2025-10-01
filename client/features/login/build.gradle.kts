

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    //alias(libs.plugins.kotlin.android)

    alias(libs.plugins.jetbrainsCompose)

    alias(libs.plugins.compose.compiler) // ✅ Uncommented - Required for Kotlin 2.0+

    //id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlin-parcelize")
}

kotlin {



    /* listOf(
         iosX64(),
         iosArm64(),
         iosSimulatorArm64()
     ).forEach { iosTarget ->
         iosTarget.compilations.getByName("main") {
             cinterops {
                 val professional_list by creating {
                     defFile(project.file("src/nativeInterop/cinterop/professional_list.def"))
                     // Add the framework search path
                     compilerOpts("-framework", "MeetingLawyers")
                     // Specify the framework location
                     compilerOpts("-F${project.rootDir}/path/to/frameworks")
                 }
             }
         }
     }*/

    /*iosX64 {
        binaries.framework {
            baseName = "shared"
            export(project(":presentation"))
        }

        compilations["main"].cinterops {
            val professional_list by creating {
                defFile = project.file("src/iosMain/c_interop/professional_list.def")
                compilerOpts("-I${project.projectDir}/src/iosMain/include")
            }
        }
    }

    iosArm64 {
        binaries.framework {
            baseName = "shared"
            export(project(":presentation"))
        }

        compilations["main"].cinterops {
            val professional_list by creating {
                defFile = project.file("src/iosMain/c_interop/professional_list.def")
                compilerOpts("-I${project.projectDir}/src/iosMain/include")
            }
        }
    }

    iosSimulatorArm64 {
        binaries.framework {
            baseName = "shared"
            export(project(":presentation"))
        }

        compilations["main"].cinterops {
            val professional_list by creating {
                defFile = project.file("src/iosMain/c_interop/professional_list.def")
                compilerOpts("-I${project.projectDir}/src/iosMain/include")
            }
        }
    }*/


    jvmToolchain(17)

    applyDefaultHierarchyTemplate()  // 🔑 Requerido en Kotlin ≥1.9.20

    androidTarget()

    /*iosX64 {
        binaries.framework {
            baseName = "DoctoriPresentation"
            //export(project(":presentation"))
        }
    }*/
    //iosArm64 {
        /*binaries.framework {
            baseName = "DoctoriPresentation"
            //export(project(":presentation"))
        }*/
    //}
    //iosSimulatorArm64 {
       /* binaries.framework {
            baseName = "DoctoriPresentation"
            //export(project(":presentation"))
        }*/
   // }

//    cocoapods {
//        summary = "KMP App with MeetingLawyers"
//        homepage = "https://isalud.com"
//        ios.deploymentTarget = "14.0"
//        version = "1.12.1"
//
//        // Add pod sources
//        specRepos {
//            url("https://cdn.cocoapods.org/")
//            url("https://bitbucket.org/meetinglawyers/ios-cocoapods-specs.git")
//        }
//
//
//        framework {
//            //baseName = "shared"
//            baseName = "DoctoriPresentation"
//            isStatic = true
//        }
//
//        // If you need to add pods
//        pod("MeetingLawyers") {
//            version = "2.0.6"
//            // Configuración para evitar problemas con cinterop
//            extraOpts = listOf(
//                "-compiler-option", "-fmodules",
//                "-compiler-option", "-fcxx-modules",
//                "-compiler-option", "-fmodule-name=MeetingLawyers",
//                "-compiler-option", "-DKOTLIN_NATIVE_COCOAPODS_POD"
//            )
//            // Si el pod es solo Objective-C/Swift, desactiva cinterop
//            linkOnly = true
//        }
//    }

    sourceSets {

        //  val mlVersion = libs.versions.meetingLawyers.get()   // ← recupera la versión del catálogo


        val commonMain by getting {
            //resources.srcDirs("src/commonMain/resources")
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(projects.shared)

                implementation(libs.koin.android)
                implementation(libs.koin.compose)
                implementation(libs.navigationCompose)
                implementation(libs.coreKtx)
                //implementation(project.dependencies.platform(libs.kotlinBom))
                implementation(libs.lifecycleRuntime)
                implementation(libs.lifecycleViewModelCompose)
                implementation(libs.coroutinesCore)
                implementation(libs.coroutinesAndroid)
                implementation(libs.retrofit)
                implementation(libs.converterGson)
                implementation(libs.okhttp)
                implementation(libs.okhttpLoggingInterceptor)

                implementation(libs.mqtt)

                // implementation(libs.maplibreSdkExtensions)
                implementation(libs.accompanistPermissions)

                implementation(libs.realm)
            }
        }
        val androidMain by getting {



            dependencies {


                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)

                implementation("org.maplibre.gl:android-sdk:11.3.0")
                implementation("org.maplibre.gl:android-plugin-annotation-v9:3.0.0")

                implementation(libs.androidx.datastore.core.android)

                implementation("androidx.datastore:datastore-preferences:1.1.2")
                implementation("androidx.datastore:datastore-core:1.1.2")

                implementation(libs.realm)
                implementation(libs.compose.ui)
                implementation(libs.compose.ui.tooling)
                implementation("androidx.compose.material3:material3:1.3.1")

                implementation(libs.koin.androidx.compose)

                // implementation(libs.realm)

                //   implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
                //   implementation("com.squareup.moshi:moshi-kotlin:1.15.0")

                /*implementation("com.meetinglawyers:sdk:$mlVersion") {
                    exclude(group = "io.insert-koin")
                }*/
//                implementation("com.meetinglawyers:sdk:$mlVersion") {
//                    // Mantén Koin fuera…
//                    exclude(group = "io.insert-koin")
//
//                    /* ⬇️  EXCLUIMOS los converters que sobran */
//                    exclude(group = "com.squareup.retrofit2", module = "converter-moshi")
//                    exclude(group = "com.squareup.retrofit2", module = "adapter-rxjava2")
//                    // Moshi-kotlin ya no será traído en tránsito
//                }

                //        compileOnly("org.jetbrains:annotations:23.0.0")
                //       implementation("org.jetbrains:annotations:23.0.0")

                //implementation(project(":core"))
                //      implementation(project(":domain"))

                //       implementation(libs.bundles.android)

                //       implementation(libs.meeting.doctors)
                //       implementation(libs.meeting.doctors.video)
                //implementation(libs.meeting.lawyers)

                //implementation(files("../koin3-shadow/build/libs/koin3-shadow.jar"))
                //implementation(project(":koin3-shadow", configuration = "shadow"))
                //     implementation(project(":koin3-shadow", configuration = "shadowRuntimeElements"))


                /*implementation(project(":meeting-lawyers-wrapper")) {
                    isTransitive = false
                }*/




                /*configurations.all {
                    exclude(group = "org.jetbrains", module = "annotations")
                    exclude(group = "org.intellij", module = "annotations")
                }*/

                /* implementation("com.meetinglawyers:sdk:$mlVersion") {
                     exclude(group = "io.insert-koin")
                 }*/

                /*implementation("com.meetinglawyers:sdk:$mlVersion") {
                    exclude(group = "io.insert-koin")
                }*/

                /* implementation(libs.meeting.lawyers.get()) {
                     // quita TODO el grupo de Koin
                     exclude(group = "io.insert-koin")

                     // …o, si quieres ser fino:
                     // exclude(group = "io.insert-koin", module = "koin-core")
                     // exclude(group = "io.insert-koin", module = "koin-android")
                     // exclude(group = "io.insert-koin", module = "koin-androidx-compose")
                 }*/

                /*
                                implementation(libs.bundles.android)
                                implementation(libs.bundles.compose)

                                implementation(libs.dagger)
                                implementation(libs.play.services.auth)
                                implementation(libs.play.services.auth.api.phone)

                                implementation(libs.accompanist.pager)
                                implementation(libs.coil)
                                implementation(libs.composeMaterial3)
                                implementation("com.github.poovamraj:PinEditTextField:1.2.6")
                                implementation("androidx.compose.material:material-icons-extended:1.7.8")
                                implementation(libs.bundles.retrofit)*/
            }
        }
        /*val iosMain by getting {
            dependencies {
                //implementation(libs.bundles.ios)

                //implementation(project(":shared"))
            }
        }*/
    }
}


android {
    namespace = "com.croniot.client.features.login"
    //compileSdk = 35
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    /* defaultConfig {
         applicationId = "com.croniot.presentation"
         minSdk = 33
         targetSdk = 35
         versionCode = 1
         versionName = "1.0"

         testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
     }*/


    defaultConfig {
       // minSdk = 21 // o el que uses en el proyecto (no necesitas targetSdk en libs)
        libs.versions.android.minSdk.get().toInt()
        // NO applicationId en módulos library
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    /* kotlinOptions {
         jvmTarget = "21"
     }*/
}

dependencies {
    implementation(projects.client.core)
    implementation(projects.client.data)
    implementation(projects.client.domain)
    implementation(projects.client.presentation)


    implementation(projects.shared)


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}