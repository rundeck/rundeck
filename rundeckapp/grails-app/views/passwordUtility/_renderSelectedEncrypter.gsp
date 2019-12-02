<g:each in="${selectedEncrypter}" var="prop">
    <g:render template="/framework/pluginConfigPropertyFormField" model="${[prop:prop,
                                                                            error: report?.errors ? report.errors[prop.name] : null,
                                                                            fieldname: prop.name,
                                                                            origfieldname: "orig."+prop.name,
    ]}" />
</g:each>