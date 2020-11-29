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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.Set;

import org.jdom2.Element;

/**
 * JDOM2 implementation of poms PROPERTIES element
 *
 * @author Robert Scholte
 * @since 3.0
 */
public class JDomProperties extends Properties
{
    private final Element properties;

    public JDomProperties( Element properties )
    {
        this.properties = properties;
    }

    @Override
    public synchronized Object setProperty( String key, String value )
    {
        Element property = properties.getChild( key, properties.getNamespace() );

        JDomUtils.rewriteValue( property, value );

        // todo follow specs of Hashtable.put
        return null;
    }

    @Override
    public synchronized void load( Reader reader )
        throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void load( InputStream inStream )
        throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings( "deprecation" )
    @Override
    public void save( OutputStream out, String comments )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void store( Writer writer, String comments )
        throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void store( OutputStream out, String comments )
        throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void loadFromXML( InputStream in )
        throws IOException, InvalidPropertiesFormatException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeToXML( OutputStream os, String comment )
        throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void storeToXML( OutputStream os, String comment, String encoding )
        throws IOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProperty( String key )
    {
        Element property = properties.getChild( key, properties.getNamespace() );

        if ( property == null )
        {
            return null;
        }
        else
        {
            return property.getTextTrim();
        }
    }

    @Override
    public String getProperty( String key, String defaultValue )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Enumeration<?> propertyNames()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> stringPropertyNames()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void list( PrintStream out )
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void list( PrintWriter out )
    {
        throw new UnsupportedOperationException();
    }
}
