To launch the following integration test, make sure to have rundeck launched and listening on port 4440 (default).

export vars pointing at correct directories, and specify name of xmlstarlet executable:

    TMP_DIR=/tmp \
    RDECK_BASE=/var/lib/rundeck \
    RDECK_ETC=/etc/rundeck \
    RDECK_PROJECTS=/var/rundeck/projects \
    XMLSTARLET=xmlstarlet \
    sh src/test.sh http://localhost:4440 admin admin


