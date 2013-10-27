package org.apache.maven.shared.release.scm;

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

import java.io.File;

/**
 * Jazz tag translator.
 *
 * @author <a href="mailto:ChrisGWarp@gmail.com">Chris Graham</a>
 * @plexus.component role="org.apache.maven.shared.release.scm.ScmTranslator" role-hint="jazz"
 */
public class JazzScmTranslator
    implements ScmTranslator
{
    /**
     * {@inheritDoc}
     */
    public String translateBranchUrl( String url, String branchName, String branchBase )
    {
        // Jazz URL's (currently) take the form:
        // "scm:jazz:[username[;password]@]http[s]://server_name[:port]/jazzPath:repositoryWorkspace"
        // Eg:
        // scm:jazz:Deb;Deb@https://rtc:9444/jazz:BogusRepositoryWorkspace
        int i = url.lastIndexOf( ':' );
        url = url.substring( 0, i + 1 );
        if ( branchName != null && branchName.endsWith( "/" ) )
        {
            // Remove the trailing "/", if present.
            branchName = branchName.substring( 0, branchName.length() - 1 );
        }
        url = url + branchName;
        return url;
    }

    /**
     * {@inheritDoc}
     */
    public String translateTagUrl( String url, String tag, String tagBase )
    {
        // Jazz URL's (currently) take the form:
        // "scm:jazz:[username[;password]@]http[s]://server_name[:port]/jazzPath:repositoryWorkspace"
        // Eg:
        // scm:jazz:Deb;Deb@https://rtc:9444/jazz:BogusRepositoryWorkspace
        int i = url.lastIndexOf( ':' );
        url = url.substring( 0, i + 1 );
        if ( tag != null && tag.endsWith( "/" ) )
        {
            // Remove the trailing "/", if present.
            tag = tag.substring( 0, tag.length() - 1 );
        }
        url = url + tag;
        return url;
    }

    /**
     * {@inheritDoc}
     */
    public String resolveTag( String tag )
    {
        // project.scm.tag is not required, so return null.
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String toRelativePath( String path )
    {
        String relativePath;
        if ( path.startsWith( "\\" ) || path.startsWith( "/" ) )
        {
            relativePath = path.substring( 1 );
        }
        else
        {
            relativePath = path;
        }
        return relativePath.replace( "\\", File.separator ).replace( "/", File.separator );
    }
}
