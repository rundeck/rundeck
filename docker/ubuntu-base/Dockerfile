# Build remco from specific commit
##################################
FROM golang:1.22.2 as remco

# REMCO_VERSION can be a release version "v0.XX.X", commit id, tag or branch name.

# 2024-03-15: As of this writing, the latest remco version (v0.12.4) doesn't include the fixes for CVE-2023-44487
# however, the following commit (fbd1eb0) includes the fix. We will use this commit until a new release is available.
ENV REMCO_VERSION=fbd1eb066af9ca58fc452263eeceee685e2a14fd

RUN go install github.com/HeavyHorst/remco/cmd/remco@$REMCO_VERSION

# Build base container
######################
FROM rundeck/ubuntu-base-plain

# Install Remco
COPY --from=remco /go/bin/remco /usr/local/bin/remco

USER rundeck

WORKDIR /home/rundeck
