An hData hData Interoperability Testing Tool reference implemementation in Java.

Validates that a hData service endpoint implements the hData interfaces
and conforms to the specifications.

Author: Jason Mathews

Copyright 2012, The MTIRE Corporation (http://www.mitre.org/)

-----------------------------------
Bulding and running the test client
-----------------------------------

Create local.properties file to overide default project properties in build.properties
For example set proxy properties if behind a http proxy.

Run ant "test" target to run basic junit tests.

Run ant "run" target to execute test assertions
By default the Loader will use config.xml for the runtime properties.
You can override the test environment by setting system environment variable.

-------------------
System requirements
-------------------

Ant build requires maven ant tasks plugin in the ant lib directory.
See http://maven.apache.org/ant-tasks/download.html

JDK version 1.6 or above is required
Ant version 1.7.x or 1.8.x is required
