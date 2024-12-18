/*
 * Copyright 2024 Web3 Labs Ltd.
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

import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
@CompileStatic
abstract class SolidityExtractImports extends DefaultTask {

    @Input
    abstract Property<String> getProjectName()

    @InputFiles
    @PathSensitive(value = PathSensitivity.RELATIVE)
    @SkipWhenEmpty
    abstract ConfigurableFileCollection getSources()

    @OutputFile
    abstract RegularFileProperty getPackageJson()

    SolidityExtractImports() {
        projectName.convention(project.name)
    }

    @TaskAction
    void resolveSolidity() {
        final Set<String> packages = new TreeSet<>()

        sources.each { contract ->
            packages.addAll(ImportsResolver.extractImports(contract))
        }

        final jsonMap = [
                "name"        : projectName.get(),
                "description" : "",
                "repository"  : "",
                "license"     : "UNLICENSED",
                "dependencies": packages.collectEntries {
                    [(it): "latest"]
                }
        ]

        packageJson.get().asFile.text = new JsonBuilder(jsonMap).toPrettyString()
    }
}
