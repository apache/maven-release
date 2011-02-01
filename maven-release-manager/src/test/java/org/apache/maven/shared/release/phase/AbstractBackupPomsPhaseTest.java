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

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.util.ReaderFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Edwin Punzalan
 */
public abstract class AbstractBackupPomsPhaseTest
    extends PlexusTestCase
{
    private final String pomFilename = "pom.xml";

    protected final String releaseBackupSuffix = ".releaseBackup";

    protected ReleasePhase phase;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = getReleasePhase();
    }

    abstract ReleasePhase getReleasePhase()
        throws Exception;

    protected List<MavenProject> getReactorProjects( String projectPath )
        throws Exception
    {
        List<MavenProject> reactorProjects = new ArrayList<MavenProject>();

        File pomFile = new File( projectPath, pomFilename );

        MavenProject mainProject = createMavenProject( pomFile );

        reactorProjects.add( mainProject );

        for( Iterator modules = mainProject.getModel().getModules().iterator(); modules.hasNext(); )
        {
            String module = modules.next().toString();

            File modulePom = new File( projectPath + "/" + module, pomFilename );

            MavenProject subproject = createMavenProject( modulePom );

            reactorProjects.add( subproject );
        }

        return reactorProjects;
    }

    private MavenProject createMavenProject( File pomFile )
        throws Exception
    {
        MavenXpp3Reader reader = new MavenXpp3Reader();

        Model model = reader.read( ReaderFactory.newXmlReader( pomFile ) );

        MavenProject project = new MavenProject( model );

        project.setFile( pomFile );

        return project;
    }

}
