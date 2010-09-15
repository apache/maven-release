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

import org.apache.maven.scm.CommandParameters;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.ScmResult;
import org.apache.maven.scm.command.AbstractCommand;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.codehaus.plexus.util.FileUtils;

/**
 * A dummy check-in command.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
class StubCheckInCommand
    extends AbstractCommand
{

    protected ScmResult executeCommand( ScmProviderRepository repository, ScmFileSet fileSet,
                                        CommandParameters parameters )
        throws ScmException
    {
        List checkedInFiles = new ArrayList();

        try
        {
            StubScmProviderRepository stubRepo = (StubScmProviderRepository) repository;

            File workingCopyRoot = stubRepo.getWorkingCopyRoot( fileSet );
            File repoRoot = stubRepo.getRoot();

            getLogger().info( "Committing: " + workingCopyRoot + " > " + repoRoot );

            List paths = stubRepo.getPaths( workingCopyRoot, fileSet );

            for ( Iterator it = paths.iterator(); it.hasNext(); )
            {
                String path = (String) it.next();
                File srcFile = new File( workingCopyRoot, path );
                File dstFile = new File( repoRoot, path );

                getLogger().info( "  " + path );

                if ( dstFile.isDirectory() )
                {
                    if ( !srcFile.isDirectory() )
                    {
                        throw new IOException( "Cannot commit a normal file to a directory: " + srcFile + " -> "
                            + dstFile );
                    }
                }
                else if ( dstFile.isFile() )
                {
                    if ( !srcFile.isFile() )
                    {
                        throw new IOException( "Cannot commit a directory to a normal file: " + srcFile + " -> "
                            + dstFile );
                    }
                    FileUtils.copyFile( srcFile, dstFile );
                }
                else
                {
                    throw new IOException( "Cannot commit to non-existing location: " + srcFile + " -> " + dstFile );
                }

                checkedInFiles.add( new ScmFile( path, ScmFileStatus.CHECKED_IN ) );
            }
        }
        catch ( IOException e )
        {
            throw new ScmException( "Error while checking in the files.", e );
        }

        return new CheckInScmResult( null, checkedInFiles );
    }

}
