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

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS
import static org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE
import static org.junit.jupiter.api.Assertions.assertEquals
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
