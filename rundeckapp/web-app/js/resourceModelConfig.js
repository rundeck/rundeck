/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var ResourceModelConfigControl = Class.create({
    configCount: 0,  /* This element is used by passwordFieldsService to track when resources are deleted.  Do not reuse
     the configCount values after delete.  i.e. if you delete #2 and add a new resource, ensure that it is #3. */
    prefixKey: 'x',
    initialize: function(pfix) {
        if(pfix){
            this.prefixKey=pfix;
        }
    },
    addConfigChrome: function (elem, type, prefix, index, edit) {

    var parentNode = $(elem).parentNode;
    var top = new Element("div");
    var top1 = new Element("div");
    var wrapper = new Element("div");
    top.appendChild(top1);
    top1.appendChild(wrapper);
    top1.addClassName('panel panel-default');
    wrapper.addClassName('panel-body');
    $(parentNode).insert(top, {after:elem});
    var content = parentNode.removeChild(elem);
    if(content.down('.rsrcConfigContent')){
        content= content.down('.rsrcConfigContent');
    }else{
        content.addClassName('rsrcConfigContent');
    }
    $(content).select('input').each(function(elem) {
        if (elem.type == 'text') {
            elem.observe('keypress', noenter);
        }
    });

    var hidden3 = new Element("input");
    hidden3.setAttribute("type", "hidden");
    hidden3.setAttribute("name", "index");
    hidden3.setAttribute("value", index);
    hidden3.addClassName("configindex");

    var buttons1 = new Element("div");
    buttons1.addClassName('panel-footer ');
    var buttons = new Element("div");
    buttons.addClassName('buttons');
    buttons1.appendChild(buttons);

    if (edit) {
        var self=this;
        var button;
        var isinvalid = $(elem).down(".invalidProvider");
        if(!isinvalid){
            button = new Element("button");
            Event.observe(button, 'click', function(e) {
                Event.stop(e);
                self.editConfig(top, type, prefix, index);
            });
            setText(button,"Edit");
            button.addClassName('btn-info');
        }

        var cancelbutton = new Element("button");
        Event.observe(cancelbutton, 'click', function(e) {
            Event.stop(e);
            self.cancelConfig(top);
        });
        setText(cancelbutton,"Delete");
        cancelbutton.addClassName('btn-danger');

        buttons.appendChild(cancelbutton);
        if(button){
            buttons.appendChild(button);
        }
    } else {
        var self = this;
        var button = new Element("button");
        Event.observe(button, 'click', function(e) {
            Event.stop(e);
            self.checkConfig(top, type, prefix, index);
        });
        setText(button,"Save");
        button.addClassName("needsSave");
        button.addClassName('btn-primary');

        var cancelbutton = new Element("button");
        Event.observe(cancelbutton, 'click', function(e) {
            Event.stop(e);
            self.cancelConfig(top, type, prefix, index);
        });
        setText(cancelbutton,"Cancel");

        buttons.appendChild(cancelbutton);
        buttons.appendChild(button);
    }

    content.insert(hidden3);

    wrapper.appendChild(content);
    $(buttons).descendants('button').each(function(b){
        $(b).addClassName('btn btn-default btn-sm');
    });
    top1.appendChild(buttons1);
},
error: function(req) {
    var data = req.responseJSON;
    if($('errors')){
        setText($('errors'),req);
        $('errors').show();
    }
},
renderConfig: function (elem, type, prefix, index, revert) {
    this.hidePicker();
    var params = Form.serialize(elem);
    if (revert) {
        params += "&revert=true";
    }
    var self = this;
    new Ajax.Updater(elem, appLinks.frameworkViewResourceModelConfig, {
        parameters:params,
        onComplete:function(ajax) {
            if (ajax.request.success()) {
                self.addConfigChrome(elem, type, prefix, index, true);
            }else{
                self.error(ajax);
            }
        }
    });
},
checkConfig: function (elem, type, prefix, index, revert) {
    var params = Form.serialize(elem);
    if (revert) {
        params += "&revert=true";
    }
    var self = this;
    new Ajax.Request(appLinks.frameworkCheckResourceModelConfig, {
        parameters:params,
        evalScripts:true,
        evalJSON:true,
        onSuccess:function(req) {
            var data = req.responseJSON;
            if (data.valid) {
                self.renderConfig(elem, type, prefix, index, revert);
            } else {
                self.editConfig(elem, type, prefix, index);
            }
        },
        onFailure:self.error
    });
},
editConfig: function (elem, type, prefix, index) {
    var params = Form.serialize(elem);
    var self = this;
    new Ajax.Updater(elem, appLinks.frameworkEditResourceModelConfig, {
        parameters:params,
        onComplete:function(ajax) {
            if (ajax.request.success()) {
                self.addConfigChrome(elem, type, prefix, index);
            } else {
                self.error(ajax);
            }
        }
    });
},
cancelConfig: function (elem, type, prefix, index) {
    this.hidePicker();
    var li;
    if (elem.tagName == 'li') {
        li = elem;
    } else {
        li = elem.up('li');
    }
    if (li.down('input.isEdit')) {
        //discard changes, submit using original values
        this.checkConfig(elem, type, prefix, index, true);
    } else {
        //cancel new entry
        li.parentNode.removeChild(li);
    }
},
addConfig: function(type) {
    this.hidePickerAll();
    var num = ++this.configCount;
    var prefix = this.prefixKey+'.' + num + '.';
    var wrapper = new Element("li");
    var content = new Element("div");
    wrapper.appendChild(content);
    var self = this;
    $('configs').appendChild(wrapper);
    new Ajax.Updater(content, appLinks.frameworkCreateResourceModelConfig, {
            parameters:{prefix:prefix,type:type},
            onComplete:function(ajax) {
                if (ajax.request.success()) {
                    self.addConfigChrome(content, type, prefix, num + '');
                } else {
                    self.error(ajax);
                }
            }

        }
    );
},
checkForm: function () {
    if ($('configs').down('button.needsSave')) {
        var p=[];
        $('configs').select('button.needsSave').each(function(e) {
            jQuery($(e).up('div.panel')).highlight(2000);
            p.push($(e).up('div.panel'));
        });
        var panel=p[0];
        jQuery(panel).scrollTo(500);
        return false;
    }
    return true;
},
showPicker: function () {
    $('sourcebutton').hide();
    $('sourcepicker').show();
},
hidePicker: function () {
    $('sourcebutton').show();
    $('sourcepicker').hide();
},
hidePickerAll: function() {
    $$('.sourcechrome').each(Element.hide);
},
pageInit:function () {
    var self = this;
    Event.observe($('sourcebutton'), 'click', function(e) {
        Event.stop(e);
        self.showPicker();
    });
    Event.observe($('sourcecancel'), 'click', function(e) {
        Event.stop(e);
        self.hidePicker();
    });
    //load widgets for any in-page configs
    // widgets must be indexed from 1.  PasswordFieldsService depends on this ordering.
    var n = 0;
    $('configs').select('li div.inpageconfig').each(function(e) {
        n++;
        self.addConfigChrome(e, null, '${prefixKey}.' + n + '.', n, true);
    });
    this.configCount=n;
}
});
