package com.dtolabs.rundeck.app.api.tokens

import groovy.transform.CompileStatic
import io.swagger.v3.oas.annotations.media.Schema

@CompileStatic
@Schema(description = 'Create Token request using a list of Roles')
class CreateToken extends CreateTokenBase {
    @Schema(description = 'since: v19')
    List<String> roles = []
}
