<div id="stepDashboard"
     style="width: 100%; height: 150px; border: 1.5px solid lightgray; border-radius: 5px; margin-bottom: 2rem; margin-top: 2rem;">
    <div style="width: 100%;height: 100%;padding: 1rem; display: flex;justify-content: center;font-weight: bold;flex-direction: column;">
        <p style="width: 100%;">Step Dashboard</p>
        <div style="width: 100%;height: 90%; display: flex;justify-content: center;align-items: center;">
            <div
                    style="width: 30%; height: 100%;display: flex;flex-direction: column;align-items: center;justify-content: center;font-size: 20px;font-weight: bold;">
                <h6>Total</h6>

                <p id="totalStepDashboardData"
                   style="font-size: 25px;">${workflow?.commands?.size()}</p>
            </div>

            <div style="width: 30%; height: 100%;display: flex;flex-direction: column;align-items: center;justify-content: center;font-size: 20px;font-weight: bold;">
                <h6>Enabled</h6>

                <p id="enabledStepDashboard"
                   style="font-size: 25px;">${workflow?.commands.count { it.enabled } }</p>
            </div>

            <div style="width: 30%; height: 100%;display: flex;flex-direction: column;align-items: center;justify-content: center;font-size: 20px;font-weight: bold;">
                <h6>Disabled</h6>

                <p id="disabledStepDashboard"
                   style="font-size: 25px;">${workflow?.commands.count { !it.enabled } }</p>
            </div>
        </div>
    </div>
</div>
<script lang="text/javascript">
</script>