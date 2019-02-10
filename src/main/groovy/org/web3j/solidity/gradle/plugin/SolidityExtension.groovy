package org.web3j.solidity.gradle.plugin

import org.gradle.api.Project

import javax.inject.Inject

import static EVMVersion.BYZANTIUM
import static org.web3j.solidity.gradle.plugin.OutputComponent.ABI
import static org.web3j.solidity.gradle.plugin.OutputComponent.BIN

/**
 * Extension for Solidity compilation options.
 */
class SolidityExtension {

    static final NAME = 'solidity'

    private Project project

    private Boolean overwrite

    private Boolean optimize

    private Integer optimizeRuns

    private Boolean prettyJson

    private Boolean ignoreMissing

    private List<String> allowPaths

    private EVMVersion evmVersion

    private OutputComponent[] outputComponents

    @Inject
    SolidityExtension(final Project project) {
        this.project = project
        this.optimize = true
        this.overwrite = true
        this.optimizeRuns = 0
        this.prettyJson = false
        this.ignoreMissing = false
        this.allowPaths = []
        this.evmVersion = BYZANTIUM
        this.outputComponents = [BIN, ABI]
    }

    boolean getOptimize() {
        return optimize
    }

    void setOptimize(final boolean optimize) {
        this.optimize = optimize
    }

    int getOptimizeRuns() {
        return optimizeRuns
    }

    void setOptimizeRuns(final int optimizeRuns) {
        this.optimizeRuns = optimizeRuns
    }

    boolean getPrettyJson() {
        return prettyJson
    }

    void setPrettyJson(final boolean prettyJson) {
        this.prettyJson = prettyJson
    }

    boolean getOverwrite() {
        return overwrite
    }

    void setOverwrite(final boolean overwrite) {
        this.overwrite = overwrite
    }

    Boolean getIgnoreMissing() {
        return ignoreMissing
    }

    void setIgnoreMissing(final Boolean ignoreMissing) {
        this.ignoreMissing = ignoreMissing
    }

    List<String> getAllowPaths() {
        return allowPaths
    }

    void setAllowPaths(final List<String> allowPaths) {
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

}
