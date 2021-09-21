import Vue from 'vue'

export default {
    title: 'Class Components'
}

export const typography = () => (Vue.extend({
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
                <div><strong domPropsInnerHTML="&lt;strong&gt;Text is bold.&lt;/strong&gt;"></strong></div>
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
                <div class="alert-info">Info alert</div>
                <div class="alert-success">Success alert</div>
                <div class="alert-warning">Warning alert</div>
                <div class="alert-danger">Danger alert</div>
            </div>
        )
    }
}))

export const buttons = () => (Vue.extend({
    render(h) {
        return (
            <div style="max-width: 800px; margin-top: 10px;background-color: pink;">
                <div style="display:flex;justify-content: space-evenly;">
                    <a class="btn btn-default" role="button">Link</a>
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
                <h3>Disabled</h3>
                <div style="display:flex;justify-content: space-evenly;">
                    <a class="btn btn-disabled btn-default" role="button">Link</a>
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
}))

export const pagination = () => (Vue.extend({
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
}))

export const cards = () => (Vue.extend({
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
}))

export const tabs = () => (Vue.extend({
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
}))


export const inputs = () => (Vue.extend({
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
}))


export const panels = () => (Vue.extend({
    render(h) {
        return (
            <div class="content">
                <div class="panel panel-default" style="width: 500px; height: 200px;">
                    <div class="panel-heading">Default</div>
                    <div class="panel-body">
                        Panel body
                    </div>
                </div>
            </div>
        )
    }

}))