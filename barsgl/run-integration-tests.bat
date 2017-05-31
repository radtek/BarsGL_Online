set JAVA_HOME="C:\JDK\jdk1.8.0_131"

rem mvn clean install -pl barsgl-ejbparent/barsgl-ejbtest -P run-test-default,run-integration-tests,with-remote-tests -e -X
rem mvn clean install -pl barsgl-ejbparent/barsgl-ejbtest -P with-remote-tests -e
rem mvn clean install -DskipTests -pl barsgl-ejbparent/barsgl-ejbtest -Pwith-remote-tests,run-integration-tests -e
mvn clean install -pl barsgl-ejbparent/barsgl-ejbtest -Pwith-remote-tests,run-integration-tests -e -Dfile.encoding=UTF8

mvn site -Pwith-remote-tests,run-integration-tests -pl barsgl-ejbparent/barsgl-ejbtest -e -Dfile.encoding=UTF8