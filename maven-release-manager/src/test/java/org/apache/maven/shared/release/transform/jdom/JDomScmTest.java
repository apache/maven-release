package org.apache.maven.shared.release.transform.jdom;

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

import static org.junit.Assert.*;

import java.io.StringReader;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Test;

public class JDomScmTest
{
    private SAXBuilder builder = new SAXBuilder();
    
    @Test
    public void testGetConnection() throws Exception
    {
        String content = "<scm></scm>";
        Document document = builder.build( new StringReader( content ) );
        assertNull( new JDomScm( document.getRootElement() ).getConnection() );

        content = "<scm><connection/></scm>";
        document = builder.build( new StringReader( content ) );
        // hmm, null or empty String
        assertEquals( "", new JDomScm( document.getRootElement() ).getConnection() );

        content = "<scm><connection>CONNECTION</connection></scm>";
        document = builder.build( new StringReader( content ) );
        assertEquals( "CONNECTION", new JDomScm( document.getRootElement() ).getConnection() );
    }

    @Test
    public void testGetDeveloperConnection() throws Exception
    {
        String content = "<scm></scm>";
        Document document = builder.build( new StringReader( content ) );
        assertNull( new JDomScm( document.getRootElement() ).getDeveloperConnection() );

        content = "<scm><developerConnection/></scm>";
        document = builder.build( new StringReader( content ) );
        // hmm, null or empty String
        assertEquals( "", new JDomScm( document.getRootElement() ).getDeveloperConnection() );

        content = "<scm><developerConnection>DEVELOPERCONNECTION</developerConnection></scm>";
        document = builder.build( new StringReader( content ) );
        assertEquals( "DEVELOPERCONNECTION", new JDomScm( document.getRootElement() ).getDeveloperConnection() );
    }

    @Test
    public void testGetTag() throws Exception
    {
        String content = "<scm></scm>";
        Document document = builder.build( new StringReader( content ) );
        assertNull( new JDomScm( document.getRootElement() ).getTag() );

        content = "<scm><tag/></scm>";
        document = builder.build( new StringReader( content ) );
        // hmm, null or empty String
        assertEquals( "", new JDomScm( document.getRootElement() ).getTag() );

        content = "<scm><tag>TAG</tag></scm>";
        document = builder.build( new StringReader( content ) );
        assertEquals( "TAG", new JDomScm( document.getRootElement() ).getTag() );
    }

    @Test
    public void testGetUrl() throws Exception
    {
        String content = "<scm></scm>";
        Document document = builder.build( new StringReader( content ) );
        assertNull( new JDomScm( document.getRootElement() ).getUrl() );

        content = "<scm><url/></scm>";
        document = builder.build( new StringReader( content ) );
        // hmm, null or empty String
        assertEquals( "", new JDomScm( document.getRootElement() ).getUrl() );

        content = "<scm><url>URL</url></scm>";
        document = builder.build( new StringReader( content ) );
        assertEquals( "URL", new JDomScm( document.getRootElement() ).getUrl() );
    }

}
