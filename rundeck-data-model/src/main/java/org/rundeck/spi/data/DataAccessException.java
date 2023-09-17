package org.rundeck.spi.data;

public class DataAccessException
        extends Exception
{
    public DataAccessException() {
    }

    public DataAccessException(final String message) {
        super(message);
    }

    public DataAccessException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(final Throwable cause) {
        super(cause);
    }

    public DataAccessException(
            final String message,
            final Throwable cause,
            final boolean enableSuppression,
            final boolean writableStackTrace
    )
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
