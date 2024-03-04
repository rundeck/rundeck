#!/bin/sh
ATTEMPT=0
MAX_ATTEMPT=20

while true; do
    sleep 1
    ATTEMPT=$(($ATTEMPT + 1))
    STATUS_CODE=$(curl -LI localhost:3000 -o /dev/null -w '%{http_code}\n' -s)
    if [ $STATUS_CODE = "200" ]; then
        su -c "gitea admin user create --admin --username ${GITEA_ADMIN_USER} --password ${GITEA_ADMIN_PASSWORD} --email your@email.org --must-change-password=false" git
        exit 0
    elif [ $ATTEMPT = $MAX_ATTEMPT ]; then
        echo "Maximum attempts (${MAX_ATTEMPT}) has been reached with gitea server response: ${STATUS_CODE}"
        exit 1
    fi;
done & /usr/bin/entrypoint
