#
# BASH shell tab completion for RUNDECK's "dispatch" and "rd-project" commands
#
# Source this file from your login shell. 
#
# @author: <a href="mailto:alex@dtosolutions.com">alex@dtosolutions.com</a>
# @version: $Revision: 1931 $

[ -n "${RDECK_BASE}" -a -d "${RDECK_BASE}" ] && export _rdeck_projectdir=$(awk -F= '/framework.projects.dir/ {print $2}' \
	${RDECK_BASE}/etc/framework.properties)

# list all the child directory names in specified parent 
_listdirnames()
{
    local dirs dir
    [ -d "$1" ] && dir=$1 || { return 1 ; }
    for d in $(echo ${dir}/*) 
    do 
	[ -d "$d" ] && dirs="$dirs $(basename $d)"
    done
    echo $dirs
}

# check if item is in the list
_lexists()
{
    local  item="$1" list="$2"
    for e in $(eval echo $list)
    do
	[ "${item}" = "${e}" ] && return 0
    done
    return 1	
}
# remove the item from the list
_lremove() 
{
    local list item retlist
    list=$2 item=$1 retlist=""
    for e in $(eval echo $list)
    do
	    [ "$e" = "$item" ] || {
		retlist="$retlist $e"
	    }
    done
    echo $retlist
}
# subtract the items in list2 from list1
_lsubtract() 
{
    local list1="$1" list2="$2" retlist=""
    for item in $(eval echo $list1)
    do
	_lexists $item "$list2" || {
	   retlist="$retlist $item"
	}
    done
    echo $retlist    
}



#
# program completion for the 'rd-project' command
#
_runproject()
{
    [ -z "${RDECK_BASE}" -o ! \( -d "${RDECK_BASE}" \) ] && {
	return 0 ; 
    }
    local cur prev context comp_line opts_project opts_type opts_object opts_command OPT
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"
    comp_line=$COMP_LINE
    context=()
    eval set $COMP_LINE
    shift; # shift once to drop the "rd-project" from the argline
    while [ "$#" -gt 0 ]; do
	OPT="$1"
	case "$OPT" in
            -p)	[ -n "$2" ] && { context[0]="$2" ; shift ; } 
		;;
            -a)	[ -n "$2" ] && { context[1]="$2" ; shift ; } 
		;;
	    *)	break
		;;
	esac
	shift
    done
    [ ${#context[@]} -gt 0 ] && {
	[ -d ${_rdeck_projectdir}/${context[0]} ] && opts_project=${context[0]}
    }
    [ ${#context[@]} -gt 1 ] && {
	[ ${context[1]} = "create" -o ${context[1]} = "install" ] && opts_action=${context[1]}
    }

    # If just the "rd-project" command was typed, offer the first clopt
    [ -z "${opts_project}" -a ${prev} != "-p" ] && {
	COMPREPLY=( "-p" )
	return 0
    }
    [ -n "${opts_action}" ] && {
	# nothing else to offer
	return 0
    }
    # offer the action names
    [ -n "${opts_project}"  -a "$prev" = "-a" ] && {
	COMPREPLY=( $(compgen -W "create install purge remove" -- ${cur}) )
	return 0
    }
    [ -n "${opts_project}"  -a "$prev" != "-a" ] && {
	COMPREPLY=( $(compgen -W "-a" -- ${cur}) )
	return 0
    }
    [ ${prev} = "-p" ] && {
	COMPREPLY=( $(compgen -W "$(_listdirnames $_rdeck_projectdir)" -- ${cur}) )
	return 0
    }

}
# register the completion function
complete -F _runproject rd-project

#
# program completion for the 'dispatch' command
#
_dispatch_complete()
{
    [ -z "${RDECK_BASE}" -o ! \( -d "${RDECK_BASE}" \) ] && {
	return 0 ; 
    }
    local cur prev context comp_line opts_project opts_script opts_command OPT
    COMPREPLY=()
    cur="${COMP_WORDS[COMP_CWORD]}"
    prev="${COMP_WORDS[COMP_CWORD-1]}"
    comp_line=$COMP_LINE
    context=()
    eval set $COMP_LINE
    shift; # shift once to drop the "rd-project" from the argline
    while [ "$#" -gt 0 ]; do
	OPT="$1"
	case "$OPT" in
            -p)	[ -n "$2" ] && { context[0]="$2" ; shift ; } 
		;;
	    -s) [ -n "$2" ] && { opts_script="$2" ; shift ; }
		;;
	    --) [ -n "$2" ] && { opts_command="$2" ; shift ; }
		;;
	    *)	break
		;;
	esac
	shift
    done
    [ ${#context[@]} -gt 0 ] && {
	[ -d ${_rdeck_projectdir}/${context[0]} ] && opts_project=${context[0]}
    }

    # If just the "dispatch" command was typed, offer the first clopt
    [ -z "${opts_project}" -a ${prev} != "-p" ] && {
	COMPREPLY=( "-p" )
	return 0
    }
    [ ${prev} = "-p" ] && {
	COMPREPLY=( $(compgen -W "$(_listdirnames $_rdeck_projectdir)" -- ${cur}) )
	return 0
    }

    # Depot context but no execution flag yet. Offer it.
    [ -n "$opts_project" -a -z "$opts_script" -a -z "$opts_command" -a "$prev" != "-s" -a "$prev" != "--" ] && {
        COMPREPLY=( $(compgen -W "-s --" -- ${cur}) )
        return 0
    }

    [ ${prev} = "-s" ] && {
        # use filename completion in this case
        COMPREPLY=( $(compgen -o filenames -A file -- ${cur}) )
    }
}
# register the completion function
complete -F _dispatch_complete dispatch

