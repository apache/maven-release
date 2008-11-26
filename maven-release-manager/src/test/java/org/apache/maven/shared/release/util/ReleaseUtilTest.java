/**
 * 
 */
package org.apache.maven.shared.release.util;

import org.codehaus.plexus.PlexusTestCase;

/**
 * Tests for ReleaseUtil methods
 * 
 * @author aheritier
 */
public class ReleaseUtilTest
    extends PlexusTestCase
{
    /**
     * MRELEASE-273 : Tests if there no pom passed as parameter
     */
    public void testProjectIsNull()
        throws Exception
    {
        assertNull( ReleaseUtil.getReleasePom( null ) );
        assertNull( ReleaseUtil.getStandardPom( null ) );
    }

}
