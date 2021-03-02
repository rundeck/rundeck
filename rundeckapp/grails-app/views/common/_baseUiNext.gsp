<body class="view">

<g:set var="projectName" value="${params.project ?: request.project}"/>

<section id="section-header" style="grid-area: header; background-color: red;">
    <g:render template="/common/mainbar"/>
</section>


<section id="section-main" class="${projectName ? 'with-project' : ''}" style="grid-area: main;">
    <g:if test="${projectName}">
        <section id="section-navbar" style="grid-area: nav;}">
            <div id="navbar"/>
        </section>
    </g:if>

    <section id="section-content" style="grid-area: content;">
        <g:ifPageProperty name="page.subtitle">
            <nav id="subtitlebar" class="navbar navbar-default subtitlebar standard">
                <div class="container-fluid">
                    <div class="navbar-header">
                        <ul class="nav navbar-nav">
                            <li conclass="primarylink">
                                <a href="#">
                                    <g:pageProperty name="page.subtitle"/>
                                </a>
                            </li>
                        </ul>
                    </div>
                </div>
            </nav>
        </g:ifPageProperty>
        <g:ifPageProperty name="page.subtitlesection">
            <nav id="subtitlebar" class=" subtitlebar has-content ${pageProperty(name: 'page.subtitlecss')}">

                <g:pageProperty name="page.subtitlesection"/>

            </nav>
        </g:ifPageProperty>

        <div class="vue-project-motd container-fluid">
            <motd :event-bus="EventBus" tab-page="${enc(attr:pageProperty(name:'meta.tabpage'))}" style="margin-top:15px"></motd>
        </div>

        <div class="content">
        <div id="layoutBody">
        <g:ifPageProperty name="page.searchbarsection">
            <nav id="searchbar" class=" searchbar has-content ${pageProperty(name: 'page.searchbarcss')}">

                <g:pageProperty name="page.searchbarsection"/>

            </nav>
        </g:ifPageProperty>
        <g:layoutBody/>
        <g:render template="/common/footer"/>
        </div>
        </div>
    </section>
</section>

<g:if test="${uiplugins && uipluginsPath && params.uiplugins!='false'}">
    <script type="text/javascript" defer>
        //call after gsp page has loaded javascript
        jQuery(function(){window.rundeckPage.onPageLoad();});
    </script>
</g:if>

<!-- VUE JS MODULES -->
<asset:javascript src="static/components/motd.js"/>
<asset:javascript src="static/components/version.js"/>
<asset:javascript src="static/components/tour.js"/>
<g:if test="${grailsApplication.config.rundeck.communityNews.disabled.isEmpty() ||!grailsApplication.config.rundeck.communityNews.disabled in [false,'false']}">
    <asset:javascript src="static/components/community-news-notification.js"/>
</g:if>

<!-- /VUE JS MODULES -->
</body>