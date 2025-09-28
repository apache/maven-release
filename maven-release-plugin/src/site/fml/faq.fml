<?xml version="1.0" encoding="UTF-8"?>

<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->


<faqs xmlns="http://maven.apache.org/FML/1.0.1"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/FML/1.0.1 http://maven.apache.org/xsd/fml-1.0.1.xsd"
  id="FAQ" title="Frequently Asked Questions">
  <part id="General">
    <faq id="versioning">
      <question>When releasing from a parent POM, won't all its modules have the same release version and dev version as
        this parent?
      </question>
      <answer>
        <p>By default Maven Release Plugin will prompt for each project's version.
          However, you can set the <a href="./prepare-mojo.html#autoVersionSubmodules"><code>autoVersionSubmodules</code> parameter</a>
          to <code>true</code> to automatically use the parent version.</p>
      </answer>
    </faq>
    <faq id="nonrecursive">
      <question>How can I release a parent POM without releasing all its modules?
      </question>
      <answer>
        <p>You need to pass the <code>-N</code> flag to Maven <b>and</b> configure the Release Plugin to pass the
          <code>-N</code> flag on its forked builds with the <code>-Darguments</code> flag.</p>
        <p>Here's an example:</p>
<source>
mvn -N -Darguments=-N release:prepare
mvn -N -Darguments=-N release:perform
</source>
      </answer>
    </faq>
    <faq id="credentials">
      <question>How can I hide my username and password?</question>
      <answer>
        <p>Add a server-entry to your <code>settings.xml</code>, where you define your credentials (see <a href="http://maven.apache.org/settings.html#Servers">settings.xml#servers</a> for more details). 
      	Since you can't set an id for the <code>scm</code> in your <code>pom.xml</code>, you should add a property pointing to the server-id.
      	</p>
<source>
<![CDATA[
  <project>
   ...
    <properties>
      <project.scm.id>my-scm-server</project.scm.id>
    </properties>
  </project>
]]>
</source>
      </answer>
    </faq>
    <faq id="scmCommentPrefix"> <!-- MRELEASE-156 -->
      <question>How can I customize the comment prefix of a commit during release preparation?</question>
      <answer>
        <p>
        By default the plugin will prefix the comment with <code>[maven-release-plugin] </code>.
        You can change this by adding <code>-DscmCommentPrefix=#42</code> to the Maven command.<br/>
        If you need to end this with a linebreak, add <code>${line.separator}</code>.<br/>
        If the comment prefix contains whitespace characters, surround the argument with quotes.
        </p>
        <p>
        When using the plugin-configuration, you can't end with a space, because Plexus will trim the content.
        But you can end with a linebreak just like the example below:
        </p>
<source>
<![CDATA[
  <configuration>
    <scmCommentPrefix>#42${line.separator}</scmCommentPrefix>
    ...
  </configuration>
]]>
</source>
      </answer>
    </faq>
    <faq id="inheritScm">
      <question>Why is <code>release:prepare</code> failing when trying to tag with
        <code>"svn: Path '...' does not exist in revision ..."</code>?</question>
      <answer>
        <p>If the <code>pom.xml</code> has no scm-section but inherits one from its parent, the prepare goal will fail
         when trying to tag. The message will be something like:</p>
        <pre>svn: Path '...' does not exist in revision ...</pre>
        <p>To fix it, roll-back the release, add the <code>scm</code> section to the <code>pom.xml</code>, commit and retry the release.</p>
      </answer>
    </faq>
    <faq id="customDeployPlugin">
      <question>Can I use a plugin other than Maven Deploy Plugin for deployment?</question>
      <answer>
        <p>Yes, you can use any other plugin for deployment. There is no strong interconnection between the
          Maven Release Plugin and the Maven Deploy Plugin. The Maven Release Plugin simply runs the <code>deploy</code>
          Maven goal during release (or any <a href="./perform-mojo.html#goals">other goals you specified</a>).
          This allows you for example to use the <code>central-publishing-maven-plugin</code> when
          <a href="https://central.sonatype.org/publish/publish-portal-maven/">deploying to Maven Central</a>.</p>
        <p>However, it might be necessary to configure the Maven Deploy Plugin to skip execution because the
          plugin is <a href="https://maven.apache.org/ref/current/maven-core/default-bindings.html#Plugin_bindings_for_jar_packaging">
          automatically configured by default</a> (note that some custom deploy plugins automatically disable
          the Maven Deploy Plugin):</p>
<source>
<![CDATA[
  <build>
    <pluginManagement>
      <plugins>
        <!-- Skip execution of Maven Deploy Plugin due to usage of custom deploy plugin -->
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-deploy-plugin</artifactId>
          <version>...</version>
          <configuration>
            <skip>true</skip>
          </configuration>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
]]>
</source>
      </answer>
    </faq>
  </part>
</faqs>
