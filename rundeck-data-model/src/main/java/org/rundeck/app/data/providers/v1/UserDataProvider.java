package org.rundeck.app.data.providers.v1;

import org.rundeck.app.data.model.v1.user.RdUser;
import org.rundeck.app.data.model.v1.user.dto.SaveUserResponse;
import org.rundeck.app.data.model.v1.user.dto.UserFilteredResponse;
import org.rundeck.spi.data.DataAccessException;

import java.util.HashMap;
import java.util.List;

public interface UserDataProvider extends DataProvider {
    /**
     * Retrieves a User based on the login, otherwise create a User with that login.
     *
     * @param login of the User, format String
     * @return User if found, otherwise create user
     */
    RdUser findOrCreateUser(String login) throws DataAccessException;

    RdUser registerLogin(String login, String sessionId) throws DataAccessException;

    RdUser registerLogout(String login) throws DataAccessException;

    SaveUserResponse updateUserProfile(String username, String lastName, String firstName, String email) throws DataAccessException;

    SaveUserResponse createUserWithProfile(String username, String lastName, String firstName, String email) throws DataAccessException;

    String getLoginStatus(RdUser user);

    UserFilteredResponse findWithFilters(boolean loggedInOnly, HashMap<String, String> filters, Integer offset, Integer max);

    boolean validateUserExists(String username);

    List<RdUser> listAllOrderByLogin();
    List<RdUser> findAll();
    RdUser findByLogin(String login);
    RdUser buildUser();
    RdUser buildUser(String login);
    String getEmailWithNewSession(String login);

}
