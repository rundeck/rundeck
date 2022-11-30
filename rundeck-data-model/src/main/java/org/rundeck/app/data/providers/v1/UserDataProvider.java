package org.rundeck.app.data.providers.v1;

import org.rundeck.app.data.model.v1.user.RdUser;
import org.rundeck.app.data.model.v1.user.dto.SaveUserResponse;
import org.rundeck.app.data.model.v1.user.dto.UserFilteredResponse;
import org.rundeck.app.data.model.v1.user.dto.UserProperties;
import org.rundeck.spi.data.DataAccessException;

import java.util.HashMap;
import java.util.List;

public interface UserDataProvider extends DataProvider {
    RdUser get(Long userid);
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
    /**
     * Retrieves an new instance of RdUser, not stored on the database
     *
     * @return User instance
     */
    RdUser buildUser();
    /**
     * Retrieves an new instance of RdUser with given login, not stored on the database
     *
     * @param login of the User, format String
     * @return User instance
     */
    RdUser buildUser(String login);
    /**
     * Updates filterPref from a user given by its login
     *
     * @param login of the User, format String
     * @param filterPref new filter pref for user, format String
     * @return SaveUserResponse, that has the updated User, if its saved correctly and its errors if not
     */
    SaveUserResponse updateFilterPref(String login, String filterPref);
    /**
     * Retrieves the User's email based on the login, opening a new session on the database
     *
     * @param login of the User, format String
     * @return User's email
     */
    String getEmailWithNewSession(String login);

    /**
     * Retrieves a map of properties for given users, indexed by each users' login
     *
     * @param usernames of the Users to search for
     * @return A map of properties from given usernames, their key is its login, and it contains firstName, lastName and email from users
     */
    HashMap<String, UserProperties> getInfoFromUsers(List<String> usernames);

}
