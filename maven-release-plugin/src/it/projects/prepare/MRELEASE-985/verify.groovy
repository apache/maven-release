
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

def project = new XmlSlurper().parse( new File( basedir, "pom.xml" ) )
assert project.version.text() == "1.0-SNAPSHOT"
assert project.parent.version.text() == "1-SNAPSHOT"
assert project.dependencies.dependency.version.text() == "1.0-SNAPSHOT"

def projectTag = new XmlSlurper().parse( new File( basedir, "pom.xml.tag" ) )
assert projectTag.version.text() == "1.0"
assert projectTag.parent.version.text() == "1"
assert projectTag.dependencies.dependency.version.text() == "1.0.RELEASE"

def projectNext = new XmlSlurper().parse( new File( basedir, "pom.xml.next" ) )
assert projectNext.version.text() == "1.1-SNAPSHOT"
assert projectNext.parent.version.text() == "2-SNAPSHOT"
assert projectNext.dependencies.dependency.version.text() == "2.0-SNAPSHOT"

return true
