package org.apache.maven.plugins.release;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.File;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.shared.release.ReleaseCleanRequest;
import org.apache.maven.shared.release.ReleaseManager;
import org.mockito.ArgumentCaptor;

/**
 * Test release:clean.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 */
public class CleanReleaseMojoTest
    extends AbstractMojoTestCase
{
    protected CleanReleaseMojo mojo;

    private File workingDirectory;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        File testFile = getTestFile( "target/test-classes/mojos/clean/clean.xml" );
        mojo = (CleanReleaseMojo) lookupMojo( "clean", testFile );
        workingDirectory = testFile.getParentFile();
        mojo.setBasedir( workingDirectory );
    }

    public void testClean()
        throws Exception
    {
        // prepare
        ArgumentCaptor<ReleaseCleanRequest> request = ArgumentCaptor.forClass( ReleaseCleanRequest.class );
        
        ReleaseManager mock = mock( ReleaseManager.class );
        mojo.setReleaseManager( mock );

        // execute
        mojo.execute();

        // verify
        verify( mock ).clean( request.capture() );
        
        assertEquals( mojo.getReactorProjects(), request.getValue().getReactorProjects() );
        
        verifyNoMoreInteractions( mock );
    }
}