package org.apache.maven.shared.release.versions;

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

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

class MavenArtifactVersion
    implements ArtifactVersion
{
    private final ArtifactVersion version;

    MavenArtifactVersion( String version )
    {
        this.version = new DefaultArtifactVersion( version );
    }

    public int compareTo( Object o )
    {
        if ( o instanceof MavenArtifactVersion )
        {
            return version.compareTo( ( (MavenArtifactVersion) o ).version );
        }
        else
        {
            return version.compareTo( version );
        }
    }

    /**
     * <p>getMajorVersion.</p>
     *
     * @return a int
     */
    public int getMajorVersion()
    {
        return version.getMajorVersion();
    }

    /**
     * <p>getMinorVersion.</p>
     *
     * @return a int
     */
    public int getMinorVersion()
    {
        return version.getMinorVersion();
    }

    /**
     * <p>getIncrementalVersion.</p>
     *
     * @return a int
     */
    public int getIncrementalVersion()
    {
        return version.getIncrementalVersion();
    }

    /**
     * <p>getBuildNumber.</p>
     *
     * @return a int
     */
    public int getBuildNumber()
    {
        return version.getBuildNumber();
    }

    /**
     * <p>getQualifier.</p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getQualifier()
    {
        return version.getQualifier();
    }

    public void parseVersion( String version )
    {
        this.version.parseVersion( version );
    }

    @Override
    public String toString()
    {
        return this.version.toString();
    }

    @Override
    public int hashCode()
    {
        return this.version.hashCode();
    }
    
    @Override
    public boolean equals( Object other )
    {
        if ( this == other )
        {
            return true;
        }
        if ( other == null )
        {
            return false;
        }

        if ( other instanceof MavenArtifactVersion )
        {
            return version.equals( ( (MavenArtifactVersion) other ).version );
        }
        return false;
    }

    
}
