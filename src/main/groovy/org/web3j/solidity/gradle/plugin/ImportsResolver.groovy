/*
 * Copyright 2021 Web3 Labs Ltd.
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

import groovy.io.FileType

import java.util.regex.Pattern

/**
 * Helper class to resolve the external imports from a Solidity file.
 *
 * The import resolving is done in three steps:
 * <ul>
 *     <li>
 *         First, all packages needed for direct imports are extracted from sol files to generate a package.json.
 *         This is done in a separate Gradle task, so that the next steps are only performed when direct imports change.
 *     </li>
 *     <li>
 *         Second, required packages are downloaded by npm.
 *     </li>
 *     <li>
 *         Third, sol files that were downloaded are analyzed as well and all packages required are collected.
 *         This information is used in compileSolidity for the allowed paths and the path remappings.
 *     </li>
 * </ul>
 */
class ImportsResolver {

    private static final IMPORT_PROVIDER_PATTERN = Pattern.compile(".*import.*['\"](@[^/]+/[^/]+).*");

    /**
     * Looks for external imports in Solidity files, eg:
     * <br>
     * <p>
     * <code>
     * import "@openzeppelin/contracts/token/ERC721/ERC721.sol";
     * </code>
     * </p>
     *
     * @param solFile where to search external imports
     * @param nodeProjectDir the Node.js project directory
     * @return
     */
    static Set<String> extractImports(final File solFile) {
        final Set<String> imports = new TreeSet<>()

        solFile.readLines().each { String line ->
            final importProviderMatcher = IMPORT_PROVIDER_PATTERN.matcher(line)
            final importFound = importProviderMatcher.matches()
            if (importFound) {
                final provider = importProviderMatcher.group(1)
                imports.add(provider)
            }
        }

        return imports
    }

    static Set<String> resolveTransitive(Set<String> directImports, File nodeModulesDir) {
        final Set<String> allImports = new TreeSet<>()
        if (directImports.isEmpty()) {
            return allImports
        }

        def transitiveResolved = 0
        allImports.addAll(directImports)

        while (transitiveResolved != allImports.size()) {
            transitiveResolved = allImports.size()
            allImports.collect().each { nodeModule ->
                final packageFolder = new File(nodeModulesDir, nodeModule)
                if (packageFolder.exists()) { // this may be a dev dependency from a test that we do not need
                    packageFolder.eachFileRecurse(FileType.FILES) { dependencyFile ->
                        if (dependencyFile.name.endsWith('.sol')) {
                            allImports.addAll(extractImports(dependencyFile))
                        }
                    }
                }
            }
        }

        return allImports
    }
}
