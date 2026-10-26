plugins {
    kotlin("jvm") version "2.0.21"
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
    sourceSets {
        main {
            // Compile the real application helper, not a copy of its logic.
            kotlin.srcDir("../Android/app/src/main/java")
            kotlin.include("com/katchy/focuslive/ui/courses/CourseDates.kt")
        }
        test {
            kotlin.srcDir("../Android/app/src/test/java")
            kotlin.include("com/katchy/focuslive/ui/courses/CourseDatesTest.kt")
            kotlin.include("com/katchy/focuslive/ui/courses/CalendarEdgeCasesTest.kt")
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
