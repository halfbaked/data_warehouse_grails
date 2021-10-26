FROM tomcat:8-jdk8-openjdk
COPY ../build/libs/data-warehouse-grails-0.1.war /usr/local/tomcat/webapps/ROOT.war
RUN ["/usr/local/tomcat/bin/catalina.sh", "run"]