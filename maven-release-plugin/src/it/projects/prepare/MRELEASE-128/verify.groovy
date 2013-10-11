
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

File pomXml = new File( basedir, 'pom.xml' )
assert pomXml.exists()
assert 1 == pomXml.getText().count("<connection>scm:svn:http://\${scm.host}/svn/\${project.artifactId}/trunk/</connection>")
assert 1 == pomXml.getText().count("<developerConnection>scm:svn:http://\${scm.host}/svn/\${project.artifactId}/trunk/</developerConnection>")

File pomXmlTag = new File( basedir, 'pom.xml.tag' )
assert pomXmlTag.exists()
assert 1 == pomXmlTag.getText().count("<connection>scm:svn:http://\${scm.host}/svn/\${project.artifactId}/tags/mrelease-128-1.0</connection>")
assert 1 == pomXmlTag.getText().count("<developerConnection>scm:svn:http://\${scm.host}/svn/\${project.artifactId}/tags/mrelease-128-1.0</developerConnection>")

File pomXmlNext = new File( basedir, 'pom.xml.next' )
assert pomXmlNext.exists()
assert 1 == pomXmlNext.getText().count("<connection>scm:svn:http://\${scm.host}/svn/\${project.artifactId}/trunk/</connection>")
assert 1 == pomXmlNext.getText().count("<developerConnection>scm:svn:http://\${scm.host}/svn/\${project.artifactId}/trunk/</developerConnection>")

File pomXmlReleaseBackup = new File( basedir, 'pom.xml.releaseBackup' )
assert pomXmlReleaseBackup.exists()
assert 1 == pomXmlReleaseBackup.getText().count("<connection>scm:svn:http://\${scm.host}/svn/\${project.artifactId}/trunk/</connection>")
assert 1 == pomXmlReleaseBackup.getText().count("<developerConnection>scm:svn:http://\${scm.host}/svn/\${project.artifactId}/trunk/</developerConnection>")