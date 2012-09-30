package org.apache.maven.shared.release.exec;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.apache.maven.shared.invoker.InvocationRequest;
import org.codehaus.plexus.logging.Logger;
import org.junit.Test;


public class InvokerMavenExecutorTest
{

    @Test
    public void testThreads() throws Exception
    {
        InvokerMavenExecutor executor = new InvokerMavenExecutor();
        InvocationRequest req = mock( InvocationRequest.class );
        Logger logger = mock( Logger.class );
        executor.enableLogging( logger );
        
        executor.setupRequest( req, null, "-T 3" );
        executor.setupRequest( req, null, "-T3" );
        executor.setupRequest( req, null, "\"-T3\"" );
        
        verify( logger, times( 3 ) ).warn( "Specifying the threadcount is currently not supported ." );
    }
}
