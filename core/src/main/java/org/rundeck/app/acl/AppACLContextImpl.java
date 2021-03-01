package org.rundeck.app.acl;

import lombok.*;

@Builder
@RequiredArgsConstructor
@EqualsAndHashCode
class AppACLContextImpl
        implements AppACLContext
{
    @Getter private final boolean system;
    @Getter private final String project;

    @Override
    public String toString() {
        return system ? "[context: System]" : ("[context: Project " + project + "]");
    }
}
