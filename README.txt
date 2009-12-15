README for FedICT eID Applet Project
====================================

=== 1. Introduction

This project contains the source code tree of the FedICT eID Applet.
The source code is hosted at: http://code.google.com/p/eid-applet/


=== 2. Requirements

The following is required for compiling the eID Applet software:
* Sun Java 1.6.0_16
* Apache Maven 2.0.10 or 2.2.1

When sitting behind an HTTP proxy and you experience weird download behaviour,
you might want to consider using Apache Maven 2.0.10.


=== 3. Build

The project can be build via:
	mvn clean install

This will also build a test web application EAR artifact named:
	eid-applet-test-deploy

Deploy the test web application to a local running JBoss AS 5.0.x via:
	cd eid-applet-test/eid-applet-test-deploy
	mvn jboss:undeploy jboss:deploy

We provide a JBoss AS 5.0.x package artifact named:
	eid-applet-jboss-as

Missing dependencies can be added to your local Maven repository via:
        mvn install:install-file -Dfile=Downloads/jboss-5.0.1.GA-jdk6.zip \
	-DgroupId=org.jboss -DartifactId=jboss-as-distribution \
	-Dversion=5.0.1.GA -Dpackaging=zip -DgeneratePom=true -Dclassifier=jdk6

During the build process a token is required to sign the applet JAR.
By default the Maven build will use a software token to sign the applet JAR.
One can configure the usage of an eToken via the following Maven property:
	-Dtoken=etoken
The eToken configuration is located in pom.xml under the eid-applet-package 
artifact.

You can speed up the development build cycle by skipping the unit tests via:
	mvn -Dmaven.test.skip=true clean install


=== 4. SDK Release

An SDK build can be performed via:
	mvn -Dhttp.proxyHost=proxy.yourict.net -Dhttp.proxyPort=8080 -Denv=sdk
-Dtoken=etoken clean install

The final artifact is located under:
	eid-applet-sdk/target/

An SDK release build should use the production eToken containing the official
FedICT code signing certificate.


=== 5. Eclipse IDE

The Eclipse project files can be created via:
	mvn -Denv=sdk eclipse:eclipse

Afterwards simply import the projects in Eclipse via:
	File -> Import... -> General:Existing Projects into Workspace

First time you use an Eclipse workspace you might need to add the maven 
repository location. Do this via:
    mvn eclipse:add-maven-repo -Declipse.workspace=<location of your workspace>


=== 6. NetBeans IDE

As of NetBeans version 6.7 this free IDE from Sun has native Maven 2 support.


=== 7. License

The source code of the eID Applet Project is licensed under GNU LGPL v3.0.
Part of the source code (OOXML signature code) is dual-licensed under both 
the GNU LGPL v3.0 and the Apache License v2.0. Only the files with a header
containing both the GNU LGPL v3.0 and Apache License v2.0 license texts are
dual-licensed. The dual-licensing was offered in response to a request from
the Apache POI open source project. All other source code files remain under
control of the GNU LGPL v3.0 license unless otherwise decided in the future
by _ALL_ eID Applet Project copyright holders.
The license conditions can be found in the file: LICENSE.txt

