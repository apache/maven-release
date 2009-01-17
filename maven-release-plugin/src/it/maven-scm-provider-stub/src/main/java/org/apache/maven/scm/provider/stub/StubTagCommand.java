package org.apache.maven.scm.provider.stub;

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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.scm.CommandParameter;
import org.apache.maven.scm.CommandParameters;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.ScmResult;
import org.apache.maven.scm.command.AbstractCommand;
import org.apache.maven.scm.command.tag.TagScmResult;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.codehaus.plexus.util.FileUtils;

/**
 * A dummy tag command.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
class StubTagCommand
    extends AbstractCommand
{

    protected ScmResult executeCommand( ScmProviderRepository repository, ScmFileSet fileSet,
                                        CommandParameters parameters )
        throws ScmException
    {
        List taggedFiles = new ArrayList();

        try
        {
            StubScmProviderRepository stubRepo = (StubScmProviderRepository) repository;

            File workingCopyRoot = stubRepo.getWorkingCopyRoot( fileSet );
            File repoRoot = stubRepo.getRoot();
            String tagName = parameters.getString( CommandParameter.TAG_NAME );
            File tagRoot = new File( stubRepo.getTagBase(), tagName );

            getLogger().info( "Tagging: " + repoRoot + " > " + tagRoot );

            if ( tagRoot.exists() )
            {
                throw new IOException( "Cannot override existing tag" );
            }

            List paths = stubRepo.getPaths( workingCopyRoot, fileSet );
            if ( paths.isEmpty() )
            {
                paths.add( "." );
            }

            for ( Iterator it = paths.iterator(); it.hasNext(); )
            {
                String path = (String) it.next();
                File srcFile = new File( repoRoot, path );
                File dstFile = new File( tagRoot, path );

                getLogger().info( "  " + path );

                if ( srcFile.isDirectory() )
                {
                    FileUtils.copyDirectoryStructure( srcFile, dstFile );
                }
                else if ( srcFile.isFile() )
                {
                    FileUtils.copyFile( srcFile, dstFile );
                }
                else
                {
                    throw new IOException( "Cannot tag non-existing file: " + srcFile );
                }

                taggedFiles.add( new ScmFile( path, ScmFileStatus.TAGGED ) );
            }
        }
        catch ( IOException e )
        {
            throw new ScmException( "Error while tagging the files.", e );
        }

        return new TagScmResult( null, taggedFiles );
    }

}
