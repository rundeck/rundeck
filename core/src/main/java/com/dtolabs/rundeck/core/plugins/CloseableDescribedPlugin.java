package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.plugins.configuration.Description;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * A described plugin which can be closed
 *
 * @author greg
 * @since 3/8/17
 */
public class CloseableDescribedPlugin<T>
        extends DescribedPlugin<T>
        implements Closeable
{
    public CloseableDescribedPlugin(final DescribedPlugin<T> plugin) {
        super(plugin.getInstance(), plugin.getDescription(), plugin.getName(), plugin.getFile());
        this.closeable = Closeables.closeableProvider(plugin.getInstance());
    }

    public CloseableDescribedPlugin(
            final CloseableProvider<T> closeable,
            final Description description,
            final String name,
            final File file
    )
    {
        super(closeable.getProvider(), description, name, file);
        this.closeable = closeable;
    }

    public CloseableDescribedPlugin(
            final CloseableProvider<T> closeable,
            final Description description,
            final String name
    )
    {
        super(closeable.getProvider(), description, name);
        this.closeable = closeable;
    }

    @Override
    public void close() throws IOException {
        closeable.close();
    }

    public CloseableProvider<T> getCloseable() {
        return closeable;
    }

    public void setCloseable(CloseableProvider<T> closeable) {
        this.closeable = closeable;
    }

    private CloseableProvider<T> closeable;
}
