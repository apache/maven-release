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

def pom = new XmlSlurper().parse( new File( basedir, 'pom.xml.tag' ) )
assert pom.scm.tag.text() == 'gh-1420-1.0'
assert pom.version.text() == '1.0'

def pomNext = new XmlSlurper().parse( new File( basedir, 'pom.xml.next' ) )
assert pomNext.scm.tag.text() == 'HEAD'
assert pomNext.version.text() == '1.1-SNAPSHOT'

def pom1 = new XmlSlurper().parse( new File( basedir, 'module1/pom.xml.tag' ) )
assert pom1.scm.size() == 0
assert pom1.parent.version.text() == '1.0'

def pom1Next = new XmlSlurper().parse( new File( basedir, 'module1/pom.xml.next' ) )
assert pom1Next.scm.size() == 0
assert pom1Next.parent.version.text() == '1.1-SNAPSHOT'

def pom2 = new XmlSlurper().parse( new File( basedir, 'module1/module2/pom.xml.tag' ) )
assert pom2.scm.size() == 0
assert pom2.parent.version.text() == '1.0'

def pom2Next = new XmlSlurper().parse( new File( basedir, 'module1/module2/pom.xml.next' ) )
assert pom2Next.scm.size() == 0
assert pom2Next.parent.version.text() == '1.1-SNAPSHOT'
