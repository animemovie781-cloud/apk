/*
 *  This file is part of AndroidCodeStudio.
 *
 *  AndroidCodeStudio is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidCodeStudio is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidCodeStudio.  If not, see <https://www.gnu.org/licenses/>.
 */

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":core:projects"))
    implementation(project(":logging:logger"))
    implementation(project(":utilities:shared"))
    implementation(project(":utilities:preferences"))

    implementation(libs.common.kotlin.coroutines.core)

    // ECJ for Java compilation
    implementation(libs.ecj)

    // D8/R8
    implementation(libs.bundles.shrinker)

    // ZipAlign
    implementation(libs.zipalign.java)

    // apksig
    implementation(libs.apksig)

    // Kotlin compiler embedded
    compileOnly(libs.kotlin.compiler)
    implementation(libs.bundles.kotlin.compiler)
}
