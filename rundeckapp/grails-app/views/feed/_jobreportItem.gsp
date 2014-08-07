<g:render template="/reports/baseReport" model="${[reports: [report], absoluteLinks:true, hideDate:true, hideShowLink:true, hideEdit:true, options:[summary:true]]}"/>
<style>
table td, table th {
    padding: 2px;
    font-weight: normal;
}

table.jobsList.history tr.fail td.eventtitle {
    color: red;
}

tr.sectionhead td.eventtitle.adhoc {
    font-style: italic;
}

table.jobsList.history td.eventtitle {
    max-width: 400px;
    white-space: nowrap;
    text-overflow: ellipsis;
}

tr.sectionhead td.eventtitle {
    overflow: hidden;
    white-space: nowrap;
    width: 50px;
    max-width: 400px;
}

table.jobsList.history td {
    vertical-align: baseline;
}

table.jobsList td {
    text-align: left;
}

tr.sectionhead td.nodecount.fail {
    color: red;
    font-weight: bold;
}

table.jobsList.history td {
    vertical-align: baseline;
}

table.jobsList td {
    text-align: left;
}

</style>
