package org.rundeck.core.auth.access;

/**
 * Represents a singleton
 */
public final class Singleton {
    private Singleton() {
    }

    public static final Singleton ONLY = new Singleton();
}
