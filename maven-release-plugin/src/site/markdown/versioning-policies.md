---
title: Versioning Policies
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

# Versioning Policies

When performing a release, the Maven Release Plugin needs to determine the next release and development version.

By default, it simply removes `-SNAPSHOT` for release version and increments the last digit of the version number for next development.

However, you can customize this behavior by using different version policies,
by settings the `projectVersionPolicyId` parameter to the desired version policy.

Maven Release Plugin comes with built-in version policies:

- `default` - increments the last digit of the version number for next development
- `OddEvenVersionPolicy` - proposes even version numbers only for releases and odd numbers for development
- `SemVerMajorDevelopment` - increases major element for next development version
- `SemVerMinorDevelopment` - increases minor element for next development version
- `SemVerPatchDevelopment` - increases patch element for next development version, similar to default policy
- `SemVerMajorRelease` - increases major element for release version
- `SemVerMinorRelease` - increases minor element for release version

The `SemVer*` policies enforce [Semantic Versioning](https://semver.org/) rules.

Examples of version policies:

| projectVersionPolicyId | project version | next release version | next development version |
|------------------------|-----------------|----------------------|--------------------------|
| default                | 1.2.3-SNAPSHOT  | 1.2.3                | 1.2.4-SNAPSHOT           |
| OddEvenVersionPolicy   | 1.0.1-SNAPSHOT  | 1.0.2                | 1.0.3-SNAPSHOT           |
| SemVerMajorDevelopment | 1.2.3-SNAPSHOT  | 1.2.3                | 2.0.0-SNAPSHOT           |
| SemVerMinorDevelopment | 1.2.3-SNAPSHOT  | 1.2.3                | 1.3.0-SNAPSHOT           |
| SemVerPatchDevelopment | 1.2.3-SNAPSHOT  | 1.2.3                | 1.2.4-SNAPSHOT           |
| SemVerMajorRelease     | 1.2.3-SNAPSHOT  | 2.0.0                | 2.0.1-SNAPSHOT           |
| SemVerMinorRelease     | 1.2.3-SNAPSHOT  | 1.3.0                | 1.3.1-SNAPSHOT           |

