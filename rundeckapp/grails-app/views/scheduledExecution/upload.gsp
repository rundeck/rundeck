<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="tabpage" content="jobs"/>
    <meta name="layout" content="base"/>
    <title><g:appTitle/> - <g:message code="upload.job.page.title" /></title>
    <script type="text/javascript">
        jQuery(function(){
            jQuery('.act_job_action_dropdown').click(function(){
                var id=jQuery(this).data('jobId');
                var el=jQuery(this).parent().find('.dropdown-menu');
                el.load(_genUrl(appLinks.scheduledExecutionActionMenuFragment,{id:id,jobDeleteSingle:true}));
            });
        });
    </script>
</head>
<body>

    <tmpl:uploadForm />
</body>
</html>
