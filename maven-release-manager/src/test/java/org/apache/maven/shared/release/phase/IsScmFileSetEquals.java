package org.apache.maven.shared.release.phase;

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

import java.io.IOException;

import org.apache.maven.scm.ScmFileSet;
import org.mockito.ArgumentMatcher;

/**
 * JMock constraint to compare file sets since it has no equals method.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @todo add an equals() method
 */
public class IsScmFileSetEquals extends ArgumentMatcher<ScmFileSet>
{
    private final ScmFileSet fileSet;

    public IsScmFileSetEquals( ScmFileSet fileSet )
    {
        this.fileSet = fileSet;
    }

    @Override
    public boolean matches( Object argument )
    {
        ScmFileSet fs = (ScmFileSet) argument;
        
        try
        {
            return fs.getBasedir().getCanonicalPath().equals( fileSet.getBasedir().getCanonicalPath() )
                && fs.getFileList().equals( fileSet.getFileList() );
        }
        catch ( IOException e )
        {
            // should not happened so RuntimeException
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}