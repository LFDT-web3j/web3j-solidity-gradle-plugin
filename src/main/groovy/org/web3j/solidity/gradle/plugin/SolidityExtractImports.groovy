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
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

@CacheableTask
@CompileStatic
abstract class SolidityExtractImports extends DefaultTask {

    @Input
    abstract Property<String> getProjectName()

    /**
     * Additional npm packages to resolve, mapped to their version (e.g. {@code 'latest'}).
     * <p>
     * These are merged into the generated {@code package.json} on top of the packages detected
     * from Solidity imports, allowing users to declare dependencies that are not directly imported
     * or to pin a specific version. Explicitly declared versions take precedence over detected ones.
     */
    @Input
    abstract MapProperty<String, String> getPackages()

    /*
     Note: intentionally not @SkipWhenEmpty. When a project has no Solidity sources this task
     must still run and emit a package.json (with empty dependencies); otherwise the downstream
     npmInstall / resolveSolidity tasks fail input validation on the missing file.
    */
    @InputFiles
    @PathSensitive(value = PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getSources()

    @OutputFile
    abstract RegularFileProperty getPackageJson()

    SolidityExtractImports() {
        projectName.convention(project.name)
    }

    @TaskAction
    void resolveSolidity() {
        final Map<String, String> dependencies = new TreeMap<>()

        sources.each { contract ->
            ImportsResolver.extractImports(contract).each { detected ->
                dependencies.put(detected, "latest")
            }
        }

        getPackages().get().each { name, version ->
            dependencies.put(name, version)
        }

        final jsonMap = [
                "name"        : projectName.get(),
                "description" : "",
                "repository"  : "",
                "license"     : "UNLICENSED",
                "dependencies": dependencies
        ]

        packageJson.get().asFile.text = new JsonBuilder(jsonMap).toPrettyString()
    }
}
