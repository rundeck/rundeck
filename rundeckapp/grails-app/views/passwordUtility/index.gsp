<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title><g:message code="gui.menu.PasswordUtility"/></title>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="configure"/>
    <meta name="tabtitle" content="${g.message(code:'gui.menu.PasswordUtility')}"/>

</head>

<body>
<div class="container-fluid">
    <div class="row">
        <div class="col-sm-12">
            <g:render template="/common/messages"/>
        </div>
    </div>
    <div class="row">
        <div class="col-xs-12">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title"><g:message code="passwordUtility.page.label"/></h3>
                </div>
                <div class="card-content">
                    <g:form class="form-horizontal" controller="passwordUtility" action="encode" method="POST" useToken="true">
                        <div class="form-group">
                            <label class="col-sm-2 control-label input-sm"><g:message code="passwordUtility.encoder.label"/></label>
                            <div class="col-sm-10">
                                <g:select from="${encrypters.keySet().toSorted()}" name="encrypter" class="form-control" onchange="updateEncrypterProps()" value="${flash.encrypter}"></g:select>
                            </div>
                        </div>
                        <div id="encryptionFormProps">
                            <g:render template="renderSelectedEncrypter" model="${[selectedEncrypter:properties]}" />
                        </div>
                        <div class="form-group"><div class="col-sm-10 col-sm-offset-2"><g:actionSubmit value="Encode" class="btn btn-sm btn-default" /></div></div>
                    </g:form>
                </div>
            </div>
            <g:if test="${flash.output}">
            <div class="card">
                <div class="card-header">
                    <h3 class="card-title"><g:message code="passwordUtility.output.label"/></h3>
                </div>
                <div class="card-content">
                    <g:each in="${flash.output}" var="kv">
                        <div class="row"><label class="col-sm-1">${kv.key}</label><label class="col-sm-11">${kv.value}</label></div>
                    </g:each>
                </div>
            </div>
            </g:if>
        </div>
    </div>
</div>
<script type="text/javascript">
function updateEncrypterProps() {
    jQuery.ajax({
        url: "${f.createLink(controller:'passwordUtility',action:'selectedEncrypterProps')}",
        data: {selectedEncrypter:jQuery("#encrypter").val()},
        type: 'get',
        success : function (data) { jQuery('#encryptionFormProps').html(data) }
    })
}
</script>
</body>
</html>