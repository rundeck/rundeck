
# Notes on CircleCI pileline


## Guidelines & Best Practices

- Whenever possible use the docker executor, as its the only one that can be run locally.
- `config.yml` cannot be split so keep it tidy. If a step requires multiple lines of commands, 
better create a function in dir `scripts/circleci`
- Keep all your environment variables on `scripts/circleci/setup.sh` so they're easy to find.
- Create functions within script files in `scripts/circleci/`, do your best to keep the convention
of naming the functions with the same prefix as their script file so they are easy to find 
and to avoid naming collisions.
- Avoid scripts that execute commands when imported. Just declare functions and call them from the circleci workflow.
- Avoid including scripts within scripts. Keep includes only in `setup.sh`

### Noteworthy files

- `.circleci/config.yml` - CircleCI pipeline configuration file.
- `scripts/circleci/setup.sh` - Setup script that runs before every job (see `run-build-step` at config.yml).
- `scripts/circleci/` - Scripts to be used by the pipeline.

### Docker Executor Caveats

For jobs running docker containers, certain things are not supported by circleci's docker executor. These are common limitations of
running containers within another container by the means of Docker Remote:
- Mounting local files as volumes in a container won't work. As you're already within a container trying to
do so will attempt to mount a file from the host machine (which we don't control).
- Accessing containers using the network from the parent container is not possible because of circleci security policies.
Networking between containers will only work when spawned within a common docker-compose file.


## Running locally

### Limitations
- Only jobs that use the docker executor can be run locally.
- Context and Environment from CircleCI won't be available.
- Git submodules are not well supported. So you'll need to clone the submodules manually

### Cloning submodule repositories
- If you already cloned the packaging repo with `git submodule update --init`, you'll need to delete the packaging directory.
- Leave the packaging dir empty or run `git clone git@github.com:rundeck/packaging packaging/packaging` on the repository root.
- Using git submodule update --init won't work correctly with the circle cli.

### install circleci cli

- Check https://circleci.com/docs/local-cli/

### Setup environment

- Create a file `.circleci/.env` to provide environment to the local jobs, with the following content:
```shell
# If you omit any of these, then the defaults will be used
# Docker credentials
DOCKER_USERNAME=your-docker-user
DOCKER_PASSWORD=your-docker-key
# Override repositories (optional)
DOCKER_REPO=ahormazabal/rdimg
DOCKER_CI_REPO=ahormazabal/rdci
# Twistlock credentials (for rinning twistlock scan)
TL_USER=circleci-rundeck
TL_PASS=the-twistlock-console-password
TL_CONSOLE_URL=https://the.twistlock.console.url
# GPG credentials (for packaging tasks)
RUNDECK_SIGNING_PASSWORD=gpg_signing_key_password
RUNDECK_SIGNING_KEYID=gpg_signing_key_id
# Packagecloud token (optional)
PKGCLD_WRITE_TOKEN=mytoken
# Path to gpg signatures for signing
RUNDECK_GPG_FILES_DIR=/path/to/.gnupg/dir
# Slack Access Token (can be anything but must exist)
SLACK_ACCESS_TOKEN=mytoken
# Sonatype credentials (optional)
SONATYPE_USERNAME=mytoken
SONATYPE_PASSWORD=mytoken
```

- To check all the supported variables, or add more, check the `.circleci/Makefile` file.
- The `.circleci/Makefile` is provided as a convenience to run the circleci cli commands and build its parameters
to ease the environment setup.
- Before running jobs, its recommended to run a full build first so the war files can be found by the testing jobs.

### Running jobs

- At the repository root, run one of the available make targets.
- The makefile will set the `CIRCLE_LOCAL_BUILD` environment variable to `true`, this will make
the setup script to run an additional script `scripts/circleci/local-overrides.sh` which will rewrite some functions
so the jobs can run locally.

### Available jobs

As of this writing, the following jobs are available to run locally. Execute these commands on the repository root:

#### Build rundeck 
```shell
make -f .circleci/Makefile rundeck-build
```

#### Twistlock scan 
```shell
make -f .circleci/Makefile twistlock-scan
```

#### Ansible Test
```shell
make -f .circleci/Makefile ansible-test
```

#### Packaging Test
```shell
make -f .circleci/Makefile packaging-test
```

#### Maven Publishing
```shell
make -f .circleci/Makefile maven-test
```

#### Docker Publishing
```shell
make -f .circleci/Makefile docker-test
```

