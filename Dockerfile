# Copyright 2018 <Marco Lattuada>
FROM ubuntu:16.04
MAINTAINER marco.lattuada@polimi.it

ENV DICE_USER dice
ENV DICE_PASSWORD password
ENV DICE_HOME /home/${DICE_USER}

# Install environment dependencies
RUN apt update
RUN apt -y dist-upgrade
RUN apt -y install git maven openjdk-8-jdk openssh-server sudo make gcc openmpi-bin openmpi-doc libopenmpi-dev glpk-utils

RUN useradd -ms /bin/bash ${DICE_USER}
RUN echo "${DICE_USER}:${DICE_PASSWORD}" | chpasswd
RUN echo "${DICE_USER} ALL=(root) NOPASSWD:ALL" > /etc/sudoers.d/user
RUN chmod 0440 /etc/sudoers.d/user

USER ${DICE_USER}
WORKDIR ${DICE_HOME}

ENV DICE_SHARED_REPO https://github.com/lattuada/diceH2020-space4clouds_shared.git
RUN git clone ${DICE_SHARED_REPO}
RUN cd diceH2020-space4clouds_shared && mvn initialize compile package install

ENV DICE_BACKEND_REPO http://github.com/lattuada/diceH2020-space4cloudsWS.git
RUN git clone  ${DICE_BACKEND_REPO}
RUN cd diceH2020-space4cloudsWS && mvn initialize compile package

RUN ssh-keygen -N "" -f ${DICE_HOME}/.ssh/dice_key
RUN echo "Host 127.0.0.1" >> ${DICE_HOME}/.ssh/config
RUN echo "   IdentityFile ~/.ssh/dice_key" >> ${DICE_HOME}/.ssh/config
RUN cat ${DICE_HOME}/.ssh/dice_key.pub >> ${DICE_HOME}/.ssh/authorized_keys

COPY src/main/resources/application-docker.properties ${DICE_HOME}/application-docker.properties
COPY src/main/resources/vm-instances.mv.db ${DICE_HOME}
RUN sudo chown ${DICE_USER}:${DICE_USER} ${DICE_HOME}/vm-instances.mv.db

ENV DAGSIM_REPO https://github.com/eubr-bigsea/dagSim
RUN git clone ${DAGSIM_REPO}
RUN cd dagSim && make
RUN chmod ugo+X dagSim/dagsim.sh

RUN wget https://sourceforge.net/projects/jmt/files/jmt/JMT-1.0.2/JMT-singlejar-1.0.2.jar

EXPOSE 8080

ENTRYPOINT sudo service ssh start && ssh-keyscan 127.0.0.1 >> ${DICE_HOME}/.ssh/known_hosts && java -jar diceH2020-space4cloudsWS/target/D-SPACE4Cloud-0.3.4-SNAPSHOT.jar --spring.config.location=file:/home/dice/application-docker.properties
