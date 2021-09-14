package org.rundeck.security

import com.dtolabs.rundeck.core.plugins.PluginBlocklist
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

class RundeckPluginBlocklist implements PluginBlocklist {

    String blackListFileName

    @Override
    Boolean isPluginFilePresent(String fileName) {
        Yaml yaml = new Yaml(new SafeConstructor());
        Map data = yaml.load(new FileReader(blackListFileName));
        Boolean blacklisted = false
        List<String> list = data.get("fileNameEntries")
        List<String> blackListFileNamesList = []
        list.each {
            blackListFileNamesList.add(it)
        }
        for (String item: blackListFileNamesList){
            if(fileName.startsWith(item)){
                blacklisted = true
            }
        }
        return blacklisted
    }

    @Override
    Boolean isPluginProviderPresent(String service, String providerName) {
        Yaml yaml = new Yaml(new SafeConstructor());
        Map data = yaml.load(new FileReader(blackListFileName));
        List<Map> list = data.get("providerNameEntries")
        Map<String,List<String>> blackListMap = [:]
        list.each {
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
