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

import org.apache.maven.shared.release.transform.MavenCoordinate;
import org.jdom2.Element;

/**
 *
 * @author Robert Scholte
 * @since 3.0
 */
public class JDomMavenCoordinate implements MavenCoordinate
{
    private final Element element;

    public JDomMavenCoordinate( Element elm )
    {
        this.element = elm;
    }

    @Override
    public String getGroupId()
    {
        return element.getChildTextTrim( "groupId", element.getNamespace() );
    }

    @Override
    public String getArtifactId()
    {
        return element.getChildTextTrim( "artifactId", element.getNamespace() );
    }

    @Override
    public String getVersion()
    {
        Element version = getVersionElement();
        if ( version == null )
        {
            return null;
        }
        else
        {
            return version.getTextTrim();
        }

    }

    private Element getVersionElement()
    {
        return element.getChild( "version", element.getNamespace() );
    }

    @Override
    public void setVersion( String version )
    {
        JDomUtils.rewriteValue( getVersionElement(), version );
    }

    @Override
    public String getName()
    {
        return element.getName();
    }
}
