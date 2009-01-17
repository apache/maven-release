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
    private ByteArrayOutputStream bout = new ByteArrayOutputStream( 1024 * 8 );
    private byte indent[];
    private int last = '\n';

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
            int c = b[off + x];
            if ( last == '\n' || ( last == '\r' && c != '\n' ) )
            {
                out.write( b, off, x );
                bout.write( b, off, x );
                out.write( indent );
                off += x;
                len -= x;
                x = 0;
            }
            last = c;
        }
        out.write( b, off, len );
        bout.write( b, off, len );
    }

    public void write( int b )
        throws IOException
    {
        if ( last == '\n' || ( last == '\r' && b != '\n' ) )
        {
            out.write( indent );
        }
        out.write( b );
        bout.write( b );
        last = b;
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
