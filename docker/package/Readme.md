# build with docker

You can use this to build rundeck with docker.

Modified from: https://hub.docker.com/r/bwits/rundeck-build/

Instead of cloning rundeck repo, it this Dockerfile defines a volume at
`/home/rundeck/rundeck` to use for the rundeck repo source.

From the root checkout dir for the rundeck source repo use
`-v $PWD:/home/rundeck/rundeck` to mount the rundeck source volume.

## create image

	docker build docker/package

copy image id e.g. `53c9cb76cbe1`

## make everything:

	docker run -it -v $PWD:/home/rundeck/rundeck [imageid] bash jenkins-build.sh

## or, build jar/war separately:

	docker run -it -v $PWD:/home/rundeck/rundeck [imageid] ./gradlew build

## build rpm/deb separately:

e.g. if you have alredy done `./gradlew build` locally, and just want to build rpm/deb.

	docker run -it -v $PWD:/home/rundeck/rundeck [imageid] make rpm deb