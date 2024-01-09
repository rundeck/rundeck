
export default {
    title: 'Class Components'
}

export const typography = () => ({
    render(h) {
        return (
            <div>
                <h1 class="text-info">Headings</h1>
                <div class="h1">H1 Heading</div>
                <div class="h2">H2 Heading</div>
                <div class="h3">H3 Heading</div>
                <div class="h4">H4 Heading</div>
                <div class="h5">H5 Heading</div>
                <div class="h6">H6 Heading</div>
                <h4 class="text-info">Text Heading Styles</h4>
                <div class="text-h1">H1 Text</div>
                <div class="text-h2">H2 Text</div>
                <div class="text-h3">H3 Text</div>
                <div class="text-h4">H4 Text</div>
                <div class="text-h5">H5 Text</div>
                <div class="text-h6">H6 Text</div>

                <h1 class="text-info">Links</h1>
                <div style="margin-top: 20px;"><a>Default links are accessible blue.</a></div>
                <div><a class="link-quiet">Quiet links have no color.</a></div>

                <h1 class="text-info">Text</h1>
                <div>Just some text</div>
                <div><strong v-html="&lt;strong&gt;Text is bold.&lt;/strong&gt;"></strong></div>
                <div class="text-strong">.text-strong Text is bold.</div>
                <div class="text-primary">Text primary</div>
                <div class="text-muted">.text-muted is ??</div>
                <div class="text-secondary">.text-secondary is ??</div>

                <h4 class="text-info">Color Text</h4>
                <div class="text-success">Success text.</div>
                <div class="text-info">Info text.</div>
                <div class="text-warning">Warning text.</div>
                <div class="text-danger">Danger text.</div>

                <h4 class="text-info">Backgrounds</h4>
                <div class="bg-success">Success background.</div>
                <div class="bg-info">Info background.</div>
                <div class="bg-warning">Warning background.</div>
                <div class="bg-danger">Danger background.</div>

                <h4 class="text-info">Alerts</h4>
                <div class="alert">Default alert</div>
                <div class="alert alert-info">Info alert</div>
                <div class="alert alert-success">Success alert</div>
                <div class="alert alert-warning">Warning alert</div>
                <div class="alert alert-danger">Danger alert</div>
            </div>
        )
    }
})
export const labels = () => ({
  render(h) {
    return (
      <div>
        <h3>H3 Text <span class="label label-default">default</span></h3>
        <h3>H3 Text <span class="label label-secondary">secondary</span></h3>
        <h3>H3 Text <span class="label label-muted">muted</span></h3>
        <h3>H3 Text <span class="label label-danger">danger</span></h3>
        <h3>H3 Text <span class="label label-warning">warning</span></h3>
        <h3>H3 Text <span class="label label-success">success</span></h3>
        <h3>H3 Text <span class="label label-info">info</span></h3>
        <h3>H3 Text <span class="label label-white">white</span></h3>
      </div>
    )
  }
})
export const buttons = () => ({
    render(h) {
        return (
            <div style="max-width: 800px; margin-top: 10px;background-color: pink;">
                <div style="display:flex;justify-content: space-evenly;">
                    <a class="btn btn-default btn-link" role="button">Link</a>
                    <button class="btn btn-default">Button</button>
                    <input class="btn btn-default" value="Input"/>
                </div>
                <div style="display: flex;justify-content: space-evenly; margin-top: 10px;">
                    <button class="btn btn-default">Default</button>
                    <button class="btn btn-default btn-simple">Default Simple</button>
                    <button class="btn btn-primary">Primary</button>
                    <button class="btn btn-info">Info</button>
                    <button class="btn btn-cta">CTA</button>
                    <button class="btn btn-success">Success</button>
                    <button class="btn btn-warning">Warning</button>
                    <button class="btn btn-danger">Danger</button>
                    <button class="btn btn-default btn-transparent">Transparent</button>
                </div>
                <div style="display: flex;justify-content: space-evenly; margin-top: 10px;">
                    <div class="btn-group">
                        <button class="btn btn-default">Default</button>
                        <button class="btn btn-success">Success</button>
                        <button class="btn btn-warning">Warning</button>
                    </div>
                </div>
                <div style="display: flex;justify-content: space-evenly; margin-top: 10px;">
                    <div class="btn-group">
                        <div class="btn btn-default">Default</div>
                        <div class="btn btn-default">Default</div>
                        <div class="btn btn-default">Default</div>
                    </div>
                </div>
                <div style="display: flex;justify-content: space-evenly; margin-top: 10px;">
                    <div class="btn-group pull-right" id="execOptFormRunButtons">
                    <button type="submit" name="_action_runJobNow" id="execFormRunButton" class=" btn btn-cta  ">
                        <b class="fas fa-bug" data-bind="visible: debug()" style="display: none;"></b>
                        <b class="fas fa-eye" data-bind="visible: follow()"></b>
                        Run Job Now
                        <b class="glyphicon glyphicon-play"></b>
                    </button>
                    <button type="button" class="btn  btn-secondary btn-cta dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        <span class="fas fa-chevron-down"></span>
                        <span class="sr-only">Toggle Dropdown</span>
                    </button>
                    <ul class="dropdown-menu ">
                        <li>
                            <a href="#" data-bind="click: function(){debug(!debug())}">
                                <b class="fas fa-bug"></b>
                                Run with Debug Output
                                <b class="fas fa-check" data-bind="visible: debug()" style="display: none;"></b>
                            </a>
                        </li>
                        <li>
                            <a href="#" data-bind="click: function(){follow(!follow())}">
                                <b class="fas fa-eye"></b>
                                Follow output
                                <b class="fas fa-check" data-bind="visible: follow()"></b>
                            </a>
                        </li>
                    </ul>
                </div>
                </div>
                <h3>Disabled</h3>
                <div style="display:flex;justify-content: space-evenly;">
                    <a class="btn btn-disabled btn-default btn-link" role="button">Link</a>
                    <button class="btn btn-disabled btn-default">Button</button>
                    <input class="btn btn-disabled btn-default" value="Input"/>
                </div>
                <div style="display: flex;justify-content: space-evenly; margin-top: 10px;">
                    <button class="btn btn-disabled btn-default">Default</button>
                    <button class="btn btn-disabled btn-primary">Primary</button>
                    <button class="btn btn-disabled btn-info">Info</button>
                    <button class="btn btn-disabled btn-cta">CTA</button>
                    <button class="btn btn-disabled btn-success">Success</button>
                    <button class="btn btn-disabled btn-warning">Warning</button>
                    <button class="btn btn-disabled btn-danger">Danger</button>
                    <button class="btn btn-disabled btn-default btn-transparent">Transparent</button>
                </div>
            </div>
            
        )
    }
})
export const table = () => ({
  props: {
    rowStyle: {
      default:  'table-bordered'
    }
  },
  template:`
            <div >
              <table class="table table-condensed  table-embed table-data-embed " :class="rowStyle">
                <tbody>
                <tr>
                  <th colSpan="2" class="table-header">global</th>
                </tr>
                <tr>
                  <th class="table-header">Key</th>
                  <th class="table-header">Value</th>
                </tr>
                <tr>
                  <td>globals</td>
                  <td><span class="text-muted">Empty Map</span></td>
                </tr>
                <tr>
                  <td>job</td>
                  <td>
                    <table class="table table-condensed table-embed table-data-embed "  :class="rowStyle">
                      <tbody>
                      <tr>
                        <th class="table-header">Key</th>
     <th class="table-header">Value</th>
                      </tr>
                      <tr>
                        <td>successOnEmptyNodeFilter</td>
                        <td>false</td>
                      </tr>
                      <tr>
                        <td>executionType</td>
                        <td>user</td>
                      </tr>
                      <tr>
                        <td>wasRetry</td>
                        <td>false</td>
                      </tr>
                      <tr>
                        <td>user.name</td>
                        <td>admin</td>
                      </tr>
                      <tr>
                        <td>project</td>
                        <td>test1</td>
                      </tr>
                      <tr>
                        <td>threadcount</td>
                        <td>1</td>
                      </tr>
                      <tr>
                        <td>url</td>
                        <td>http://artorias.local:4440/rundeckpro/project/test1/execution/follow/91</td>
                      </tr>
                      <tr>
                        <td>execid</td>
                        <td>91</td>
                      </tr>
                      <tr>
                        <td>filter</td>
                        <td>name: outatime.local</td>
                      </tr>
                      <tr>
                        <td>retryPrevExecId</td>
                        <td>0</td>
                      </tr>
                      <tr>
                        <td>serverUUID</td>
                        <td>a3de6030-2b7a-47e3-b46f-3e46a11a85d9</td>
                      </tr>
                      <tr>
                        <td>serverUrl</td>
                        <td>http://artorias.local:4440/rundeckpro/</td>
                      </tr>
                      <tr>
                        <td>loglevel</td>
                        <td>NORMAL</td>
                      </tr>
                      <tr>
                        <td>name</td>
                        <td>test echo</td>
                      </tr>
                      <tr>
                        <td>retryInitialExecId</td>
                        <td>0</td>
                      </tr>
                      <tr>
                        <td>id</td>
 <td>a616e247-8000-45b0-bce8-4cf8b5712e62</td>
                      </tr>
                      <tr>
                        <td>retryAttempt</td>
                        <td>0</td>
                      </tr>
                      <tr>
                        <td>group</td>
                        <td><span class="text-muted">Null Value</span></td>
                      </tr>
                      <tr>
                        <td>username</td>
                        <td>admin</td>
                      </tr>
                      </tbody>
                    </table>
                  </td>
                </tr>
                <tr>
                  <td>option</td>
                  <td><span class="text-muted">Empty Map</span></td>
                </tr>
                </tbody>
              </table>
            </div>
        `
})


export const pagination = () => ({
    render(h) {
        return (
            <ul data-v-06f48450="" class="pagination pagination-sm">
                <li data-v-06f48450="" class="disabled">
                    <a data-v-06f48450="" href="#" title="Previous Page" class="page_nav_btn"><i data-v-06f48450="" class="glyphicon glyphicon-arrow-left"></i></a>
                </li>
                <li data-v-06f48450="" class="active">
                    <span data-v-06f48450="" title="Page 1">1</span>
                </li>
                <li data-v-06f48450="" class="">
                    <a data-v-06f48450="" href="#" title="Page 2" class="page_nav_btn">2</a></li><li data-v-06f48450="" class=""><a data-v-06f48450="" href="#" title="Page 3" class="page_nav_btn">3</a></li><li data-v-06f48450="" class=""><a data-v-06f48450="" href="#" title="Page 4" class="page_nav_btn">4</a></li><li data-v-06f48450="" class=""><a data-v-06f48450="" href="#" title="Page 5" class="page_nav_btn">5</a></li><li data-v-06f48450="" class=""><a data-v-06f48450="" href="#" title="Page 6" class="page_nav_btn">6</a></li><li data-v-06f48450="" class=""><a data-v-06f48450="" href="#" title="Page 7" class="page_nav_btn">7</a></li><li data-v-06f48450="" class=""><a data-v-06f48450="" href="#" title="Page 8" class="page_nav_btn">8</a></li><li data-v-06f48450="" class=""><a data-v-06f48450="" href="#" title="Next Page" class="page_nav_btn"><i data-v-06f48450="" class="glyphicon glyphicon-arrow-right"></i></a></li></ul>
        )
    }
})

export const cards = () => ({
    render(h) {
        return (
            <div style="padding: 20px; max-width: 500px" class="content">
                <div class="card">
                    <div class="card-content" style="padding-bottom: 20px;">
                    <span class="h3 text-primary">
                        <span data-bind="messageTemplate: projectNamesTotal, messageTemplatePluralize:true">220 Projects
                        </span>
                    </span>
                    
                    <a href="/resources/createProject" class="btn  btn-success pull-right">
                        New Project
                        <b class="glyphicon glyphicon-plus"></b>
                    </a>

                    <div>
                        Card content text.
                    </div>

                    </div>
                </div>
            </div>
        )
    }
})

export const tabs = () => ({
    render(h) {
        return (
            <div class="vue-tabs">
                <div class="nav-tabs-navigation">
                    <div class="nav-tabs-wrapper">
                        <ul class="nav nav-tabs">
                            <li role="presentation" class="active"><a href="#">Home</a></li>
                            <li role="presentation"><a href="#">Profile</a></li>
                            <li role="presentation"><a href="#">Messages</a></li>
                        </ul>
                    </div>
                </div>
            </div>
        )
    }
})


export const inputs = () => ({
    render(h) {
        return (
            <div>
                <div class="card" style="padding: 20px;">
                    <div class="checkbox">
                        <input type="hidden" name="_exportExecutions"/><input type="checkbox" name="exportExecutions" checked="checked" value="true" id="exportExecutions"/>
                        <label for="exportExecutions">Executions</label>
                    </div>
                    <div class="checkbox">
                        <input type="hidden" name="_exportExecutions"/><input type="checkbox" name="exportExecutions" value="false" id="exportExecutions"/>
                        <label for="exportExecutions">Executions</label>
                    </div>
                    <div class="radio">
                        <input type="radio" name="stripJobRef" checked="checked" value="no" id="dontStrip"/>
                        <label for="dontStrip">
                        Do not modify referenced jobs at export.
                        </label>
                    </div>
                    <div class="radio">
                        <input type="radio" name="stripJobRef" value="name" id="stripName"/>
                        <label for="stripName">
                        Strip Names. If possible, use only the UUID on referenced jobs.
                        </label>
                    </div>
                    <label for="optlabel_41bbe448" class="col-sm-2 control-label    ">Option Label</label>
                    <input type="text" class="form-control" name="label" id="opt_label" value="" size="40" placeholder="Option Label"></input>
                    <label style="margin-top: 10px;">Input Group</label>
                    <div class="input-group">
                        <span class="input-group-btn">
                            <a class="btn btn-default">Foo</a>
                            <a class="btn btn-default">Foo</a>
                        </span>
                        <input type="text" class="form-control"/>
                    </div>
                </div>

                <div class="card card-accent" style="padding: 20px;">
                    <div class="checkbox">
                        <input type="hidden" name="_exportExecutions"/><input type="checkbox" name="exportExecutions" checked="checked" value="true" id="exportExecutions"/>
                        <label for="exportExecutions">Executions</label>
                    </div>
                    <div class="checkbox">
                        <input type="hidden" name="_exportExecutions"/><input type="checkbox" name="exportExecutions" value="false" id="exportExecutions"/>
                        <label for="exportExecutions">Executions</label>
                    </div>
                    <div class="radio">
                        <input type="radio" name="stripJobRef" checked="checked" value="no" id="dontStrip"/>
                        <label for="dontStrip">
                        Do not modify referenced jobs at export.
                        </label>
                    </div>
                    <div class="radio">
                        <input type="radio" name="stripJobRef" value="name" id="stripName"/>
                        <label for="stripName">
                        Strip Names. If possible, use only the UUID on referenced jobs.
                        </label>
                    </div>
                    <label for="optlabel_41bbe448" class="col-sm-2 control-label    ">Option Label</label>
                    <input type="text" class="form-control" name="label" id="opt_label" value="" size="40" placeholder="Option Label"></input>
                </div>
            </div>
        )
    }
})


export const panels = () => ({
    render(h) {
        return (
            <div class="content">
                <div class="panel panel-default" style="width: 500px; height: 100px;">
                    <div class="panel-heading">Default</div>
                    <div class="panel-body">
                        Panel body
                    </div>
                </div>
                <div class="panel panel-primary" style="width: 500px; height: 100px;">
                    <div class="panel-heading">Primary</div>
                    <div class="panel-body">
                        Panel body
                    </div>
                </div>
                <div class="panel panel-info" style="width: 500px; height: 100px;">
                    <div class="panel-heading">Primary</div>
                    <div class="panel-body">
                        Panel body
                    </div>
                </div>
                <div class="panel panel-success" style="width: 500px; height: 100px;">
                    <div class="panel-heading">Success</div>
                    <div class="panel-body">
                        Panel body
                    </div>
                </div>
                <div class="panel panel-warning" style="width: 500px; height: 100px;">
                    <div class="panel-heading">Success</div>
                    <div class="panel-body">
                        Panel body
                    </div>
                </div>
                <div class="panel panel-danger" style="width: 500px; height: 100px;">
                    <div class="panel-heading">Success</div>
                    <div class="panel-body">
                        Panel body
                    </div>
                </div>
            </div>
        )
    }

})

export const layouts = () => ({
    render(h) {
        return (
            <section class="layout-base">
                <div class="layout-base--header">
                    <div class="layout-base--content">
                        <h1>Runners</h1>
                        <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam faucibus auctor nunc, vel rhoncus nulla consectetur et. Aliquam ac laoreet turpis, eu pharetra mauris. Suspendisse imperdiet feugiat elementum.</p>
                    </div>
                </div>
                <div class="layout-base--body">
                    <div class="layout-base--content">
                        <div class="card">
                            Sample
                        </div>
                    </div>
                </div>
            </section>
        )
    }

})