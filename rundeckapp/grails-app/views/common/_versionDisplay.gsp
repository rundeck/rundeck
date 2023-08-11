<div class="vue-app-version-display">
    <version-display version="${enc(attr: servletContextAttribute(attribute: 'app.ident'))}"
                     date="${enc(attr: servletContextAttribute(attribute: 'version.date_string'))}"
                     app-name="${enc(attr: g.appTitle())}"
                     :show-name="${showName?:true}"
                     :show-date="${showDate?:true}"
                     :show-relative-date="${showRelativeDate?:true}"
                     :show-tooltip="${showTooltip?:false}"
    ></version-display>
</div>
