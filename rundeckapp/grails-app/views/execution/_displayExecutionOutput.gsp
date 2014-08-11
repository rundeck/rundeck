<div>
    <p class='smlabel'>file: ${execution.outputfilepath}</p>
    <table border='0' width='100%' class="execoutput" cellpadding="0" cellspacing="0">
        <thead>
            <tr>
                <th>
                </th>
                <th>Time
                </th>
                <th>Message
                </th>
            </tr>
        </thead>
        <tbody >
            <% def i =0 %>
            <g:each in="${entries?}">
            <tr class="${i%2==1?'alternateRow':''}">
                <td width='10' class="info"  style=" vertical-align:top;">
                    <g:if test="${it.level != 'INFO' && it.level != 'CONFIG'}">
                        <img src='${resource(dir:"images",file:"icon-small-"+it.level.toLowerCase()+".png")}' alt='${enc(attr:it.level)}' title="${enc(attr:it.level)}"/>
                    </g:if>
                </td>
                <td width='10' class="info" style=" vertical-align:top;">
                    <span class="${enc(attr:it.level)}"><g:enc>${it.time}</g:enc> </span>
                </td>
                <td  class="data" style=" vertical-align:top;"><g:enc>${it.mesg}</g:enc></td>
            </tr>
                <% i++ %>
            </g:each>
        </tbody>
    </table>
</div>
