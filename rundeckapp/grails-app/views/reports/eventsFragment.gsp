<g:render template="baseReport"
          model="['reports': reports, options: params.compact ? [tags: false, summary: false] : [summary: true], hiliteSince: params.hiliteSince]"/>
