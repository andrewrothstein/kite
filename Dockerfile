FROM ubuntu:trusty
MAINTAINER Andrew Rothstein <andrew.rothstein@gmail.com>

ENV DEBIAN_FRONTEND noninteractive
RUN apt-get update
RUn apt-get -yq install software-properties-common python-software-properties
RUN add-apt-repository ppa:webupd8team/java -y
RUN apt-get update
RUN apt-get -yq install maven
RUN echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections
RUN echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections
RUN apt-get -yq install oracle-java7-installer
RUN apt-get -yq install oracle-java7-set-default

RUN apt-get -yq install git
RUN git clone https://github.com/andrewrothstein/kite.git
WORKDIR kite
RUN git checkout release-1.1.0
RUN mvn install -Dhadoop.profile=2