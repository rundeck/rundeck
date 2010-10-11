<div id="pageMessage" style="display: none;"></div>
<script type="text/javascript">
        function showPageMessage(text,elem){
            var name='pageMessage';
            if($(elem)){
                name=elem;
            }
            if($(text)){
                var it =$(text).parentNode.removeChild($(text));
                $(name).appendChild(it);
                $(it).show();
                Element.show(name);
            }else{
                $(name).innerHTML=text;
                Element.show(name);
            }


        }
        function showPageError(text,elem){
            var name='pageMessage';
            if($(elem)){
                name=elem;
            }
            $(name).addClassName('error');
            if($(text)){
                var it =$(text).parentNode.removeChild($(text));
                $(name).appendChild(it);
                $(it).show();
                Element.show(name);
            }else{
                $(name).innerHTML=text;
                Element.show(name);
            }


        }
</script>