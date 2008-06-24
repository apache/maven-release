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

import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Site;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.release.ReleaseManager;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.ReleaseEnvironment;
import org.jmock.Mock;
import org.jmock.core.Constraint;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.constraint.IsInstanceOf;
import org.jmock.core.constraint.IsNull;
import org.jmock.core.matcher.InvokeOnceMatcher;

import java.io.File;

/**
 * Test release:perform.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class StageReleaseMojoTest
    extends AbstractMojoTestCase
{
    private File workingDirectory;

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

        Mock mock = new Mock( ReleaseManager.class );

        Constraint[] constraints = new Constraint[] {
            new IsEqual( releaseDescriptor ),
            new IsInstanceOf( ReleaseEnvironment.class ),
            new IsNull(),
            new IsEqual( Boolean.FALSE )
        };

        mock.expects( new InvokeOnceMatcher() ).method( "perform" ).with( constraints );
        mojo.setReleaseManager( (ReleaseManager) mock.proxy() );

        mojo.execute();

        assertTrue( true );
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
