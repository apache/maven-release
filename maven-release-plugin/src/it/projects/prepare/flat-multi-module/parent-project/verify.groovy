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

File buildLog = new File( basedir, "build.log" )

println "Checking logs.."

String contents = buildLog.getText()

String expected1 = "Full run would tag working copy '" + basedir.getParentFile() + "'"

boolean found = (contents.contains(expected1) && contents.contains("  with label 'parent-project-1.0'")) ||
    contents.contains("module-b with label 'module-b-1.0'") ||
    contents.contains("module-a with label 'module-a-1.0'")
assert found : "No expected tag label found in build.log"
