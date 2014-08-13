<g:set var="boxrkey" value="${g.rkey()}"/>
<script type="text/javascript">
    if(typeof(_updateBoxInfo)=='function'){
        _updateBoxInfo('${enc(js:name)}',${enc(json:model)});
    }
</script>
