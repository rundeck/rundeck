<g:each in="${selectedEncrypter.formProperties()}" var="prop">
    <g:render template="/framework/pluginConfigPropertyFormField" model="${[prop:prop, error: null,
                                                                            fieldname: prop.name,
                                                                            origfieldname: "orig."+prop.name,
    ]}" />
</g:each>