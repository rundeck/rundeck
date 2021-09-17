package org.rundeck.security

import com.dtolabs.rundeck.core.plugins.PluginBlocklist
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

class RundeckPluginBlocklist implements PluginBlocklist {

    String blackListFileName
    List<String> fileNameEntries
    List<Map<String,List<String>>> providerNameEntries

    @Override
    Boolean isPluginFilePresent(String fileName) {
        Boolean blacklisted = false
        if(!fileNameEntries){
            Yaml yaml = new Yaml(new SafeConstructor());
            Map data = yaml.load(new FileReader(blackListFileName));
            fileNameEntries = data.get("fileNameEntries")
        }

        for (String item: fileNameEntries){
            if(fileName.startsWith(item)){
                blacklisted = true
            }
        }
        return blacklisted
    }

    @Override
    Boolean isPluginProviderPresent(String service, String providerName) {
        if(!providerNameEntries){
            Yaml yaml = new Yaml(new SafeConstructor());
            Map data = yaml.load(new FileReader(blackListFileName));
            providerNameEntries = data.get("providerNameEntries")
        }

        Map<String,List<String>> blackListMap = [:]
        providerNameEntries.each {
            blackListMap.putAll(it)
        }

        if(blackListMap.containsKey(service)){
            return blackListMap.get(service).contains(providerName)
        }
        else{
            return false
        }
    }

    @Override
    Boolean isBlocklistSet() {
        return blackListFileName != null
    }
}
