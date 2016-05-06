package org.apache.maven.shared.release.transform.jdom;

import static org.junit.Assert.*;

import java.io.StringReader;

import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.junit.Test;

public class JDomScmTest
{
    private SAXBuilder builder = new SAXBuilder();
    
    @Test( expected = UnsupportedOperationException.class )
    public void testGetConnection()
    {
        new JDomScm( null ).getConnection();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetDeveloperConnection()
    {
        new JDomScm( null ).getDeveloperConnection();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetTag()
    {
        new JDomScm( null ).getTag();
    }

    @Test( expected = UnsupportedOperationException.class )
    public void testGetUrl()
    {
        new JDomScm( null ).getUrl();
    }

    @Test
    public void testSetConnectionString() throws Exception
    {
        String content = "<scm></scm>";
        Element scmElm = builder.build( new StringReader( content ) ).getRootElement();
        
        assertNull( getConnection( scmElm ) );
        
        new JDomScm( scmElm ).setConnection( "CONNECTION" );
        assertEquals( "CONNECTION", getConnection( scmElm ) );

        new JDomScm( scmElm ).setConnection( null );
        assertNull( getConnection( scmElm ) );
    }

    @Test
    public void testSetDeveloperConnectionString() throws Exception
    {
        String content = "<scm></scm>";
        Element scmElm = builder.build( new StringReader( content ) ).getRootElement();
        
        assertNull( getDeveloperConnection( scmElm ) );
        
        new JDomScm( scmElm ).setDeveloperConnection( "DEVELOPERCONNECTION" );
        assertEquals( "DEVELOPERCONNECTION", getDeveloperConnection( scmElm ) );

        new JDomScm( scmElm ).setDeveloperConnection( null );
        assertNull( getDeveloperConnection( scmElm ) );
    }

    @Test
    public void testSetTagString() throws Exception
    {
        String content = "<scm></scm>";
        Element scmElm = builder.build( new StringReader( content ) ).getRootElement();
        
        assertNull( getUrl( scmElm ) );
        
        new JDomScm( scmElm ).setUrl( "URL" );
        assertEquals( "URL", getUrl( scmElm ) );

        new JDomScm( scmElm ).setUrl( null );
        assertNull( getUrl( scmElm ) );
    }

    @Test
    public void testSetUrlString() throws Exception
    {
        String content = "<scm></scm>";
        Element scmElm = builder.build( new StringReader( content ) ).getRootElement();
        
        assertNull( getTag( scmElm ) );
        
        new JDomScm( scmElm ).setTag( "TAG" );
        assertEquals( "TAG", getTag( scmElm ) );

        new JDomScm( scmElm ).setTag( null );
        assertNull( getTag( scmElm ) );
    }
    
    private String getConnection( Element scmElm )
    {
        return scmElm.getChildText( "connection", scmElm.getNamespace() );
    }

    private String getDeveloperConnection( Element scmElm )
    {
        return scmElm.getChildText( "developerConnection", scmElm.getNamespace() );
    }
    
    private String getTag( Element scmElm )
    {
        return scmElm.getChildText( "tag", scmElm.getNamespace() );
    }

    private String getUrl( Element scmElm )
    {
        return scmElm.getChildText( "url", scmElm.getNamespace() );
    }
}
