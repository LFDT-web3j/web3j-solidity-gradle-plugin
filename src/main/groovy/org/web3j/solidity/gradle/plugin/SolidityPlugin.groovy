/*
 * Copyright 2019 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.web3j.solidity.gradle.plugin

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.NodePlugin
import com.github.gradle.node.npm.task.NpmInstallTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.language.jvm.tasks.ProcessResources

import javax.inject.Inject

import static org.codehaus.groovy.runtime.StringGroovyMethods.capitalize
import static org.web3j.solidity.gradle.plugin.SoliditySourceSet.NAME

/**
 * Gradle plugin for Solidity compile automation.
 */
class SolidityPlugin implements Plugin<Project> {

    private final ObjectFactory objectFactory
    private final SourceDirectorySet resolvedSolidity

    @Inject
    SolidityPlugin(final ObjectFactory objectFactory) {
        this.objectFactory = objectFactory
        this.resolvedSolidity = objectFactory.sourceDirectorySet(NAME, "Solidity Sources")
    }

    @Override
    void apply(final Project target) {
        target.pluginManager.apply(JavaPlugin.class)
        target.pluginManager.apply(NodePlugin.class)
        target.extensions.create(SolidityExtension.NAME, SolidityExtension, target)

        final SourceSetContainer sourceSets = target.extensions.getByType(SourceSetContainer.class)

        sourceSets.all { SourceSet sourceSet ->
            configureSourceSet(target, sourceSet)
        }
        // Set nodeProjectDir to build before the node plugin evaluation
        def nodeExtension = target.extensions.getByName(NodeExtension.NAME) as NodeExtension
        nodeExtension.nodeProjectDir = target.objects.directoryProperty().convention(target.layout.buildDirectory)
        nodeExtension.download.set(true)

        target.afterEvaluate {
            sourceSets.all { SourceSet sourceSet ->
                configureSolidityCompile(target, sourceSet, nodeExtension.nodeProjectDir)
                configureAllowPath(target, sourceSet)
                sourceSet.allSource.srcDirs.forEach {
                    resolvedSolidity.srcDir(it)
                }
            }
            configureSolidityResolve(target, nodeExtension.nodeProjectDir)
        }
    }

    /**
     * Add default source set for Solidity.
     */
    private void configureSourceSet(final Project project, final SourceSet sourceSet) {

        def srcSetName = capitalize((CharSequence) sourceSet.name)
        def soliditySourceSet = objectFactory.newInstance(DefaultSoliditySourceSet,
                objectFactory.sourceDirectorySet(NAME, srcSetName + " Solidity Sources"))

        sourceSet.extensions.add(NAME, soliditySourceSet)

        def defaultSrcDir = new File(project.projectDir, "src/$sourceSet.name/$NAME")
        def defaultOutputDir = project.layout.buildDirectory.dir("solidity/$sourceSet.name/$NAME")

        soliditySourceSet.srcDir(defaultSrcDir)
        soliditySourceSet.destinationDirectory.set(defaultOutputDir)

        sourceSet.allJava.source(soliditySourceSet)
        sourceSet.allSource.source(soliditySourceSet)
    }

    /**
     * Configures code compilation tasks for the Solidity source sets defined in the project
     * (e.g. main, test).
     * <p>
     * By default the generated task name for the <code>main</code> source set
     * is <code>compileSolidity</code> and for <code>test</code>
     * <code>compileTestSolidity</code>.
     */
    private static void configureSolidityCompile(final Project project, final SourceSet sourceSet, final nodeProjectDir) {

        def compileTask = project.tasks.create(sourceSet.getTaskName("compile", "Solidity"), SolidityCompile)
        def soliditySourceSet = sourceSet.extensions.getByType(SoliditySourceSet)

        if (!requiresBundledExecutable(project)) {
            // Leave executable as specified by the user
            compileTask.executable = project.solidity.executable
        }
        compileTask.pathRemappings = project.solidity.pathRemappings
        if (soliditySourceSet.getVersion()){
            compileTask.version = soliditySourceSet.getVersion()
        } else {
            compileTask.version = project.solidity.version
        }
        compileTask.source = soliditySourceSet
        compileTask.outputComponents = project.solidity.outputComponents
        compileTask.combinedOutputComponents = project.solidity.combinedOutputComponents
        compileTask.overwrite = project.solidity.overwrite
        if (soliditySourceSet.getOptimize()){
            compileTask.optimize = soliditySourceSet.getOptimize()
        } else {
            compileTask.optimize = project.solidity.optimize
        }
        if (soliditySourceSet.getOptimizeRunsn()){
            compileTask.optimizeRuns = soliditySourceSet.getOptimizeRunsn()
        } else {
            compileTask.optimizeRuns = project.solidity.optimizeRuns
        }
        compileTask.prettyJson = project.solidity.prettyJson
        if (soliditySourceSet.getEvmVersion()){
            compileTask.evmVersion = soliditySourceSet.getEvmVersion()
        } else {
            compileTask.evmVersion = project.solidity.evmVersion
        }
        compileTask.allowPaths = project.solidity.allowPaths

        if (soliditySourceSet.getIgnoreMissing()){
            compileTask.ignoreMissing = soliditySourceSet.getIgnoreMissing()
        } else {
            compileTask.ignoreMissing = project.solidity.ignoreMissing
        }
        compileTask.destinationDirectory.set(soliditySourceSet.destinationDirectory)
        compileTask.destinationSubDirectory.set("solidity")
        compileTask.description = "Compiles $sourceSet.name Solidity source."

        project.getTasks().named('build').configure {
            it.dependsOn(compileTask)
        }

        compileTask.nodeModulesDir.set(nodeProjectDir.dir("node_modules"))

        project.tasks.named("processResources", ProcessResources) {
            it.from(compileTask)
        }
    }

    private void configureSolidityResolve(Project target, DirectoryProperty nodeProjectDir) {

        if (target.solidity.resolvePackages) {
            def extractSolidityImports = target.tasks.register("extractSolidityImports", SolidityExtractImports) {
                it.description = "Extracts imports of external Solidity contract modules."
                it.sources.from(resolvedSolidity)
                it.packageJson.set(nodeProjectDir.file("package.json"))
            }
            def npmInstall = target.tasks.named(NpmInstallTask.NAME) {
                it.dependsOn(extractSolidityImports)
            }
            def resolveSolidity = target.tasks.register("resolveSolidity", SolidityResolve) {
                it.description = "Resolve external Solidity contract modules."

                it.dependsOn(npmInstall)
                it.packageJson.set(nodeProjectDir.file("package.json"))
                it.nodeModules.set(nodeProjectDir.dir("node_modules"))

                it.allImports.set(target.layout.buildDirectory.file("sol-imports-all.txt"))
            }

            final SourceSetContainer sourceSets = target.extensions.getByType(SourceSetContainer.class)
            sourceSets.all { SourceSet sourceSet ->
                target.tasks.named(sourceSet.getTaskName("compile", "Solidity"), SolidityCompile) {
                    it.resolvedImports.set(resolveSolidity.flatMap { it.allImports })
                }
            }
        }
    }

    /**
     * Configure the SolcJ compiler with the bundled executable.
     */
    private static void configureAllowPath(final Project project, final SourceSet sourceSet) {
        def allowPath = "$project.projectDir/src/$sourceSet.name/$NAME"
        project.solidity.allowPaths.add(allowPath)
    }

    private static boolean requiresBundledExecutable(final Project project) {
        return project.solidity.executable == null
    }
}
