# Source Migration Map

This document maps Sketchware Pro components to their new Android Code Studio targets.

| Sketchware Component | Source File | ACS Target | Target File | License | Adaptation |
|---|---|---|---|---|---|
| ProjectBuilder | `a/a/a/ProjectBuilder.java` | LightweightBuildBackend | `build/lightweight/LightweightBuildBackend.kt` | Source-available → re-implemented | Full rewrite, same tools |
| ResourceCompiler | `mod/jbk/build/compiler/resource/ResourceCompiler.java` | ResourceCompiler | `build/lightweight/resource/ResourceCompiler.kt` | Source-available → re-implemented | Full rewrite |
| DexCompiler | `mod/jbk/build/compiler/dex/DexCompiler.java` | DexCompiler | `build/lightweight/dex/DexCompiler.kt` | Source-available → re-implemented | Full rewrite |
| KotlinCompiler | `mod/hey/studios/compiler/kotlin/KotlinCompiler.kt` | KotlinCompiler | `build/lightweight/kotlin/KotlinCompiler.kt` | **GPL-3.0** (CodeAssist) | Adapted — interfaces retargeted |
| BuildProgressReceiver | `mod/jbk/build/BuildProgressReceiver.java` | BuildProgress | `build/lightweight/BuildProgress.kt` | Source-available → re-implemented | Full rewrite |
| BuildSettings | `mod/hey/studios/build/BuildSettings.java` | BuildInput (partial) | `build/lightweight/BuildInput.kt` | Source-available → re-implemented | Settings collapsed into BuildInput |
| ProjectsAdapter | `com/besome/sketch/adapters/ProjectsAdapter.java` | ProjectsAdapter | `fragments/ProjectsAdapter.kt` | Source-available → re-implemented | Written from scratch |
| Projects Home Screen | Various Sketchware activities | ProjectsHomeFragment | `fragments/ProjectsHomeFragment.kt` | Re-implemented | UX-inspired, not copied |
| ProjectModel (Sketchware) | `com/besome/sketch/beans/ProjectBean.java` | **DISCARDED** | — | — | ACS project model used |
| Templates (Sketchware) | Various | **DISCARDED** | — | — | ACS TemplateRegistry used |
