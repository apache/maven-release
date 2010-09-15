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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.provider.ScmProviderRepository;

/**
 * A stub SCM repository used for the Maven Release Plugin when doing integration testing.
 * 
 * @author Benjamin Bentmann
 * @version $Id$
 */
class StubScmProviderRepository
    extends ScmProviderRepository
{

    /**
     * The root directory of the remote copy that corresponds to the root directory of the working copy.
     */
    private File root;

    /**
     * The root directory to create tags in. This path is derived from the root directory, assuming SVN conventions.
     */
    private File tagBase;

    /**
     * The root directory to create branches in. This path is derived from the root directory, assuming SVN conventions.
     */
    private File branchBase;

    /**
     * The simple name of the file used to mark the root directory of the working copy.
     */
    static final String WORKING_COPY_ROOT_MARKER_FILE = "_base.properties";

    public StubScmProviderRepository( String url )
    {
        root = new File( url ).getAbsoluteFile();
        initTagBranchBase();
    }

    private void initTagBranchBase()
    {
        File base;
        for ( base = root; base != null; base = base.getParentFile() )
        {
            if ( "trunk".equals( base.getName() ) )
            {
                base = base.getParentFile();
                break;
            }
        }
        if ( base == null )
        {
            base = root.getParentFile();
        }
        tagBase = new File( base, "tags" );
        branchBase = new File( base, "branches" );
    }

    public File getRoot()
    {
        return root;
    }

    public File getTagBase()
    {
        return tagBase;
    }

    public File getBranchBase()
    {
        return branchBase;
    }

    public List getPaths( File workingCopyRoot, ScmFileSet fileSet )
    {
        List paths = new ArrayList();

        Collection files = fileSet.getFileList();
        if ( files != null && !files.isEmpty() )
        {
            for ( Iterator it = files.iterator(); it.hasNext(); )
            {
                File file = (File) it.next();
                if ( !file.isAbsolute() )
                {
                    file = new File( fileSet.getBasedir(), file.getPath() );
                }

                String path = getPath( workingCopyRoot, file );
                paths.add( path );
            }
        }

        return paths;
    }

    private String getPath( File root, File file )
    {
        StringBuffer path = new StringBuffer( 256 );

        for ( File parent = file; !root.equals( parent ); )
        {
            if ( path.length() > 0 )
            {
                path.insert( 0, '/' );
            }
            path.insert( 0, parent.getName() );

            parent = parent.getParentFile();
            if ( parent == null )
            {
                throw new IllegalArgumentException( "Cannot relativize " + file + " against " + root );
            }
        }

        return path.toString();
    }

    public File getWorkingCopyRoot( ScmFileSet fileSet )
    {
        for ( File root = fileSet.getBasedir(); root != null; root = root.getParentFile() )
        {
            if ( new File( root, WORKING_COPY_ROOT_MARKER_FILE ).isFile() )
            {
                return root;
            }
        }
        throw new IllegalArgumentException( "SCM file set is not part of stub working copy: " + fileSet );
    }

    public String toString()
    {
        return "StubScmProviderRepository[root=" + root + ", tags=" + tagBase + ", branches=" + branchBase + "]";
    }

}
