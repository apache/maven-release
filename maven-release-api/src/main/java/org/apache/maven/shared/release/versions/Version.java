package org.apache.maven.shared.release.versions;

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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.ArtifactUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * 
 */
public class Version
    implements Comparable<Version>, Cloneable
{
    private final AetherVersion aetherVersion;

    private final MavenArtifactVersion mavenArtifactVersion;

    private final String strVersion;

    private final List<String> digits;

    private String annotation;

    private String annotationRevision;

    private final String buildSpecifier;

    private String annotationSeparator;

    private String annotationRevSeparator;

    private String buildSeparator;

    private static final int DIGITS_INDEX = 1;

    private static final int ANNOTATION_SEPARATOR_INDEX = 2;

    private static final int ANNOTATION_INDEX = 3;

    private static final int ANNOTATION_REV_SEPARATOR_INDEX = 4;

    private static final int ANNOTATION_REVISION_INDEX = 5;

    private static final int BUILD_SEPARATOR_INDEX = 6;

    private static final int BUILD_SPECIFIER_INDEX = 7;

    private static final String SNAPSHOT_IDENTIFIER = "SNAPSHOT";

    private static final String DIGIT_SEPARATOR_STRING = ".";
    
    private static final String DEFAULT_ANNOTATION_REV_SEPARATOR = "-";

    private static final String DEFAULT_BUILD_SEPARATOR = "-";

    public static final Pattern STANDARD_PATTERN = Pattern.compile( "^((?:\\d+\\.)*\\d+)" // digit(s) and '.' repeated -
                                                                                          // followed by digit (version
                                                                                          // digits 1.22.0, etc)
        + "([-_])?" // optional - or _ (annotation separator)
        + "([a-zA-Z]*)" // alpha characters (looking for annotation - alpha, beta, RC, etc.)
        + "([-_])?" // optional - or _ (annotation revision separator)
        + "(\\d*)" // digits (any digits after rc or beta is an annotation revision)
        + "(?:([-_])?(.*?))?$" ); // - or _ followed everything else (build specifier)

    /* *
     * cmaki 02242009 FIX for non-digit release numbers, e.g. trunk-SNAPSHOT or just SNAPSHOT This alternate pattern
     * supports version numbers like: trunk-SNAPSHOT branchName-SNAPSHOT SNAPSHOT
     */
    // for SNAPSHOT releases only (possible versions include: trunk-SNAPSHOT or SNAPSHOT)
    public static final Pattern ALTERNATE_PATTERN = Pattern.compile( "^(SNAPSHOT|[a-zA-Z]+[_-]SNAPSHOT)" );
    
    private Version( List<String> digits, String annotation, String annotationRevision, String buildSpecifier,
                               String annotationSeparator, String annotationRevSeparator, String buildSeparator )
    {
        this.digits = digits;
        this.annotation = annotation;
        this.annotationRevision = annotationRevision;
        this.buildSpecifier = buildSpecifier;
        this.annotationSeparator = annotationSeparator;
        this.annotationRevSeparator = annotationRevSeparator;
        this.buildSeparator = buildSeparator;
        this.strVersion = getVersionString( this, buildSpecifier, buildSeparator );

        // for now no need to reparse, original version was valid 
        this.aetherVersion = null;
        this.mavenArtifactVersion = null;
    }

    public Version( String version )
        throws VersionParseException
    {
        this.strVersion = version;
        this.aetherVersion = new AetherVersion( version );
        this.mavenArtifactVersion = new MavenArtifactVersion( version );

        // FIX for non-digit release numbers, e.g. trunk-SNAPSHOT or just SNAPSHOT
        Matcher matcher = ALTERNATE_PATTERN.matcher( strVersion );
        // TODO: hack because it didn't support "SNAPSHOT"
        if ( matcher.matches() )
        {
            annotation = null;
            digits = null;
            buildSpecifier = version;
            buildSeparator = null;
            return;
        }

        Matcher m = STANDARD_PATTERN.matcher( strVersion );
        if ( m.matches() )
        {
            digits = parseDigits( m.group( DIGITS_INDEX ) );
            if ( !SNAPSHOT_IDENTIFIER.equals( m.group( ANNOTATION_INDEX ) ) )
            {
                annotationSeparator = m.group( ANNOTATION_SEPARATOR_INDEX );
                annotation = nullIfEmpty( m.group( ANNOTATION_INDEX ) );

                if ( StringUtils.isNotEmpty( m.group( ANNOTATION_REV_SEPARATOR_INDEX ) )
                    && StringUtils.isEmpty( m.group( ANNOTATION_REVISION_INDEX ) ) )
                {
                    // The build separator was picked up as the annotation revision separator
                    buildSeparator = m.group( ANNOTATION_REV_SEPARATOR_INDEX );
                    buildSpecifier = nullIfEmpty( m.group( BUILD_SPECIFIER_INDEX ) );
                }
                else
                {
                    annotationRevSeparator = m.group( ANNOTATION_REV_SEPARATOR_INDEX );
                    annotationRevision = nullIfEmpty( m.group( ANNOTATION_REVISION_INDEX ) );

                    buildSeparator = m.group( BUILD_SEPARATOR_INDEX );
                    buildSpecifier = nullIfEmpty( m.group( BUILD_SPECIFIER_INDEX ) );
                }
            }
            else
            {
                // Annotation was "SNAPSHOT" so populate the build specifier with that data
                buildSeparator = m.group( ANNOTATION_SEPARATOR_INDEX );
                buildSpecifier = nullIfEmpty( m.group( ANNOTATION_INDEX ) );
            }
        }
        else
        {
            throw new VersionParseException( "Unable to parse the version string: \"" + version + "\"" );
        }
    }

    public boolean isSnapshot()
    {
        return ArtifactUtils.isSnapshot( strVersion );
    }

    public String toString()
    {
        return strVersion;
    }

    protected static String getVersionString( Version info, String buildSpecifier, String buildSeparator )
    {
        StringBuilder sb = new StringBuilder();

        if ( info.digits != null )
        {
            sb.append( joinDigitString( info.digits ) );
        }

        if ( StringUtils.isNotEmpty( info.annotation ) )
        {
            sb.append( StringUtils.defaultString( info.annotationSeparator ) );
            sb.append( info.annotation );
        }

        if ( StringUtils.isNotEmpty( info.annotationRevision ) )
        {
            if ( StringUtils.isEmpty( info.annotation ) )
            {
                sb.append( StringUtils.defaultString( info.annotationSeparator ) );
            }
            else
            {
                sb.append( StringUtils.defaultString( info.annotationRevSeparator ) );
            }
            sb.append( info.annotationRevision );
        }

        if ( StringUtils.isNotEmpty( buildSpecifier ) )
        {
            sb.append( StringUtils.defaultString( buildSeparator ) );
            sb.append( buildSpecifier );
        }

        return sb.toString();
    }

    /**
     * Simply joins the items in the list with "." period
     * 
     * @return a {@code String} containing the items in the list joined by "." period
     * @param digits the list of digits {@code List<String>}
     */
    protected static String joinDigitString( List<String> digits )
    {
        return digits != null ? StringUtils.join( digits.iterator(), DIGIT_SEPARATOR_STRING ) : null;
    }

    /**
     * Splits the string on "." and returns a list containing each digit.
     * 
     * @param strDigits
     */
    private List<String> parseDigits( String strDigits )
    {
        return Arrays.asList( StringUtils.split( strDigits, DIGIT_SEPARATOR_STRING ) );
    }

    private static String nullIfEmpty( String s )
    {
        return StringUtils.isEmpty( s ) ? null : s;
    }

    public List<String> getDigits()
    {
        return digits;
    }
    
    public String getAnnotation()
    {
        return annotation;
    }

    public String getAnnotationRevSeparator()
    {
        return annotationRevSeparator;
    }

    public String getAnnotationRevision()
    {
        return annotationRevision;
    }

    public String getBuildSeparator()
    {
        return buildSeparator;
    }

    public String getBuildSpecifier()
    {
        return buildSpecifier;
    }
    
    /**
     * 
     * @param newDigits the new list of digits
     * @return a new instance of Version
     */
    public Version setDigits( List<String> newDigits )
    {
        return new Version( newDigits, this.annotation, this.annotationRevision, this.buildSpecifier,
                            this.annotationSeparator, this.annotationRevSeparator, this.buildSeparator );
    }
    
    /**
     * 
     * @param newAnnotationRevision the new annotation revision
     * @return a new instance of Version
     */
    public Version setAnnotationRevision( String newAnnotationRevision )
    {
        return new Version( this.digits, this.annotation, newAnnotationRevision, this.buildSpecifier,
                            this.annotationSeparator,
                            Objects.toString( this.annotationRevSeparator, DEFAULT_ANNOTATION_REV_SEPARATOR ),
                            this.buildSeparator );
    }
    
    /**
     * 
     * @param newBuildSpecifier the new build specifier
     * @return a new instance of Version
     */
    public Version setBuildSpecifier( String newBuildSpecifier )
    {
        return new Version( this.digits, this.annotation, this.annotationRevision, newBuildSpecifier,
                            this.annotationSeparator, this.annotationRevSeparator,
                            Objects.toString( this.buildSeparator, DEFAULT_BUILD_SEPARATOR ) );
    }
    
    /**
     * @throws VersionComparisonConflictException if {@link org.eclipse.aether.version.Version} and
     *             {@link org.apache.maven.artifact.versioning.ArtifactVersion ArtifactVersion} give different results
     */
    public int compareTo( Version other )
        throws VersionComparisonConflictException
    {
        int aetherComparisonResult = this.aetherVersion.compareTo( other.aetherVersion );
        int mavenComparisonResult = this.mavenArtifactVersion.compareTo( other.mavenArtifactVersion );

        if ( aetherComparisonResult < 0 && mavenComparisonResult < 0 )
        {
            return -1;
        }
        else if ( aetherComparisonResult == 0 && mavenComparisonResult == 0 )
        {
            return 0;
        }
        else if ( aetherComparisonResult > 0 && mavenComparisonResult > 0 )
        {
            return 1;
        }
        else
        {
            throw new VersionComparisonConflictException( this.strVersion, other.strVersion, aetherComparisonResult,
                                                          mavenComparisonResult );
        }
    }

}
