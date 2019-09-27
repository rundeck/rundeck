package rundeck.controllers

import com.dtolabs.rundeck.core.encrypter.EncryptorResponse
import com.dtolabs.rundeck.core.encrypter.PasswordUtilityEncrypter
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope

class PasswordUtilityController {
    def passwordUtilityEncrypterLoaderService
    def frameworkService

    Map<String,Object> encrypters = [:]
    PasswordUtilityController() {
        ServiceLoader<PasswordUtilityEncrypter> encrypterServices = ServiceLoader.load(
                PasswordUtilityEncrypter
        )
        encrypterServices.each { encrypters[it.name()] = it }
    }

    def index() {
        listPasswordEncryptorPlugin()
        def encrypter
        if(!flash.encrypter){
            def key = encrypters.keySet().toSorted()[0]
            encrypter = encrypters[key]
            flash.encrypter = key
        }else{
            encrypter = encrypters[flash.encrypter]
        }

        return ["encrypters":encrypters,"properties":getProperties(encrypter), "report": flash.report?flash.report:null]
    }

    def encode() {
        def encrypter = encrypters[params.encrypter]
        if(!encrypter){
            encrypter = getPasswordEncryptorPlugin(params.selectedEncrypter)
        }

        if(encrypter instanceof PasswordUtilityEncrypter){
            flash.output = encrypter.encrypt(params)
        }else{
            def plugin = passwordUtilityEncrypterLoaderService.getPasswordEncoder(params.encrypter, params)

            Map result = frameworkService.validateDescription(encrypter.description,
                    '',
                    params,
                    null,
                    PropertyScope.Instance,
                    PropertyScope.Project
            )

            if(result.valid){
                EncryptorResponse response = plugin.instance?.encrypt(result.props)
                if(response.valid){
                    flash.output = response.outputs
                }else{
                    if(response.error){
                        flash.error = response.error
                    }else{
                        flash.error = "Error running ${params.encrypter} plugin"
                    }
                }
            }else{
                //description error message properties
                flash.report = result.report
            }
        }

        flash.encrypter = params.encrypter
        redirect action: 'index'
    }

    def selectedEncrypterProps() {
        def encrypter = encrypters[params.selectedEncrypter]
        if(!encrypter){
            encrypter = getPasswordEncryptorPlugin(params.selectedEncrypter)
        }

        render(template:"renderSelectedEncrypter",model:[selectedEncrypter:getProperties(encrypter)])
    }

    Map listPasswordEncryptorPlugin(){
        Map plugins = passwordUtilityEncrypterLoaderService.getPasswordUtilityEncrypters()
        plugins.each({ plugin->
            if(encrypters.get(plugin.key)){
                encrypters.remove(plugin.key)
            }
            encrypters.put(plugin.key, plugin.value)
        })
    }

    def getPasswordEncryptorPlugin(String provider){
        listPasswordEncryptorPlugin()
        encrypters[provider]
    }

    def getProperties(def encrypter){
        def properties
        if(encrypter instanceof PasswordUtilityEncrypter){
            properties = encrypter.formProperties()
        }else{
            properties = encrypter.description?.properties
        }
        properties
    }

}