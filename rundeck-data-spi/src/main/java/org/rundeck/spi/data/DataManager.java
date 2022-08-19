package org.rundeck.spi.data;

public interface DataManager {
     void registerDataProvider(
            Object dataProvider);

     Object getProviderForType(String className);
}
