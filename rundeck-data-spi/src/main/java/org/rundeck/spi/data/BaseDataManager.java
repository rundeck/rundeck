package org.rundeck.spi.data;

import java.util.HashMap;
import java.util.Map;

public class BaseDataManager
        implements DataManager
{
    private final Map<Class<?>, DataProvider<?, ?>> providers = new HashMap<>();

    public <D, T extends DataType<D>, C> void registerDataProvider(
            ContextDataProvider<C, D, T> dataProvider, AccessContextProvider<C> accessContextProvider
    )
    {
        providers.put(
                dataProvider.getDataType().getJavaType(),
                new ContextResolvedProvider<>(accessContextProvider, dataProvider)
        );

    }

    public <D, T extends DataType<D>> DataProvider<D, T> getProviderForType(Class<D> tClass) {
        return (DataProvider<D, T>) providers.get(tClass);
    }
}
