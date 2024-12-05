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

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.web3j.sokt.SolcInstance
import org.web3j.sokt.SolidityFile
import org.web3j.sokt.VersionResolver

import java.nio.file.Paths

@CacheableTask
abstract class SolidityCompile extends SourceTask {

    @Input
    @Optional
    private String executable

    @Input
    @Optional
    private String version

    @Input
    @Optional
    private Boolean overwrite

    @Input
    @Optional
    private Boolean optimize

    @Input
    @Optional
    private Integer optimizeRuns

    @Input
    @Optional
    private Boolean prettyJson

    @Input
    @Optional
    private Boolean ignoreMissing

    @Input
    @Optional
    private Set<String> allowPaths

    @Input
    @Optional
    private Map<String, String> pathRemappings

    @Input
    @Optional
    private EVMVersion evmVersion

    @Input
    @Optional
    private OutputComponent[] outputComponents

    @Input
    @Optional
    private CombinedOutputComponent[] combinedOutputComponents

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getResolvedImports()

    SolidityCompile() {
        resolvedImports.convention(project.provider {
            // Optional file input workaround: https://github.com/gradle/gradle/issues/2016
            // This is a provider that is only triggered when not overwritten (solidity.resolvePackages = false).
            def emptyImportsFile = project.layout.buildDirectory.file("sol-imports-empty.txt").get()
            emptyImportsFile.asFile.parentFile.mkdirs()
            emptyImportsFile.asFile.createNewFile()
            return emptyImportsFile
        })
    }

    @TaskAction
    void compileSolidity() {
        final imports = resolvedImports.get().asFile.readLines().findAll { !it.isEmpty() }
        final File nodeModulesDir = project.node.nodeProjectDir.dir("node_modules").get().asFile

        for (def contract in source) {
            def options = []

            for (output in outputComponents) {
                options.add("--$output")
            }

            if (combinedOutputComponents?.length > 0) {
                options.add("--combined-json")
                options.add(combinedOutputComponents.join(","))
            }

            if (optimize) {
                options.add('--optimize')

                if (0 < optimizeRuns) {
                    options.add('--optimize-runs')
                    options.add(optimizeRuns)
                }
            }

            if (overwrite) {
                options.add('--overwrite')
            }

            if (prettyJson) {
                options.add('--pretty-json')
            }

            if (ignoreMissing) {
                options.add('--ignore-missing')
            }

            if (!allowPaths.isEmpty() || !imports.isEmpty()) {
                options.add("--allow-paths")
                options.add((allowPaths + imports.collect { new File(nodeModulesDir,it).absolutePath }).join(','))
            }

            pathRemappings.each { key, value ->
                options.add("$key=$value")
            }

            imports.each { provider ->
                options.add("$provider=$nodeModulesDir/$provider")
            }

            options.add('--output-dir')
            options.add(project.projectDir.relativePath(outputs.files.singleFile))
            options.add(project.projectDir.relativePath(contract))

            def compilerVersion = version
            def solidityFile = new SolidityFile(contract.getAbsolutePath())
            String compilerExecutable = executable
            SolcInstance compilerInstance

            if (compilerExecutable == null) {
                if (compilerVersion != null) {
                    def resolvedVersion = new VersionResolver(".web3j").getSolcReleases().stream().filter {
                        it.version == version && it.isCompatibleWithOs()
                    }.findAny().orElseThrow {
                        return new Exception("Failed to resolve Solidity version $version from available versions. " +
                                "You may need to use a custom executable instead.")
                    }
                    compilerInstance = new SolcInstance(resolvedVersion, ".web3j", false)
                } else {
                    compilerInstance = solidityFile.getCompilerInstance(".web3j", true)
                    compilerVersion = compilerInstance.solcRelease.version
                }

                if (compilerInstance.installed() || !compilerInstance.installed() && compilerInstance.install()) {
                    compilerExecutable = compilerInstance.solcFile.getAbsolutePath()
                }
            }

            if (evmVersion != null && supportsEvmVersionOption(compilerVersion)) {
                options.add("--evm-version")
                options.add(evmVersion.value)
            }

            if (Paths.get(compilerExecutable).toFile().exists()) {
                // if the executable string is a file which exists, it may be a direct reference
                // to the solc executable with a space in the path (Windows)
                project.exec {
                    executable = compilerExecutable
                    args = options
                }
            } else {
                // otherwise we assume it's a normal reference to solidity or docker, possibly with args
                def executableParts = compilerExecutable.split(' ')
                options.addAll(0, executableParts.drop(1))
                project.exec {
                    // Use first part as executable
                    executable = executableParts[0]
                    // Use other parts and options as args
                    args = options
                }
            }

            if (combinedOutputComponents?.length > 0) {
                def metajsonFile = new File(outputs.files.singleFile, "combined.json")
                def contractName = contract.getName()
                def newMetaName = contractName.substring(0, contractName.length() - 4) + ".json"

                metajsonFile.renameTo(new File(metajsonFile.getParentFile(), newMetaName))
            }
        }
    }

    String getExecutable() {
        return executable
    }

    void setExecutable(final String executable) {
        this.executable = executable
    }

    String getVersion() {
        return version
    }

    void setVersion(String version) {
        this.version = version
    }

    static boolean supportsEvmVersionOption(String version) {
        return version.split('\\.').last().toInteger() >= 24 || version.split('\\.')[1].toInteger() > 4
    }

    Boolean getOverwrite() {
        return overwrite
    }

    void setOverwrite(final Boolean overwrite) {
        this.overwrite = overwrite
    }

    Boolean getOptimize() {
        return optimize
    }

    void setOptimize(final Boolean optimize) {
        this.optimize = optimize
    }

    Integer getOptimizeRuns() {
        return optimizeRuns
    }

    void setOptimizeRuns(final Integer optimizeRuns) {
        this.optimizeRuns = optimizeRuns
    }

    Boolean getPrettyJson() {
        return prettyJson
    }

    void setPrettyJson(final Boolean prettyJson) {
        this.prettyJson = prettyJson
    }

    Boolean getIgnoreMissing() {
        return ignoreMissing
    }

    void setIgnoreMissing(final Boolean ignoreMissing) {
        this.ignoreMissing = ignoreMissing
    }

    Map<String, String> getPathRemappings() {
        return pathRemappings
    }

    void setPathRemappings(Map<String, String> pathRemappings) {
        this.pathRemappings = pathRemappings
    }

    Set<String> getAllowPaths() {
        return allowPaths
    }

    void setAllowPaths(final Set<String> allowPaths) {
        this.allowPaths = allowPaths
    }

    EVMVersion getEvmVersion() {
        return evmVersion
    }

    void setEvmVersion(final EVMVersion evmVersion) {
        this.evmVersion = evmVersion
    }

    OutputComponent[] getOutputComponents() {
        return outputComponents
    }

    void setOutputComponents(final OutputComponent[] outputComponents) {
        this.outputComponents = outputComponents
    }

    CombinedOutputComponent[] getCombinedOutputComponents() {
        return combinedOutputComponents
    }

    void setCombinedOutputComponents(CombinedOutputComponent[] combinedOutputComponents) {
        this.combinedOutputComponents = combinedOutputComponents
    }
}
