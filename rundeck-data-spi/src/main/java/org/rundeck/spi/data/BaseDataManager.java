package org.rundeck.spi.data;

import java.util.HashMap;
import java.util.Map;

public class BaseDataManager
        implements DataManager
{
    private final Map<String, DataProvider<?>> providers = new HashMap<>();

    public <D> void registerDataProvider(
            DataProvider<D> dataProvider
    )
    {
        providers.put(
                dataProvider.getClass().getSimpleName(),
                dataProvider
        );

    }

    public <D> DataProvider<D> getProviderForType(String className) {
        return (DataProvider<D>) providers.get(className);
    }
}
