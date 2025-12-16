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
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.language.jvm.tasks.ProcessResources

import static org.codehaus.groovy.runtime.StringGroovyMethods.capitalize
import static org.web3j.solidity.gradle.plugin.SoliditySourceSet.NAME

/**
 * Gradle plugin for Solidity compile automation.
 */
class SolidityPlugin implements Plugin<Project> {

    @Override
    void apply(final Project project) {
        project.pluginManager.apply(JavaPlugin.class)
        project.pluginManager.apply(NodePlugin.class)
        project.extensions.create(SolidityExtension.NAME, SolidityExtension)

        // Set nodeProjectDir to 'build' before the node plugin evaluation
        def nodeExtension = project.extensions.getByName(NodeExtension.NAME) as NodeExtension
        nodeExtension.nodeProjectDir.set(project.layout.buildDirectory)
        nodeExtension.download.set(true)

        final sourceSets = project.extensions.getByType(SourceSetContainer.class)

        configureSolidityResolve(project, nodeExtension.nodeProjectDir)

        sourceSets.configureEach { SourceSet sourceSet ->
            configureAllowPath(project, sourceSet)
            configureSourceSet(project, sourceSet)
            configureSolidityCompile(project, sourceSet, nodeExtension.nodeProjectDir)
        }
    }

    private static void configureAllowPath(final Project project, final SourceSet sourceSet) {
        def solidity = project.extensions.getByType(SolidityExtension)
        def allowPath = project.layout.projectDirectory.dir("src/$sourceSet.name/$NAME")
        solidity.allowPaths.add(project.relativePath(allowPath.asFile))
    }

    private static void configureSolidityResolve(Project project, DirectoryProperty nodeProjectDir) {
        def extractSolidityImports = project.tasks.register("extractSolidityImports", SolidityExtractImports) {
            it.description = "Extracts imports of external Solidity contract modules."
            it.packageJson.set(nodeProjectDir.file("package.json"))
        }
        def npmInstall = project.tasks.named(NpmInstallTask.NAME) {
            it.dependsOn(extractSolidityImports)
        }
        project.tasks.register("resolveSolidity", SolidityResolve) {
            it.description = "Resolve external Solidity contract modules."

            it.dependsOn(npmInstall)
            it.packageJson.set(nodeProjectDir.file("package.json"))
            it.nodeModules.set(nodeProjectDir.dir("node_modules"))

            it.allImports.set(project.layout.buildDirectory.file("sol-imports-all.txt"))
        }
    }

    /**
     * Add default source set for Solidity.
     */
    private static void configureSourceSet(final Project project, final SourceSet sourceSet) {
        def solidity = project.extensions.getByType(SolidityExtension)

        def srcSetName = capitalize((CharSequence) sourceSet.name)
        def soliditySourceSet = project.objects.newInstance(DefaultSoliditySourceSet,
                project.objects.sourceDirectorySet(NAME, srcSetName + " Solidity Sources"),
                solidity)

        sourceSet.extensions.add(NAME, soliditySourceSet)

        def defaultSrcDir = project.layout.projectDirectory.dir("src/$sourceSet.name/$NAME")
        def defaultOutputDir = project.layout.buildDirectory.dir("solidity/$sourceSet.name/$NAME")

        soliditySourceSet.srcDir(defaultSrcDir)
        soliditySourceSet.destinationDirectory.set(defaultOutputDir)

        sourceSet.allJava.source(soliditySourceSet)
        sourceSet.allSource.source(soliditySourceSet)

        project.tasks.named("extractSolidityImports", SolidityExtractImports) {
            it.sources.from(soliditySourceSet)
        }
    }

    /**
     * Configures code compilation tasks for the Solidity source sets defined in the project
     * (e.g. main, test).
     * <p>
     * By default the generated task name for the <code>main</code> source set
     * is <code>compileSolidity</code> and for <code>test</code>
     * <code>compileTestSolidity</code>.
     */
    private static void configureSolidityCompile(final Project project, final SourceSet sourceSet, final DirectoryProperty nodeProjectDir) {
        def solidity = project.extensions.getByType(SolidityExtension)
        def soliditySourceSet = sourceSet.extensions.getByType(SoliditySourceSet)
        def resolveSolidity = project.tasks.named('resolveSolidity', SolidityResolve)

        def compileTask = project.tasks.register(sourceSet.getTaskName("compile", "Solidity"), SolidityCompile) {
            it.description = "Compiles $sourceSet.name Solidity source."

            it.source = soliditySourceSet
            it.destinationDirectory.convention(soliditySourceSet.destinationDirectory)
            it.destinationSubDirectory.convention("solidity")
            it.nodeModulesDir.convention(nodeProjectDir.dir("node_modules"))

            it.executable.convention(solidity.executable)
            it.pathRemappings.convention(solidity.pathRemappings)
            it.outputComponents.convention(solidity.outputComponents)
            it.combinedOutputComponents.convention(solidity.combinedOutputComponents)
            it.optimize.convention(solidity.optimize)
            it.overwrite.convention(solidity.overwrite)
            it.prettyJson.convention(solidity.prettyJson)
            it.allowPaths.convention(solidity.allowPaths)

            it.version.convention(soliditySourceSet.version)
            it.optimize.convention(soliditySourceSet.optimize)
            it.optimizeRuns.convention(soliditySourceSet.optimizeRuns)
            it.evmVersion.convention(soliditySourceSet.evmVersion)
            it.ignoreMissing.convention(soliditySourceSet.ignoreMissing)

            it.resolvedImports.set(solidity.resolvePackages.flatMap {
                it ? resolveSolidity.flatMap { it.allImports } : emptyImports(project)
            })
        }

        project.tasks.named(sourceSet.processResourcesTaskName, ProcessResources) {
            it.from(compileTask)
        }
    }

    private static Provider<RegularFile> emptyImports(Project project) {
        return project.provider {
            // Optional file input workaround: https://github.com/gradle/gradle/issues/2016
            // This is a provider that is only triggered when solidity.resolvePackages = false.
            def emptyImportsFile = project.layout.buildDirectory.file("sol-imports-empty.txt").get()
            emptyImportsFile.asFile.parentFile.mkdirs()
            emptyImportsFile.asFile.createNewFile()
            return emptyImportsFile
        }
    }
}
