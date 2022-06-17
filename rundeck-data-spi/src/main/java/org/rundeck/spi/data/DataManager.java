package org.rundeck.spi.data;

public interface DataManager {
    <D, T extends DataType<D>, C> void registerDataProvider(
            ContextDataProvider<C, D, T> dataProvider,
            AccessContextProvider<C> accessContextProvider
    );

    <D, T extends DataType<D>> DataProvider<D, T> getProviderForType(Class<D> tClass);
}
