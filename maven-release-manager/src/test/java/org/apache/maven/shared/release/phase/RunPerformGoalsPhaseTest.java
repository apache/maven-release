package org.apache.maven.shared.release.phase;

import org.apache.maven.settings.Settings;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.config.ReleaseDescriptor;
import org.apache.maven.shared.release.exec.MavenExecutor;
import org.apache.maven.shared.release.exec.MavenExecutorException;
import org.codehaus.plexus.PlexusTestCase;
import org.jmock.Mock;
import org.jmock.core.Constraint;
import org.jmock.core.constraint.IsAnything;
import org.jmock.core.constraint.IsEqual;
import org.jmock.core.matcher.InvokeOnceMatcher;
import org.jmock.core.stub.ThrowStub;

import java.io.File;
import java.util.List;

/**
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @version $Id$
 */
public class RunPerformGoalsPhaseTest
    extends PlexusTestCase
{
    private RunPerformGoalsPhase phase;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        phase = (RunPerformGoalsPhase) lookup( ReleasePhase.ROLE, "run-perform-goals" );
    }

    public void testExecuteException()
        throws Exception
    {
        File testFile = getTestFile( "target/checkout-directory" );

        ReleaseDescriptor config = new ReleaseDescriptor();
        config.setPerformGoals( "goal1 goal2" );
        config.setCheckoutDirectory( testFile.getAbsolutePath() );

        Mock mock = new Mock( MavenExecutor.class );
        Constraint[] constraints = new Constraint[]{new IsEqual( testFile ), new IsEqual( "goal1 goal2" ),
            new IsEqual( Boolean.TRUE ), new IsEqual( "-DperformRelease=true" ), new IsAnything()};
        mock.expects( new InvokeOnceMatcher() ).method( "executeGoals" ).with( constraints ).will(
            new ThrowStub( new MavenExecutorException( "...", new Exception() ) ) );

        phase.setMavenExecutor( (MavenExecutor) mock.proxy() );

        try
        {
            phase.execute( config, (Settings) null, (List) null );

            fail( "Should have thrown an exception" );
        }
        catch ( ReleaseExecutionException e )
        {
            assertEquals( "Check cause", MavenExecutorException.class, e.getCause().getClass() );
        }
    }

}
