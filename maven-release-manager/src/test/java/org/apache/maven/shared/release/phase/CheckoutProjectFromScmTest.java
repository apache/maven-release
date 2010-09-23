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

import java.io.File;
import java.util.List;

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
import org.jmock.Mock;
import org.jmock.core.Constraint;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.stub.ReturnStub;

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
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        File checkoutDirectory = getTestFile( "target/checkout-test/standard" );
        descriptor.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        descriptor.setScmReleaseLabel( "release-label" );
        String sourceUrl = "file://localhost/tmp/scm-repo/trunk";
        String scmUrl = "scm:svn:" + sourceUrl;
        descriptor.setScmSourceUrl( scmUrl );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository( sourceUrl );
        ScmRepository repository = new ScmRepository( "svn", scmProviderRepository );
        Constraint[] arguments =
            new Constraint[]{new IsEqual( repository ), new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ),
                new IsScmTagEquals( new ScmTag( "release-label" ) )};
        scmProviderMock.expects( new InvokeOnceMatcher() ).method( "checkOut" ).with( arguments ).will(
            new ReturnStub( new CheckOutScmResult( "", null ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );
        stub.addScmRepositoryForUrl( scmUrl, repository );

        List reactorProjects = createReactorProjects( "scm-commit", "/single-pom" );
        phase.execute( descriptor, new DefaultReleaseEnvironment(), reactorProjects );

        assertEquals( "", descriptor.getScmRelativePathProjectDirectory() );
    }

    public void testExecuteMultiModuleWithDeepSubprojects()
        throws Exception
    {
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        File checkoutDirectory = getTestFile( "target/checkout-test/multimodule-with-deep-subprojects" );
        descriptor.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        descriptor.setScmReleaseLabel( "release-label" );
        String sourceUrl = "file://localhost/tmp/scm-repo/trunk";
        String scmUrl = "scm:svn:" + sourceUrl;
        descriptor.setScmSourceUrl( scmUrl );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository( sourceUrl );
        ScmRepository repository = new ScmRepository( "svn", scmProviderRepository );
        Constraint[] arguments =
            new Constraint[]{new IsEqual( repository ), new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ),
                new IsScmTagEquals( new ScmTag( "release-label" ) )};
        scmProviderMock.expects( new InvokeOnceMatcher() ).method( "checkOut" ).with( arguments ).will(
            new ReturnStub( new CheckOutScmResult( "", null ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );
        stub.addScmRepositoryForUrl( scmUrl, repository );

        List reactorProjects = createReactorProjects( "scm-commit", "/multimodule-with-deep-subprojects" );
        phase.execute( descriptor, new DefaultReleaseEnvironment(), reactorProjects );

        assertEquals( "", descriptor.getScmRelativePathProjectDirectory() );
    }

    public void testExecuteFlatMultiModule()
        throws Exception
    {
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        File checkoutDirectory = getTestFile( "target/checkout-test/flat-multi-module" );
        descriptor.setCheckoutDirectory( checkoutDirectory.getAbsolutePath() );
        descriptor.setScmReleaseLabel( "release-label" );
        String sourceUrl = "file://localhost/tmp/scm-repo/trunk/root-project";
        String scmUrl = "scm:svn:" + sourceUrl;
        descriptor.setScmSourceUrl( scmUrl );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        SvnScmProviderRepository scmProviderRepository = new SvnScmProviderRepository( sourceUrl );
        ScmRepository repository = new ScmRepository( "svn", scmProviderRepository );
        Constraint[] arguments =
            new Constraint[]{new IsEqual( repository ), new IsScmFileSetEquals( new ScmFileSet( checkoutDirectory ) ),
                new IsScmTagEquals( new ScmTag( "release-label" ) )};
        scmProviderMock.expects( new InvokeOnceMatcher() ).method( "checkOut" ).with( arguments ).will(
            new ReturnStub( new CheckOutScmResult( "", null ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );
        stub.addScmRepositoryForUrl( scmUrl, repository );

        List reactorProjects = createReactorProjects( "rewrite-for-release/pom-with-parent-flat", "/root-project" );
        phase.execute( descriptor, new DefaultReleaseEnvironment(), reactorProjects );

        assertEquals( "root-project", descriptor.getScmRelativePathProjectDirectory() );
    }

}