package org.rundeck.security

import com.dtolabs.rundeck.core.plugins.PluginBlocklist
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

class RundeckPluginBlocklist implements PluginBlocklist {

    String blackListFileName
    Yaml blacklistYaml

    @Override
    List<String> getBlockListPluginFileName() {
        Yaml yaml = new Yaml(new SafeConstructor());
        Map data = yaml.load(new FileReader(blackListFileName));
        List<String> list = data.get("fileNameEntries")
        List<String> blackListFileNamesList = []
        list.each {
            blackListFileNamesList.add(it)
        }

        return blackListFileNamesList
    }

    @Override
    Map<String, List<String>> getBlockListMap() {
        Yaml yaml = new Yaml(new SafeConstructor());
        Map data = yaml.load(new FileReader(blackListFileName));
        List<Map> list = data.get("providerNameEntries")
        Map<String,List<String>> blackListMap = [:]
        list.each {
            blackListMap.putAll(it)
        }
        return blackListMap
    }

    @Override
    Boolean isBlocklistSet() {
        if(blackListFileName==null){
            return false
        }
        else{
            return true
        }
    }
}
