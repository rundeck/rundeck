<g:set var="enabled" value="${ workflow !== null && workflow.commands ?  workflow?.commands?.count{ it.enabled } as Integer : 0}" />
<g:set var="disabled" value="${ workflow !== null && workflow.commands  ?  workflow?.commands?.count{ !it.enabled } as Integer : 0 }" />
<div id="stepDashboard"
     style="width: 100%; height: 150px; border: 1.5px solid lightgray; border-radius: 5px; margin-bottom: 2rem; margin-top: 2rem;">
    <div style="width: 100%;height: 100%;padding: 1rem; display: flex;justify-content: center;font-weight: bold;flex-direction: column;">
        <p style="width: 100%; font-weight: normal;">Step Dashboard</p>
        <div style="width: 100%;height: 90%; display: flex;justify-content: center;align-items: center;">
            <div
                    style="width: 30%; height: 100%;display: flex;flex-direction: column;align-items: center;justify-content: center;font-size: 20px;font-weight: bold;">
                <h6>Total</h6>

                <span id="totalStepDashboardData"
                   style="font-size: 30px;">${enabled + disabled}</span>
            </div>

            <div style="width: 30%; height: 100%;display: flex;flex-direction: column;align-items: center;justify-content: center;font-size: 20px;font-weight: bold;">
                <h6>Enabled</h6>

                <span id="enabledStepDashboard"
                   style="font-size: 30px;">${enabled}</span>
            </div>

            <div style="width: 30%; height: 100%;display: flex;flex-direction: column;align-items: center;justify-content: center;font-size: 20px;font-weight: bold;">
                <h6>Disabled</h6>

                <span id="disabledStepDashboard"
                   style="font-size: 30px;">${disabled}</span>
            </div>
        </div>
    </div>
</div>
<script lang="text/javascript">
</script>