package org.apache.maven.shared.release.exec;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.codehaus.plexus.logging.Logger;
import org.junit.Test;


public class InvokerMavenExecutorTest
{

    @Test
    public void testThreads() throws Exception
    {
        InvokerMavenExecutor executor = new InvokerMavenExecutor();
        Logger logger = mock( Logger.class );
        executor.enableLogging( logger );
        
        InvocationRequest req = new DefaultInvocationRequest();
        executor.setupRequest( req, null, "-T 3" );
        assertEquals( "3", req.getThreads() );
        
        req = new DefaultInvocationRequest();
        executor.setupRequest( req, null, "-T4" );
        assertEquals( "4", req.getThreads() );
        
        req = new DefaultInvocationRequest();
        executor.setupRequest( req, null, "\"-T5\"" );
        assertEquals( "5", req.getThreads() );
        
    }
}
