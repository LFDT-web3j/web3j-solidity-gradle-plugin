/*
 * Copyright 2020 Web3 Labs Ltd.
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

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

@CompileStatic
@CacheableTask
abstract class SolidityResolve extends DefaultTask {

    @InputFile
    @PathSensitive(value = PathSensitivity.NAME_ONLY)
    abstract RegularFileProperty getPackageJson()

    @InputDirectory
    @PathSensitive(value = PathSensitivity.RELATIVE)
    abstract DirectoryProperty getNodeModules()

    @OutputFile
    abstract RegularFileProperty getAllImports()

    @TaskAction
    void resolveSolidity() {
        final imports = new JsonSlurper().parse(getPackageJson().get().asFile)['dependencies'] as Map<String, String>

        final all = ImportsResolver.resolveTransitive(imports.keySet(), getNodeModules().get().asFile)

        allImports.get().asFile.text = all.join('\n')
    }

}
