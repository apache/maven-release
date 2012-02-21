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

import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;

import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.scm.provider.svn.repository.SvnScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.stubs.ScmManagerStub;

/**
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CheckoutProjectFromScmTest
    extends AbstractReleaseTestCase
{
    private CheckoutProjectFromScm phase;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (CheckoutProjectFromScm) lookup( ReleasePhase.ROLE, "checkout-project-from-scm" );
    }

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
        when( scmProviderMock.checkOut( eq( repository), 
                                        argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ), 
                                        argThat( new IsScmTagEquals( new ScmTag( "release-label" ) ) ) ) ).thenReturn( new CheckOutScmResult( "", null ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );
        stub.addScmRepositoryForUrl( scmUrl, repository );

        List<MavenProject> reactorProjects = createReactorProjects( "scm-commit", "/single-pom" );
        
        // execute
        phase.execute( descriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // prepare
        assertEquals( "", descriptor.getScmRelativePathProjectDirectory() );
        
        verify( scmProviderMock ).checkOut( eq( repository), 
                                            argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ), 
                                            argThat( new IsScmTagEquals( new ScmTag( "release-label" ) ) ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

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
                                        argThat( new IsScmTagEquals( new ScmTag( "release-label" ) ) ) ) ).thenReturn( new CheckOutScmResult( "", null ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );
        stub.addScmRepositoryForUrl( scmUrl, repository );

        List<MavenProject> reactorProjects = createReactorProjects( "scm-commit", "/multimodule-with-deep-subprojects" );
        
        // execute
        phase.execute( descriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "", descriptor.getScmRelativePathProjectDirectory() );
        
        verify( scmProviderMock ).checkOut( eq( repository ), 
                                            argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                            argThat( new IsScmTagEquals( new ScmTag( "release-label" ) ) ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

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
                                        argThat( new IsScmTagEquals( new ScmTag( "release-label" ) ) ) ) ).thenReturn( new CheckOutScmResult( "", null ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( scmProviderMock );
        stub.addScmRepositoryForUrl( scmUrl, repository );

        List<MavenProject> reactorProjects = createReactorProjects( "rewrite-for-release/pom-with-parent-flat", "/root-project" );
        
        // execute
        phase.execute( descriptor, new DefaultReleaseEnvironment(), reactorProjects );

        // verify
        assertEquals( "not found root-project but " + descriptor.getScmRelativePathProjectDirectory(), "root-project",
                      descriptor.getScmRelativePathProjectDirectory() );
        
        verify( scmProviderMock ).checkOut( eq( repository ), 
                                            argThat( new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ) ),
                                            argThat( new IsScmTagEquals( new ScmTag( "release-label" ) ) ) );
        verifyNoMoreInteractions( scmProviderMock );
    }

}