package org.rundeck.app.acl;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor
@EqualsAndHashCode
class AppACLContextImpl
        implements AppACLContext
{
    @Getter private final boolean system;
    @Getter private final String project;
}
