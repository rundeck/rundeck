%{--
  - Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -     http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  --}%<%@ page contentType="text/xml;charset=UTF-8" %><?xml version="1.0" encoding="UTF-8"?>
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
