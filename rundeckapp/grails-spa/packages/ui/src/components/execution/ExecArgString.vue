<template>
  <g:set var="parsed" value="${g.parseOptsFromString(args: argString)}"/>
  <g:if test="${parsed}">
    <g:each in="${parsed}" var="entry">
      <span class="optkey"><g:enc>${entry.key}</g:enc></span>
      <g:if test="${entry.value}">
        <g:if test="${inputFilesMap && inputFilesMap[entry.value]}">
          <g:set var="frkey" value="${g.rkey()}"/>
          <span class="optvalue optextra" title="File Input Option"
                data-toggle="popover"
                data-placement="bottom"
                data-popover-content-ref="#f_${frkey}"
                data-trigger="hover"
                data-container="body">
                    ${inputFilesMap[entry.value].fileName ?: entry.value}
                </span>
          <span id="f_${frkey}" style="display: none">
                    <g:basicData classes="table-condensed table-bordered"
                                 fieldTitle="${[
                                         'fileName': 'Name',
                                         'size'    : 'Size',
                                         'sha'     : 'SHA256',
                                         'id'      : 'ID',
                                 ]}"
                                 fields="${[
                                         'fileName',
                                         'size',
                                         'sha',
                                         'id'
                                 ]}"
                                 data="${[
                                         fileName: inputFilesMap[entry.value].fileName ?: '(not set)',
                                         sha     : inputFilesMap[entry.value].sha?.substring(0, 15) + '...',
                                         size    : g.humanizeValue(
                                                 [value: inputFilesMap[entry.value].size, unit: 'byte']
                                         ),
                                         id      : entry.value,
                                 ]}"
                                 dataTitles="${[sha: inputFilesMap[entry.value].sha]}">
                    </g:basicData>
                </span>
        </g:if>
        <g:else>
          <code class="optvalue"><g:enc>${entry.value}</g:enc></code>
        </g:else>
      </g:if>
    </g:each>
  </g:if>
  <g:else>
    <code class="optvalue"><g:enc>${argString}</g:enc></code>
  </g:else>
</template>

<script lang="ts">
export default {
  name: 'ExecArgString',
  components: {},
  props: {
    argString: String
  },
  computed: {

  },
  methods: {

  },
  data() {
    return {
      edit: true,
      ukey: g.rkey()
    }
  }
}
</script>
