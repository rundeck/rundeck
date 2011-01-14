
<script type="text/javascript">
    boxdata={
        <g:each in="${model}" var="item" status="i">
        <g:if test="${i>0}">,</g:if>${item.key.encodeAsJavaScript()}: "${item?.value?.encodeAsJavaScript()}"
        </g:each>
    };
    if(typeof(_updateBoxInfo)=='function'){
        _updateBoxInfo('${name}',boxdata);
    }
</script>