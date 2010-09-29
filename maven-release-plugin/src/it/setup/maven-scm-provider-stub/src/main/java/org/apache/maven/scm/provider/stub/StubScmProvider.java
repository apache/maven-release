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

import org.apache.maven.scm.CommandParameters;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmResult;
import org.apache.maven.scm.command.Command;
import org.apache.maven.scm.command.branch.BranchScmResult;
import org.apache.maven.scm.command.checkin.CheckInScmResult;
import org.apache.maven.scm.command.checkout.CheckOutScmResult;
import org.apache.maven.scm.command.status.StatusScmResult;
import org.apache.maven.scm.command.tag.TagScmResult;
import org.apache.maven.scm.provider.AbstractScmProvider;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;

/**
 * A stub SCM provider used for the Maven Release Plugin when doing integration testing.
 * 
 * @plexus.component role="org.apache.maven.scm.provider.ScmProvider" role-hint="stub"
 * @author Benjamin Bentmann
 * @version $Id$
 */
public class StubScmProvider
    extends AbstractScmProvider
{

    public String getScmType()
    {
        return "stub";
    }

    public ScmProviderRepository makeProviderScmRepository( String scmSpecificUrl, char delimiter )
        throws ScmRepositoryException
    {
        return new StubScmProviderRepository( scmSpecificUrl );
    }

    protected ScmResult executeCommand( Command command, ScmProviderRepository repository, ScmFileSet fileSet,
                                        CommandParameters parameters )
        throws ScmException
    {
        command.setLogger( getLogger() );

        return command.execute( repository, fileSet, parameters );
    }

    protected StatusScmResult status( ScmProviderRepository repository, ScmFileSet fileSet, CommandParameters parameters )
        throws ScmException
    {
        return (StatusScmResult) executeCommand( new StubStatusCommand(), repository, fileSet, parameters );
    }

    protected CheckInScmResult checkin( ScmProviderRepository repository, ScmFileSet fileSet,
                                        CommandParameters parameters )
        throws ScmException
    {
        return (CheckInScmResult) executeCommand( new StubCheckInCommand(), repository, fileSet, parameters );
    }

    protected CheckOutScmResult checkout( ScmProviderRepository repository, ScmFileSet fileSet,
                                          CommandParameters parameters )
        throws ScmException
    {
        return (CheckOutScmResult) executeCommand( new StubCheckOutCommand(), repository, fileSet, parameters );
    }

    protected BranchScmResult branch( ScmProviderRepository repository, ScmFileSet fileSet,
                                        CommandParameters parameters )
        throws ScmException
    {
        return (BranchScmResult) executeCommand( new StubBranchCommand(), repository, fileSet, parameters );
    }

    protected TagScmResult tag( ScmProviderRepository repository, ScmFileSet fileSet, CommandParameters parameters )
        throws ScmException
    {
        return (TagScmResult) executeCommand( new StubTagCommand(), repository, fileSet, parameters );
    }

}
