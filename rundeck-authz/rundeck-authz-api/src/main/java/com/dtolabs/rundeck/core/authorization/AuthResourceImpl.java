package com.dtolabs.rundeck.core.authorization;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Getter
@RequiredArgsConstructor
public class AuthResourceImpl
        implements AuthResource
{
    final AuthResource.Context context;
    final Map<String, String> resourceMap;
}
