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

import groovy.transform.CompileStatic
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.web3j.sokt.SolcInstance
import org.web3j.sokt.SolidityFile
import org.web3j.sokt.VersionResolver

import javax.inject.Inject
import java.nio.file.Paths

@CacheableTask
@CompileStatic
abstract class SolidityCompile extends SourceTask {

    @Input
    @Optional
    abstract Property<String> getExecutable()

    @Input
    @Optional
    abstract Property<String> getVersion()

    @Input
    abstract Property<Boolean> getOverwrite()

    @Input
    abstract Property<Boolean> getOptimize()

    @Input
    abstract Property<Integer> getOptimizeRuns()

    @Input
    abstract Property<Boolean> getPrettyJson()

    @Input
    abstract Property<Boolean> getIgnoreMissing()

    @Input
    abstract SetProperty<String> getAllowPaths()

    @Input
    abstract MapProperty<String, String> getPathRemappings()

    @Input
    @Optional
    abstract Property<EVMVersion> getEvmVersion()

    @Input
    abstract ListProperty<OutputComponent> getOutputComponents()

    @Input
    abstract ListProperty<CombinedOutputComponent> getCombinedOutputComponents()

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getResolvedImports()

    @Input
    abstract Property<String> getDestinationSubDirectory();

    @OutputDirectory
    abstract DirectoryProperty getDestinationDirectory();

    @Internal
    abstract DirectoryProperty getNodeModulesDir()

    @Inject
    protected abstract ExecOperations getExec();

    @TaskAction
    void compileSolidity() {
        final imports = resolvedImports.get().asFile.readLines().findAll { !it.isEmpty() }

        for (def contract in source) {
            List<String> options = []

            for (output in outputComponents.get()) {
                options.add("--$output".toString())
            }

            if (combinedOutputComponents.get().size() > 0) {
                options.add("--combined-json")
                options.add(combinedOutputComponents.get().join(","))
            }

            if (optimize.get()) {
                options.add('--optimize')

                if (0 < optimizeRuns.get()) {
                    options.add('--optimize-runs')
                    options.add(optimizeRuns.get().toString())
                }
            }

            if (overwrite.get()) {
                options.add('--overwrite')
            }

            if (prettyJson.get()) {
                options.add('--pretty-json')
            }

            if (ignoreMissing.get()) {
                options.add('--ignore-missing')
            }

            if (!allowPaths.get().isEmpty() || !imports.isEmpty()) {
                options.add("--allow-paths")
                options.add((allowPaths.get() + imports.collect { new File(nodeModulesDir.get().asFile, it).absolutePath }).join(','))
            }

            pathRemappings.get().each { key, value ->
                options.add("$key=$value".toString())
            }

            imports.each { provider ->
                options.add("$provider=${nodeModulesDir.get().asFile}/$provider".toString())
            }

            options.add('--output-dir')
            if (destinationSubDirectory.isPresent()) {
                options.add(new File(destinationDirectory.get().asFile, destinationSubDirectory.get()).absolutePath)
            } else {
                options.add(destinationDirectory.get().asFile.absolutePath)
            }
            options.add(contract.absolutePath)

            def compilerVersion = version.getOrNull()
            def solidityFile = new SolidityFile(contract.absolutePath)
            String compilerExecutable = executable.getOrNull()
            SolcInstance compilerInstance

            if (compilerExecutable == null) {
                if (compilerVersion != null) {
                    def resolvedVersion = new VersionResolver(".web3j").getSolcReleases().stream().filter {
                        it.version == version.get() && it.isCompatibleWithOs()
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

            if (evmVersion.isPresent() && supportsEvmVersionOption(compilerVersion)) {
                options.add("--evm-version")
                options.add(evmVersion.get().value)
            }

            if (Paths.get(compilerExecutable).toFile().exists()) {
                // if the executable string is a file which exists, it may be a direct reference
                // to the solc executable with a space in the path (Windows)
                exec.exec {
                    it.executable = compilerExecutable
                    it.args = options
                }
            } else {
                // otherwise we assume it's a normal reference to solidity or docker, possibly with args
                def executableParts = compilerExecutable.split(' ')
                options.addAll(0, executableParts.drop(1))
                exec.exec {
                    // Use first part as executable
                    it.executable = executableParts[0]
                    // Use other parts and options as args
                    it.args = options
                }
            }

            if (combinedOutputComponents.get().size() > 0) {
                def metajsonFile = new File(outputs.files.singleFile, "combined.json")
                def contractName = contract.getName()
                def newMetaName = contractName.substring(0, contractName.length() - 4) + ".json"

                metajsonFile.renameTo(new File(metajsonFile.getParentFile(), newMetaName))
            }
        }
    }

    private static boolean supportsEvmVersionOption(String version) {
        return version.split('\\.').last().toInteger() >= 24 || version.split('\\.')[1].toInteger() > 4
    }
}
