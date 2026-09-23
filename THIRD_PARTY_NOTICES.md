# Third-Party Notices

This project incorporates components and code from several third-party open-source projects.

| Component | Source Project | License | Copyright | Modified |
|---|---|---|---|---|
| Android Code Studio | Source A | GPL-3.0 | Contributors | Base project |
| AndroidIDE | Upstream | GPL-3.0 | Harsh Shandilya et al. | Yes (fork) |
| Sora Editor | Dependency | Apache 2.0 | Rosemoe | No |
| ECJ (Eclipse Java Compiler) | Dependency | EPL-2.0 | Eclipse Foundation | No |
| D8 / R8 | Dependency | Apache 2.0 | Google | No |
| AAPT2 binary | Android Build Tools | Apache 2.0 | Google | No |
| ZipAlign-Java | Dependency | Apache 2.0 | iyxan23 | No |
| apksig | Dependency | Apache 2.0 | Google | No |
| KotlinCompiler (adapted) | CodeAssist (via Sketchware) | GPL-3.0 | tyron12233 | Yes — re-targeted to ACS interfaces |
| ProGuard | Dependency | GPL-2.0 w/ exception | Guardsquare | No |

## Kotlin Compiler Adaptation

The Kotlin compilation component (`KotlinCompiler.kt`) was adapted from the `CodeAssist` project, originally authored by tyron12233, and is licensed under the GPL-3.0. It was migrated into this project to support the lightweight build pipeline.
