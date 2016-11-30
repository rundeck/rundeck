# Debian install in docker

You can use this to test install of a locally build .deb for rundeck.

From the root checkout dir for the rundeck source repo use
`-v $PWD:/home/rundeck/rundeck` to mount the rundeck source volume.

## create image

	docker build docker/debinstall

copy image id e.g. `53c9cb76cbe1`

## run to install debian

	docker run -it -v $PWD:/home/rundeck/rundeck [imageid] bash