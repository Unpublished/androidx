/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.build

import androidx.build.dependencyTracker.AffectedModuleDetector
import androidx.build.gradle.getByType
import com.android.build.gradle.internal.dsl.LintOptions
import org.gradle.api.Project
import java.io.File

/**
 * Setting this property means that lint will update lint-baseline.xml if it exists.
 */
private const val UPDATE_LINT_BASELINE = "updateLintBaseline"

/**
 * Property used by Lint to continue creating baselines without failing lint, normally set by:
 * -Dlint.baselines.continue=true from command line.
 */
private const val LINT_BASELINE_CONTINUE = "lint.baselines.continue"

fun Project.configureNonAndroidProjectForLint(extension: AndroidXExtension) {
    apply(mapOf("plugin" to "com.android.lint"))

    // Create fake variant tasks since that is what is invoked by developers.
    val lintTask = tasks.named("lint")
    lintTask.configure { task ->
        AffectedModuleDetector.configureTaskGuard(task)
    }
    tasks.register("lintDebug") {
        it.dependsOn(lintTask)
        it.enabled = false
    }
    tasks.register("lintRelease") {
        it.dependsOn(lintTask)
        it.enabled = false
    }
    addToBuildOnServer(lintTask)

    val lintOptions = extensions.getByType<LintOptions>()
    configureLint(lintOptions, extension)
}

fun Project.configureAndroidProjectForLint(lintOptions: LintOptions, extension: AndroidXExtension) {
    configureLint(lintOptions, extension)
    tasks.named("lint").configure { task ->
        // We already run lintDebug, we don't need to run lint which lints the release variant
        task.enabled = false
    }
    afterEvaluate {
        tasks.named("lintDebug").configure { task ->
            AffectedModuleDetector.configureTaskGuard(task)
        }
    }
}

fun Project.configureLint(lintOptions: LintOptions, extension: AndroidXExtension) {
    project.dependencies.add(
        "lintChecks",
        project.rootProject.project(":lint-checks")
    )

    // If -PupdateLintBaseline was set we should update the baseline if it exists
    val updateLintBaseline = hasProperty(UPDATE_LINT_BASELINE)

    // Lint is configured entirely in afterEvaluate so that individual projects cannot easily
    // disable individual checks in the DSL for any reason. That being said, when rolling out a new
    // check as fatal, it can be beneficial to set it to fatal above this comment. This allows you
    // to override it in a build script rather than messing with the baseline files. This is
    // especially relevant for checks which cause hundreds or more failures.
    afterEvaluate {
        lintOptions.apply {
            isAbortOnError = true
            isIgnoreWarnings = true

            // Workaround for b/177359055 where 27.2.0-beta04 incorrectly computes severity.
            isCheckAllWarnings = true

            // Skip lintVital tasks on assemble. We explicitly run lintRelease for libraries.
            isCheckReleaseBuilds = false

            // Write output directly to the console (and nowhere else).
            textReport = true
            htmlReport = false

            // Format output for convenience.
            isExplainIssues = true
            isNoLines = false
            isQuiet = true

            fatal("VisibleForTests")

            // Disable dependency checks that suggest to change them. We want libraries to be
            // intentional with their dependency version bumps.
            disable("KtxExtensionAvailable")
            disable("GradleDependency")

            // Disable a check that's only relevant for real apps. For our test apps we're not
            // concerned with drawables potentially being a little bit blurry
            disable("IconMissingDensityFolder")

            // Disable a check that's only triggered by translation updates which are
            // outside of library owners' control, b/174655193
            disable("UnusedQuantity")

            // Disable until it works for our projects, b/171986505
            disable("JavaPluginLanguageLevel")

            // Disable the TODO check until we have a policy that requires it.
            disable("StopShip")

            // Provide stricter enforcement for project types intended to run on a device.
            if (extension.type.compilationTarget == CompilationTarget.DEVICE) {
                fatal("Assert")
                fatal("NewApi")
                fatal("ObsoleteSdkInt")
                fatal("NoHardKeywords")
                fatal("UnusedResources")
                fatal("KotlinPropertyAccess")
                fatal("LambdaLast")
                fatal("UnknownNullness")

                // Only override if not set explicitly.
                // Some Kotlin projects may wish to disable this.
                if (
                    severityOverrides!!["SyntheticAccessor"] == null &&
                    extension.type != LibraryType.SAMPLES
                ) {
                    fatal("SyntheticAccessor")
                }

                // Only check for missing translations in finalized (beta and later) modules.
                if (extension.mavenVersion?.isFinalApi() == true) {
                    fatal("MissingTranslation")
                } else {
                    disable("MissingTranslation")
                }
            }

            // If the project has not overridden the lint config, set the default one.
            if (lintConfig == null) {
                // suppress warnings more specifically than issue-wide severity (regexes)
                // Currently suppresses warnings from baseline files working as intended
                lintConfig = project.rootProject.file("buildSrc/lint.xml")
            }

            // Teams shouldn't be able to generate new baseline files or add new violations to
            // existing files; they should only be able to burn down existing violations. That's
            // hard to enforce, though, so we'll just prevent them from creating new ones.
            //
            // If you are working on enabling a new check -- and ONLY if you are working on a new
            // check, then you may need to comment out this line  so that you can suppress all
            // the new failures.
            if (updateLintBaseline) {
                // Continue generating baselines regardless of errors
                isAbortOnError = false
                // Avoid printing every single lint error to the terminal
                textReport = false
                val lintDebugTask = tasks.named("lintDebug")
                lintDebugTask.configure {
                    it.doFirst {
                        lintBaseline.delete()
                    }
                }
                val lintTask = tasks.named("lint")
                lintTask.configure {
                    it.doFirst {
                        lintBaseline.delete()
                    }
                }
                System.setProperty(LINT_BASELINE_CONTINUE, "true")
            }
            baseline(lintBaseline)
        }
    }
}

val Project.lintBaseline get() = File(projectDir, "/lint-baseline.xml")
