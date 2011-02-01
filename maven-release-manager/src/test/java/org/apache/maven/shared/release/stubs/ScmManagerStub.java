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

package org.apache.maven.shared.release.stubs;

import java.util.HashMap;
import java.util.Map;

import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;

/**
 * Override the makeRepository methods to honour the URL passed in.
 */
public class ScmManagerStub
    extends org.apache.maven.scm.manager.ScmManagerStub
{
    private Map<String, ScmRepository> scmRepositoriesForUrl = new HashMap<String, ScmRepository>();

    /*@Override*/
    public ScmRepository makeScmRepository( String scmUrl )
        throws ScmRepositoryException, NoSuchScmProviderException
    {
        if ( scmRepositoriesForUrl.isEmpty() )
        {
            // we didn't configure any for URLs, return the preset one
            return getScmRepository();
        }

        ScmRepository repository = (ScmRepository) scmRepositoriesForUrl.get( scmUrl );
        if ( repository == null )
        {
            throw new ScmRepositoryException( "Unexpected URL: " + scmUrl );
        }
        return repository;
    }

    public void addScmRepositoryForUrl( String url, ScmRepository repository )
    {
        scmRepositoriesForUrl.put( url, repository );
    }
}
