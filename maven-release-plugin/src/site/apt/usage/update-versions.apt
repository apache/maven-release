  ------
  Update POM Versions
  ------
  Paul Gier
  ------
  2010-01-03
  ------

~~ Licensed to the Apache Software Foundation (ASF) under one
~~ or more contributor license agreements.  See the NOTICE file
~~ distributed with this work for additional information
~~ regarding copyright ownership.  The ASF licenses this file
~~ to you under the Apache License, Version 2.0 (the
~~ "License"); you may not use this file except in compliance
~~ with the License.  You may obtain a copy of the License at
~~
~~   http://www.apache.org/licenses/LICENSE-2.0
~~
~~ Unless required by applicable law or agreed to in writing,
~~ software distributed under the License is distributed on an
~~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~~ KIND, either express or implied.  See the License for the
~~ specific language governing permissions and limitations
~~ under the License.

Updating POM Versions

  In some situations you may want an easy way to update the version numbers in each POM of a multi-module
  project.  The <<<update-versions>>> goal is designed to accomplish this.

  To update the version numbers in your POMs, run:

-------
mvn release:update-versions
-------

  You will be prompted for the version number for each module of the project.  If you prefer that each module
  version be the same as the parent POM, you can use the option <<<autoVersionSubmodules>>>.

-------
mvn release:update-versions -DautoVersionSubmodules=true
-------

  In this case you will only be prompted for the desired version number once.


* Specify versions on the command line.

  You may want to specify the version(s) to use on the command line.  This can be useful for example if you are running
  the update in non-interactive mode.  The <<<update-versions>>> goal can use the same properties used by the <<<prepare>>> goal
  for specifying the versions to be used.


-----------
mvn --batch-mode release:update-versions -DdevelopmentVersion=1.2.0-SNAPSHOT
-----------

  In this example, the local POM will be set to the version 1.2.0-SNAPSHOT
