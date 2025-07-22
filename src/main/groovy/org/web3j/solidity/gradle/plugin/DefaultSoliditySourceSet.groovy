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
class DefaultSoliditySourceSet extends DefaultSourceDirectorySet implements SoliditySourceSet, HasPublicType {

    private final SourceDirectorySet allSolidity
    private EVMVersion evmVersion
    private String version
    private Boolean optimize
    private Integer optimizeRuns
    private Boolean ignoreMissing

    @Inject
    DefaultSoliditySourceSet(
            final SourceDirectorySet sourceDirectorySet,
            final ObjectFactory objectFactory,
            final TaskDependencyFactory taskDependencyFactory) {
        super(sourceDirectorySet, taskDependencyFactory);
        getFilter().include("**/*.sol")
        allSolidity = objectFactory.sourceDirectorySet("allSolidity", displayName)
        allSolidity.getFilter().include("**/*.sol")
        allSolidity.source(this)
    }

    @Override
    SourceDirectorySet getAllSolidity() {
        return allSolidity
    }

    @Override
    TypeOf<?> getPublicType() {
        return TypeOf.typeOf(SoliditySourceSet.class)
    }

    @Override
    void setEvmVersion(EVMVersion evmVersion) {
        this.evmVersion =  evmVersion
    }

    @Override
    EVMVersion getEvmVersion() {
        return this.evmVersion
    }

    @Override
    void setVersion(String version) {
        this.version =  version
    }

    @Override
    String getVersion() {
        return this.version
    }

    @Override
    void setOptimize(Boolean optimize) {
        this.optimize = optimize
    }

    @Override
    Boolean getOptimize() {
        return this.optimize
    }

    @Override
    void setOptimizeRuns(Integer optimizeRuns) {
        this.optimizeRuns = optimizeRuns
    }

    @Override
    Integer getOptimizeRunsn() {
        return this.optimizeRuns
    }

    @Override
    void setIgnoreMissing(Boolean ignoreMissing) {
        this.ignoreMissing = ignoreMissing
    }

    @Override
    Boolean getIgnoreMissing() {
        return this.ignoreMissing
    }
}
