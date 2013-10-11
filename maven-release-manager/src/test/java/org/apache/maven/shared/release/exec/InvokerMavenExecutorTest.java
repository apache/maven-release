package org.apache.maven.shared.release.exec;

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

    @Test
    public void testGlobalSettings() throws Exception
    {
        InvokerMavenExecutor executor = new InvokerMavenExecutor();
        Logger logger = mock( Logger.class );
        executor.enableLogging( logger );
        
        InvocationRequest req = new DefaultInvocationRequest();
        executor.setupRequest( req, null, "-gs custom-settings.xml" );
        assertEquals( "custom-settings.xml", req.getGlobalSettingsFile().getPath() );

        req = new DefaultInvocationRequest();
        executor.setupRequest( req, null, "--global-settings other-settings.xml" );
        assertEquals( "other-settings.xml", req.getGlobalSettingsFile().getPath() );
    }
}
