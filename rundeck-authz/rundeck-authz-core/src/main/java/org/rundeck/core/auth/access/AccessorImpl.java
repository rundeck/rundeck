package org.rundeck.core.auth.access;

import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

@RequiredArgsConstructor
class AccessorImpl<T>
        implements Accessor<T>
{
    private final AccessActions actions;
    private final Required<T> requireActions;
    private final Allowed allowedTest;
    private final Supplier<T> getter;

    static interface Allowed {
        boolean isAllowed(AccessActions actions) throws NotFound;
    }

    static interface Required<T> {
        T getAccess(AccessActions actions) throws UnauthorizedAccess, NotFound;
    }


    @Override
    public T getAccess() throws UnauthorizedAccess, NotFound {
        return requireActions.getAccess(actions);
    }


    @Override
    public boolean isExists() {
        T resource = getter.get();
        return resource != null;
    }

    @Override
    public boolean isAllowed() throws NotFound {
        return allowedTest.isAllowed(actions);
    }
}
