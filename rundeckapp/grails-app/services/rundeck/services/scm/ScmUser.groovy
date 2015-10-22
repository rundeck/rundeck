package rundeck.services.scm

import com.dtolabs.rundeck.plugins.scm.ScmUserInfo

/**
 * Created by greg on 10/2/15.
 */
class ScmUser implements ScmUserInfo {
    String email
    String fullName
    String firstName
    String lastName
    String userName
}
