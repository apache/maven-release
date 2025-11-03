---
title: Introduction
author: 
  - Carlos Sanchez _carlos@apache.org_
  - Brett Porter _brett@apache.org_
  - John Tolentino _jtolentino@apache.org_
date: 2010-01-03
---

<!-- Licensed to the Apache Software Foundation (ASF) under one-->
<!-- or more contributor license agreements.  See the NOTICE file-->
<!-- distributed with this work for additional information-->
<!-- regarding copyright ownership.  The ASF licenses this file-->
<!-- to you under the Apache License, Version 2.0 (the-->
<!-- "License"); you may not use this file except in compliance-->
<!-- with the License.  You may obtain a copy of the License at-->
<!---->
<!--   http://www.apache.org/licenses/LICENSE-2.0-->
<!---->
<!-- Unless required by applicable law or agreed to in writing,-->
<!-- software distributed under the License is distributed on an-->
<!-- "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY-->
<!-- KIND, either express or implied.  See the License for the-->
<!-- specific language governing permissions and limitations-->
<!-- under the License.-->
# Maven Release Plugin

This plugin is used to release a project with Maven, saving a lot of repetitive, manual work. Releasing a project is made in two steps: prepare and perform.

**Note: Maven 3 users are encouraged to use at least [Maven-3\.0\.4](/download.html) due to some settings related issues.** 

## Goals Overview

- [release:clean](./clean-mojo.html) Clean up after a release preparation.
- [release:prepare](./prepare-mojo.html) Prepare for a release in SCM.
- [release:prepare-with-pom](./prepare-with-pom-mojo.html) Prepare for a release in SCM, and generate release POMs that record the fully resolved projects used.
- [release:rollback](./rollback-mojo.html) Rollback a previous release.
- [release:perform](./perform-mojo.html) Perform a release from SCM.
- [release:stage](./stage-mojo.html) Perform a release from SCM into a staging folder/repository.
- [release:branch](./branch-mojo.html) Create a branch of the current project with all versions updated.
- [release:update-versions](./update-versions-mojo.html) Update the versions in the POM\(s\).

## Usage

General instructions on how to use the Release Plugin can be found on the [usage page](./usage.html), with one additional page per goal. Some more specific use cases are described in the examples given below.

In case you still have questions regarding the plugin&apos;s usage, please have a look at the [FAQ](./faq.html) and feel free to contact the [user mailing list](./mailing-lists.html). The posts to the mailing list are archived and could already contain the answer to your question as part of an older thread. Hence, it is also worth browsing/searching the [mail archive](./mailing-lists.html).

If you feel like the plugin is missing a feature or has a defect, you can fill a feature request or bug report in our [issue tracker](./issue-management.html). When creating a new issue, please provide a comprehensive description of your concern. Especially for fixing bugs it is crucial that the developers can reproduce your problem. For this reason, entire debug logs, POMs or most preferably little demo projects attached to the issue are very much appreciated. Of course, patches are welcome, too. Contributors can check out the project from our [source repository](./scm.html) and will find supplementary information in the [guide to helping with Maven](/guides/development/guide-helping.html).

## Examples

To provide you with better understanding on some usages of the Maven Release Plugin, you can take a look into the following examples:

- [Run Additional Goals Before Commit](./examples/run-goals-before-commit.html)
- [Lock Files During Release](./examples/lock-files.html)
