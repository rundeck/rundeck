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
    configCount: 0,
    prefixKey: 'x',
    initialize: function(pfix) {
        if(pfix){
            this.prefixKey=pfix;
        }
    },
    addConfigChrome: function (elem, type, prefix, index, edit) {

    var parentNode = $(elem).parentNode;
    var top = new Element("div");
    var wrapper = new Element("div");
    top.appendChild(wrapper);
    $(parentNode).insert(top, {after:elem});
    var content = parentNode.removeChild(elem);
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

    var buttons = new Element("div");
    buttons.setStyle({"text-align":"right"});

    if (edit) {
        wrapper.addClassName("rounded");
        var self=this;
        var button;
        var isinvalid = $(elem).down(".invalidProvider");
        if(!isinvalid){
            button = new Element("button");
            Event.observe(button, 'click', function(e) {
                Event.stop(e);
                self.editConfig(top, type, prefix, index);
            });
            button.innerHTML = "Edit";
        }

        var cancelbutton = new Element("button");
        Event.observe(cancelbutton, 'click', function(e) {
            Event.stop(e);
            self.cancelConfig(top);
        });
        cancelbutton.innerHTML = "Delete";

        buttons.appendChild(cancelbutton);
        if(button){
            buttons.appendChild(button);
        }
    } else {
        var self = this;
        wrapper.addClassName("popout");
        var button = new Element("button");
        Event.observe(button, 'click', function(e) {
            Event.stop(e);
            self.checkConfig(top, type, prefix, index);
        });
        button.innerHTML = "Save";
        button.addClassName("needsSave");

        var cancelbutton = new Element("button");
        Event.observe(cancelbutton, 'click', function(e) {
            Event.stop(e);
            self.cancelConfig(top, type, prefix, index);
        });
        cancelbutton.innerHTML = "Cancel";

        buttons.appendChild(cancelbutton);
        buttons.appendChild(button);
    }

    content.insert(hidden3);

    wrapper.appendChild(content);

    wrapper.appendChild(buttons);

},
error: function(req) {
    var data = req.responseJSON;
    if($('errors')){
        $('errors').innerHTML=req;
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
        $('configs').select('button.needsSave').each(function(e) {
            new Effect.Highlight($(e).up('div.popout'));
        });
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
    var n = 0;
    $('configs').select('li div.inpageconfig').each(function(e) {
        n++;
        self.addConfigChrome(e, null, '${prefixKey}.' + n + '.', n, true);
    });
    this.configCount=n;
}
});