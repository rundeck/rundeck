class BuilderUtil{
    ArrayList context
    public BuilderUtil(){
        context=new ArrayList()
    }
    
    public mapToDom( Map map, builder){
        //generate a builder strucure using the map components
        for(Object o: map.keySet()){
            final Object val = map.get(o)
            this.objToDom(o,val,builder)
        }
    }
    public objToDom(key,obj,builder){
        if(null==obj){
            builder."${key}"()
        }else if (obj instanceof Collection){
            //iterate
            if(key instanceof String && ((String)key).length()>1 && ((String)key).endsWith('s')){
                String keys=(String)key
                String name=keys-'s';
                builder."${key}"(){
                    for(Object o: (Collection)obj){
                        this.objToDom(name,o,builder)
                    }
                }
            }else{
                for(Object o: (Collection)obj){
                    this.objToDom(key,o,builder)
                }
            }
        }else if(obj instanceof Map){
            //try to collect '@' prefixed keys to apply as attributes
            Map map = (Map)obj
            def attrs = map.keySet().findAll{it=~/^@/}
            def attrmap=[:]
            if(attrs){
                attrs.each{String s->
                    def x =s.substring(1)
                    attrmap[x]=map.remove(s)
                }
            }
            System.err.println("attrmap: ${attrmap}, map: ${map}");
            builder."${key}"(attrmap){
                this.mapToDom(map,delegate)
            }
        }else if(obj instanceof String){
            String os=(String)obj
            builder."${key}"(os)
        }else if(obj.metaClass.respondsTo(obj,'toMap')){
            def map = obj.toMap()
            builder."${key}"(){
                this.mapToDom(map,delegate)
            }
        }else {
            String os=obj.toString()
            builder."${key}"(os)
        }
    }
}