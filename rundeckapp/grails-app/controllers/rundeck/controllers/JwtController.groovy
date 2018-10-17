package rundeck.controllers

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTCreationException
import grails.converters.JSON
import groovy.time.TimeCategory

class JwtController {

    def index() {

    }

    def get() {
        try {
            def expireTime = new Date()
            use(TimeCategory) {
                expireTime += 1.hour
            }

            Algorithm algorithm = Algorithm.HMAC256("secret");
            String token = JWT.create()
                              .withIssuer("rundeck")
                              .withSubject(session.user)
                              .withExpiresAt(expireTime)
                              .sign(algorithm);
            render(["token":token] as JSON)
        } catch (JWTCreationException ex){
            ex.printStackTrace()
            //Invalid Signing configuration / Couldn't convert Claims.
            render(["err":"Could not generate JWT token"] as JSON)
        }
    }
}
