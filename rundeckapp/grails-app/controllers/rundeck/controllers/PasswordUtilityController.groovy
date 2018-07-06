package rundeck.controllers

import com.dtolabs.rundeck.core.encrypter.PasswordUtilityEncrypter

class PasswordUtilityController {
    Map<String,PasswordUtilityEncrypter> encrypters = [:]
    PasswordUtilityController() {
        ServiceLoader<PasswordUtilityEncrypter> encrypterServices = ServiceLoader.load(
                PasswordUtilityEncrypter
        )
        encrypterServices.each { encrypters[it.name()] = it }
    }

    def index() {
        if(!flash.encrypter) flash.encrypter = encrypters.keySet().toSorted()[0]
        return ["encrypters":encrypters]
    }

    def encode() {
        PasswordUtilityEncrypter encrypter = encrypters[params.encrypter]
        flash.output = encrypter.encrypt(params)
        flash.encrypter = params.encrypter
        redirect action: 'index'
    }

    def selectedEncrypterProps() {
        render(template:"renderSelectedEncrypter",model:[selectedEncrypter: encrypters[params.selectedEncrypter]])
    }

}
