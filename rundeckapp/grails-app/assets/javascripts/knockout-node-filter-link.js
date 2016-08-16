/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

function NodeFilterLinkParams(params){
    var self=this;
    self.linkicon=params.linkicon||'';
    self.classnames=params.classnames||'';
    self.filterval=params.filterval||'';
    self.filterkey=params.filterkey||'';
    self.filter=params.filter||'';
    self.linktext=params.linktext||'';
    self.textcss=params.textcss||'';
    self.suffix=params.suffix||'';
    self.tag=params.tag;
    self.title=params.title||'';
    self.filterData=function($root){
        if(self.filter){
            return {
                'data-node-filter':ko.unwrap(self.filter),
                href:$root.linkForFilterString(ko.unwrap(self.filter))
            }
        }else{
            return {
                'data-node-filter': $root.escapeFilter(self.filterkey + ':')+' ' + $root.escapeFilter(ko.unwrap(self.filterval)) ,
                href: $root.linkForFilterParams(self.filterkey,ko.unwrap(self.filterval))
            };
        }
    };
    self.attributes=function($root){
        var data= self.filterData($root);
        if(self.tag){
            data['data-node-tag']=ko.unwrap(self.tag);
        }
        if(self.title){
            data['title']=ko.unwrap(self.title);
        }
        return data;
    };
    self.viewtext=function(){
        return (self.linktext? ko.unwrap(self.linktext) : self.filter?ko.unwrap(self.filter):ko.unwrap(self.filterval))+ko.unwrap(self.suffix);
    };
}
ko.components.register('node-filter-link', {
    viewModel:NodeFilterLinkParams,
    template: '<a  class="nodefilterlink"  href="#"  data-bind="attr: attributes($root),  css: classnames"  > \
    <span data-bind="if: linkicon"><i data-bind="css: linkicon"></i></span>\
<span data-bind="if: !linkicon"><span data-bind="text: viewtext(), css: textcss"></span></span></a>'
});