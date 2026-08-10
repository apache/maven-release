---
title: Frequently Asked Questions
---

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

<a id="top"></a>

# Frequently Asked Questions

1. [When releasing from a parent POM, won't all its modules have the same release version and dev version as this parent?](#versioning)
2. [How can I release a parent POM without releasing all its modules?](#nonrecursive)
3. [How can I hide my username and password?](#credentials)
4. [How can I customize the comment prefix of a commit during release preparation?](#scmCommentPrefix)
5. [Why is `release:prepare` failing when trying to tag with `"svn: Path '...' does not exist in revision ..."`?](#inheritScm)
6. [Can I use a plugin other than Maven Deploy Plugin for deployment?](#customDeployPlugin)

<a id="versioning"></a>

### When releasing from a parent POM, won't all its modules have the same release version and dev version as this parent?

By default Maven Release Plugin will prompt for each project's version.
However, you can set the [`autoVersionSubmodules` parameter](./prepare-mojo.html#autoVersionSubmodules)
to `true` to automatically use the parent version.

<a id="nonrecursive"></a>

### How can I release a parent POM without releasing all its modules?

You need to pass the `-N` flag to Maven **and** configure the Release Plugin to pass the
`-N` flag on its forked builds with the `-Darguments` flag.

Here's an example:

```
mvn -N -Darguments=-N release:prepare
mvn -N -Darguments=-N release:perform
```

<a id="credentials"></a>

### How can I hide my username and password?

Add a server-entry to your `settings.xml`, where you define your credentials (see [settings.xml#servers](http://maven.apache.org/settings.html#Servers) for more details).
Since you can't set an id for the `scm` in your `pom.xml`, you should add a property pointing to the server-id.

```xml
  <project>
   ...
    <properties>
      <project.scm.id>my-scm-server</project.scm.id>
    </properties>
  </project>
```

<a id="scmCommentPrefix"></a>

### How can I customize the comment prefix of a commit during release preparation?

By default the plugin will prefix the comment with `[maven-release-plugin] `.
You can change this by adding `-DscmCommentPrefix=#42` to the Maven command.<br />
If you need to end this with a linebreak, add `${line.separator}`.<br />
If the comment prefix contains whitespace characters, surround the argument with quotes.

When using the plugin-configuration, you can't end with a space, because Plexus will trim the content.
But you can end with a linebreak just like the example below:

```xml
  <configuration>
    <scmCommentPrefix>#42${line.separator}</scmCommentPrefix>
    ...
  </configuration>
```

<a id="inheritScm"></a>

### Why is `release:prepare` failing when trying to tag with `"svn: Path '...' does not exist in revision ..."`?

If the `pom.xml` has no scm-section but inherits one from its parent, the prepare goal will fail
when trying to tag. The message will be something like:

```
svn: Path '...' does not exist in revision ...
```

To fix it, roll-back the release, add the `scm` section to the `pom.xml`, commit and retry the release.

<a id="customDeployPlugin"></a>

### Can I use a plugin other than Maven Deploy Plugin for deployment?

Yes, you can use any other plugin for deployment. There is no strong interconnection between the
Maven Release Plugin and the Maven Deploy Plugin. The Maven Release Plugin simply runs the `deploy`
Maven goal during release (or any [other goals you specified](./perform-mojo.html#goals)).
This allows you for example to use the `central-publishing-maven-plugin` when
[deploying to Maven Central](https://central.sonatype.org/publish/publish-portal-maven/).

However, it might be necessary to configure the Maven Deploy Plugin to skip execution because the
plugin is [automatically configured by default](https://maven.apache.org/ref/current/maven-core/default-bindings.html#Plugin_bindings_for_jar_packaging)
(note that some custom deploy plugins automatically disable
the Maven Deploy Plugin):

```xml
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
```
