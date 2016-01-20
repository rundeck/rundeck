(function($){
    if(typeof(jQuery)=='function'){
        jQuery(document).ready(function () {
        jQuery('#CTable').dataTable( {
            "aaSorting": [[1,'asc'],[0,'asc']],
            "sPaginationType": "bootstrap",
            "aLengthMenu": [ 10, 25, 50 ],
            "iDisplayLength": 10,
            "bAutoWidth": false,
            "oLanguage": {
            "sLengthMenu": "_MENU_ records per page"
            } /*olan*/
        }); /* dataTable */
            jQuery('.has_Dtooltip').tooltip({});
            _initPopoverContentRef();
            _initPopoverContentFor();
            _initAffix();
            _initIEPlaceholder();
            _initCollapseExpander();
            _initAnsiToggle();
   });
};
})(jQuery);

