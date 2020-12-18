package org.rundeck.app.acl;

import lombok.*;

@Builder
@RequiredArgsConstructor
@EqualsAndHashCode
@ToString
class AppACLContextImpl
        implements AppACLContext
{
    @Getter private final boolean system;
    @Getter private final String project;
}
