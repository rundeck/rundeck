package org.rundeck.core.auth.access;

import lombok.RequiredArgsConstructor;

import java.util.function.Supplier;

@RequiredArgsConstructor
class AccessorImpl<T>
        implements Accessor<T>
{
    private final AuthActions actions;
    private final Required<T> requireActions;
    private final Allowed allowedTest;
    private final Supplier<T> getter;

    interface Allowed {
        boolean isAllowed(AuthActions actions) throws NotFound;
    }

    interface Required<T> {
        T getAccess(AuthActions actions) throws UnauthorizedAccess, NotFound;
    }

    @Override
    public void authorize() throws UnauthorizedAccess, NotFound {
        getResource();
    }

    @Override
    public T getResource() throws UnauthorizedAccess, NotFound {
        return requireActions.getAccess(actions);
    }


    @Override
    public boolean isExists() {
        return getter.get() != null;
    }

    @Override
    public boolean isAllowed() throws NotFound {
        return allowedTest.isAllowed(actions);
    }
}
