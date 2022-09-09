package org.rundeck.spi.data;

public interface DataManager {
    <D> void registerDataProvider(Class<D> clazz, D provider);
    <D> D getProviderForType(Class<D> clazz);
}
