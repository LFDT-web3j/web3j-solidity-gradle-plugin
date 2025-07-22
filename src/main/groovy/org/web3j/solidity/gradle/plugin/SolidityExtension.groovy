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
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

import javax.inject.Inject

/**
 * Extension for Solidity compilation options.
 */
@CompileStatic
abstract class SolidityExtension {
    static final NAME = 'solidity'

    @Inject
    SolidityExtension() {
        optimize.convention(true)
        resolvePackages.convention(true)
        overwrite.convention(true)
        optimizeRuns.convention(0)
        prettyJson.convention(false)
        ignoreMissing.convention(false)
        outputComponents.convention([
                OutputComponent.BIN,
                OutputComponent.ABI,
                OutputComponent.METADATA
        ])
        combinedOutputComponents.convention([
                CombinedOutputComponent.BIN,
                CombinedOutputComponent.BIN_RUNTIME,
                CombinedOutputComponent.SRCMAP,
                CombinedOutputComponent.SRCMAP_RUNTIME
        ])
    }

    abstract Property<String> getVersion()

    abstract Property<String> getExecutable()

    abstract Property<Boolean> getOptimize()

    abstract Property<Boolean> getResolvePackages()

    abstract Property<Integer> getOptimizeRuns()

    abstract Property<Boolean> getPrettyJson()

    abstract Property<Boolean> getOverwrite()

    abstract Property<Boolean> getIgnoreMissing()

    abstract SetProperty<String> getAllowPaths()

    abstract MapProperty<String, String> getPathRemappings()

    abstract Property<EVMVersion> getEvmVersion()

    abstract ListProperty<OutputComponent> getOutputComponents()

    abstract ListProperty<CombinedOutputComponent> getCombinedOutputComponents()
}
