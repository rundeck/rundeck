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
setup_jaas(){
	cat <<END | tee "$HOME/server/config/$JAAS_FILE_NAME"
$JAAS_MODULE_NAME {
    com.dtolabs.rundeck.jetty.jaas.JettyCachingLdapLoginModule required
    debug="true"
    contextFactory="com.sun.jndi.ldap.LdapCtxFactory"
    providerUrl="$JAAS_LDAP_URL"
    bindDn="$JAAS_LDAP_ADMIN_DN"
    bindPassword="$JAAS_LDAP_PASS"
    authenticationMethod="simple"
    forceBindingLogin="$JAAS_LDAP_BINDING_LOGIN"
    forceBindingLoginUseRootContextForRoles="true"
    userBaseDn="ou=users,$JAAS_LDAP_BASE_DN"
    userRdnAttribute="cn"
    userIdAttribute="cn"
    userPasswordAttribute="userPassword"
    userObjectClass="person"
    roleBaseDn="ou=roles,$JAAS_LDAP_BASE_DN"
    roleNameAttribute="cn"
    roleMemberAttribute="uniqueMember"
    roleObjectClass="groupOfUniqueNames"
    
    cacheDurationMillis="600000"
    reportStatistics="true";
};
END

	test -f "$HOME/server/config/$JAAS_FILE_NAME"
	

}

main() {
    check_args
    # use local vars
    #local i
    setup_jaas
}
main
