/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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
//= require version
var VersionIdentity=function(data){
    var self=this;
    self.version = new RundeckVersion(data);

    self.showVersionIdentity=function(dom){
        var color=self.version.color();
        var name=self.version.name();
        var icon=self.version.icon();
        var text=self.version.text();
        var data=self.version.data();
        var span2= jQuery('<span></span>');
        var ispan = jQuery('<span></span>').addClass('version-icon').css({ 'color': color});
        ispan.append(jQuery('<i></i>').addClass('glyphicon glyphicon-' + icon));
        span2.append(ispan);
        var span3 = jQuery('<span></span>').addClass('rundeck-version-name').text(text);
        if(data.tag && data.tag!='GA'){
            ispan.append(span3);
        }else{
            span2.append(span3)
        }
        var span=jQuery('<span></span>').attr('title',self.version.appId+' '+self.version.versionString +' ('+text+')' + (self.version.versionDate?' '+self.version.versionDate:'')).append(span2);

        if (data.tag && data.tag != 'GA') {
            var s = data.tag.toLowerCase();
            if(s=~/snapshot/i && self.version.versionDate){
                s = s.replace(/snapshot/i,self.version.versionDate);
            }
            span.append(jQuery('<span></span>').addClass('badge badge-default').text(' ' + s));
        } else if (self.version.versionDate) {
            var vdate = jQuery('<span></span>').addClass('rundeck-version-date').text(' ' + self.version.versionDate);
            span.append(vdate);
        }
        jQuery(dom).append(span);
    };
    self.stripeBg=function(color,px1,colorb,px2){
        return "repeating-linear-gradient(" +
            "-45deg, "+
            color+", "+
            color+" "+px1+"px, "+
            colorb+" "+px1+"px, "+
            colorb+" "+px2+"px "+
            ")"
            ;
    };
    self.stripeAllBg=function(angle,colors,width){
        var s="repeating-linear-gradient("+angle+", ";
        var w=0;
        for(var x=0;x<colors.length;x++){
            if(x>0){
                s+=', '
            }
            s+='#'+colors[x]+" "+w+"px, ";
            w+=width;
            s+='#'+colors[x]+" "+w+"px";
        }
        return s+")";

    };
    self.showVersionBlock=function(dom){
        var color=self.version.color();
        var name=self.version.name();
        var icon=self.version.icon();
        var text=self.version.text();
        var data=self.version.data();
        var span2= jQuery('<span></span>');
        var ispan = jQuery('<span></span>').addClass('version-icon');
        ispan.append(jQuery('<i></i>').addClass('glyphicon glyphicon-' + icon));
        span2.append(ispan);
        var span3 = jQuery('<span></span>').addClass('rundeck-version-name').text(text);
        span2.append(span3);
        var span=jQuery('<div></div>')
            .text(self.version.appId+' '+self.version.versionString+' ' )
            .append(span2);
        if (data.tag && data.tag != 'GA') {
            jQuery(dom).css({ 'background': self.stripeBg(color,15,'#5c5c5c',20), 'color': 'white'}).append(span);
        }else{
            jQuery(dom).css({ 'background': color, 'color': 'white'}).append(span);
        }
        if (self.version.versionDate) {
            var vdate = jQuery('<span></span>').addClass('rundeck-version-date').text(' ' + self.version.versionDate);
            span.append(vdate);
        }
    };
    self.showServerName=function(dom){
        if(!data.serverUuid){
            return;
        }
        var color='#'+((self.version.versionData['hexuuid0'].substring(0,6)));
        var icon=self.version.iconForVersion(self.version.versionData['uuid0']);
        var codename=[
            icon,
            self.version.versionData['hexuuid0'].substring(0,2)
        ].join('-').toLowerCase();
        var name=self.version.serverName?self.version.serverName:'';
        var shortname=self.version.versionData['hexuuid0'].substring(0,2);
        var nodeicon= jQuery('<i></i>').addClass('rdicon node node-runnable icon-small');
        var glyphicon= jQuery('<span></span>');
        var ispan = jQuery('<span></span>');
        ispan.append(jQuery('<i></i>').addClass('glyphicon glyphicon-' + icon));
        glyphicon.append(ispan);
        var nametext= jQuery('<span></span>').text(' '+name+' ');
        var colorpill= jQuery('<span></span>').css({
            'border':'1px solid transparent',
            'border-image': self.stripeAllBg('90deg',self.version.versionData['sixes'],20),
            'border-width':'0 0 2px 0',
            'padding': '1px',
            'border-color':'solid transparent',
            'border-image-slice':'1'
            //'color': 'white',
            //'text-shadow': '1px 1px 3px #333333'
        });
        var span=jQuery('<span></span>')
                .attr('title',codename+' / '+data.serverUuid)
                //.addClass('version-icon')
                .css({
                    //'color': color
                    //'text-shadow': '1px 1px 3px #333333'
                })
            .append(
                colorpill
                    .append(nametext)
                    .append(glyphicon)
                    .append(' '+(self.version.serverName?shortname:codename))
            )
            //.append(nodeicon)

            .append(jQuery('<span></span>').text(' '))
            ;
            jQuery(dom).append(span);
            //jQuery(dom).append(jQuery('<a></a>').attr('href','/menu/systemInfo').append(span));
    };
};
(function(){
    jQuery(function(){
        jQuery('.rundeck-version-identity').each(function () {
            new VersionIdentity(jQuery(this).data()).showVersionIdentity(this);
        });
        jQuery('.rundeck-version-block').each(function () {
            new VersionIdentity(jQuery(this).data()).showVersionBlock(this);
        });
        jQuery('.rundeck-server-uuid').each(function () {
            new VersionIdentity(jQuery(this).data()).showServerName(this);
        });
    });
})();
