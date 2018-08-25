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

File buildLog = new File( basedir, 'build.log' )
assert buildLog.exists()

def addArgsExpr = /\Q[DEBUG] Additional arguments: \E(?:-Dhttps.protocols=TLSv1.2 )?-P(.+)\Q-DperformRelease=true -f pom.xml\E/
def matcher = ( buildLog.getText() =~ addArgsExpr )

// M2:  [DEBUG] Additional arguments: -P custom-release -DperformRelease=true -f pom.xml
// M3:  [DEBUG] Additional arguments: -P it-repo,it-repo,custom-release -DperformRelease=true -f pom.xml 

assert matcher.find()
assert matcher.getCount() == 1

assert matcher[0][1].contains( "custom-release" )
