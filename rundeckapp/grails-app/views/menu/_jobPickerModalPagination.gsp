<g:if test="${max && max < total && jobsListSize < total}">
    <div id="modal-pagination" style="display: none;">
        <div style="display:flex; justify-content: space-between; align-items: center;margin-top: 10px;">
            <div class="gsp-pager">
                <div class="modal-pagination">
                    <g:set var="numPages" value="${new java.math.BigDecimal(total/max).setScale(0, java.math.RoundingMode.UP)}"/>
                    <div class="modal-pagination-item">
                        <a class="prevLink" id="modalPaginationPrevLink" style="display: none">Previous</a>
                        <script nonce="${security.cspNonce()}" type="text/javascript">
                        document.getElementById('modalPaginationPrevLink').addEventListener('click', function(event) {
                          handleModalPagination(this, ${offset-max}, ${max}, ${total});
                        });
                        </script>
                    </div>
                    <g:each in="${1..numPages}" var="step">
                        <div class="modal-pagination-item">
                            <span style="display: ${step == 1 ? 'inline' : 'none'}" class="currentStep step-${step}">${step}</span>
                            <a style="display: ${step == 1 ? 'none' : 'inline'}" id="modalPaginationStep-${step}" class="step step-${step}">${step}</a>
                            <script nonce="${security.cspNonce()}" type="text/javascript">
                            document.getElementById('modalPaginationStep-${step}').addEventListener('click', function(event) {
                              handleModalPagination(this, ${(step-1)*max}, ${max}, ${total});
                            });
                            </script>
                        </div>
                    </g:each>
                    <div class="modal-pagination-item">
                        <a class="nextLink" id="modalPaginationNextLink">Next</a>
                        <script nonce="${security.cspNonce()}" type="text/javascript">
                        document.getElementById('modalPaginationNextLink').addEventListener('click', function(event) {
                          handleModalPagination(this, ${offset+max}, ${max}, ${total});
                        });
                        </script>
                    </div>
                </div>
            </div>
            <div>
                Showing ${offset+1}-${offset+max > total ? total : offset+max} of ${total}
            </div>
        </div>
    </div>
</g:if>

<script nonce="${security.cspNonce()}" type="text/javascript">
    function handleModalPagination(elem, offset, max, total) {
        jQuery(document).ready(function() {
        jQuery(".modal").on("hidden.bs.modal", function() {
            jQuery(".modal-body").html("");
        });
        });
        if (jQuery(elem).hasClass('active')) {
            jQuery('#' + jobChooseModalId).modal('hide');
            return;
        }
        var project = selFrameworkProject;
        if (projectFieldId) {
            project = jQuery('#' + projectFieldId).val();
        }
        jQuery(elem).button('loading').addClass('active');

        jQuery.ajax({
            url: _genUrl(appLinks.menuJobsPicker, {
                jobsjscallback: 'true',
                runAuthRequired: true,
                projFilter: project,
                offset: offset
            }),
            success: function (resp, status, jqxhr) {
                var modal = jQuery('#' + jobChooseModalId);
                jQuery(elem).button('reset').removeClass('active');
                modal.find('#' + jobChooseModalId + "_content .job-display-tree").html(resp);
                modal.find("#modal-pagination").show()
                modal.find('a.step').show()
                modal.find('span.currentStep').hide()
                modal.find('a.step-' + ((offset/max) + 1) ).hide()
                modal.find('span.step-' + ((offset/max) + 1) ).show()
                if(offset+max > total) {
                    modal.find('.nextLink').hide()
                } else {
                    modal.find('.nextLink').show()
                }
                if(offset-max < 0) {
                    modal.find('.prevLink').hide()
                } else {
                    modal.find('.prevLink').show()
                }
            },
            error: function (jqxhr, status, resp) {
                showError("Error performing request: menuJobsPicker: " + resp);
                jQuery(elem).button('reset').removeClass('active');
            }
        });
    }
</script>

