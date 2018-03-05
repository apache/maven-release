package org.apache.maven.shared.release.phase;

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

import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseResult;

/**
 * Additional interface for ReleasePhase if the phase generates resources, which should be cleaned up afterwards.
 * 
 * @author Robert Scholte
 * @since 3.0.0
 */
public interface ResourceGenerator
{
    /**
     * Clean up after a phase if it leaves any additional files in the checkout.
     *
     * @param reactorProjects the reactor projects
     * @return the release result
     */
    ReleaseResult clean( List<MavenProject> reactorProjects );
}
