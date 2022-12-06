package com.dtolabs.rundeck.app.api.feature

import com.dtolabs.rundeck.app.api.marshall.ApiResource
import com.dtolabs.rundeck.app.api.marshall.ApiVersion
import com.dtolabs.rundeck.app.api.marshall.XmlAttribute
import io.swagger.v3.oas.annotations.media.Schema

@ApiResource
@Schema
class FeatureEnabledResult {
    @ApiVersion(42)
    @XmlAttribute
    @Schema(description = "since: v42")
    Boolean enabled;

    public FeatureEnabledResult(Boolean enabled) {
        this.enabled = enabled
    }
}
