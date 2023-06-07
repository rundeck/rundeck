package org.rundeck.security

import com.dtolabs.rundeck.core.plugins.PluginBlocklist
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

@CompileStatic
class RundeckPluginBlocklist implements PluginBlocklist {

    String blockListFileName
    private List<String> fileNameEntries = []
    private Map<String, List<String>> providerNameEntries = [:]
    private boolean loaded

    @Override
    boolean isPluginFilePresent(String fileName) {
        if (!isBlocklistSet()) {
            return false
        }
        load()

        for (String item : fileNameEntries) {
            if (fileName.startsWith(item)) {
                return true
            }
        }
        return false
    }

    @CompileDynamic
    private load() {
        if (!loaded) {
            Yaml yaml = new Yaml(new LoaderOptions());
            Map data = yaml.load(new FileReader(blockListFileName));
            fileNameEntries = data.get("fileNameEntries")
            providerNameEntries = data.get("providerNameEntries")
            loaded = true
        }
    }

    @Override
    boolean isPluginProviderPresent(String service, String providerName) {
        if (!isBlocklistSet()) {
            return false
        }
        load()
        providerNameEntries.get(service)?.contains(providerName) ?: false
    }

    private Boolean isBlocklistSet() {
        return blockListFileName != null
    }
}
