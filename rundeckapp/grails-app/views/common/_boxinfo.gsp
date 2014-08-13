<g:set var="boxrkey" value="${g.rkey()}"/>
<g:embedJSON id="box_data_${boxrkey}" data="${model}"/>
<script type="text/javascript">
    if(typeof(_updateBoxInfo)=='function'){
        _updateBoxInfo('${enc(js:name)}',loadJsonData('box_data_${enc(js:boxrkey)}'));
    }
</script>
