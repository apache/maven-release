package org.apache.maven.shared.release.exec;

import org.apache.maven.shared.release.ReleaseResult;
import org.apache.maven.shared.release.env.DefaultReleaseEnvironment;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

import java.io.File;

public abstract class AbstractMavenExecutor
    implements MavenExecutor, LogEnabled
{

    private Logger logger;

    protected AbstractMavenExecutor()
    {
    }

    public void executeGoals( File workingDirectory,
                              String goals,
                              boolean interactive,
                              String additionalArguments,
                              String pomFileName,
                              ReleaseResult result )
        throws MavenExecutorException
    {
        executeGoals( workingDirectory, goals, new DefaultReleaseEnvironment(), interactive, additionalArguments, pomFileName, result );
    }

    public void executeGoals( File workingDirectory,
                              String goals,
                              boolean interactive,
                              String additionalArguments,
                              ReleaseResult result )
        throws MavenExecutorException
    {
        executeGoals( workingDirectory, goals, new DefaultReleaseEnvironment(), interactive, additionalArguments, result );
    }

    protected final Logger getLogger()
    {
        return logger;
    }

    public void enableLogging( Logger logger )
    {
        this.logger = logger;
    }

}
