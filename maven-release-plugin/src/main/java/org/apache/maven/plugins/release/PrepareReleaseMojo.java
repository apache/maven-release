package org.apache.maven.plugins.release;

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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.release.DefaultReleaseManagerListener;
import org.apache.maven.shared.release.ReleaseExecutionException;
import org.apache.maven.shared.release.ReleaseFailureException;
import org.apache.maven.shared.release.ReleasePrepareRequest;
import org.apache.maven.shared.release.config.ReleaseDescriptorBuilder;

/**
 * Prepare for a release in SCM. Steps through several phases to ensure the POM is ready to be released and then
 * prepares SCM to eventually contain a tagged version of the release and a record in the local copy of the parameters
 * used. This can be followed by a call to <tt>release:perform</tt>. For more info see <a
 * href="https://maven.apache.org/plugins/maven-release-plugin/examples/prepare-release.html"
 * >https://maven.apache.org/plugins/maven-release-plugin/examples/prepare-release.html</a>.
 *
 * @author <a href="mailto:jdcasey@apache.org">John Casey</a>
 * @author <a href="mailto:evenisse@apache.org">Emmanuel Venisse</a>
 * @author <a href="mailto:jason@maven.org">Jason van Zyl</a>
 * @author <a href="mailto:brett@apache.org">Brett Porter</a>
 * @version $Id$
 */
// TODO [!] check how this works with version ranges
@Mojo( name = "prepare", aggregator = true, requiresDependencyCollection = ResolutionScope.TEST )
public class PrepareReleaseMojo
    extends AbstractScmReleaseMojo
{

    /**
     * Resume a previous release attempt from the point where it was stopped.
     */
    @Parameter( defaultValue = "true", property = "resume" )
    private boolean resume;

    /**
     * @deprecated Please use release:prepare-with-pom instead.
     */
    @Deprecated
    @Parameter( defaultValue = "false", property = "generateReleasePoms" )
    private boolean generateReleasePoms;

    /**
     * Whether to use "edit" mode on the SCM, to lock the file for editing during SCM operations.
     */
    @Parameter( defaultValue = "false", property = "useEditMode" )
    private boolean useEditMode;

    /**
     * Whether to update dependencies version to the next development version.
     *
     * @since 2.0-beta-5
     */
    @Parameter( defaultValue = "true", property = "updateDependencies" )
    private boolean updateDependencies;

    /**
     * Whether to automatically assign submodules the parent version. If set to false, the user will be prompted for the
     * version of each submodules.
     *
     * @since 2.0-beta-5
     */
    @Parameter( defaultValue = "false", property = "autoVersionSubmodules" )
    private boolean autoVersionSubmodules;

    /**
     * Dry run: don't checkin or tag anything in the scm repository, or modify the checkout. Running
     * <code>mvn -DdryRun=true release:prepare</code> is useful in order to check that modifications to poms and scm
     * operations (only listed on the console) are working as expected. Modified POMs are written alongside the
     * originals without modifying them.
     */
    @Parameter( defaultValue = "false", property = "dryRun" )
    private boolean dryRun;

    /**
     * Whether to add a schema to the POM if it was previously missing on release.
     */
    @Parameter( defaultValue = "true", property = "addSchema" )
    private boolean addSchema;

    /**
     * Goals to run as part of the preparation step, after transformation but before committing. Space delimited.
     */
    @Parameter( defaultValue = "clean verify", property = "preparationGoals" )
    private String preparationGoals;

    /**
     * Goals to run on completion of the preparation step, after transformation back to the next development version but
     * before committing. Space delimited.
     *
     * @since 2.2
     */
    @Parameter( defaultValue = "", property = "completionGoals" )
    private String completionGoals;

    /**
     * Commits to do are atomic or by project.
     *
     * @since 2.0-beta-5
     */
    @Parameter( defaultValue = "false", property = "commitByProject" )
    private boolean commitByProject;

    /**
     * Whether to allow timestamped SNAPSHOT dependencies. Default is to fail when finding any SNAPSHOT.
     *
     * @since 2.0-beta-7
     */
    @Parameter( defaultValue = "false", property = "ignoreSnapshots" )
    private boolean allowTimestampedSnapshots;

    /**
     * Whether to allow usage of a SNAPSHOT version of the Release Plugin. This in an internal property used to support
     * testing of the plugin itself in batch mode.
     *
     * @since 2.0-beta-9
     */
    @Parameter( defaultValue = "false", property = "allowReleasePluginSnapshot", readonly = true )
    private boolean allowReleasePluginSnapshot;

    /**
     * A list of additional exclude filters that will be skipped when checking for modifications on the working copy. Is
     * ignored, when checkModificationExcludes is set.
     *
     * @since 2.1
     */
    @Parameter
    private String[] checkModificationExcludes;

    /**
     * Command-line version of checkModificationExcludes.
     *
     * @since 2.1
     */
    @Parameter( property = "checkModificationExcludeList" )
    private String checkModificationExcludeList;

    /**
     * Default version to use when preparing a release or a branch.
     *
     * @since 2.0-beta-8
     */
    @Parameter( property = "releaseVersion" )
    private String releaseVersion;

    /**
     * Default version to use for new local working copy.
     *
     * @since 2.0-beta-8
     */
    @Parameter( property = "developmentVersion" )
    private String developmentVersion;

    /**
     * Currently only implemented with svn scm.
     * <ul>
     * <li>Enables a workaround to prevent issue due to svn client > 1.5.0 (fixed in 1.6.5)
     * (https://issues.apache.org/jira/browse/SCM-406)</li>
     * <li>You may not want to use this in conjunction with <code>suppressCommitBeforeTag</code>, such that no poms with
     * released versions are committed to the working copy ever.</li>
     * </ul>
     *
     * @since 2.0-beta-9
     */
    @Parameter( defaultValue = "true", property = "remoteTagging" )
    private boolean remoteTagging;

    /**
     * Whether to bump the working copy versions to <code>developmentVersion</code>.
     *
     * @since 2.1
     */
    @Parameter( defaultValue = "true", property = "updateWorkingCopyVersions" )
    private boolean updateWorkingCopyVersions;

    /**
     * Whether to suppress a commit of changes to the working copy before the tag is created. <br/>
     * <br/>
     * This requires <code>remoteTagging</code> to be set to false. <br/>
     * <br/>
     * <code>suppressCommitBeforeTag</code> is useful when you want to avoid poms with released versions in all
     * revisions of your trunk or development branch.
     *
     * @since 2.1
     */
    @Parameter( defaultValue = "false", property = "suppressCommitBeforeTag" )
    private boolean suppressCommitBeforeTag;

    /**
     * Wait the specified number of seconds before creating the tag. <br/>
     * <code>waitBeforeTagging</code> is useful when your source repository is synced between several instances and
     * access to it is determined by geographical location, like the SVN repository at the Apache Software Foundation.
     *
     * @since 2.2
     */
    @Parameter( defaultValue = "0", property = "waitBeforeTagging" )
    private int waitBeforeTagging;

    /**
     * The role-hint for the {@link org.apache.maven.shared.release.policy.version.VersionPolicy}
     * implementation used to calculate the project versions.
     *
     * @since 2.5.1
     * @see org.apache.maven.shared.release.policies.DefaultVersionPolicy
     */
    @Parameter( defaultValue = "default", property = "projectVersionPolicyId" )
    private String projectVersionPolicyId;

    /**
     * The role-hint for the {@link org.apache.maven.shared.release.policy.naming.NamingPolicy}
     * implementation used to calculate the project branch and tag names.
     *
     * @since 3.0.0
     * @see org.apache.maven.shared.release.policies.DefaultNamingPolicy
     */
    @Parameter( property = "projectNamingPolicyId" )
    private String projectTagNamingPolicyId;

    /**
     * The SCM commit comment when setting pom.xml to release.
     * Defaults to "@{prefix} prepare release @{releaseLabel}".
     * <p>
     * Property interpolation is performed on the value, but in order to ensure that the interpolation occurs
     * during release, you must use <code>@{...}</code> to reference the properties rather than <code>${...}</code>.
     * The following properties are available:
     * <ul>
     *     <li><code>prefix</code> - The comment prefix.
     *     <li><code>groupId</code> - The groupId of the root project.
     *     <li><code>artifactId</code> - The artifactId of the root project.
     *     <li><code>releaseLabel</code> - The release version of the root project.
     * </ul>
     *
     * @since 3.0.0
     */
    @Parameter( defaultValue = "@{prefix} prepare release @{releaseLabel}", property = "scmReleaseCommitComment" )
    private String scmReleaseCommitComment = "@{prefix} prepare release @{releaseLabel}";

    /**
     * The SCM commit comment when setting pom.xml back to development.
     * Defaults to "@{prefix} prepare for next development iteration".
     * <p>
     * Property interpolation is performed on the value, but in order to ensure that the interpolation occurs
     * during release, you must use <code>@{...}</code> to reference the properties rather than <code>${...}</code>.
     * The following properties are available:
     * <ul>
     *     <li><code>prefix</code> - The comment prefix.
     *     <li><code>groupId</code> - The groupId of the root project.
     *     <li><code>artifactId</code> - The artifactId of the root project.
     *     <li><code>releaseLabel</code> - The release version of the root project.
     * </ul>
     *
     * @since 3.0.0
     */
    @Parameter( 
            defaultValue = "@{prefix} prepare for next development iteration", 
            property = "scmDevelopmentCommitComment" )
    private String scmDevelopmentCommitComment = "@{prefix} prepare for next development iteration";

    /**
     * Specifies whether unresolved SNAPSHOT dependencies should automatically be resolved.
     * If this is set, then this specifies the default answer to be used when unresolved SNAPSHOT
     * dependencies should automatically be resolved ( 0:All 1:Project Dependencies 2:Plugins
     * 3:Reports 4:Extensions ). Possible values are:
     * <ul>
     * <li>"all" or "0": resolve all kinds of snapshots, ie. project, plugin, report and extension dependencies </li>
     * <li>"dependencies" or "1": resolve project dependencies</li>
     * <li>"plugins" or "2": resolve plugin dependencis</li>
     * <li>"reports" or "3": resolve report dependencies</li>
     * <li>"extensions" or "4": resolve extension dependencies</li>
     * </ul>
     * 
     * @since 3.0.0
     */
    @Parameter( property = "autoResolveSnapshots" )
    private String autoResolveSnapshots;

    /**
     * Currently only implemented with svn scm. Enable the {@code --pin-externals} option in
     * {@code svn copy} command which is new in Subversion 1.9.
     *
     * @since 3.0.0
     */
    @Parameter( defaultValue = "false", property = "pinExternals" )
    private boolean pinExternals;

    /**
     * Specifies the line separator to format pom.xml. The default value is system. The following properties are
     * available:
     * <ul>
     * <li><code>system</code> - Use the system line separator.</li>
     * <li><code>lf</code> - Use \n as line separator.</li>
     * <li><code>cr</code> - Use \r as line separator.</li>
     * <li><code>crlf</code> - Use \r\n as line separator.</li>
     * <li><code>source</code> - Use the same line separator as it is specified in the current pom.xml.</li>
     * </ul>
     *
     * @since 3.0.0
     */
    @Parameter( defaultValue = "source", property = "lineSeparator" )
    private String lineSeparator;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute()
        throws MojoExecutionException, MojoFailureException
    {
        if ( generateReleasePoms )
        {
            throw new MojoFailureException( "Generating release POMs is no longer supported in release:prepare. "
                + "Please run release:prepare-with-pom instead." );
        }

        prepareRelease( generateReleasePoms );
    }

    protected void prepareRelease( boolean generateReleasePoms )
        throws MojoExecutionException, MojoFailureException
    {
        // this is here so the subclass can call it without getting the extra generateReleasePoms check in execute()
        // above
        super.execute();

        final ReleaseDescriptorBuilder config = createReleaseDescriptor();
        config.setAddSchema( addSchema );
        config.setGenerateReleasePoms( generateReleasePoms );
        config.setScmUseEditMode( useEditMode );
        config.setPreparationGoals( preparationGoals );
        config.setCompletionGoals( completionGoals );
        config.setCommitByProject( commitByProject );
        config.setUpdateDependencies( updateDependencies );
        config.setAutoVersionSubmodules( autoVersionSubmodules );
        config.setAllowTimestampedSnapshots( allowTimestampedSnapshots );
        config.setSnapshotReleasePluginAllowed( allowReleasePluginSnapshot );
        config.setDefaultReleaseVersion( releaseVersion );
        config.setDefaultDevelopmentVersion( developmentVersion );
        config.setRemoteTagging( remoteTagging );
        config.setUpdateWorkingCopyVersions( updateWorkingCopyVersions );
        config.setSuppressCommitBeforeTagOrBranch( suppressCommitBeforeTag );
        config.setWaitBeforeTagging( waitBeforeTagging );
        config.setProjectVersionPolicyId( projectVersionPolicyId );
        config.setProjectNamingPolicyId( projectTagNamingPolicyId );
        config.setScmDevelopmentCommitComment( scmDevelopmentCommitComment );
        config.setScmReleaseCommitComment( scmReleaseCommitComment );
        config.setAutoResolveSnapshots( autoResolveSnapshots );
        config.setPinExternals( pinExternals );
        if ( generateReleasePoms )
        {
            config.setLineSeparator( resolveLineSeparator() );
        }
        
        if ( checkModificationExcludeList != null )
        {
            checkModificationExcludes = checkModificationExcludeList.replaceAll( "\\s", "" ).split( "," );
        }

        if ( checkModificationExcludes != null )
        {
            config.setCheckModificationExcludes( Arrays.asList( checkModificationExcludes ) );
        }
        
        ReleasePrepareRequest prepareRequest = new ReleasePrepareRequest();
        prepareRequest.setReleaseDescriptorBuilder( config );
        prepareRequest.setReleaseEnvironment( getReleaseEnvironment() );
        prepareRequest.setReactorProjects( getReactorProjects() );
        prepareRequest.setReleaseManagerListener( new DefaultReleaseManagerListener( getLog(), dryRun ) );
        prepareRequest.setResume( resume );
        prepareRequest.setDryRun( dryRun );
        prepareRequest.setUserProperties( session.getUserProperties() );

        try
        {
            releaseManager.prepare( prepareRequest );
        }
        catch ( ReleaseExecutionException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }
        catch ( ReleaseFailureException e )
        {
            throw new MojoFailureException( e.getMessage(), e );
        }
    }

    private String resolveLineSeparator() throws MojoExecutionException
    {
        if ( lineSeparator  == null )
        {
            return getLineSeparatorFromPom();
        }

        switch ( lineSeparator )
        {
            case "lf":
                return "\n";
            case "cr":
                return "\r";
            case "crlf":
                return "\r\n"; 
            case "system":
                return System.getProperty( "line.separator" );
            case "source":
                return getLineSeparatorFromPom();
            default:
                throw new IllegalArgumentException( String.format( "Unknown property lineSeparator: '%s'. Use one of" 
                  + " the following: 'source', 'system', 'lf', 'cr', 'crlf'.", lineSeparator ) );
        }
    }

    private String getLineSeparatorFromPom()
      throws MojoExecutionException
    {
        char current;
        String lineSeparator = "";
        try ( FileInputStream fis = new FileInputStream( this.project.getFile() ) )
        {
            while ( fis.available() > 0 )
            {
                current = ( char ) fis.read();
                if ( ( current == '\n' ) || ( current == '\r' ) )
                {
                    lineSeparator += current;
                    if ( fis.available() > 0 ) 
                    {
                        char next = ( char ) fis.read();
                        if ( ( next != current )
                          && ( ( next == '\r' ) || ( next == '\n' ) ) )
                        {
                            lineSeparator += next;
                        }
                    }
                    return lineSeparator;
                }
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( e.getMessage(), e );
        }

        return lineSeparator;
    }

}
