
<script type="text/javascript">
    boxdata={
        <g:each in="${model}" var="item">
        ${item.key}: "${item?.value?.encodeAsJavaScript()}",
        </g:each>
    };
    if(typeof(_updateBoxInfo)=='function'){
        _updateBoxInfo('${name}',boxdata);
    }
</script>