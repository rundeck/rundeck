package rundeck.controllers

class TokenVerifierController {

    def index() { }

    void refreshTokens() {
        g.refreshFormTokensHeader()
    }
}
