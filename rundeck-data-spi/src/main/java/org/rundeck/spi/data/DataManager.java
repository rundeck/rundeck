package org.rundeck.spi.data;

public interface DataManager {
    <D> void registerDataProvider(
            DataProvider<D> dataProvider);

    <D> DataProvider<D> getProviderForType(String className);
}
