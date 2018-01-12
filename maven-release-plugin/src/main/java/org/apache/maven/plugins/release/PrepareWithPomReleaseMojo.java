package org.apache.maven.plugins.release;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Prepare for a release in SCM, fully resolving dependencies for the purpose of producing a "release POM".
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @since 2.0
 */
@Mojo( name = "prepare-with-pom", aggregator = true, requiresDependencyResolution = ResolutionScope.TEST )
public class PrepareWithPomReleaseMojo
    extends PrepareReleaseMojo
{
    /**
     * Whether to generate <code>release-pom.xml</code> files that contain resolved information about the project.
     */
    @Parameter( defaultValue = "true", property = "generateReleasePoms" )
    private boolean generateReleasePoms;

    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        prepareRelease( generateReleasePoms );
    }
}
