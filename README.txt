A RHEx Interoperability Testing Tool reference implemementation in Java.

Validates that a hData service endpoint implements the hData interfaces
and conforms to the specifications.

Currently implements a number of test assertions related to the OMG 
hData RESTful Transport and HL7 hData Record Format specifications.

Author: Jason Mathews

Copyright 2012, The MTIRE Corporation (http://www.mitre.org/)

-----------------------------------
Bulding and running the test client
-----------------------------------

Create local.properties file to overide default project properties in build.properties file.
For example set proxy properties if behind a http proxy.

You may have to create a local 'settings.xml' file for Maven to work.
See http://maven.apache.org/settings.html

Run ant test target to run basic junit tests.
> ant test

Alternatively can use gradle to build and run tests:
> gradle test

If all tests pass then test tool compiles correctly and ready to configure
the tool to test your hData-compliant service.

Before running the testing tool you must edit/create a config.xml file
that defines the inputs and context for the tests starting with a baseURL
for a target patient record URL. This record must already have document sections
and documents for some tests to work.

Run ant "run" target to execute test assertions
By default the Loader will use the config.xml file for the runtime properties.
You can override the test environment by setting the variable for
'config.xml.file' in the local.properties file.

-------------------
System requirements
-------------------

Ant build requires maven ant tasks plugin in the ant lib directory.
See http://maven.apache.org/ant-tasks/download.html

JDK version 1.6 or above is required
Ant version 1.7.x or 1.8.x is required
