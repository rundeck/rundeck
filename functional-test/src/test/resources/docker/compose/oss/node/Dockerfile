FROM rundeck/node-demo:latest@sha256:587fe613b9a4d70c0d417918e092400399597305e5db450fb8844ea2001e93cc

#Add commands here to customize node-demo image.
#Adjust docker-compose to build from this file by replacing:
# image: rundeck/node-demo
# with
# build:
#   context: node-demo


COPY init.sh  /usr/local/sbin/init
RUN chmod +x /usr/local/sbin/init

CMD /usr/local/sbin/init && /usr/local/sbin/rdeck_ssh_start
