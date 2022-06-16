package com.dtolabs.rundeck.app.api.tokens

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema

@CompileStatic
@Schema(description = 'Create Token request using a comma-separated string for Roles')
class CreateTokenStringRoles extends CreateTokenBase{

    @Schema(description = 'since: v19')
    String roles
}
