<g:set var="enabled" value="${ workflow !== null && workflow.commands ?  workflow?.commands?.count{ it.enabled } as Integer : 0}" />
<g:set var="disabled" value="${ workflow !== null && workflow.commands  ?  workflow?.commands?.count{ !it.enabled } as Integer : 0 }" />
<div class="form-inline" style="margin-left: 1rem;display: inline-flex;width: 300px; justify-content: space-around;height: 40px;border: 0.5px solid white;border-radius: 5px;padding-top: 10px;">
    <p>Steps: </p>
    <p>Total <span class="label label-secondary" style="font-weight: bolder">${enabled + disabled}</span></p>
    <p>Enabled <span class="label label-secondary" style="font-weight: bolder">${enabled}</span></p>
    <p>Disabled <span class="label label-secondary" style="font-weight: bolder">${disabled}</span></p>
</div>
<script lang="text/javascript">
</script>