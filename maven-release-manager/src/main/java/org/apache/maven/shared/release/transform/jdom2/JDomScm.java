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

import org.apache.maven.model.Scm;
import org.jdom2.Element;

/**
 * JDOM2 implementation of poms SCM element
 *
 * @author Robert Scholte
 * @since 3.0
 */
public class JDomScm extends Scm
{
    private Element scm;

    JDomScm( Element scm )
    {
        this.scm = scm;
    }

    @Override
    public String getConnection()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setConnection( String connection )
    {
        JDomUtils.rewriteElement( "connection", connection, scm, scm.getNamespace() );
    }

    @Override
    public String getDeveloperConnection()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDeveloperConnection( String developerConnection )
    {
        JDomUtils.rewriteElement( "developerConnection", developerConnection, scm, scm.getNamespace() );
    }

    @Override
    public String getTag()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTag( String tag )
    {
        JDomUtils.rewriteElement( "tag", tag, scm, scm.getNamespace() );
    }

    @Override
    public String getUrl()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUrl( String url )
    {
        JDomUtils.rewriteElement( "url", url, scm, scm.getNamespace() );
    }
}
