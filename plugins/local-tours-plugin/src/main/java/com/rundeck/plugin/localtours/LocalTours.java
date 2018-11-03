package com.rundeck.plugin.localtours;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.tours.Tour;
import com.dtolabs.rundeck.plugins.tours.TourLoaderPlugin;
import com.dtolabs.rundeck.plugins.tours.TourManifest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

@Plugin(service="TourLoader",name="localtours")
@PluginDescription(title="Local Tours", description="Local Tours")
public class LocalTours implements TourLoaderPlugin {
    Logger LOG = LoggerFactory.getLogger(LocalTours.class);

    private static final ObjectMapper mapper = new ObjectMapper();
    static  {
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @PluginProperty(name = "tourBaseDir", title = "Tour Directory", description = "Absolute path to base directory where tour files are located. ")
    String tourBaseDir;
    @PluginProperty(name = "manifestFileName", title = "Manifest File Name", defaultValue = "tour-manifest.json",description = "Name of tour manifest file.")
    String manifestFileName;
    @PluginProperty(name = "toursSubDir", title = "Tours Sub Directory", defaultValue = "tours",description = "Name of sub dir within the tour base dir where tours are located")
    String toursSubDir;

    @Override
    public String getLoaderName() {
        return "Local Tours";
    }

    @Override
    public Map getTourManifest() {
       File manifest = new File(tourBaseDir,manifestFileName);
       LOG.debug("Loading tour: " + manifest.getAbsolutePath());
       if(!manifest.exists()) {
           LOG.error("Manifest file: " + manifest.getAbsolutePath() + " does not exist");
           throw new RuntimeException("Manifest file: " +  manifest.getAbsolutePath() + " does not exist");
       }
        try {
            return mapper.readValue(manifest, TreeMap.class);
        } catch (IOException e) {
            LOG.error("Unable to load tour manifest",e);
        }
        return null;
    }

    @Override
    public Map getTour(final String tourId) {
        String tourKey = tourId.endsWith(".json") ? tourId : tourId +".json";
        File tourFile = new File(tourBaseDir,toursSubDir+"/"+tourKey);
        LOG.debug("Loading tour: " + tourFile.getAbsolutePath());
        if(!tourFile.exists()) {
            LOG.error("Tour file: " + tourFile.getAbsolutePath() + " does not exist");
            throw new RuntimeException("Tour file: " +  tourFile.getAbsolutePath() + " does not exist");
        }
        try {
            return mapper.readValue(tourFile, TreeMap.class);
        } catch (IOException e) {
            LOG.error("Unable to load tour manifest",e);
        }
        return null;
    }

}