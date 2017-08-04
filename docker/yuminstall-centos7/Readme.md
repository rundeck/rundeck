# Yum install in docker

Centos 7:

You can use this to test install of a locally build .rpms for rundeck.

From the root checkout dir for the rundeck source repo use
`-v $PWD:/home/rundeck/rundeck` to mount the rundeck source volume.

## create image

	docker build --rm -t local/c7-systemd docker/centos7systemd
	docker build docker/yuminstall-centos7

copy image id e.g. `53c9cb76cbe1`

## run to install rpms

	docker run -it -v $PWD:/home/rundeck/rundeck $IMG bash
