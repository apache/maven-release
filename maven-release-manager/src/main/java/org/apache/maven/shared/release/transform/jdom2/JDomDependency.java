package org.apache.maven.shared.release.transform.jdom2;

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

import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.shared.release.transform.MavenCoordinate;
import org.jdom2.Element;

/**
 * JDOM2 implementation of poms DEPENDENCY element
 *
 * @author Robert Scholte
 * @since 3.0
 */
public class JDomDependency extends Dependency implements MavenCoordinate
{
    private final MavenCoordinate coordinate;

    public JDomDependency( Element dependency )
    {
        this.coordinate = new JDomMavenCoordinate( dependency );
    }

    @Override
    public void addExclusion( Exclusion exclusion )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getArtifactId()
    {
        return coordinate.getArtifactId();
    }

    @Override
    public String getClassifier()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Exclusion> getExclusions()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getGroupId()
    {
        return coordinate.getGroupId();
    }

    @Override
    public String getScope()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getSystemPath()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getType()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVersion()
    {
        return coordinate.getVersion();
    }

    @Override
    public boolean isOptional()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeExclusion( Exclusion exclusion )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setArtifactId( String artifactId )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setClassifier( String classifier )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setExclusions( List<Exclusion> exclusions )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setGroupId( String groupId )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOptional( boolean optional )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setScope( String scope )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSystemPath( String systemPath )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setType( String type )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVersion( String version )
    {
        coordinate.setVersion( version );
    }

    @Override
    public String getName()
    {
        return "dependency";
    }
}
