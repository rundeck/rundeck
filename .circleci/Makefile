-include .circleci/.env
export

# Directory with gpg signatures.
RUNDECK_GPG_FILES_DIR?=.gnupg

# Circle ENV Simulation
DOCKER_USERNAME?=
DOCKER_PASSWORD?=
TL_USER?=
TL_PASS?=
TL_CONSOLE_URL?=
DOCKER_REPO?=
DOCKER_CI_REPO?=
RUNDECK_SIGNING_PASSWORD?=
PKGCLD_WRITE_TOKEN?=
SLACK_ACCESS_TOKEN?=
SONATYPE_USERNAME?=
SONATYPE_PASSWORD?=

rundeck-build: create-caches
	circleci local execute Build \
		-e CIRCLE_LOCAL_BUILD=true \
		-e DOCKER_USERNAME=$(DOCKER_USERNAME) \
		-e DOCKER_PASSWORD=$(DOCKER_PASSWORD) \
		-e DOCKER_REPO=$(DOCKER_REPO) \
		-e DOCKER_CI_REPO=$(DOCKER_CI_REPO) \
		-e SLACK_ACCESS_TOKEN=$(SLACK_ACCESS_TOKEN) \
		-v circleci-local-gradle-cache:/home/circleci/.gradle:rw

twistlock-scan:
	circleci local execute "Twistlock Scan Preview" \
		-e CIRCLE_LOCAL_BUILD=true \
		-e SLACK_ACCESS_TOKEN=$(SLACK_ACCESS_TOKEN) \
		-e DOCKER_USERNAME=$(DOCKER_USERNAME) \
		-e DOCKER_PASSWORD=$(DOCKER_PASSWORD) \
		-e DOCKER_REPO=$(DOCKER_REPO) \
		-e DOCKER_CI_REPO=$(DOCKER_CI_REPO) \
		-e TL_USER=$(TL_USER) \
		-e TL_PASS=$(TL_PASS) \
		-e TL_CONSOLE_URL=$(TL_CONSOLE_URL)


ansible-test:
	circleci local execute "Ansible Test" \
		-e CIRCLE_LOCAL_BUILD=true \
		-e SLACK_ACCESS_TOKEN=$(SLACK_ACCESS_TOKEN) \
		-e DOCKER_USERNAME=$(DOCKER_USERNAME) \
		-e DOCKER_PASSWORD=$(DOCKER_PASSWORD) \
		-e DOCKER_REPO=$(DOCKER_REPO) \
		-e DOCKER_CI_REPO=$(DOCKER_CI_REPO) \
 		-v $(shell pwd)/rundeckapp/build/libs:/home/circleci/rlibs

packaging-test: create-caches
	circleci local execute "Test Packaging" \
		-e CIRCLE_LOCAL_BUILD=true \
		-e SLACK_ACCESS_TOKEN=$(SLACK_ACCESS_TOKEN) \
		-e DOCKER_USERNAME=$(DOCKER_USERNAME) \
		-e DOCKER_PASSWORD=$(DOCKER_PASSWORD) \
		-e DOCKER_REPO=$(DOCKER_REPO) \
		-e DOCKER_CI_REPO=$(DOCKER_CI_REPO) \
		-e RUNDECK_SIGNING_PASSWORD=$(RUNDECK_SIGNING_PASSWORD) \
		-e RUNDECK_SIGNING_KEYID=$(RUNDECK_SIGNING_KEYID) \
		-e PKGCLD_WRITE_TOKEN=$(PKGCLD_WRITE_TOKEN) \
		-e SONATYPE_USERNAME=$(SONATYPE_USERNAME) \
		-e SONATYPE_PASSWORD=$(SONATYPE_PASSWORD) \
 		-v $(shell pwd)/rundeckapp/build/libs:/home/circleci/rlibs \
 		-v $(RUNDECK_GPG_FILES_DIR):/home/circleci/ciresources \
		-v circleci-local-gradle-cache:/home/circleci/.gradle:rw

maven-test: create-caches
	circleci local execute "Maven Publish" \
		-e CIRCLE_LOCAL_BUILD=true \
		-e SLACK_ACCESS_TOKEN=$(SLACK_ACCESS_TOKEN) \
		-e DOCKER_USERNAME=$(DOCKER_USERNAME) \
		-e DOCKER_PASSWORD=$(DOCKER_PASSWORD) \
		-e DOCKER_REPO=$(DOCKER_REPO) \
		-e DOCKER_CI_REPO=$(DOCKER_CI_REPO) \
		-e RUNDECK_SIGNING_PASSWORD=$(RUNDECK_SIGNING_PASSWORD) \
		-e RUNDECK_SIGNING_KEYID=$(RUNDECK_SIGNING_KEYID) \
		-e SONATYPE_USERNAME=$(SONATYPE_USERNAME) \
		-e SONATYPE_PASSWORD=$(SONATYPE_PASSWORD) \
		-e DRY_RUN=true \
 		-v $(shell pwd)/rundeckapp/build/libs:/home/circleci/rlibs \
 		-v $(RUNDECK_GPG_FILES_DIR):/home/circleci/ciresources \
		-v circleci-local-gradle-cache:/home/circleci/.gradle:rw

docker-publish: create-caches
	circleci local execute "Docker Publish" \
		-e CIRCLE_LOCAL_BUILD=true \
		-e SLACK_ACCESS_TOKEN=$(SLACK_ACCESS_TOKEN) \
		-e DOCKER_USERNAME=$(DOCKER_USERNAME) \
		-e DOCKER_PASSWORD=$(DOCKER_PASSWORD) \
		-e DOCKER_REPO=$(DOCKER_REPO) \
		-e DOCKER_CI_REPO=$(DOCKER_CI_REPO) \
		-e RUNDECK_SIGNING_PASSWORD=$(RUNDECK_SIGNING_PASSWORD) \
		-e RUNDECK_SIGNING_KEYID=$(RUNDECK_SIGNING_KEYID) \
		-e SONATYPE_USERNAME=$(SONATYPE_USERNAME) \
		-e SONATYPE_PASSWORD=$(SONATYPE_PASSWORD) \
		-e DRY_RUN=true \
 		-v $(shell pwd)/rundeckapp/build/libs:/home/circleci/rlibs \
 		-v $(RUNDECK_GPG_FILES_DIR):/home/circleci/ciresources \
		-v circleci-local-gradle-cache:/home/circleci/.gradle:rw


clean-caches:
	docker volume rm -f circleci-local-gradle-cache

create-caches:
	docker volume create circleci-local-gradle-cache
	docker run \
		-v circleci-local-gradle-cache:/home/circleci/.gradle:rw \
		--rm -it cimg/base:2023.09-22.04 \
		/bin/bash -c "sudo chown circleci.circleci /home/circleci/.gradle"

clean: clean-caches
