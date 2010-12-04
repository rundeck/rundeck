class UserService {

    boolean transactional = true
    
    FrameworkService frameworkService

    def findOrCreateUser(String login) {
        def User user = User.findByLogin(login)
        if(!user){
            def User u = new User(login:login)
            if(!u.save(flush:true)){
                System.err.println("unable to save user: ${u}, ${u.errors.each{g.message(error:it)}}");
            }
            user=u
        }
        return user
    }

    def boolean userHasAuthorization(String login, String auth){
        
        throw new Exception("Not supported.")
    }
}
