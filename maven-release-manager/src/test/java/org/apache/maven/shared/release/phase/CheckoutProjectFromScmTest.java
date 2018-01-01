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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.CommandParameters;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.scm.ReleaseScmRepositoryException;
import org.apache.maven.shared.release.stubs.ScmManagerStub;
import org.junit.Test;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CheckoutProjectFromScmTest
    extends AbstractReleaseTestCase
{
    private CheckoutProjectFromScm phase;

    public void setUp()
        throws Exception
    {
        super.setUp();

        phase = (CheckoutProjectFromScm) lookup( ReleasePhase.ROLE, "checkout-project-from-scm" );
    }

    @Test
    public void testExecuteStandard()
        throws Exception
    {
        // prepare
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        File checkoutDirectory = getTestFile( "target/checkout-test/standard" );
        descriptor.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        descriptor.setScmReleaseLabel( "release-label" );
        String sourceUrl = "file://localhost/tmp/scm-repo/trunk";
        String scmUrl = "scm:svn:" + sourceUrl;
        descriptor.setScmSourceUrl( scmUrl );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository( sourceUrl );
        ScmRepository repository = new ScmRepository( "svn", scmProviderRepository );
        when( scmProviderMock.checkOut( eq( repository ),
                                        argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                        argThat( new IsScmTagEquals( new ScmTag( "release-label" ) ) ),
                                        any( CommandParameters.class)))
            .thenReturn( new CheckOutScmResult( "",null ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );
        stub.addScmRepositoryForUrl( scmUrl, repository );

        List<MavenProject> reactorProjects = createReactorProjects( "scm-commit", "single-pom" );

        // execute
        phase.execute( descriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // prepare
        assertEquals( "", descriptor.getScmRelativePathProjectDirectory() );

        verify( scmProviderMock ).checkOut( eq( repository ),
                                            argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                            argThat( new IsScmTagEquals( new ScmTag( "release-label" ) ) ),
                                            any( CommandParameters.class ));
        verifyNoMoreInteractions( scmProviderMock );
    }

    @Test
    public void testExecuteMultiModuleWithDeepSubprojects()
        throws Exception
    {
        // prepare
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        File checkoutDirectory = getTestFile( "target/checkout-test/multimodule-with-deep-subprojects" );
        descriptor.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        descriptor.setScmReleaseLabel( "release-label" );
        String sourceUrl = "file://localhost/tmp/scm-repo/trunk";
        String scmUrl = "scm:svn:" + sourceUrl;
        descriptor.setScmSourceUrl( scmUrl );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository( sourceUrl );
        ScmRepository repository = new ScmRepository( "svn", scmProviderRepository );
        when( scmProviderMock.checkOut( eq( repository ),
                                        argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                        argThat( new IsScmTagEquals( new ScmTag( "release-label" ) ) ),
                                        any(CommandParameters.class)))
            .thenReturn( new CheckOutScmResult( "", null ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );
        stub.addScmRepositoryForUrl( scmUrl, repository );

        List<MavenProject> reactorProjects =
            createReactorProjects( "scm-commit", "multimodule-with-deep-subprojects" );

        // execute
        phase.execute( descriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "", descriptor.getScmRelativePathProjectDirectory() );

        verify( scmProviderMock ).checkOut( eq( repository ),
                                            argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                            argThat( new IsScmTagEquals( new ScmTag( "release-label" ) ) ),
                                            any( CommandParameters.class ));
        verifyNoMoreInteractions( scmProviderMock );
    }

    @Test
    public void testExecuteFlatMultiModule()
        throws Exception
    {
        // prepare
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        File checkoutDirectory = getTestFile( "target/checkout-test/flat-multi-module" );
        descriptor.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        descriptor.setScmReleaseLabel( "release-label" );
        String sourceUrl = "file://localhost/tmp/scm-repo/trunk/root-project";
        String scmUrl = "scm:svn:" + sourceUrl;
        descriptor.setScmSourceUrl( scmUrl );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository( sourceUrl );
        ScmRepository repository = new ScmRepository( "svn", scmProviderRepository );
        when( scmProviderMock.checkOut( eq( repository ),
                                        argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                        argThat( new IsScmTagEquals( new ScmTag( "release-label" ) ) ),
                                        any( CommandParameters.class )) )
            .thenReturn( new CheckOutScmResult( "",null ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );
        stub.addScmRepositoryForUrl( scmUrl, repository );

        List<MavenProject> reactorProjects =
            createReactorProjects( "rewrite-for-release/pom-with-parent-flat", "root-project" );

        // execute
        phase.execute( descriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "not found root-project but " + descriptor.getScmRelativePathProjectDirectory(), "root-project",
                      descriptor.getScmRelativePathProjectDirectory() );

        verify( scmProviderMock ).checkOut( eq( repository ),
                                            argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                            argThat( new IsScmTagEquals( new ScmTag( "release-label" ) ) ),
                                            any( CommandParameters.class ));
        verifyNoMoreInteractions( scmProviderMock );
    }
    
    @Test
    public void testNoSuchScmProviderExceptionThrown()
                    throws Exception
    {
        // prepare
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        releaseDescriptor.setWorkingDirectory( getTestFile( "target/test/checkout" ).getAbsolutePath() );
        
        ScmManagerStub scmManagerStub = (ScmManagerStub) lookup( ScmManager.ROLE );
        scmManagerStub.setException( new NoSuchScmProviderException( "..." )  );

        List<MavenProject> reactorProjects = createReactorProjects( "scm-commit", "single-pom" );
        
        // execute
        try
        {
            releaseDescriptor.setUseReleaseProfile( false );

            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "commit should have failed" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "check cause", NoSuchScmProviderException.class, e.getCause().getClass() );
        }
    }

    @Test
    public void testScmRepositoryExceptionThrown()
        throws Exception
    {
        // prepare
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        releaseDescriptor.setScmSourceUrl( "scm-url" );
        releaseDescriptor.setWorkingDirectory( getTestFile( "target/test/checkout" ).getAbsolutePath() );

        ScmManagerStub scmManagerStub = (ScmManagerStub) lookup( ScmManager.ROLE );
        scmManagerStub.setException( new ScmRepositoryException( "..." )  );

        List<MavenProject> reactorProjects = createReactorProjects( "scm-commit", "single-pom" );
        
        // execute
        try
        {
            releaseDescriptor.setUseReleaseProfile( false );

            phase.execute( releaseDescriptor, new DefaultReleaseEnvironment(), reactorProjects );

            fail( "commit should have failed" );
        }
        catch ( ReleaseScmRepositoryException e )
        {
            assertNull( "Check no additional cause", e.getCause() );
        }
    }

}