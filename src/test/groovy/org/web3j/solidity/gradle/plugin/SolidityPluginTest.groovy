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

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

import groovy.json.JsonSlurper

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

import static org.gradle.testkit.runner.TaskOutcome.SKIPPED
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertNull
import static org.junit.jupiter.api.Assertions.assertTrue

class SolidityPluginTest {
    final static String gradleVersionUnderTest = System.getProperty("gradleVersionUnderTest")

    /**
     * Gradle project directory where the test project will be run.
     * Has to be under <code>/tmp</code> because of Docker file sharing defaults.
     */
    private Path testProjectDir

    /**
     * Folder containing Solidity smart contracts with different versions.
     */
    private final String differentVersionsFolderName = "different_versions"

    /**
     * Solidity sources directory.
     */
    private static Path sourcesDir

    /**
     * Gradle build file.
     */
    private Path buildFile

    @BeforeAll
    static void setUp() throws Exception {
        final def resource = SolidityPlugin.getClassLoader().getResource('solidity/eip/EIP20.sol')
        sourcesDir = Paths.get(resource.toURI()).getParent().getParent()
    }

    @BeforeEach
    void setup() throws IOException {
        testProjectDir = Files.createTempDirectory("testProjectDir")
        buildFile = Files.createFile(testProjectDir.resolve('build.gradle'))
        Files.createDirectories(testProjectDir.resolve('src/main/solidity'))
        Files.walk(sourcesDir).forEach {
            if (Files.isRegularFile(it)) {
                // Copy .sol files into temp folder for Docker
                final def fileName = sourcesDir.relativize(it).toString()
                final def file = testProjectDir.resolve("src/main/solidity/$fileName")
                Files.createDirectories(file.getParent())
                Files.copy(it, file, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    @Test
    void compileSolidity() {
        Files.writeString(buildFile, """
            plugins {
               id 'org.web3j.solidity'
            }
            sourceSets {
                main {
                    solidity {
                        exclude "minimal_forwarder/**"
                        exclude "eip/**"
                        exclude "greeter/**"
                        exclude "common/**"
                        exclude "openzeppelin/**"
                        exclude "$differentVersionsFolderName/**"
                    }
                }
            }
        """)

        def success = build()
        assertEquals(SUCCESS, success.task(":compileSolidity").getOutcome())

        def compiledSolDir = testProjectDir.resolve("build/resources/main/solidity")
        assertTrue(Files.exists(compiledSolDir.resolve("Greeter.abi")))
        assertTrue(Files.exists(compiledSolDir.resolve("Greeter.bin")))
        assertTrue(Files.exists(compiledSolDir.resolve("Greeter_meta.json")))

        def upToDate = build()
        assertEquals(UP_TO_DATE, upToDate.task(":compileSolidity").getOutcome())
    }

    @Test
    void compileSolidityWithLibraryImports() throws IOException {
        Files.writeString(buildFile, """
            plugins {
               id 'org.web3j.solidity'
            }
            node {
                nodeProjectDir = file("\$project.rootDir/test")
            }
            sourceSets {
                main {
                    solidity {
                        exclude "minimal_forwarder/**"
                        exclude "sol5/**"
                        exclude "common/**"
                        exclude "eip/**"
                        exclude "$differentVersionsFolderName/**"
                        exclude "greeter/**"
                    }
                }
            }
        """)

        def success = build()
        assertEquals(SUCCESS, success.task(":compileSolidity").getOutcome())

        def compiledSolDir = testProjectDir.resolve("build/resources/main/solidity")
        assertTrue(Files.exists(compiledSolDir.resolve("MyCollectible.abi")))
        assertTrue(Files.exists(compiledSolDir.resolve("MyCollectible.bin")))
        assertTrue(Files.exists(compiledSolDir.resolve("ERC721.abi")))
        assertTrue(Files.exists(compiledSolDir.resolve("MyOFT.abi")))
        assertTrue(Files.exists(compiledSolDir.resolve("MyOFT.bin")))

        def upToDate = build()
        assertEquals(UP_TO_DATE, upToDate.task(":compileSolidity").getOutcome())
        assertEquals(UP_TO_DATE, upToDate.task(":resolveSolidity").getOutcome())
    }

    @Test
    void compileSolidityWithVersion() throws IOException {
        Files.writeString(buildFile, """
            plugins {
               id 'org.web3j.solidity'
            }
            solidity {
                version = '0.8.7'
            }
            sourceSets {
               main {
                   solidity {
                       exclude "minimal_forwarder/**"
                       exclude "sol5/**"
                       exclude "greeter/**"
                       exclude "common/**"
                       exclude "openzeppelin/**"
                       exclude "$differentVersionsFolderName/**"
                   }
               }
            }
        """)

        def success = build()
        assertEquals(SUCCESS, success.task(":compileSolidity").getOutcome())

        def compiledSolDir = testProjectDir.resolve("build/resources/main/solidity")
        assertTrue(Files.exists(compiledSolDir.resolve("EIP20.abi")))
        assertTrue(Files.exists(compiledSolDir.resolve("EIP20.bin")))

        def upToDate = build()
        assertEquals(UP_TO_DATE, upToDate.task(":compileSolidity").getOutcome())
    }

    @Test
    void compileSolidityWithEvmVersion() throws IOException {
        Files.writeString(buildFile, """
            plugins {
               id 'org.web3j.solidity'
            }
            solidity {
                evmVersion = 'ISTANBUL'
            }
            sourceSets {
               main {
                   solidity {
                       exclude "sol5/**"
                       exclude "eip/**"
                       exclude "greeter/**"
                       exclude "common/**"
                       exclude "openzeppelin/**"
                       exclude "$differentVersionsFolderName/**"
                   }
               }
            }
        """)

        def success = build()
        assertEquals(SUCCESS, success.task(":compileSolidity").getOutcome())

        def compiledSolDir = testProjectDir.resolve("build/resources/main/solidity")
        assertTrue(Files.exists(compiledSolDir.resolve("MinimalForwarder.abi")))
        assertTrue(Files.exists(compiledSolDir.resolve("MinimalForwarder.bin")))

        def upToDate = build()
        assertEquals(UP_TO_DATE, upToDate.task(":compileSolidity").getOutcome())
    }

    @Test
    void compileSolidityWithSourceSetsSpecificConfig() throws IOException {
        Files.writeString(buildFile, """
            plugins {
               id 'org.web3j.solidity'
            }
            
            sourceSets {
               main {
                   solidity {
                       exclude "sol5/**"
                       exclude "eip/**"
                       exclude "greeter/**"
                       exclude "common/**"
                       exclude "openzeppelin/**"
                       exclude "$differentVersionsFolderName/**"

                       setEvmVersion('ISTANBUL')
                       setOptimize(true)
                       setOptimizeRuns(200)
                       setVersion('0.8.12')
                   }
               }
            }
        """)

        def success = build()
        assertEquals(SUCCESS, success.task(":compileSolidity").getOutcome())

        def compiledSolDir = testProjectDir.resolve("build/resources/main/solidity")
        assertTrue(Files.exists(compiledSolDir.resolve("MinimalForwarder.abi")))
        assertTrue(Files.exists(compiledSolDir.resolve("MinimalForwarder.bin")))

        def upToDate = build()
        assertEquals(UP_TO_DATE, upToDate.task(":compileSolidity").getOutcome())
    }

    @Test
    void compileSolidityFromCustomSourceDirectory() throws IOException {
        def customSourceDir = testProjectDir.resolve("src/test/resources/contracts/EvmCodes")
        Files.createDirectories(customSourceDir)
        // Use a self-contained contract (no imports) so the custom srcDir test is isolated
        Files.copy(
                testProjectDir.resolve("src/main/solidity/sol5/Greeter.sol"),
                customSourceDir.resolve("Greeter.sol"),
                StandardCopyOption.REPLACE_EXISTING
        )

        Files.writeString(buildFile, """
            plugins {
               id 'org.web3j.solidity'
            }

            sourceSets {
                main {
                    solidity {
                        srcDir "src/test/resources/contracts/EvmCodes"
                        include "Greeter.sol"
                        output.resourcesDir = file("out/compiledSol")
                    }
                }
            }
        """)

        def success = build()
        assertEquals(SUCCESS, success.task(":compileSolidity").getOutcome())

        def customOutputDir = testProjectDir.resolve("out/compiledSol/solidity")
        assertTrue(Files.exists(customOutputDir.resolve("Greeter.abi")))
        assertTrue(Files.exists(customOutputDir.resolve("Greeter.bin")))
    }

    @Test
    @Disabled("Requires a specific solc version on the machine to pass")
    void compileSolidityWithExecutable() throws IOException {
        Files.writeString(buildFile, """
            plugins {
               id 'org.web3j.solidity'
            }
            solidity {
                executable = 'solc'
            }
            sourceSets {
                main {
                    solidity {
                        exclude "minimal_forwarder/**"
                        exclude "sol5/**"
                        exclude "greeter/**"
                        exclude "common/**"
                        exclude "openzeppelin/**"
                        exclude "$differentVersionsFolderName/**"
                    }
                }
            }
        """)

        def success = build()
        assertEquals(SUCCESS, success.task(":compileSolidity").getOutcome())

        def compiledSolDir = testProjectDir.resolve("build/resources/main/solidity")
        assertTrue(Files.exists(compiledSolDir.resolve("EIP20.abi")))
        assertTrue(Files.exists(compiledSolDir.resolve("EIP20.bin")))

        def upToDate = build()
        assertEquals(UP_TO_DATE, upToDate.task(":compileSolidity").getOutcome())
    }

    @Test
    @Disabled("This is cool but fails if docker is not running. // Needs to be solved in the CI")
    void compileSolidityWithDocker() throws IOException {
        Files.writeString(buildFile, """
            plugins {
               id 'org.web3j.solidity'
            }
            sourceSets {
                main {
                    solidity {
                        exclude "minimal_forwarder/**"
                        exclude "sol5/**"
                        exclude "eip/**"
                        exclude "openzeppelin/**"
                        exclude "$differentVersionsFolderName/**"
                    }
                }
            }
            solidity {
                executable = 'docker run --rm -v \$testProjectDir.root:/src satran004/aion-fastvm:0.3.1 solc'
                allowPaths = ['/src/src/main/solidity']
                version = '0.4.15'
            }
        """)

        def success = build()
        assertEquals(SUCCESS, success.task(":compileSolidity").getOutcome())

        def compiledSolDir = testProjectDir.resolve("build/resources/main/solidity")
        assertTrue(Files.exists(compiledSolDir.resolve("Greeter.abi")))
        assertTrue(Files.exists(compiledSolDir.resolve("Greeter.bin")))

        def upToDate = build()
        assertEquals(UP_TO_DATE, upToDate.task(":compileSolidity").getOutcome())
    }

    @Test
    void compileSolidityWithDifferentVersions() throws IOException {
        Files.writeString(buildFile, """
            plugins {
               id 'org.web3j.solidity'
            }
            sourceSets {
                main {
                    solidity {
                        exclude "minimal_forwarder/**"
                        exclude "eip/**"
                        exclude "greeter/**"
                        exclude "common/**"
                        exclude "sol5/**"
                        exclude "openzeppelin/**"
                    }
                }
            }
        """)

        def success = build()
        assertEquals(SUCCESS, success.task(":compileSolidity").getOutcome())
    }

    @Test
    void linuxArm64UsesNativeSolcFromVersion0831() {
        def linuxUrl = SolidityCompile.linuxUrlForArchitecture(
                "0.8.35",
                "https://github.com/ethereum/solidity/releases/download/v0.8.35/solc-linux-amd64",
                "Linux",
                "aarch64")

        assertEquals("https://github.com/ethereum/solidity/releases/download/v0.8.35/solc-linux-arm64", linuxUrl)
    }

    @Test
    void linuxArm64UsesAmd64SolcBeforeVersion0831() {
        def amd64Url = "https://github.com/ethereum/solidity/releases/download/v0.8.30/solc-linux-amd64"

        def linuxUrl = SolidityCompile.linuxUrlForArchitecture("0.8.30", amd64Url, "Linux", "aarch64")

        assertEquals(amd64Url, linuxUrl)
    }

    @Test
    void linuxAmd64KeepsUsingAmd64Solc() {
        def amd64Url = "https://github.com/ethereum/solidity/releases/download/v0.8.35/solc-linux-amd64"

        def linuxUrl = SolidityCompile.linuxUrlForArchitecture("0.8.35", amd64Url, "Linux", "amd64")

        assertEquals(amd64Url, linuxUrl)
    }

    @Test
    void macKeepsExistingSolcBinary() {
        def linuxUrl = "https://github.com/ethereum/solidity/releases/download/v0.8.35/solc-linux-amd64"

        def result = SolidityCompile.linuxUrlForArchitecture(
                "0.8.35",
                linuxUrl,
                "Mac OS X",
                "aarch64")

        assertEquals(linuxUrl, result)
    }

    /**
     * Verifies that applying the plugin to a project without Solidity sources completes successfully.
     *
     * <p>When no Solidity files are present, the import extraction task is skipped and does not
     * generate its {@code package.json} output. Downstream tasks must handle this case gracefully
     * without failing task validation.
     */
    @Test
    void buildSucceedsWithoutSoliditySources() throws IOException {
        // Remove the sample sources copied in by setup() so the project has no .sol files.
        testProjectDir.resolve("src/main/solidity").toFile().deleteDir()

        Files.writeString(buildFile, """
            plugins {
               id 'org.web3j.solidity'
            }
        """)

        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("build", "-s")
                .withPluginClasspath()
                .forwardOutput().with {
                    gradleVersionUnderTest ? it.withGradleVersion(gradleVersionUnderTest) : it
                }.build()

        assertEquals(SUCCESS, result.task(":build").getOutcome())
    }

    /**
     * Verifies that packages declared via the {@code packages} DSL property are written into the
     * generated {@code package.json}, and that an explicitly declared version takes precedence over
     * the {@code latest} version used for packages detected from Solidity imports.
     */
    @Test
    void declaredPackagesAreAddedToPackageJson() throws IOException {
        Files.writeString(buildFile, """
            plugins {
               id 'org.web3j.solidity'
            }
            sourceSets {
                main {
                    solidity {
                        exclude "minimal_forwarder/**"
                        exclude "sol5/**"
                        exclude "common/**"
                        exclude "eip/**"
                        exclude "$differentVersionsFolderName/**"
                        exclude "greeter/**"
                    }
                }
            }
            solidity {
                packages = [
                    '@custom-org/custom-lib'  : '1.2.3',
                    '@openzeppelin/contracts' : '4.9.0'
                ]
            }
        """)

        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("extractSolidityImports", "-s")
                .withPluginClasspath()
                .forwardOutput().with {
                    gradleVersionUnderTest ? it.withGradleVersion(gradleVersionUnderTest) : it
                }.build()

        assertEquals(SUCCESS, result.task(":extractSolidityImports").getOutcome())

        def packageJson = testProjectDir.resolve("build/package.json")
        assertTrue(Files.exists(packageJson))

        def dependencies = new JsonSlurper().parse(packageJson.toFile())['dependencies'] as Map
        assertEquals('1.2.3', dependencies['@custom-org/custom-lib'])
        // Declared version overrides the "latest" used for imports detected from sources.
        assertEquals('4.9.0', dependencies['@openzeppelin/contracts'])
    }

    /**
     * With {@code solidity.compilerEnabled = false} the plugin can be applied without invoking
     * the Solidity compiler or the Node/npm machinery: the compile task is skipped (so no solc is
     * resolved, downloaded or executed) and the {@code resolveSolidity} / {@code npmInstall} chain
     * is never pulled into the task graph.
     */
    @Test
    void disablingCompilerSkipsCompilationAndNode() throws IOException {
        Files.writeString(buildFile, """
            plugins {
               id 'org.web3j.solidity'
            }
            solidity {
                compilerEnabled = false
            }
        """)

        def result = build()

        assertEquals(SUCCESS, result.task(":build").getOutcome())
        assertEquals(SKIPPED, result.task(":compileSolidity").getOutcome())
        assertNull(result.task(":resolveSolidity"))
        assertNull(result.task(":npmInstall"))
    }

    /**
     * {@code solidity.resolvePackages = false} disables only the Node/npm integration.
     * Compilation still happens, but the {@code resolveSolidity} / {@code npmInstall} chain
     * is never pulled into the task graph.
     */
    @Test
    void disablingPackageResolutionSkipsNodeButStillCompiles() throws IOException {
        Files.writeString(buildFile, """
            plugins {
               id 'org.web3j.solidity'
            }
            solidity {
                resolvePackages = false
            }
            sourceSets {
                main {
                    solidity {
                        exclude "minimal_forwarder/**"
                        exclude "eip/**"
                        exclude "greeter/**"
                        exclude "common/**"
                        exclude "openzeppelin/**"
                        exclude "$differentVersionsFolderName/**"
                    }
                }
            }
        """)

        def result = build()

        assertEquals(SUCCESS, result.task(":compileSolidity").getOutcome())
        assertNull(result.task(":resolveSolidity"))
        assertNull(result.task(":npmInstall"))
    }

    private BuildResult build() {
        return GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("build", "--info", "-s", "--configuration-cache")
                .withPluginClasspath()
                .forwardOutput().with {
                    gradleVersionUnderTest? it.withGradleVersion(gradleVersionUnderTest) : it
                }.build()
    }
}
