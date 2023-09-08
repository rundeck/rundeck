<g:set var="enabled" value="${ workflow !== null && workflow.commands ?  workflow?.commands?.count{ it.enabled } as Integer : 0}" />
<g:set var="disabled" value="${ workflow !== null && workflow.commands  ?  workflow?.commands?.count{ !it.enabled } as Integer : 0 }" />
<div id="stepDashboard"
     style="width: 100%; height: 150px; border: 1.5px solid lightgray; border-radius: 5px; margin-bottom: 2rem; margin-top: 2rem;">
    <div style="width: 100%;height: 100%;padding: 1rem; display: flex;justify-content: center;font-weight: bold;flex-direction: column;">
        <p style="width: 100%; font-weight: normal;">Step Dashboard</p>
        <div style="width: 100%;height: 90%; display: flex;justify-content: center;align-items: center;">
            <div
                    style="width: 30%; height: 100%;display: flex;flex-direction: column;align-items: center;justify-content: center;font-size: 20px;font-weight: bold;">
                <h6 style="font-weight: lighter;">Total</h6>

                <span id="totalStepDashboardData" class="label label-secondary has_tooltip"
                      data-container="#section-content"
                      data-placement="auto bottom"
                      title="${enabled+disabled} steps set in job"
                      style="font-size: 30px;"
                   >${enabled + disabled}</span>
            </div>

            <div style="width: 30%; height: 100%;display: flex;flex-direction: column;align-items: center;justify-content: center;font-size: 20px;font-weight: bold;">
                <h6 style="font-weight: lighter;">Enabled</h6>

                <span id="enabledStepDashboard" class="label label-secondary has_tooltip"
                      data-container="#section-content"
                      data-placement="auto bottom"
                      title="${enabled} enabled steps in job"
                      style="font-size: 30px;">${enabled}</span>
            </div>

            <div style="width: 30%; height: 100%;display: flex;flex-direction: column;align-items: center;justify-content: center;font-size: 20px;font-weight: bold;">
                <h6 style="font-weight: lighter;">Disabled</h6>

                <span id="disabledStepDashboard" class="label label-secondary has_tooltip"
                      data-container="#section-content"
                      data-placement="auto bottom"
                      title="${disabled} disabled steps in job"
                      style="font-size: 30px;">${disabled}</span>
            </div>
        </div>
    </div>
</div>
<script lang="text/javascript">
</script>