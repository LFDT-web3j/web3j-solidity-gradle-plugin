package org.web3j.solidity.gradle.plugin

import org.junit.Rule
import org.junit.Test
import org.junit.contrib.java.lang.system.RestoreSystemProperties

import static org.assertj.core.api.Assertions.assertThat

class SolidityCompilerTest {

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties()

    @Test
    void testLinux() {
        System.setProperty("os.name", "Linux")

        final def solc = new SolidityCompiler().getExecutable()
        assertThat(solc.name).isEqualTo("solc")
    }

    @Test
    void testMac() {
        System.setProperty("os.name", "Mac OS X")

        final def solc = new SolidityCompiler().getExecutable()
        assertThat(solc.name).isEqualTo("solc")
    }

    @Test
    void testWin() {
        System.setProperty("os.name", "Windows Vista")

        final def solc = new SolidityCompiler().getExecutable()
        assertThat(solc.name).isEqualTo("solc")
    }
}
