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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This compares and increments versions for a common java versioning scheme.
 * <p/>
 * The supported version scheme has the following parts.<br>
 * <code><i>component-digits-annotation-annotationRevision-buildSpecifier</i></code><br>
 * Example:<br>
 * <code>my-component-1.0.1-alpha-2-SNAPSHOT</code>
 * <p/>
 * <ul>Terms:
 * <li><i>component</i> - name of the versioned component (log4j, commons-lang, etc)
 * <li><i>digits</i> - Numeric digits with at least one "." period. (1.0, 1.1, 1.01, 1.2.3, etc)
 * <li><i>annotationRevision</i> - Integer qualifier for the annotation. (4 as in RC-4)
 * <li><i>buildSpecifier</i> - Additional specifier for build. (SNAPSHOT, or build number like "20041114.081234-2")
 * </ul>
 * <b>Digits is the only required piece of the version string, and must contain at lease one "." period.</b>
 * <p/>
 * Implementation details:<br>
 * The separators "_" and "-" between components are also optional (though they are usually recommended).<br>
 * Example:<br>
 * <code>log4j-1.2.9-beta-9-SNAPSHOT == log4j1.2.9beta9SNAPSHOT == log4j_1.2.9_beta_9_SNAPSHOT</code>
 * <p/>
 * Leading zeros are significant when performing comparisons.
 * <p/>
 * TODO: this parser is better than DefaultArtifactVersion - replace it with this (but align naming) and then remove this from here.
 */
public class DefaultVersionInfo
    implements VersionInfo
{
    private final String strVersion;

    private final List<String> digits;

    private String annotation;

    private String annotationRevision;

    private final String buildSpecifier;

    private String annotationSeparator;

    private String annotationRevSeparator;

    private final String buildSeparator;

    private static final int DIGITS_INDEX = 1;

    private static final int ANNOTATION_SEPARATOR_INDEX = 2;

    private static final int ANNOTATION_INDEX = 3;

    private static final int ANNOTATION_REV_SEPARATOR_INDEX = 4;

    private static final int ANNOTATION_REVISION_INDEX = 5;

    private static final int BUILD_SEPARATOR_INDEX = 6;

    private static final int BUILD_SPECIFIER_INDEX = 7;

    private static final String SNAPSHOT_IDENTIFIER = "SNAPSHOT";

    private static final String DIGIT_SEPARATOR_STRING = ".";

    public static final Pattern STANDARD_PATTERN = Pattern.compile(
        "^((?:\\d+\\.)*\\d+)"      // digit(s) and '.' repeated - followed by digit (version digits 1.22.0, etc)
        + "([-_])?"                // optional - or _  (annotation separator)
        + "([a-zA-Z]*)"            // alpha characters (looking for annotation - alpha, beta, RC, etc.)
        + "([-_])?"                // optional - or _  (annotation revision separator)
        + "(\\d*)"                 // digits  (any digits after rc or beta is an annotation revision)
        + "(?:([-_])?(.*?))?$" );  // - or _ followed everything else (build specifier)

    /* *
     * cmaki 02242009
     * FIX for non-digit release numbers, e.g. trunk-SNAPSHOT or just SNAPSHOT
     * This alternate pattern supports version numbers like:
     * trunk-SNAPSHOT
     * branchName-SNAPSHOT
     * SNAPSHOT
     */
    public static final Pattern ALTERNATE_PATTERN = Pattern.compile(
        "^(SNAPSHOT|[a-zA-Z]+[_-]SNAPSHOT)"      // for SNAPSHOT releases only (possible versions include: trunk-SNAPSHOT or SNAPSHOT)
    );

    /**
     * Constructs this object and parses the supplied version string.
     *
     * @param version
     */
    public DefaultVersionInfo( String version )
        throws VersionParseException
    {
        strVersion = version;

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

    public DefaultVersionInfo( List<String> digits, String annotation, String annotationRevision, String buildSpecifier,
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
    }

    public boolean isSnapshot()
    {
        return ArtifactUtils.isSnapshot( strVersion );
    }

    public VersionInfo getNextVersion()
    {
        DefaultVersionInfo version = null;
        if ( digits != null )
        {
            List<String> digits = new ArrayList<String>( this.digits );
            String annotationRevision = this.annotationRevision;
            if ( StringUtils.isNumeric( annotationRevision ) )
            {
                annotationRevision = incrementVersionString( annotationRevision );
            }
            else
            {
                digits.set( digits.size() - 1, incrementVersionString( (String) digits.get( digits.size() - 1 ) ) );
            }

            version = new DefaultVersionInfo( digits, annotation, annotationRevision, buildSpecifier,
                                              annotationSeparator, annotationRevSeparator, buildSeparator );
        }
        return version;
    }

    /**
     * Compares this {@link DefaultVersionInfo} to the supplied {@link DefaultVersionInfo}
     * to determine which version is greater.
     *
     * @param obj the comparison version
     * @return the comparison value
     * @throws IllegalArgumentException if the components differ between the objects or if either of the annotations can not be determined.
     */
    public int compareTo( VersionInfo obj )
    {
        DefaultVersionInfo that = (DefaultVersionInfo) obj;

        int result;
        // TODO: this is a workaround for a bug in DefaultArtifactVersion - fix there - 1.01 < 1.01.01
        if ( strVersion.startsWith( that.strVersion ) && !strVersion.equals( that.strVersion )
            && strVersion.charAt( that.strVersion.length() ) != '-' )
        {
            result = 1;
        }
        else if ( that.strVersion.startsWith( strVersion ) && !strVersion.equals( that.strVersion )
            && that.strVersion.charAt( strVersion.length() ) != '-' )
        {
            result = -1;
        }
        else
        {
            // TODO: this is a workaround for a bug in DefaultArtifactVersion - fix there - it should not consider case in comparing the qualifier
            // NOTE: The combination of upper-casing and lower-casing is an approximation of String.equalsIgnoreCase()
            String thisVersion = strVersion.toUpperCase( Locale.ENGLISH ).toLowerCase( Locale.ENGLISH );
            String thatVersion = that.strVersion.toUpperCase( Locale.ENGLISH ).toLowerCase( Locale.ENGLISH );

            result = new DefaultArtifactVersion( thisVersion ).compareTo( new DefaultArtifactVersion( thatVersion ) );
        }
        return result;
    }

    public boolean equals( Object obj )
    {
        if ( !( obj instanceof DefaultVersionInfo ) )
        {
            return false;
        }

        return compareTo( (VersionInfo) obj ) == 0;
    }

    /**
     * Takes a string and increments it as an integer.
     * Preserves any lpad of "0" zeros.
     *
     * @param s
     */
    protected String incrementVersionString( String s )
    {
        int n = Integer.valueOf( s ).intValue() + 1;
        String value = String.valueOf( n );
        if ( value.length() < s.length() )
        {
            // String was left-padded with zeros
            value = StringUtils.leftPad( value, s.length(), "0" );
        }
        return value;
    }

    public String getSnapshotVersionString()
    {
        if ( strVersion.equals( Artifact.SNAPSHOT_VERSION ) )
        {
            return strVersion;
        }

        String baseVersion = getReleaseVersionString();

        if ( baseVersion.length() > 0 )
        {
            baseVersion += "-";
        }

        return baseVersion + Artifact.SNAPSHOT_VERSION;
    }

    public String getReleaseVersionString()
    {
        String baseVersion = strVersion;

        Matcher m = Artifact.VERSION_FILE_PATTERN.matcher( baseVersion );
        if ( m.matches() )
        {
            baseVersion = m.group( 1 );
        }
        // MRELEASE-623 SNAPSHOT is case-insensitive
        else if ( StringUtils.right( baseVersion, 9 ).equalsIgnoreCase( "-" + Artifact.SNAPSHOT_VERSION ) )
        {
            baseVersion = baseVersion.substring( 0, baseVersion.length() - Artifact.SNAPSHOT_VERSION.length() - 1 );
        }
        else if ( baseVersion.equals( Artifact.SNAPSHOT_VERSION ) )
        {
            baseVersion = "1.0";
        }
        return baseVersion;
    }

    public String toString()
    {
        return strVersion;
    }

    protected static String getVersionString( DefaultVersionInfo info, String buildSpecifier, String buildSeparator )
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
     * @param digits
     */
    protected static String joinDigitString( List<String> digits )
    {
        return digits != null ? StringUtils.join( digits.iterator(), DIGIT_SEPARATOR_STRING ) : null;
    }

    /**
     * Splits the string on "." and returns a list
     * containing each digit.
     *
     * @param strDigits
     */
    private List<String> parseDigits( String strDigits )
    {
        return Arrays.asList( StringUtils.split( strDigits, DIGIT_SEPARATOR_STRING ) );
    }

    //--------------------------------------------------
    // Getters & Setters
    //--------------------------------------------------

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

    public String getAnnotationRevision()
    {
        return annotationRevision;
    }

    public String getBuildSpecifier()
    {
        return buildSpecifier;
    }

}
