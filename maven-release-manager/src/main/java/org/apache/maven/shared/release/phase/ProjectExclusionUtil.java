/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.shared.release.phase;

import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.project.MavenProject;

final class ProjectExclusionUtil {
    private ProjectExclusionUtil() {
        // utility class
    }

    /**
     * When every reactor POM matches, the patterns are used only for the SCM modification check. Excluding the
     * entire reactor from release operations would otherwise report success without changing or committing any POM.
     */
    static Set<MavenProject> getExcludedProjects(
            List<MavenProject> reactorProjects, List<String> checkModificationExcludes) {
        if (checkModificationExcludes == null || checkModificationExcludes.isEmpty()) {
            return Collections.emptySet();
        }

        List<PathMatcher> matchers = new ArrayList<>(checkModificationExcludes.size());
        for (String pattern : checkModificationExcludes) {
            matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + pattern));
        }

        Set<MavenProject> excludedProjects = new HashSet<>();
        for (MavenProject project : reactorProjects) {
            if (project.getFile() != null
                    && matchers.stream()
                            .anyMatch(
                                    matcher -> matcher.matches(project.getFile().toPath()))) {
                excludedProjects.add(project);
            }
        }

        return excludedProjects.size() == reactorProjects.size() ? Collections.emptySet() : excludedProjects;
    }
}
