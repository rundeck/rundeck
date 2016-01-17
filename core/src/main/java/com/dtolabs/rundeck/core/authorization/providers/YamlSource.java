package com.dtolabs.rundeck.core.authorization.providers;

import org.yaml.snakeyaml.Yaml;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by greg on 7/17/15.
 */
public interface YamlSource extends Closeable{
    public String getIdentity();
    public Iterable<Object> loadAll(Yaml yaml) throws IOException;
}
