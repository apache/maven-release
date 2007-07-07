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

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TeeOutputStream 
    extends FilterOutputStream 
{
    ByteArrayOutputStream bout = new ByteArrayOutputStream( 1024 );
    byte indent[];
    
    public TeeOutputStream( OutputStream out )
    {
        this( out, "    " );
    }
    
    public TeeOutputStream( OutputStream out, String i )
    {
        super( out );
        indent = i.getBytes();
    }
    
    public void write( byte[] b, int off, int len ) 
        throws IOException
    {
        for ( int x = 0; x < len; x++ )
        {
            if ( b[off + x] == '\n' )
            {
                super.write( b, off, x + 1);
                bout.write( b, off, x + 1);
                super.write( indent );
                bout.write( indent );
                off += ( x + 1 );
                len -= ( x + 1 );
                x = 0;
            }
        }
        super.write( b, off, len );
        bout.write( b, off, len );
    }

    public void write( int b )
        throws IOException
    {
        super.write( b );
        bout.write( b );
        if ( b == '\n' )
        {
            super.write( indent );
            bout.write( indent );
        }
    }
    
    public String toString() 
    {
        return bout.toString();
    }

    public String getContent()
    {
        return bout.toString();
    }

}
