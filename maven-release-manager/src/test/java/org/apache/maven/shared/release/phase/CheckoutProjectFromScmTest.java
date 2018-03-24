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
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;
import org.apache.maven.shared.release.config.ReleaseUtils;
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

    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        phase = (CheckoutProjectFromScm) lookup( ReleasePhase.class, "checkout-project-from-scm" );
    }

    @Test
    public void testExecuteStandard()
        throws Exception
    {
        // prepare
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        File checkoutDirectory = getTestFile( "target/checkout-test/standard" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        builder.setScmReleaseLabel( "release-label" );
        String sourceUrl = "file://localhost/tmp/scm-repo/trunk";
        String scmUrl = "scm:svn:" + sourceUrl;
        builder.setScmSourceUrl( scmUrl );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository( sourceUrl );
        ScmRepository repository = new ScmRepository( "svn", scmProviderRepository );
        when( scmProviderMock.checkOut( eq( repository ),
                                        argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                        argThat( new IsScmTagEquals( new ScmTag( "release-label" ) ) ),
                                        any( CommandParameters.class)))
            .thenReturn( new CheckOutScmResult( "",null ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );
        stub.addScmRepositoryForUrl( scmUrl, repository );

        String dir = "scm-commit/single-pom";
        List<MavenProject> reactorProjects = createReactorProjects( dir, dir, null );
        builder.setWorkingDirectory( getWorkingDirectory( dir ).toString() );

        // execute
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        // prepare
        assertEquals( "", ReleaseUtils.buildReleaseDescriptor( builder ).getScmRelativePathProjectDirectory() );

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
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        File checkoutDirectory = getTestFile( "target/checkout-test/multimodule-with-deep-subprojects" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        builder.setScmReleaseLabel( "release-label" );
        String sourceUrl = "file://localhost/tmp/scm-repo/trunk";
        String scmUrl = "scm:svn:" + sourceUrl;
        builder.setScmSourceUrl( scmUrl );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository( sourceUrl );
        ScmRepository repository = new ScmRepository( "svn", scmProviderRepository );
        when( scmProviderMock.checkOut( eq( repository ),
                                        argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                        argThat( new IsScmTagEquals( new ScmTag( "release-label" ) ) ),
                                        any(CommandParameters.class)))
            .thenReturn( new CheckOutScmResult( "", null ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );
        stub.addScmRepositoryForUrl( scmUrl, repository );

        String dir = "scm-commit/multimodule-with-deep-subprojects";
        List<MavenProject> reactorProjects = createReactorProjects( dir, dir, null );
        builder.setWorkingDirectory( getWorkingDirectory( dir ).toString() );

        // execute
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "", ReleaseUtils.buildReleaseDescriptor( builder ).getScmRelativePathProjectDirectory() );

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
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        File checkoutDirectory = getTestFile( "target/checkout-test/flat-multi-module" );
        builder.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        builder.setScmReleaseLabel( "release-label" );
        String sourceUrl = "file://localhost/tmp/scm-repo/trunk/root-project";
        String scmUrl = "scm:svn:" + sourceUrl;
        builder.setScmSourceUrl( scmUrl );

        ScmProvider scmProviderMock = mock( ScmProvider.class );
        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository( sourceUrl );
        ScmRepository repository = new ScmRepository( "svn", scmProviderRepository );
        when( scmProviderMock.checkOut( eq( repository ),
                                        argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                        argThat( new IsScmTagEquals( new ScmTag( "release-label" ) ) ),
                                        any( CommandParameters.class )) )
            .thenReturn( new CheckOutScmResult( "",null ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.class );
        stub.setScmProvider( scmProviderMock );
        stub.addScmRepositoryForUrl( scmUrl, repository );

        List<MavenProject> reactorProjects =
            createReactorProjects( "rewrite-for-release/pom-with-parent-flat", "root-project" );
        builder.setWorkingDirectory( getWorkingDirectory( "rewrite-for-release/pom-with-parent-flat" ) .toString() );

        // execute
        phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "not found root-project but " + ReleaseUtils.buildReleaseDescriptor( builder ).getScmRelativePathProjectDirectory(), "root-project",
                      ReleaseUtils.buildReleaseDescriptor( builder ).getScmRelativePathProjectDirectory() );

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
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        builder.setWorkingDirectory( getTestFile( "target/test/checkout" ).getAbsolutePath() );

        ScmManagerStub scmManagerStub = (ScmManagerStub) lookup( ScmManager.class );
        scmManagerStub.setException( new NoSuchScmProviderException( "..." )  );

        String dir = "scm-commit/single-pom";
        List<MavenProject> reactorProjects = createReactorProjects( dir, dir, null );

        // execute
        try
        {
            builder.setUseReleaseProfile( false );

            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

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
        ReleaseDescriptorBuilder builder = new ReleaseDescriptorBuilder();
        builder.setScmSourceUrl( "scm-url" );
        builder.setWorkingDirectory( getTestFile( "target/test/checkout" ).getAbsolutePath() );

        ScmManagerStub scmManagerStub = (ScmManagerStub) lookup( ScmManager.class );
        scmManagerStub.setException( new ScmRepositoryException( "..." )  );

        String dir = "scm-commit/single-pom";
        List<MavenProject> reactorProjects = createReactorProjects( dir, dir, null );

        // execute
        try
        {
            builder.setUseReleaseProfile( false );

            phase.execute( ReleaseUtils.buildReleaseDescriptor( builder ), new DefaultReleaseEnvironment(), reactorProjects );

            fail( "commit should have failed" );
        }
        catch ( ReleaseScmRepositoryException e )
        {
            assertNull( "Check no additional cause", e.getCause() );
        }
    }

}