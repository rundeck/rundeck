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
var VersionIdentity=function(data){
    var self=this;
    self.versionString= data['versionString'];
    self.versionData= {};
    self.colorIdentity=data && data.colorIdentity ? data.colorIdentity : 'minorPoint';
    self.nameIdentity= data && data.nameIdentity ? data.nameIdentity : 'majorMinor';
    self.iconIdentity= data && data.iconIdentity ? data.iconIdentity : 'minorPoint';
    self.appId=data && data['appId'] ? data['appId'] : 'Rundeck';
    self.serverName=data && data['serverName'] ? data['serverName'] : null;
    self.csscolors= [
        'BlueViolet',
        'CadetBlue',
        'Chocolate',
        'CornflowerBlue',
        'Crimson',
        'DodgerBlue',
        'FireBrick',
        'ForestGreen',
        'Fuchsia',
        'Goldenrod',
        'HotPink',
        'Indigo',
        'LimeGreen',
        'Magenta',
        'Maroon',
        'MidnightBlue',
        'Navy',
        'Olive',
        'OrangeRed',
        'Purple',
        'RoyalBlue',
        'SaddleBrown',
        'SeaGreen',
        'Sienna',
        'SlateBlue',
        'SteelBlue',
        'Teal',
        'Tomato',
        'Violet'
    ];

    self.glyphicons = [
        'bell',
        'book',
        'briefcase',
        'bullhorn',
        'camera',
        'cutlery',
        'flag',
        'flash',
        'gift',
        'globe',
        'headphones',
        'leaf',
        'music',
        'paperclip',
        'phone',
        'plane',
        'pushpin',
        'tower',
        'glass',
        'knight',
        'tent',
        'apple',
        'lamp',
        'piggy-bank',
        'grain',
        'sunglasses'
    ];

    self.names=[
        'Americano',
        'Cafe Au Lait',
        'Cafe Bonbon',
        'Cafecito',
        'Cafe Cubano',
        'Caffe Latte',
        'Cafe Mocha',
        'Cappuccino',
        'Caramel Latte',
        'Coconut Latte',
        'Con Panna',
        'Doppio Espresso',
        'Dry Cappuccino',
        'Espresso Breve',
        'Eye Opener',
        'Hammerhead',
        'Macchiato',
        'Pumpkin Spice Latte',
        'Ristretto',
        'Solo Espresso',
        'Toffee Latte',
        'Turkish Coffee',
        'Vanilla Latte',
        'Viennese Espresso'
    ];
    function splitVersion(versionString){
        var partsa=String(versionString).split(' ');
        var version = partsa.length > 1 ? partsa[0] : versionString;
        var parts=String(version).split('-');
        var vparts=parts[0].split('\.');
        var data={version:version};
        if(vparts.length>0){
            data['major']=parseInt(vparts[0]);
        }else{
            data['major']=0;
        }
        if(vparts.length>1){
            data['minor']=parseInt(vparts[1]);
        }else{
            data['minor'] =0;
        }
        data['majorMinor']=(data.major*10) + data.minor;
        if(vparts.length>2){
            data['point']=parseInt(vparts[2]);
        }else{
            data['point'] =0;
        }
        data['minorPoint'] = (data.minor * 10) + data.point;
        var release=1;
        var tag = '';
        if(parts.length > 1 && /^\d+$/.test(parts[1]) ){
            release=parseInt(parts[1]);
            tag = parts.length > 2 ? parts[2] : '';
        }else if(parts.length>1){
            tag=parts[1];
        }

        data['tag']=tag;
        data['release']=release;
        data['pointRelease']= data.point*10 + release;
        data['minorPointRelease']= (data.minor * 100)+ data.point*10 + release;
        data['full']=data.major*100 + data.minor*10+data.point;
        return data;
    }
    function splitUUID(versionString) {
        var partsa = String(versionString).split('-');
        var apart = partsa.length > 0 ? partsa[0].substring(0,2) : versionString;
        var data = {};
        for(var i=0;i<partsa.length;i++){
            data['uuid'+i]=parseInt(partsa[i].substring(0,2),16);
            data['hexuuid'+i]=partsa[i];
        }
        var partsb = partsa.join('');
        var sixes=[];
        for(var j=0;(j+1)*6<partsb.length;j++){
            data['6let'+i]=partsb.substring(j*6,(j+1)*6);
            sixes.push(partsb.substring(j*6,(j+1)*6));
        }
        data['sixes']=sixes;
        return data;
    }
    function inList(list,val){
        return list[val % list.length];
    }
    function colorForVersion(val){
        return inList(self.csscolors,val);
    }
    function nameForVersion(val){
        return inList(self.names,val);
    }
    function iconForVersion(val){
        return inList(self.glyphicons,val);
    }
    self.data=function(){
        return self.versionData;
    };
    self.color=function(){
        return colorForVersion(self.versionData[self.colorIdentity]);
    };
    self.name=function(){
        return nameForVersion(self.versionData[self.nameIdentity]);
    };
    self.icon=function(){
        return iconForVersion(self.versionData[self.iconIdentity]);
    };
    self.text=function(){
        var sep=' ';
        return [self.name(),self.color(),self.icon() ].join(sep).toLowerCase().replace(/[^a-z]/g, sep);
    };
    self.showVersionIdentity=function(dom){
        var color=self.color();
        var name=self.name();
        var icon=self.icon();
        var text=self.text();
        var data=self.data();
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
        var span=jQuery('<span></span>').attr('title',self.appId+' '+self.versionString +' ('+text+')').append(span2);
        if (data.tag && data.tag != 'GA') {
            span.append(jQuery('<span></span>').addClass('badge badge-info').text(' ' + data.tag.toLowerCase()));
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
        var color=self.color();
        var name=self.name();
        var icon=self.icon();
        var text=self.text();
        var data=self.data();
        var span2= jQuery('<span></span>');
        var ispan = jQuery('<span></span>').addClass('version-icon');
        ispan.append(jQuery('<i></i>').addClass('glyphicon glyphicon-' + icon));
        span2.append(ispan);
        var span3 = jQuery('<span></span>').addClass('rundeck-version-name').text(text);
        span2.append(span3)
        var span=jQuery('<div></div>')
            .text(self.appId+' '+self.versionString+' ' )
            .append(span2);
        if (data.tag && data.tag != 'GA') {
            jQuery(dom).css({ 'background': self.stripeBg(color,15,'#5c5c5c',20), 'color': 'white'}).append(span);
        }else{
            jQuery(dom).css({ 'background': color, 'color': 'white'}).append(span);
        }
    };
    self.showServerName=function(dom){
        if(!data.serverUuid){
            return;
        }
        var color='#'+((self.versionData['hexuuid0'].substring(0,6)));
        var icon=iconForVersion(self.versionData['uuid0']);
        var codename=[
            icon,
            self.versionData['hexuuid0'].substring(0,2)
        ].join('-').toLowerCase();
        var name=self.serverName?self.serverName:'';
        var shortname=self.versionData['hexuuid0'].substring(0,2);
        var nodeicon= jQuery('<i></i>').addClass('rdicon node node-runnable icon-small');
        var glyphicon= jQuery('<span></span>');
        var ispan = jQuery('<span></span>');
        ispan.append(jQuery('<i></i>').addClass('glyphicon glyphicon-' + icon));
        glyphicon.append(ispan);
        var nametext= jQuery('<span></span>').text(' '+name+' ');
        var colorpill= jQuery('<span></span>').css({
            'border-image': self.stripeAllBg('90deg',self.versionData['sixes'],20),
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
                    .append(' '+(self.serverName?shortname:codename))
            )
            //.append(nodeicon)

            .append(jQuery('<span></span>').text(' '))
            ;
            jQuery(dom).append(span);
            //jQuery(dom).append(jQuery('<a></a>').attr('href','/menu/systemInfo').append(span));
    };
    if(self.versionString) {
        self.versionData = splitVersion(self.versionString);
    }else if(data.serverUuid){
        self.versionData = splitUUID(data.serverUuid);
    }
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
