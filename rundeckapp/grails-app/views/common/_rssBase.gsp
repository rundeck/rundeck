<%@ page contentType="text/xml;charset=UTF-8" %><?xml version="1.0" encoding="UTF-8"?>
<rss xmlns:content="http://purl.org/rss/1.0/modules/content/" xmlns:taxo="http://purl.org/rss/1.0/modules/taxonomy/" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:dc="http://purl.org/dc/elements/1.1/" version="2.0">
  <channel>
    <title>${enc(xml:feedTitle)}</title>
    <link>${enc(xml:feedLink)}</link>
    <description>${enc(xml:feedDescription)}</description>

    <g:each in="${items}" var="item">
        
    <item>
        <title>${enc(xml:item.title)}</title>
        <link>${enc(xml:item.link)}</link>
        <guid>${enc(xml:item.link)}</guid>
        <g:if test="${item.description}">
            <description>${enc(xml:item.description)}</description>
        </g:if>
        <g:elseif test="${item.templateName && item.model}">
            <description>${render(template:item.templateName,model:item.model,encodeAs:'html')}</description>
        </g:elseif>
        <g:else>
            <description></description>
        </g:else>
        <pubDate><g:rfc822Date date="${item.date}"/></pubDate>
        ${raw('<dc:date>')}<g:w3cDate date="${item.date}"/>${raw('</dc:date>')}
    </item>
    </g:each>
  </channel>
</rss>
