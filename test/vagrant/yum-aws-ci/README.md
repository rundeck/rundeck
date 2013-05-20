# yum-aws-ci

Runs API test scripts on amazon linux vm in EC2 with rundeck build coming from bintray CI repo

# Usage

    AWS_ACCESS_KEY=... \
    AWS_SECRET_KEY=... \
    SSH_KEY_PATH=/path/to/build-ci.pem \
        sh run-vagrant-test.sh