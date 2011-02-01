package org.apache.maven.shared.release.util;

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

import org.codehaus.plexus.PlexusTestCase;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.LoggerManager;

import java.io.File;
import java.net.URL;


/**
 * @author <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 */
public class PomFinderTest extends PlexusTestCase
{
    private Logger logger = null;

    protected void setUp() throws Exception
    {
        super.setUp();
        LoggerManager lm = (LoggerManager) lookup(LoggerManager.ROLE);
        logger = lm.getLoggerForComponent(LoggerManager.ROLE); //X TODO use a better ROLE!
    }

    public void testPomFinderParser() throws Exception
    {
        PomFinder pf = new PomFinder( logger );

        boolean found = pf.parsePom( new File( "src/test/resources/pomfinder/pomNothere.xml" ) );
        assertFalse( found );

        URL pomUrl = getClassLoader().getResource("pomfinder/pom1.xml");
        assertNotNull( pomUrl );

        File pomFile = new File( pomUrl.getFile() );
        found = pf.parsePom( pomFile );
        assertTrue("pomFile not found pomUrl " + pomUrl + ", pomFile " + pomFile.getPath() , found );

        {
            File foundPom = pf.findMatchingPom( pomFile.getParentFile() );
            assertNotNull( foundPom );

            assertEquals( pomFile.getAbsolutePath(), foundPom.getAbsolutePath() );
        }

        {
            // try from 1 directory higher
            File foundPom = pf.findMatchingPom( pomFile.getParentFile().getParentFile() );
            assertNotNull( foundPom );

            assertEquals( pomFile.getAbsolutePath(), foundPom.getAbsolutePath() );
        }
    }

}
