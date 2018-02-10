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
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.codehaus.plexus.PlexusTestCase;

import java.util.Collections;

/**
 * Test the POM verification check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CheckPomPhaseTest
    extends PlexusTestCase
{
    private ReleasePhase phase;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.class, "check-poms" );
    }

    public void testCorrectlyConfigured()
        throws Exception
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm:svn:file://localhost/tmp/repo" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), Collections.singletonList( createProject( "1.0-SNAPSHOT" ) ) );

        phase.simulate( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), Collections.singletonList( createProject( "1.0-SNAPSHOT" ) ) );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testGetUrlFromProjectConnection()
        throws Exception
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm:svn:file://localhost/tmp/repo" );

        MavenProject project = createProject( "1.0-SNAPSHOT" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), Collections.singletonList( project ) );

        assertEquals( "Check URL", "scm:svn:file://localhost/tmp/repo", ReleaseUtils.buildReleaseDescriptor( builder ).getScmSourceUrl() );
    }

    public void testGetUrlFromProjectConnectionSimulate()
        throws Exception
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm:svn:file://localhost/tmp/repo" );

        MavenProject project = createProject( "1.0-SNAPSHOT" );

        phase.simulate( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), Collections.singletonList( project ) );

        assertEquals( "Check URL", "scm:svn:file://localhost/tmp/repo", ReleaseUtils.buildReleaseDescriptor( builder ).getScmSourceUrl() );
    }

    public void testGetUrlFromProjectDevConnection()
        throws Exception
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm:svn:https://localhost/tmp/repo" );
        
        MavenProject project = createProject( "1.0-SNAPSHOT" );

        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), Collections.singletonList( project ) );

        assertEquals( "Check URL", "scm:svn:https://localhost/tmp/repo", ReleaseUtils.buildReleaseDescriptor( builder ).getScmSourceUrl() );
    }

    public void testGetUrlFromProjectDevConnectionSimulate()
        throws Exception
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm:svn:https://localhost/tmp/repo" );

        MavenProject project = createProject( "1.0-SNAPSHOT" );

        phase.simulate( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), Collections.singletonList( project ) );

        assertEquals( "Check URL", "scm:svn:https://localhost/tmp/repo", ReleaseUtils.buildReleaseDescriptor( builder ).getScmSourceUrl() );
    }

    public void testGetInvalidUrl()
        throws Exception
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm:cvs:" );

        MavenProject project = createProject( "1.0-SNAPSHOT" );

        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), Collections.singletonList( project ) );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseScmRepositoryException e )
        {
            assertTrue( true );
        }
    }

    public void testGetInvalidProvider()
        throws Exception
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        MavenProject project = createProject( "1.0-SNAPSHOT" );
        Scm scm = new Scm();
        scm.setConnection( "scm:foo:" );
        project.setScm( scm );

        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), Collections.singletonList( project ) );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testMissingUrl()
        throws Exception
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();

        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), Collections.singletonList( createProject( "1.0-SNAPSHOT" ) ) );

            fail( "Should have failed to execute" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), Collections.singletonList( createProject( "1.0-SNAPSHOT" ) ) );

            fail( "Should have failed to simulate" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testReleasingNonSnapshot()
        throws Exception
    {
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm:svn:file://localhost/tmp/repo" );

        try
        {
            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), Collections.singletonList( createProject( "1.0" ) ) );

            fail( "Should have failed to execute" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), Collections.singletonList( createProject( "1.0" ) ) );

            fail( "Should have failed to simulate" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    private static MavenProject createProject( String version )
    {
        Model model = new Model();

        model.setArtifactId( "artifactId" );
        model.setGroupId( "groupId" );
        model.setVersion( version );

        return new MavenProject( model );
    }
}
