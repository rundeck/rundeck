<body class="view">

<section id="section-navbar" style="grid-area: nav; background-color: blue;">
    <div id="navbar"/>
</section>

<section id="section-header" style="grid-area: header; background-color: red;">
    <g:render template="/common/mainbar"/>
</section>

<section style="grid-area: content;">
    <g:layoutBody/>
</section>
%{--    <div class="sidebar" data-background-color="black" data-active-color="white">--}%

%{--        <div class="logo">--}%
%{--            <a class="home"--}%
%{--               href="${grailsApplication.config.rundeck.gui.titleLink ? enc(attr:grailsApplication.config.rundeck.gui.titleLink) : g.createLink(uri: '/')}"--}%
%{--               title="Home">--}%
%{--                <i class="rdicon app-logo"></i>--}%
%{--                <span class="appTitle"></span>--}%
%{--            </a>--}%
%{--            <%----}%
%{--              Saved for review should we switch back to another UI for opening--}%
%{--              and closing the sidebar--}%
%{--              <div class="navbar-minimize">--}%
%{--                <button class="btn btn-sm btn-icon">--}%
%{--                  <i class="fas fa-sign-out-alt fa-flip-horizontal"></i>--}%
%{--                  <i class="fas fa-sign-in-alt"></i>--}%
%{--                </button>--}%
%{--              </div>--}%
%{--            --%>--}%
%{--            <div class="navbar-minimize">--}%
%{--                <a class="triangle">--}%
%{--                    <i class="fas fa-chevron-right"></i>--}%
%{--                    <i class="fas fa-chevron-left"></i>--}%
%{--                </a>--}%
%{--            </div>--}%
%{--        </div>--}%
%{--        <div class="sidebar-wrapper">--}%
%{--            <g:render template="/common/sidebar"/>--}%
%{--            <div class="sidebar-modal-backdrop"></div>--}%
%{--        </div>--}%
%{--    </div>--}%
%{--    <div class="main-panel" id="main-panel">--}%

%{--        <g:render template="/common/mainbar"/>--}%

%{--        <div class="vue-project-motd container-fluid">--}%
%{--            <motd :event-bus="EventBus" tab-page="${enc(attr:pageProperty(name:'meta.tabpage'))}" style="margin-top:15px"></motd>--}%
%{--        </div>--}%

%{--        <g:ifPageProperty name="page.subtitle">--}%
%{--            <nav id="subtitlebar" class="navbar navbar-default subtitlebar standard">--}%
%{--                <div class="container-fluid">--}%
%{--                    <div class="navbar-header">--}%
%{--                        <ul class="nav navbar-nav">--}%
%{--                            <li class="primarylink">--}%
%{--                                <a href="#">--}%
%{--                                    <g:pageProperty name="page.subtitle"/>--}%
%{--                                </a>--}%
%{--                            </li>--}%
%{--                        </ul>--}%
%{--                    </div>--}%
%{--                </div>--}%
%{--            </nav>--}%
%{--        </g:ifPageProperty>--}%
%{--        <g:ifPageProperty name="page.subtitlesection">--}%
%{--            <nav id="subtitlebar" class=" subtitlebar has-content ${pageProperty(name: 'page.subtitlecss')}">--}%

%{--                <g:pageProperty name="page.subtitlesection"/>--}%

%{--            </nav>--}%
%{--        </g:ifPageProperty>--}%
%{--        <div class="content">--}%


%{--            <div id="layoutBody">--}%
%{--                <g:ifPageProperty name="page.searchbarsection">--}%
%{--                    <nav id="searchbar" class=" searchbar has-content ${pageProperty(name: 'page.searchbarcss')}">--}%

%{--                        <g:pageProperty name="page.searchbarsection"/>--}%

%{--                    </nav>--}%
%{--                </g:ifPageProperty>--}%

%{--                <g:layoutBody/>--}%
%{--            </div>--}%
%{--        </div>--}%
%{--        <g:render template="/common/footer"/>--}%
%{--    </div>--}%


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