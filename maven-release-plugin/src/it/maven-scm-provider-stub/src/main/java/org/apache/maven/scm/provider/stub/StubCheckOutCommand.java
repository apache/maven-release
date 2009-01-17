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
import org.apache.maven.scm.ScmBranch;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFile;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmFileStatus;
import org.apache.maven.scm.ScmResult;
import org.apache.maven.scm.ScmTag;
import org.apache.maven.scm.ScmVersion;
import org.apache.maven.scm.command.AbstractCommand;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.codehaus.plexus.util.FileUtils;

/**
 * A dummy check-out command.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
class StubCheckOutCommand
    extends AbstractCommand
{

    protected ScmResult executeCommand( ScmProviderRepository repository, ScmFileSet fileSet,
                                        CommandParameters parameters )
        throws ScmException
    {
        List checkedOutFiles = new ArrayList();

        try
        {
            StubScmProviderRepository stubRepo = (StubScmProviderRepository) repository;

            File workingCopyRoot = fileSet.getBasedir();
            ScmVersion version = parameters.getScmVersion( CommandParameter.SCM_VERSION );
            File revRoot;
            if ( version instanceof ScmTag )
            {
                revRoot = new File( stubRepo.getTagBase(), version.getName() );
            }
            else if ( version instanceof ScmBranch )
            {
                revRoot = new File( stubRepo.getBranchBase(), version.getName() );
            }
            else
            {
                revRoot = stubRepo.getRoot();
            }

            getLogger().info( "Checking out: " + revRoot + " > " + workingCopyRoot );

            if ( workingCopyRoot.isFile() )
            {
                throw new IOException( "Cannot check out into exising file" );
            }
            if ( workingCopyRoot.isDirectory() && workingCopyRoot.list().length > 0 )
            {
                throw new IOException( "Cannot check out into non-empty directory" );
            }

            List paths = stubRepo.getPaths( workingCopyRoot, fileSet );
            if ( paths.isEmpty() )
            {
                paths.add( "." );
            }

            new File( workingCopyRoot, StubScmProviderRepository.WORKING_COPY_ROOT_MARKER_FILE ).createNewFile();

            for ( Iterator it = paths.iterator(); it.hasNext(); )
            {
                String path = (String) it.next();
                File srcFile = new File( revRoot, path );
                File dstFile = new File( workingCopyRoot, path );

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
                    throw new IOException( "Cannot check out non-existing file: " + srcFile );
                }

                checkedOutFiles.add( new ScmFile( path, ScmFileStatus.CHECKED_OUT ) );
            }
        }
        catch ( IOException e )
        {
            throw new ScmException( "Error while checking out the files.", e );
        }

        return new CheckOutScmResult( null, checkedOutFiles );
    }

}
