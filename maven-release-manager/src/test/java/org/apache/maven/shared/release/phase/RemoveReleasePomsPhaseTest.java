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

import org.apache.maven.Maven;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.command.remove.RemoveScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.manager.ScmManagerStub;
import org.apache.maven.scm.provider.ScmProvider;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.apache.maven.shared.release.util.ReleaseUtil;
import org.jmock.Mock;
import org.jmock.core.Constraint;
import org.jmock.core.constraint.IsAnything;
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.matcher.TestFailureMatcher;
import org.jmock.core.stub.ReturnStub;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Test the remove release POMs phase.
 *
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 */
public class RemoveReleasePomsPhaseTest
    extends AbstractReleaseTestCase
{
    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "remove-release-poms" );
    }

    public void testExecuteBasicPom()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "basic-pom" );
        ReleaseDescriptor config = createReleaseDescriptor();
        MavenProject project = ReleaseUtil.getRootProject( reactorProjects );

        File releasePom = ReleaseUtil.getReleasePom( project );
        ScmFileSet fileSet = new ScmFileSet( new File( config.getWorkingDirectory() ), releasePom );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        Constraint[] arguments = new Constraint[] { new IsAnything(), new IsScmFileSetEquals( fileSet ), new IsAnything() };
        scmProviderMock
            .expects( new InvokeOnceMatcher() )
            .method( "remove" )
            .with( arguments )
            .will( new ReturnStub( new RemoveScmResult( "...", Collections
                       .singletonList( new ScmFile( Maven.RELEASE_POMv4, ScmFileStatus.DELETED ) ) ) ) );

        
        
        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        scmProviderMock.verify();
    }

    public void testExecutePomWithModules()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "pom-with-modules" );
        ReleaseDescriptor config = createReleaseDescriptor();

        List releasePoms = new ArrayList();
        for ( Iterator iterator = reactorProjects.iterator(); iterator.hasNext(); )
        {
            MavenProject project = (MavenProject) iterator.next();
            File releasePom = ReleaseUtil.getReleasePom( project );
            releasePoms.add( releasePom );
        }

        ScmFileSet fileSet = new ScmFileSet( new File( config.getWorkingDirectory() ), releasePoms );

        Mock scmProviderMock = new Mock( ScmProvider.class );
        Constraint[] arguments = new Constraint[] { new IsAnything(), new IsScmFileSetEquals( fileSet ), new IsAnything() };
        scmProviderMock
            .expects( new InvokeOnceMatcher() )
            .method( "remove" )
            .with( arguments )
            .will( new ReturnStub( new RemoveScmResult( "...", Collections
                       .singletonList( new ScmFile( Maven.RELEASE_POMv4, ScmFileStatus.DELETED ) ) ) ) );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        phase.execute( config, new DefaultReleaseEnvironment(), reactorProjects );

        scmProviderMock.verify();
    }

    public void testSimulateBasicPom()
        throws Exception
    {
        List reactorProjects = createReactorProjects( "basic-pom" );
        ReleaseDescriptor config = createReleaseDescriptor();

        Mock scmProviderMock = new Mock( ScmProvider.class );
        scmProviderMock.expects( new TestFailureMatcher( "Shouldn't have called remove" ) ).method( "remove" );

        ScmManagerStub stub = (ScmManagerStub) lookup( ScmManager.ROLE );
        stub.setScmProvider( (ScmProvider) scmProviderMock.proxy() );

        phase.simulate( config, new DefaultReleaseEnvironment(), reactorProjects );

        scmProviderMock.verify();
    }

    private List createReactorProjects( String path )
        throws Exception
    {
        return createReactorProjects( "remove-release-poms/", path );
    }

    private ReleaseDescriptor createReleaseDescriptor()
    {
        ReleaseDescriptor descriptor = new ReleaseDescriptor();
        descriptor.setGenerateReleasePoms( true );
        descriptor.setWorkingDirectory( getTestFile( "target/test/checkout" ).getAbsolutePath() );
        return descriptor;
    }
}
