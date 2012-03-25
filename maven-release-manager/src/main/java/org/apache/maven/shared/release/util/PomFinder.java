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

import java.io.File;
import java.io.IOException;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * <p>This utility class helps with finding a maven pom file
 * which got parsed previously. It uses the fact that the
 * combination of any parent ids plus the ids of the current
 * pom itself is unique.</p>
 * <p>This is e.g. needed for SCM systems which do not support
 * sparse checkout but only can checkout the repository as whole
 * like e.g. GIT. If the module which we are going to release is
 * not in the parent directory, we first need to search for the
 * 'right' sub directory in this case.
 * subdirectory </p>
 *
 * <h3>Usage:</h3>
 * <p>PomFinder is a statefull class. One instance of this class intended
 * for a singular use! You need to create a new instance if you like
 * to search for another pom.</p
 * <ol>
 *   <li>
 *     Parse an origin pom in a given directory with {@link #parsePom(java.io.File)}
 *     This will act as the information about what to search for.
 *   </li>
 *   <li>
 *      Search for the matching pom in a given tree using
 *      {@link #findMatchingPom(java.io.File)}
 *   </li>
 * </ol>
 *
 * @author <a href="mailto:struberg@yahoo.de">Mark Struberg</a>
 */
public class PomFinder
{

    private Logger log;
    private PomInfo foundPomInfo;

    public PomFinder( Logger log )
    {
        this.log = log;
    }

    /**
     *
     * @param originPom the pom File which should be used as blueprint for the search
     * @return <code>true</code> if a pom got parsed successfully, <code>false</code> otherwise
     */
    public boolean parsePom( File originPom )
    {
        if ( !originPom.exists() )
        {
            return false;
        }

        try
        {
            foundPomInfo = readPomInfo( originPom );
        }
        catch ( Exception e )
        {
            log.warn( "Error while parsing pom file", e );
            return false;
        }

        return foundPomInfo != null;
    }

    /**
     * Search for the previously with {@link #parsePom(java.io.File)}
     * parsed pom in the given directory.
     * @param startDirectory
     * @return the pom file which matches the previously parsed pom or <code>null</code>
     *         if no matching pom file could have been found.
     */
    public File findMatchingPom( File startDirectory )
    {
        if ( !startDirectory.exists() )
        {
            return null;
        }

        if ( !startDirectory.isDirectory() )
        {
            log.error( "PomFinder must be started with a directory! Got " + startDirectory.getAbsolutePath() );
            return null;
        }

        if ( foundPomInfo == null )
        {
            log.error( "Please run parsePom first!" );
            return null;
        }

        // look for the file in the current directory
        File matchingPom = new File( startDirectory, foundPomInfo.getFileName() );
        if ( matchingPom.exists() )
        {
            PomInfo pi = null;
            try
            {
                pi = readPomInfo( matchingPom );
            }
            catch ( Exception e )
            {
                log.warn( "Error while parsing pom file", e );
                // do nothing, just continue with the search
                // this might happen if a build contains unfinished pom.xml
                // files in integration tests, etc
            }

            if  ( pi == null || !pi.equals( foundPomInfo ) )
            {
                matchingPom = null;
            }
        }
        else
        {
            matchingPom = null;
        }

        if ( matchingPom == null )
        {
            String[] childFiles = startDirectory.list();
            for ( int i = 0; i < childFiles.length; i++ )
            {
                String childFile = childFiles[ i ];

                File subDir = new File( startDirectory, childFile );
                if ( subDir.isDirectory() && !subDir.isHidden() )
                {
                    matchingPom = findMatchingPom( subDir );
                }

                if ( matchingPom != null )
                {
                    break;
                }
            }
        }

        return matchingPom;
    }


    /**
     * Read the {@link PomInfo} from the given pom file
     * @param pomFile pom.xml file
     * @return the PomInfo or <code>null</code
     */
    private PomInfo readPomInfo( File pomFile )
            throws IOException, XmlPullParserException
    {
        if ( !pomFile.exists() || !pomFile.isFile() )
        {
            return null;
        }

        PomInfo pomInfo = null;

        MavenXpp3Reader reader = new MavenXpp3Reader();
        XmlStreamReader xmlReader = null;
        Model model = null;
        try
        {
            xmlReader = ReaderFactory.newXmlReader( pomFile );
            model = reader.read( xmlReader );
        }
        finally
        {
            IOUtil.close( xmlReader );
        }
        if ( model != null )
        {
            pomInfo = new PomInfo();
            pomInfo.setArtifactId( model.getArtifactId() );
            pomInfo.setGroupId( model.getGroupId() );

            Parent parent = model.getParent();
            if ( parent != null )
            {
                pomInfo.setParentArtifactId( parent.getArtifactId() );
                pomInfo.setParentGroupId( parent.getGroupId() );
            }

            pomInfo.setFileName( pomFile.getName() );
        }
        return pomInfo;
    }

    /***
     * Data container which helds information about a pom.
     * Information may partially be empty.
     */
    private static class PomInfo
    {
        private String fileName;
        private String artifactId;
        private String groupId;
        private String parentArtifactId;
        private String parentGroupId;

        public String getFileName()
        {
            return fileName;
        }

        public void setFileName( String fileName )
        {
            this.fileName = fileName;
        }

        public String getArtifactId()
        {
            return artifactId;
        }

        public void setArtifactId( String artifactId )
        {
            this.artifactId = artifactId;
        }

        public String getGroupId()
        {
            return groupId;
        }

        public void setGroupId( String groupId )
        {
            this.groupId = groupId;
        }

        public String getParentArtifactId()
        {
            return parentArtifactId;
        }

        public void setParentArtifactId( String parentArtifactId )
        {
            this.parentArtifactId = parentArtifactId;
        }

        public String getParentGroupId()
        {
            return parentGroupId;
        }

        public void setParentGroupId( String parentGroupId )
        {
            this.parentGroupId = parentGroupId;
        }

        public boolean equals( Object o )
        {
            if ( this == o )
            {
                return true;
            }

            if ( o == null || getClass() != o.getClass() )
            {
                return false;
            }

            PomInfo pomInfo = (PomInfo) o;

            if ( artifactId != null ? !artifactId.equals( pomInfo.artifactId ) : pomInfo.artifactId != null )
            {
                return false;
            }
            if ( groupId != null ? !groupId.equals( pomInfo.groupId ) : pomInfo.groupId != null )
            {
                return false;
            }
            if ( parentArtifactId != null ? !parentArtifactId.equals( pomInfo.parentArtifactId )
                            : pomInfo.parentArtifactId != null )
            {
                return false;
            }
            if ( parentGroupId != null ? !parentGroupId.equals( pomInfo.parentGroupId ) : pomInfo.parentGroupId != null )
            {
                return false;
            }

            return true;
        }

        public int hashCode()
        {
            int result = artifactId != null ? artifactId.hashCode() : 0;
            result = 31 * result + ( groupId != null ? groupId.hashCode() : 0 );
            result = 31 * result + ( parentArtifactId != null ? parentArtifactId.hashCode() : 0 );
            result = 31 * result + ( parentGroupId != null ? parentGroupId.hashCode() : 0 );
            return result;
        }
    }
}
