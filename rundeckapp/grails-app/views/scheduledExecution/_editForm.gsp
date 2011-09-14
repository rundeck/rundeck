<div class="pageBody form">
    <g:render template="/common/errorFragment"/>
    <g:form controller="scheduledExecution" method="post" onsubmit="if(typeof(validateJobEditForm)=='function'){return validateJobEditForm(this);}">
        <g:render template="edit" model="[scheduledExecution:scheduledExecution, crontab:crontab, command:command,authorized:authorized]"/>

        <div class="buttons">

            <g:actionSubmit id="editFormCancelButton" value="Cancel"  onclick="if(typeof(jobEditCancelled)=='function'){jobEditCancelled();}"/>
            <g:actionSubmit value="Update"/>

        </div>
    </g:form>
</div>