/**
 * 
 */
package org.apache.maven.shared.release.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
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
    
    public void testGetBaseWorkingDirectoryNoModules()
        throws Exception
    {
        assertEquals( "/working/directory/flat-multi-module/project",
                      ReleaseUtil.getBaseWorkingDirectory( "/working/directory/flat-multi-module/project", null ) );
        
        assertEquals( "/working/directory/flat-multi-module/project",
                      ReleaseUtil.getBaseWorkingDirectory( "/working/directory/flat-multi-module/project", new ArrayList() ) );
    }
    
    public void testGetBaseWorkingDirOfFlatMultiModule()
        throws Exception
    {
        List modules = new ArrayList();
        modules.add( "../core" );
        modules.add( "../webapp" );

        assertEquals( "/working/directory/flat-multi-module",
                      ReleaseUtil.getBaseWorkingDirectory( "/working/directory/flat-multi-module" + ReleaseUtil.FS +
                          "root-project", modules ) );
        assertEquals( "/working/directory/flat-multi-module",
                      ReleaseUtil.getBaseWorkingDirectory( "/working/directory/flat-multi-module" + ReleaseUtil.FS +
                          "root-project" + ReleaseUtil.FS, modules ) );
    }

    public void testGetBaseWorkingDirectoryOfRegularMultiModule()
        throws Exception
    {
        List modules = new ArrayList();
        modules.add( "core" );
        modules.add( "webapp" );

        assertEquals( "/working/directory/flat-multi-module",
                      ReleaseUtil.getBaseWorkingDirectory( "/working/directory/flat-multi-module", modules ) );
        assertEquals( "/working/directory/flat-multi-module",
                      ReleaseUtil.getBaseWorkingDirectory( "/working/directory/flat-multi-module" + ReleaseUtil.FS,
                                                           modules ) );
    }

    public void testGetBaseScmUrlNoModules()
        throws Exception
    {
        assertEquals( "scm:svn:http://svn.repo.com/flat-multi-module/trunk",
                      ReleaseUtil.getBaseScmUrl( "scm:svn:http://svn.repo.com/flat-multi-module/trunk", null ) );
        
        assertEquals( "scm:svn:http://svn.repo.com/flat-multi-module/trunk",
                      ReleaseUtil.getBaseScmUrl( "scm:svn:http://svn.repo.com/flat-multi-module/trunk", new ArrayList() ) );
    }
    
    public void testGetBaseScmUrlOfFlatMultiModule()
        throws Exception
    {
        List modules = new ArrayList();
        modules.add( "../core" );
        modules.add( "../webapp" );

        assertEquals( "scm:svn:http://svn.repo.com/flat-multi-module/trunk",
                      ReleaseUtil.getBaseScmUrl( "scm:svn:http://svn.repo.com/flat-multi-module/trunk/root-project",
                                                 modules ) );
        assertEquals( "scm:svn:http://svn.repo.com/flat-multi-module/trunk",
                      ReleaseUtil.getBaseScmUrl( "scm:svn:http://svn.repo.com/flat-multi-module/trunk/root-project/",
                                                 modules ) );
    }

    public void testGetBaseScmUrlOfRegularMultiModule()
        throws Exception
    {
        List modules = new ArrayList();
        modules.add( "core" );
        modules.add( "webapp" );

        assertEquals( "scm:svn:http://svn.repo.com/flat-multi-module/trunk",
                      ReleaseUtil.getBaseScmUrl( "scm:svn:http://svn.repo.com/flat-multi-module/trunk", modules ) );
        assertEquals( "scm:svn:http://svn.repo.com/flat-multi-module/trunk",
                      ReleaseUtil.getBaseScmUrl( "scm:svn:http://svn.repo.com/flat-multi-module/trunk/", modules ) );
    }
    
    public void testGetRootProjectPathFlatStructure()
        throws Exception
    {   
        MavenProject project = new MavenProject()
        {
            public List getModules()
            {
                List modules = new ArrayList();
                modules.add( "../core" );
                modules.add( "../webapp" );
                modules.add( "../commons" );
                
                return modules;
            }
            
            public File getBasedir()
            {
                return new File( "/flat-multi-module/root-project" );
            }
            
            public Scm getScm()
            {
                Scm scm = new Scm();
                scm.setConnection( "scm:svn:file://localhost/target/svnroot/flat-multi-module/trunk/root-project" );
                
                return scm;
            }
        };
        
        assertEquals( "/root-project", ReleaseUtil.getRootProjectPath( project ) );
    }
    
    public void testGetRootProjectPathRegularMultiModuleStructure()
        throws Exception
    {   
        MavenProject project = new MavenProject()
        {
            Scm scm = new Scm();
            
            public List getModules()
            {
                List modules = new ArrayList();
                modules.add( "core" );
                modules.add( "webapp" );
                modules.add( "commons" );
                
                return modules;
            }
            
            public File getBasedir()
            {
                return new File( "/regular-multi-module" );
            }
            
            public Scm getScm()
            {
                scm.setConnection( "scm:svn:file://localhost/target/svnroot/regular-multi-module/trunk" );
                
                return scm;
            }
        };
        
        assertEquals( "", ReleaseUtil.getRootProjectPath( project ) );
        
        project.getScm().setConnection( "scm:svn:file://localhost/target/svnroot/regular-multi-module/trunk/" );        
        assertEquals( "", ReleaseUtil.getRootProjectPath( project ) );
    }
}
