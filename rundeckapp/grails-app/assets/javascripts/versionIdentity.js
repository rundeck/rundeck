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

    self.serverNameStyle = function (name) {
        if (name === 'underline') {
            return jQuery('<span></span>').css({
                'border': '1px solid transparent',
                'border-image': self.stripeAllBg('90deg', self.version.versionData['sixes'], 20),
                'border-width': '0 0 3px 0',
                'padding': '1px',
                'border-color': 'solid transparent',
                'border-image-slice': '1'
            });
        }
        if (name === 'solid') {

            return jQuery('<span></span>').css({
                'color': '#' + self.version.versionData['sixes'][0],
                'border-radius': '5px',
                'padding': '3px'
                // 'color':'white',
                // 'text-shadow':'1px 1px 3px #333333'
            });
        }
        if (name === 'solidbg') {

            return jQuery('<span></span>').css({
                'background-color': '#' + self.version.versionData['sixes'][0],
                'border-radius': '3px',
                'padding': '2px',
                'color':'white',
                'text-shadow':'1px 1px 2px #000000'
            });
        }
        if (name === 'double-solid') {
            return jQuery('<span></span>').css({
                'color': '#' + self.version.versionData['sixes'][0],
                'background-color': '#' + self.version.versionData['sixes'][1],
                'border-radius': '5px',
                'padding': '3px',
                'text-shadow':'1px 1px 3px #333333'
            });
        }
        return jQuery('<span></span>');
    };
    self.showServerName=function(dom){
        if(!data.serverUuid){
            return;
        }
        var domdata = jQuery(dom).data();
        var uuidSize = (domdata && domdata['uuidSize']) || 2;
        var color='#'+((self.version.versionData['hexuuid0'].substring(0,6)));
        var icon=self.version.iconForVersion(self.version.versionData['uuid0']);
        var codename=[
            icon,
            self.version.versionData['uuid'].substring(0, uuidSize)
        ].join('-').toLowerCase();

        var shortname = domdata['uuidLabelNone']?'':self.version.versionData['uuid'].substring(0, uuidSize);
        var truncname = self.version.versionData['uuid'].substring(0, domdata && domdata['nameTruncated']||0);
        var name=domdata.nameTruncated?truncname:self.version.serverName?self.version.serverName:'';
        var glyphicon= jQuery('<span></span>');
        var ispan = jQuery('<span></span>');
        ispan.append(jQuery('<i></i>').addClass('glyphicon glyphicon-' + icon));
        glyphicon.append(ispan);
        var nametext= jQuery('<span></span>').text(' '+name+' ');

        var namestyle = self.serverNameStyle(domdata['nameStyle'] || 'plain');
        var idstyle = self.serverNameStyle(domdata['uuidStyle'] || 'solid');

        var span=jQuery('<span></span>')
                .attr('title',codename+' / '+data.serverUuid)

                .append(
                    namestyle
                        .append(nametext)
                ).append(
                    idstyle
                        .append(glyphicon)
                        .append(' '+(self.version.serverName?shortname:codename))
                )

            .append(jQuery('<span></span>').text(' '))
            ;

        jQuery(dom).append(span);

    };
};
(function(){
    jQuery(function(){
        jQuery('.rundeck-server-uuid').each(function () {
            new VersionIdentity(jQuery(this).data()).showServerName(this);
        });
    });
})();
