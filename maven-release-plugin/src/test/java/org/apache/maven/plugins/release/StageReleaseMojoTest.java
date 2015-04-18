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
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;
import java.util.List;

import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Site;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseManager;
import org.apache.maven.shared.release.ReleasePerformRequest;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.mockito.ArgumentCaptor;

/**
 * Test release:stage.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class StageReleaseMojoTest
    extends AbstractMojoTestCase
{
    private File workingDirectory;

    @SuppressWarnings( "unchecked" )
    public void testStage()
        throws Exception
    {
        StageReleaseMojo mojo = getMojoWithProjectSite( "stage.xml" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setWorkingDirectory( workingDirectory.getAbsolutePath() );
        File checkoutDirectory = getTestFile( "target/checkout" );
        releaseDescriptor.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        releaseDescriptor.setPerformGoals( "deploy site:stage-deploy" );
        releaseDescriptor.setAdditionalArguments( "-DaltDeploymentRepository=\"staging\"" );

        ReleasePerformRequest performRequest = new ReleasePerformRequest();
        performRequest.setReleaseDescriptor( releaseDescriptor );
        performRequest.setReleaseEnvironment( mojo.getReleaseEnvironment() );
        performRequest.setReactorProjects( mojo.getReactorProjects() );
        performRequest.setDryRun( false );

        ReleaseManager mock = mock( ReleaseManager.class );
        mojo.setReleaseManager( mock );

        mojo.execute();

        // verify
        ArgumentCaptor<ReleasePerformRequest> argument = ArgumentCaptor.forClass(ReleasePerformRequest.class);
        verify( mock ).perform( argument.capture() );
        assertEquals( releaseDescriptor, argument.getValue().getReleaseDescriptor() );
        assertNotNull( argument.getValue().getReleaseEnvironment() );
        assertNull( argument.getValue().getReactorProjects() );
        assertEquals( Boolean.FALSE, argument.getValue().getDryRun() );
        verifyNoMoreInteractions( mock );
    }

    public void testCreateGoals()
        throws Exception
    {
        StageReleaseMojo mojo = getMojoWithProjectSite( "stage.xml" );
        mojo.createGoals();
        assertEquals( "deploy site:stage-deploy", mojo.goals );
        mojo.goals = "deploy site:deploy";
        mojo.createGoals();
        assertEquals( "deploy site:stage-deploy", mojo.goals );
    }

    private StageReleaseMojo getMojoWithProjectSite( String fileName )
        throws Exception
    {
        StageReleaseMojo mojo = (StageReleaseMojo) lookupMojo( "stage", new File( workingDirectory, fileName ) );
        mojo.setBasedir( workingDirectory );

        MavenProject project = (MavenProject) getVariableValueFromObject( mojo, "project" );
        DistributionManagement distributionManagement = new DistributionManagement();
        distributionManagement.setSite( new Site() );
        project.setDistributionManagement( distributionManagement );

        return mojo;
    }

    protected void setUp()
        throws Exception
    {
        super.setUp();
        workingDirectory = getTestFile( "target/test-classes/mojos/stage" );
    }
}
