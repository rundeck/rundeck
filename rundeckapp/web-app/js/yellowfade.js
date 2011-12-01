
/* yellow fade technique */
function doyft(id){
    yellowfade(id,0.0,1500,20,500,false);
}
/* fade to light gray instead of white */
function doyftg(id){
    yellowfade(id,0.0,1500,20,500,false,null,new Array(0xf0,0xf0,0xf0));
}
function tohex(x){
   var hex=["0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"];
   return hex[Math.round((x & 0xF0)/16.0)]+hex[Math.round(x)%16.0];
}
function yellowfade(id,perc,time,rate,ramp,test,rgb1,rgb2){
    /**
     * id: id of element on the page to perform the fade on
     * perc: current percentage for the fade (from 0.0 to 1.0)
     * time: duration of the fade in ms
     * rate: frame rate in fps
     * ramp: duration of delay at 0% before beginning fade (in ms)
     * test: true to show test information
     * rgb1: array of (r,g,b) for beginning color (optional: default is 255,255,0)
     * rgb2: array off (r,g,b) for finishing color (optional: default is 255,255,255)
     */
    if(perc>1.0){
        perc=1.0;
    }
    var rgbstart=null==rgb1? new Array(255,255,153):rgb1;
    var rgbend = null==rgb2? new Array(255,255,255):rgb2;

    var bgcol="#";
    for(var i=0;i<rgbstart.length;i++){
        if(rgbstart[i]!=rgbend[i]){
            bgcol+=tohex(rgbstart[i] + (rgbend[i]-rgbstart[i])*perc);
        }else{
            bgcol+=tohex(rgbstart[i]);
        }
    }

    $(id).style.background=bgcol;

    if(test){
        $(id).innerHTML="bg: "+bgcol+", perc: "+perc+", time: "+time+", rate: "+rate+", ramp: "+ramp;
    }
    if(perc<1.0){
        var newperc = ((perc*time)+(1000/rate))/time;
        var newramp=ramp;
        if(ramp>0){
            newramp = ramp-(1000/rate);
            newperc=perc;
        }
        if(test){
        $(id).innerHTML+=", newperc: "+newperc+", newramp: "+newramp+", delay: "+(1000/rate) ;
        }
        var tostr = "yellowfade('"+ $(id).identify()+"',"+newperc+","+time+","+rate+","+newramp+", "+test+",new Array("+rgbstart[0]+","+rgbstart[1]+","+rgbstart[2]+"),new Array("+rgbend[0]+","+rgbend[1]+","+rgbend[2]+"));";
        if(test){
            $(id).innerHTML+="<br>"+tostr;
        }
        setTimeout(tostr,1000/rate);
    }
}