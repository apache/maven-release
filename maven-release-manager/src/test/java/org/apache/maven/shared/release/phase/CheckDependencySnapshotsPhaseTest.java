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

import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.jmock.Mock;
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.stub.ReturnStub;
import org.jmock.core.stub.ThrowStub;

import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Test the dependency snapshot check phase.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CheckDependencySnapshotsPhaseTest
    extends AbstractReleaseTestCase
{
    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (ReleasePhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );
    }

    public void testNoSnapshotDependencies()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "no-snapshot-dependencies" );

        phase.execute( releaseDescriptor, null, reactorProjects );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotDependenciesInProjectOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "internal-snapshot-dependencies" );

        phase.execute( releaseDescriptor, null, reactorProjects );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotReleasePluginNonInteractive()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "snapshot-release-plugin" );
        releaseDescriptor.setInteractive( false );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotReleasePluginInteractiveDeclined()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "snapshot-release-plugin" );

        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will( new ReturnStub( "no" ) );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will( new ReturnStub( "no" ) );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        mockPrompter.reset();
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will( new ReturnStub( "no" ) );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will( new ReturnStub( "no" ) );

        try
        {
            phase.simulate( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotReleasePluginInteractiveAcceptedForExecution()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "snapshot-release-plugin" );

        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will( new ReturnStub( "yes" ) );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        phase.execute( releaseDescriptor, null, reactorProjects );

        assertTrue( true );
    }

    public void testSnapshotReleasePluginInteractiveAcceptedForSimulationtion()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "snapshot-release-plugin" );

        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will( new ReturnStub( "yes" ) );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        assertTrue( true );
    }

    public void testSnapshotReleasePluginInteractiveInvalid()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "snapshot-release-plugin" );

        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will( new ReturnStub( "donkey" ) );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will( new ReturnStub( "no" ) );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        mockPrompter.reset();
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will( new ReturnStub( "donkey" ) );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will( new ReturnStub( "no" ) );

        try
        {
            phase.simulate( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotReleasePluginInteractiveException()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "snapshot-release-plugin" );

        Mock mockPrompter = new Mock( Prompter.class );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will(
            new ThrowStub( new PrompterException( "..." ) ) );
        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "Check cause", PrompterException.class, e.getCause().getClass() );
        }

        mockPrompter.reset();
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will(
            new ThrowStub( new PrompterException( "..." ) ) );

        try
        {
            phase.simulate( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "Check cause", PrompterException.class, e.getCause().getClass() );
        }
    }

    public void testSnapshotDependenciesInProjectOnlyMismatchedVersion()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "internal-differing-snapshot-dependencies" );
        Mock noPrompter = createNoMockPrompter();

        phase.setPrompter( (Prompter) noPrompter.proxy() );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        resetNoMockPrompter( noPrompter );

        try
        {
            phase.simulate( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotManagedDependenciesInProjectOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "internal-managed-snapshot-dependency" );

        phase.execute( releaseDescriptor, null, reactorProjects );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotUnusedInternalManagedDependency()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "unused-internal-managed-snapshot-dependency" );

        phase.execute( releaseDescriptor, null, reactorProjects );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotUnusedExternalManagedDependency()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "unused-external-managed-snapshot-dependency" );

        phase.execute( releaseDescriptor, null, reactorProjects );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotExternalManagedDependency()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "external-managed-snapshot-dependency" );

        releaseDescriptor.setInteractive( false );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotDependenciesOutsideProjectOnlyNonInteractive()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "external-snapshot-dependencies" );

        releaseDescriptor.setInteractive( false );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        try
        {
            phase.simulate( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotDependenciesOutsideProjectOnlyInteractiveWithSnapshotsResolved()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "external-snapshot-dependencies" );
        Mock mockPrompter;
        Stack responseStack = new Stack();

        responseStack.push( "yes" );
        responseStack.push( "1" );
        responseStack.push( "yes" );
        responseStack.push( "1.1-SNAPSHOT" );
        mockPrompter = createMockPrompter( "prompt", responseStack );

        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );

        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );
        }
        catch ( ReleaseFailureException e )
        {
            fail( e.getMessage() );
        }

        // validate
        Map versionsMap = (Map) releaseDescriptor.getResolvedSnapshotDependencies().get( "external:artifactId" );

        assertNotNull( versionsMap );
        assertEquals( "1.1-SNAPSHOT", versionsMap.get( ReleaseDescriptor.DEVELOPMENT_KEY ) );
        assertEquals( "1.0", versionsMap.get( ReleaseDescriptor.RELEASE_KEY ) );

        releaseDescriptor = new ReleaseDescriptor();

        responseStack.push( "yes" );
        responseStack.push( "1" );
        responseStack.push( "yes" );
        responseStack.push( "1.1-SNAPSHOT" );
        mockPrompter = createMockPrompter( "prompt", responseStack );

        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );

        phase.setPrompter( (Prompter) mockPrompter.proxy() );

        try
        {
            phase.simulate( releaseDescriptor, null, reactorProjects );
        }
        catch ( ReleaseFailureException e )
        {
            fail( e.getMessage() );
        }
    }

    public void testSnapshotDependenciesInsideAndOutsideProject()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "internal-and-external-snapshot-dependencies" );
        Mock noPrompter = createNoMockPrompter();

        phase.setPrompter( (Prompter) noPrompter.proxy() );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        resetNoMockPrompter( noPrompter );

        try
        {
            phase.simulate( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testNoSnapshotReportPlugins()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "no-snapshot-report-plugins" );

        phase.execute( releaseDescriptor, null, reactorProjects );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotReportPluginsInProjectOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "internal-snapshot-report-plugins" );

        phase.execute( releaseDescriptor, null, reactorProjects );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotReportPluginsOutsideProjectOnly()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "external-snapshot-report-plugins" );
        Mock noPrompter = createNoMockPrompter();

        phase.setPrompter( (Prompter) noPrompter.proxy() );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        resetNoMockPrompter( noPrompter );

        try
        {
            phase.simulate( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotReportPluginsInsideAndOutsideProject()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "internal-and-external-snapshot-report-plugins" );
        Mock noPrompter = createNoMockPrompter();

        phase.setPrompter( (Prompter) noPrompter.proxy() );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        resetNoMockPrompter( noPrompter );

        try
        {
            phase.simulate( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testNoSnapshotPlugins()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "no-snapshot-plugins" );

        phase.execute( releaseDescriptor, null, reactorProjects );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotPluginsInProjectOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "internal-snapshot-plugins" );

        phase.execute( releaseDescriptor, null, reactorProjects );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotManagedPluginInProjectOnly()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "internal-managed-snapshot-plugin" );

        phase.execute( releaseDescriptor, null, reactorProjects );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotUnusedInternalManagedPlugin()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "unused-internal-managed-snapshot-plugin" );

        phase.execute( releaseDescriptor, null, reactorProjects );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotUnusedExternalManagedPlugin()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "unused-external-managed-snapshot-plugin" );

        phase.execute( releaseDescriptor, null, reactorProjects );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotExternalManagedPlugin()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "external-managed-snapshot-plugin" );
        Mock noPrompter = createNoMockPrompter();

        phase.setPrompter( (Prompter) noPrompter.proxy() );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        resetNoMockPrompter( noPrompter );

        try
        {
            phase.simulate( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotPluginsOutsideProjectOnly()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "external-snapshot-plugins" );
        Mock noPrompter = createNoMockPrompter();

        phase.setPrompter( (Prompter) noPrompter.proxy() );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        resetNoMockPrompter( noPrompter );

        try
        {
            phase.simulate( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotPluginsInsideAndOutsideProject()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "internal-and-external-snapshot-plugins" );
        Mock noPrompter = createNoMockPrompter();

        phase.setPrompter( (Prompter) noPrompter.proxy() );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        resetNoMockPrompter( noPrompter );

        try
        {
            phase.simulate( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotExternalParent()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "external-snapshot-parent/child" );
        Mock noPrompter = createNoMockPrompter();

        phase.setPrompter( (Prompter) noPrompter.proxy() );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        resetNoMockPrompter( noPrompter );

        try
        {
            phase.simulate( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testReleaseExternalParent()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "external-parent/child" );

        phase.execute( releaseDescriptor, null, reactorProjects );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testSnapshotExternalExtension()
        throws Exception
    {
        CheckDependencySnapshotsPhase phase =
            (CheckDependencySnapshotsPhase) lookup( ReleasePhase.ROLE, "check-dependency-snapshots" );

        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "external-snapshot-extension" );
        Mock noPrompter = createNoMockPrompter();

        phase.setPrompter( (Prompter) noPrompter.proxy() );

        try
        {
            phase.execute( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }

        resetNoMockPrompter( noPrompter );

        try
        {
            phase.simulate( releaseDescriptor, null, reactorProjects );

            fail( "Should have failed execution" );
        }
        catch ( ReleaseFailureException e )
        {
            assertTrue( true );
        }
    }

    public void testSnapshotInternalExtension()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "internal-snapshot-extension" );

        phase.execute( releaseDescriptor, null, reactorProjects );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    public void testReleaseExternalExtension()
        throws Exception
    {
        ReleaseDescriptor releaseDescriptor = new ReleaseDescriptor();
        List reactorProjects = createDescriptorFromProjects( "external-extension" );

        phase.execute( releaseDescriptor, null, reactorProjects );

        phase.simulate( releaseDescriptor, null, reactorProjects );

        // successful execution is verification enough
        assertTrue( true );
    }

    private List createDescriptorFromProjects( String path )
        throws Exception
    {
        return createReactorProjects( "check-dependencies/", path, true );
    }

    private Mock createNoMockPrompter()
    {
        return createYesNoMockPrompter( false );
    }

    private Mock createYesMockPrompter()
    {
        return createYesNoMockPrompter( true );
    }

    private Mock createYesNoMockPrompter( boolean yes )
    {
        Mock mockPrompter = new Mock( Prompter.class );

        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will(
            new ReturnStub( ( yes ) ? "yes" : "no" ) );

        return mockPrompter;
    }

    private Mock resetNoMockPrompter( Mock mockPrompter )
    {
        return resetYesNoMockPrompter( mockPrompter, false );
    }

    private Mock resetYesMockPrompter( Mock mockPrompter )
    {
        return resetYesNoMockPrompter( mockPrompter, true );
    }

    private Mock resetYesNoMockPrompter( Mock mockPrompter, boolean yes )
    {
        mockPrompter.reset();
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "showMessage" );
        mockPrompter.expects( new InvokeOnceMatcher() ).method( "prompt" ).will(
            new ReturnStub( ( yes ) ? "yes" : "no" ) );

        return mockPrompter;
    }

    private Mock createMockPrompter( String methodName, Stack responseStack )
    {
        Mock mockPrompter = new Mock( Prompter.class );

        while ( !responseStack.empty() )
        {
            mockPrompter.expects( new InvokeOnceMatcher() ).method( methodName ).will(
                new ReturnStub( responseStack.pop() ) );
        }

        return mockPrompter;
    }
}
