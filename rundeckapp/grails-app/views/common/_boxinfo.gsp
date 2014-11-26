<g:set var="boxrkey" value="${g.rkey()}"/>
<% response.addHeader("X-Rundeck-data-id", "box_data_${boxrkey}") %>
<g:embedJSON id="box_data_${boxrkey}" data="${[name:name,content: model]}"/>
<script type="text/javascript">
    if(typeof(_updateBoxInfo)=='function'){
        var data= loadJsonData('box_data_${enc(js:boxrkey)}');
        _updateBoxInfo(data.name,data.content);
    }
</script>
