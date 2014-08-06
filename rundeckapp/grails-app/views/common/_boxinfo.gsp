
<script type="text/javascript">
    boxdata={
        <g:each in="${model}" var="item" status="i">
        <g:if test="${i>0}">,</g:if>${g.enc(js:item.key)}: "${g.enc(js:item?.value)}"
        </g:each>
    };
    if(typeof(_updateBoxInfo)=='function'){
        _updateBoxInfo('${name}',boxdata);
    }
</script>
