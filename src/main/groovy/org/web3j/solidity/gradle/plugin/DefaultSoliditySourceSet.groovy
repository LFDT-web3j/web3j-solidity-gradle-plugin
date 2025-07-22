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
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.internal.tasks.TaskDependencyFactory
import org.gradle.api.model.ObjectFactory
import org.gradle.api.reflect.HasPublicType
import org.gradle.api.reflect.TypeOf

import javax.inject.Inject

/**
 * SoliditySourceSet default implementation.
 */
@CompileStatic
abstract class DefaultSoliditySourceSet extends DefaultSourceDirectorySet implements SoliditySourceSet, HasPublicType {

    private final SourceDirectorySet allSolidity

    @Inject
    DefaultSoliditySourceSet(
            final SourceDirectorySet sourceDirectorySet,
            final SolidityExtension solidity,
            final ObjectFactory objectFactory,
            final TaskDependencyFactory taskDependencyFactory) {
        super(sourceDirectorySet, taskDependencyFactory);
        getFilter().include("**/*.sol")
        allSolidity = objectFactory.sourceDirectorySet("allSolidity", displayName)
        allSolidity.getFilter().include("**/*.sol")
        allSolidity.source(this)

        evmVersion.convention(solidity.evmVersion)
        version.convention(solidity.version)
        optimize.convention(solidity.optimize)
        optimizeRuns.convention(solidity.optimizeRuns)
        ignoreMissing.convention(solidity.ignoreMissing)
    }

    @Override
    SourceDirectorySet getAllSolidity() {
        return allSolidity
    }

    @Override
    TypeOf<?> getPublicType() {
        return TypeOf.typeOf(SoliditySourceSet.class)
    }
}
