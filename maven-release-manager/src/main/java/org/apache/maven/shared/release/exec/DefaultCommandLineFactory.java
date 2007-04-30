package org.apache.maven.shared.release.exec;

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

import org.codehaus.plexus.util.cli.Commandline;

/**
 * Create a command line instance.
 *
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @plexus.component role="org.apache.maven.shared.release.exec.CommandLineFactory"
 */
public class DefaultCommandLineFactory
    implements CommandLineFactory
{
    public Commandline createCommandLine( String executable )
        throws MavenExecutorException
    {
        Commandline commandline = new Commandline();
        commandline.setExecutable( executable );

        try
        {
            commandline.addSystemEnvironment();
        }
        catch ( Exception e )
        {
            throw new MavenExecutorException( e.getMessage(), e );
        }

        return commandline;
    }
}
