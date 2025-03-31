#!/bin/bash

JWT_SECRET=$(openssl rand -base64 32)
OAUTH2_SECRET=$(openssl rand -base64 32)
INTERNAL_TOKEN=$(openssl rand -base64 32)

sed -i "s|JWT_SECRET = .*|JWT_SECRET = $JWT_SECRET|g" gitea/gitea-cfg.ini
sed -i "s|INTERNAL_TOKEN = .*|INTERNAL_TOKEN = $INTERNAL_TOKEN|g" gitea/gitea-cfg.ini
sed -i "s|LFS_JWT_SECRET = .*|LFS_JWT_SECRET = $OAUTH2_SECRET|g" gitea/gitea-cfg.ini

echo "JWT_SECRET: $JWT_SECRET"
echo "INTERNAL_TOKEN: $INTERNAL_TOKEN"
echo "LFS_JWT_SECRET: $OAUTH2_SECRET"
