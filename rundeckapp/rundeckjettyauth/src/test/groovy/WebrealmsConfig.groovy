webrealms{

    loginconfig{
        authmethod= 'FORM'
        realmname= 'rundeckrealm'
        loginpage='/user/login'
        errorpage='/user/error'
    }

    securityconstraint{
        Login.urlpattern='/user/login'
        LoginError.urlpattern='/user/error'
        Static.urlpattern='/static/*'
        all{
            urlpattern='/*'
            authconstraint{
                rolename="*"
            }
        }
    }
    securityroles{
        role{
            name='user'
        }
    }

    server{
        addrealm{
            classname="org.eclipse.jetty.jaas.JAASLoginService"
            name="rundeckrealm"
            LoginModuleName="rundecklogin"
        }
    }
}

