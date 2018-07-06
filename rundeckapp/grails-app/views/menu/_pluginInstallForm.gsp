 <div class="modal" id="installplugin" tabindex="-1" role="dialog" aria-labelledby="installplugin" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
              <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
              <h4 class="modal-title">
                <g:message code="gui.admin.InstallPlugin" />
              </h4>
            </div>
            <div class="modal-body">
                <g:uploadForm controller="plugin" action="uploadPlugin" id="uploadPlugin" useToken="true" class="form-horizontal" role="form">
                <div class="form-group">
                    <div class="col-sm-4">
                     <label for="pluginFile" class="input-sm">
                      <g:message code="plugin.form.pluginFile.label" />
                    </label>
                    </div>
                    <div class="col-sm-8">
                        <input type="submit" value="Upload" class="pull-right"/><input type="file" name="pluginFile" id="pluginFile">
                    </div>
                </div>
                </g:uploadForm>
                <div class="row row-space-bottom">
                   <label for="pluginFile" class="col-sm-offset-1 input-sm"><g:message code="or"/></label>
                </div>
                <g:form controller="plugin" action="installPlugin" id="installPlugin" useToken="true" class="form-horizontal" role="form">
                <div class="form-group">
                    <div class="col-sm-4">
                     <label for="pluginUrl" class="input-sm">
                      <g:message code="plugin.form.pluginUrl.label" />
                    </label>
                    </div>
                    <div class="col-sm-8">
                        <input type="submit" value="Install" class="pull-right" />
                        <input type="text" name="pluginUrl" id="pluginUrl" class="col-sm-8">
                    </div>
                </div>
                </g:form>
            </div>
        </div>
    </div>
</div>
