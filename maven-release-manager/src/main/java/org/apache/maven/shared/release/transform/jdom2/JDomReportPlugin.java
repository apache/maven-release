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
import java.util.Map;

import org.apache.maven.model.ReportPlugin;
import org.apache.maven.model.ReportSet;
import org.apache.maven.shared.release.transform.MavenCoordinate;
import org.jdom2.Element;

/**
 * JDOM2 implementation of poms reports PLUGIN element
 *
 * @author Robert Scholte
 * @since 3.0
 */
public class JDomReportPlugin
    extends ReportPlugin implements MavenCoordinate
{
    private final MavenCoordinate coordinate;

    public JDomReportPlugin( Element reportPlugin )
    {
        this.coordinate = new JDomMavenCoordinate( reportPlugin );
    }

    @Override
    public void addReportSet( ReportSet reportSet )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getArtifactId()
    {
        return coordinate.getArtifactId();
    }

    @Override
    public Object getConfiguration()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getGroupId()
    {
        return coordinate.getGroupId();
    }

    @Override
    public String getInherited()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<ReportSet> getReportSets()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVersion()
    {
        return coordinate.getVersion();
    }

    @Override
    public void removeReportSet( ReportSet reportSet )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setArtifactId( String artifactId )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setConfiguration( Object configuration )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setGroupId( String groupId )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setInherited( String inherited )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setReportSets( List<ReportSet> reportSets )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setVersion( String version )
    {
        coordinate.setVersion( version );
    }

    @Override
    public void flushReportSetMap()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, ReportSet> getReportSetsAsMap()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getKey()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unsetInheritanceApplied()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInheritanceApplied()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName()
    {
        return "plugin";
    }
}
