package rundeck.services

import grails.events.annotation.Subscriber

import javax.servlet.http.HttpSession
import javax.servlet.http.HttpSessionEvent
import javax.servlet.http.HttpSessionListener
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

// This class probably needs to be moved to rundeckpro-license plugin
// It also needs a method to invalidate all user sessions for a given name  that should be called when a user is deleted
class HttpSessionListenerService implements HttpSessionListener{

    private static final ConcurrentMap<String, List<HttpSession>> sessions = new ConcurrentHashMap<>()

    @Override
    void sessionCreated(HttpSessionEvent se) {
    }

    @Override
    //We also need to check if this method is also called when a session is abandoned
    void sessionDestroyed(HttpSessionEvent se) {
        //Here we can remove the session from the map
        if(se.session.user){
            HttpSession sessionToRemove = sessions.get(se.session.user).find { (it.getId() == se.getSession().getId()) }
            sessions.get(se.session.user).remove(sessionToRemove)
            // We also remove the user from the map if there are no sessions left
            if (sessions.get(se.session.user).isEmpty()) {
                sessions.remove(se.session.user)
            }
        }
    }

    /**
     * Get the session for a user
     * @param user the username
     * @return the HttpSession or null if not found
     */
    @Subscriber("addSession")
    void addSession(String user, HttpSession session) {
        if (user && session) {
            if (!sessions.containsKey(user)) {
                sessions.put(user, new ArrayList<>())
            }else {
                sessions.get(user).add(session)
            }
        }
    }
}
