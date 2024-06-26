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

import groovy.xml.XmlSlurper

File buildLog = new File( basedir, 'build.log' )
assert buildLog.exists()

// tag versions
def projectRootTag = new XmlSlurper().parse( new File( basedir, 'pom.xml.tag' ) )
assert projectRootTag.version.text() == '${revision}${sha1}${changelist}'
assert projectRootTag.properties.revision.text() == "1.0.0"
assert projectRootTag.properties.sha1.text() == "-abcdef12"
assert projectRootTag.properties.changelist.text() == ""

def projectATag = new XmlSlurper().parse( new File( basedir, 'module-a/pom.xml.tag' ) )
assert projectATag.parent.version.text() == '${revision}${sha1}${changelist}'

def projectBTag = new XmlSlurper().parse( new File( basedir, 'module-b/pom.xml.tag' ) )
assert projectBTag.parent.version.text() == '${revision}${sha1}${changelist}'


// next development versions
def projectRoot = new XmlSlurper().parse( new File( basedir, 'pom.xml.next' ) )
assert projectRoot.version.text() == '${revision}${sha1}${changelist}'
assert projectRoot.properties.revision.text() == "1.0.1"
assert projectRoot.properties.sha1.text() == ""
assert projectRoot.properties.changelist.text() == "-SNAPSHOT"

def projectA = new XmlSlurper().parse( new File( basedir, 'module-a/pom.xml.next' ) )
assert projectA.parent.version.text() == '${revision}${sha1}${changelist}'

def projectB = new XmlSlurper().parse( new File( basedir, 'module-b/pom.xml.next' ) )
assert projectB.parent.version.text() == '${revision}${sha1}${changelist}'

return true
