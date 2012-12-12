Deploying web site
-------------------
You can use the script deploySite.sh
Without any profile the site will be deploy to http://maven.apache.org/maven-release-archives/maven-release-${project.version}
sh ./deploySite.sh -Preporting

To deploy main version http://maven.apache.org/maven-release
Use
sh ./deploySite.sh -Preporting -P site-release

Note you can add arguments to the script to pass your svn credentials:
-Dusername=
-Dpassword=

