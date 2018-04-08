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

import java.io.File;

/**
 * @author Edwin Punzalan
 */
public abstract class AbstractBackupPomsPhase
    extends AbstractReleasePhase
{
    private final String backupSuffix = ".releaseBackup";

    protected File getPomBackup( File workingDirectory, String pomLocation )
    {
        return new File( workingDirectory, pomLocation + backupSuffix );
    }

    protected void deletePomBackup( File workingDirectory, String pomLocation )
    {
        File pomBackup = getPomBackup( workingDirectory, pomLocation );

        if ( pomBackup.exists() )
        {
            pomBackup.delete();
        }
    }
}
