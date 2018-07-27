#!/bin/bash
#/ does something ...
#/ usage: [..]

#set -euo pipefail
IFS=$'\n\t'
# <http://www.kfirlavi.com/blog/2012/11/14/defensive-bash-programming/>
# <http://redsymbol.net/articles/unofficial-bash-strict-mode/>

usage() {
      grep '^#/' <"$0" | cut -c4- # prints the #/ lines above as usage info
}
die(){
    echo >&2 "$@" ; exit 2
}

check_args(){
   : example to check args length
#  if [ ${#ARGS[@]} -lt 1 ] ; then
#        usage
#        exit 2
#    fi
}

# func(){
#    local FARGS=("$@")
#  #    echo $FUNCNAME $@
#  # set -x
#  # do something
#  # set -x
#}
setup_pam(){
	cat <<END | tee "$HOME/server/config/$JAAS_FILE_NAME"
$JAAS_MODULE_NAME {
    org.rundeck.jaas.jetty.JettyPamLoginModule required
    debug="true"
    service="login"
    supplementalRoles="admin"
    storePass="true";
};
END

	test -f "$HOME/server/config/$JAAS_FILE_NAME"
	

}

main() {
    check_args
    # use local vars
    #local i
    setup_pam
}
main
