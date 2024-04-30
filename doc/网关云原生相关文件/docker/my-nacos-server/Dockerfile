FROM nacos/nacos-server:v1.4.4
RUN mkdir -p /home/nacos/init.d
RUN sed -i 's/^server.tomcat.basedir/#&/' /home/nacos/conf/application.properties

