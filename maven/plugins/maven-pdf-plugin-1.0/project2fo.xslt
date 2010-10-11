<?xml version='1.0'?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                version='1.0'>

<xsl:param name="basePath">.</xsl:param>

<xsl:attribute-set name="base.body.style">
    <xsl:attribute name="font-family">serif</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="base.header.style">
    <xsl:attribute name="font-family">sans-serif</xsl:attribute>
    <xsl:attribute name="color">#000036</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="chapter.header.top"
                   use-attribute-sets="base.header.style">
    <xsl:attribute name="font-size">14pt</xsl:attribute>
    <xsl:attribute name="text-align">right</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="chapter.header.bottom"
                   use-attribute-sets="base.header.style">
    <xsl:attribute name="space-before.optimum">0.5em</xsl:attribute>
    <xsl:attribute name="font-size">16pt</xsl:attribute>
    <xsl:attribute name="text-align">right</xsl:attribute>
    <xsl:attribute name="space-after.optimum">5em</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="normal.header"
                   use-attribute-sets="base.header.style">
    <xsl:attribute name="space-before.optimum">1.5em</xsl:attribute>
    <xsl:attribute name="space-before.minimum">1.2em</xsl:attribute>
    <xsl:attribute name="space-before.maximum">2.0em</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="normal.paragraph"
                   use-attribute-sets="base.body.style">
    <xsl:attribute name="space-before.optimum">1.0em</xsl:attribute>
    <xsl:attribute name="space-before.minimum">0.8em</xsl:attribute>
    <xsl:attribute name="space-before.maximum">1.2em</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="normal.pre"
                   use-attribute-sets="normal.paragraph">
    <xsl:attribute name="wrap-option">no-wrap</xsl:attribute>
    <xsl:attribute name="white-space-collapse">false</xsl:attribute>
    <xsl:attribute name="font-family">monospace</xsl:attribute>
    <xsl:attribute name="color">gray</xsl:attribute>
    <xsl:attribute name="border">dotted thin gray</xsl:attribute>
    <xsl:attribute name="padding">0.5em</xsl:attribute>
    <xsl:attribute name="start-indent">0.5em</xsl:attribute>
    <xsl:attribute name="end-indent">0.5em</xsl:attribute>
</xsl:attribute-set>

<xsl:template match="project">
    <fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format">
    <!-- defines the layout master -->
    <fo:layout-master-set>
      <fo:simple-page-master
	    master-name="main"
        margin-top="1in"
        margin-bottom="1in"
        margin-left="1in"
        margin-right="1in">
          <fo:region-body margin-bottom="0.5in"/>
          <fo:region-after extent="0.25in"/>
      </fo:simple-page-master>
    </fo:layout-master-set>

    <xsl:apply-templates/>

    </fo:root>
</xsl:template>

<xsl:template match="project/body">

    <!-- Table of contents -->

    <fo:page-sequence master-reference="main">

        <!-- header -->
        <fo:static-content flow-name="xsl-region-after">
            <fo:block font-size="8pt"
	                  font-family="serif"
                      text-align="right">
                Table of Contents
                &#8211;
                <fo:page-number/>
            </fo:block>
        </fo:static-content>

        <fo:flow flow-name="xsl-region-body">

            <fo:block id="{@href}" xsl:use-attribute-sets="chapter.header.top">
                <xsl:value-of select="../title"/>
            </fo:block>
            <fo:block>
                <fo:leader leader-pattern="rule"/>
            </fo:block>
            <fo:block xsl:use-attribute-sets="chapter.header.bottom">
                Table of Contents
            </fo:block>

            <fo:table>

                <fo:table-column column-width="1cm"/>
                <fo:table-column column-width="13cm"/>
                <fo:table-column column-width="1cm"/>

                <fo:table-body>

                    <xsl:apply-templates mode="toc"/>

                </fo:table-body>

            </fo:table>

        </fo:flow>

    </fo:page-sequence>

    <!-- The sections -->

    <xsl:apply-templates/>

</xsl:template>

<xsl:template match="project/body/menu" mode="toc">

    <fo:table-row>
        <fo:table-cell>
            <fo:block><xsl:number count="menu"/></fo:block>
        </fo:table-cell>
        <fo:table-cell>
            <fo:block font-weight="bold">
                 <xsl:value-of select="@name"/>
            </fo:block>
        </fo:table-cell>
        <fo:table-cell/>
    </fo:table-row>

    <xsl:apply-templates mode="toc"/>

</xsl:template>

<xsl:template match="project/body/menu/item" mode="toc">

    <fo:table-row>

        <fo:table-cell>
            <fo:block>
                <xsl:number count="menu"/>.<xsl:number count="item"/>
            </fo:block>
        </fo:table-cell>

        <fo:table-cell>
            <fo:block>
                <fo:basic-link internal-destination="{@href}">
                    <xsl:value-of select="@name"/>
                    <fo:leader leader-pattern="dots" keep-with-next.within-line="always"/>
                </fo:basic-link>
            </fo:block>
        </fo:table-cell>

        <fo:table-cell>
            <fo:block text-align="right">
                <fo:basic-link internal-destination="{@href}">
                    <fo:page-number-citation ref-id="{@href}"/>
                </fo:basic-link>
            </fo:block>
        </fo:table-cell>

    </fo:table-row>

</xsl:template>

<xsl:template match="project/body/menu">
    <xsl:apply-templates/>
</xsl:template>

<xsl:template match="project/body/menu/item">

    <xsl:variable name="document"
        select="document( concat( $basePath, '/', substring-before( @href, '.html' ), '.xml' ) )/document"/>

    <fo:page-sequence master-reference="main">

        <!-- header -->
        <fo:static-content flow-name="xsl-region-after">
            <fo:block font-size="8pt"
	                  font-family="serif"
                      text-align="right">
                <xsl:value-of select="$document/properties/title"/>
                &#8211;
                <fo:page-number/>
            </fo:block>
        </fo:static-content>

        <fo:flow flow-name="xsl-region-body">

            <fo:block id="{@href}" xsl:use-attribute-sets="chapter.header.top">
                <xsl:number count="menu"/>.<xsl:number count="item"/>
            </fo:block>
            <fo:block>
                <fo:leader leader-pattern="rule"/>
            </fo:block>
            <fo:block xsl:use-attribute-sets="chapter.header.bottom">
                <xsl:value-of select="$document/properties/title"/>
            </fo:block>

            <xsl:apply-templates select="$document/body"/>

        </fo:flow>

    </fo:page-sequence>

</xsl:template>

<!-- ================= the following templates are for xdocs (not projects) -->

<xsl:template match="document/body">

    <xsl:apply-templates/>

</xsl:template>

<xsl:template match="section">

  <fo:block font-size="14pt" xsl:use-attribute-sets="normal.header">
        <xsl:value-of select="@name"/>
  </fo:block>

  <xsl:apply-templates/>

</xsl:template>

<xsl:template match="subsection">

  <fo:block font-size="12pt" xsl:use-attribute-sets="normal.header">
        <xsl:value-of select="@name"/>
  </fo:block>

  <xsl:apply-templates/>

</xsl:template>

<xsl:template match="p">

    <fo:block font-size="10pt" xsl:use-attribute-sets="normal.paragraph">
        <xsl:apply-templates/>
    </fo:block>

</xsl:template>

<xsl:template match="source">

    <fo:block font-size="10pt" xsl:use-attribute-sets="normal.pre">
        <xsl:apply-templates/>
    </fo:block>

</xsl:template>

<xsl:template match="table">
    <!-- Do Nothing -->
</xsl:template>

<!-- XHTML stuff -->

<xsl:template match="a[@href]">
    <fo:basic-link external-destination="{@href}">
        <fo:inline color="blue">
            <xsl:apply-templates />
        </fo:inline>
    </fo:basic-link>
</xsl:template>

<xsl:template match="br">
    <fo:block/>
</xsl:template>

<xsl:template match="em|i">
    <fo:inline font-style="italic">
        <xsl:apply-templates />
    </fo:inline>
</xsl:template>

<xsl:template match="strong|b">
    <fo:inline font-weight="bold">
        <xsl:apply-templates />
    </fo:inline>
</xsl:template>

<xsl:template match="sub">
    <fo:inline baseline-shift="sub">
        <xsl:apply-templates />
    </fo:inline>
</xsl:template>

<xsl:template match="sup">
    <fo:inline baseline-shift="sup">
        <xsl:apply-templates />
    </fo:inline>
</xsl:template>

<xsl:template match="tt|code">
    <fo:inline font-family="monospace">
        <xsl:apply-templates />
    </fo:inline>
</xsl:template>

<xsl:template match="big">
    <fo:inline font-size="larger">
        <xsl:apply-templates />
    </fo:inline>
</xsl:template>

<xsl:template match="small">
    <fo:inline font-size="smaller">
        <xsl:apply-templates />
    </fo:inline>
</xsl:template>

<xsl:attribute-set name="list">
    <xsl:attribute name="provisional-distance-between-starts">
        1em
    </xsl:attribute>
    <xsl:attribute name="provisional-label-separation">
        1em
    </xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="list.item">
    <xsl:attribute name="start-indent">inherit</xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="dl">
    <xsl:attribute name="start-indent">inherit </xsl:attribute>
    <xsl:attribute name="end-indent">inherit </xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="dt">
    <xsl:attribute name="start-indent">inherit</xsl:attribute>
    <xsl:attribute name="end-indent">inherit </xsl:attribute>
</xsl:attribute-set>

<xsl:attribute-set name="dd">
    <xsl:attribute name="start-indent">inherit +1em</xsl:attribute>
    <xsl:attribute name="end-indent">inherit </xsl:attribute>
    <xsl:attribute name="space-before">0.6em</xsl:attribute>
    <xsl:attribute name="space-after">0.6em</xsl:attribute>
</xsl:attribute-set>

<xsl:template match="ul">
    <fo:list-block xsl:use-attribute-sets="list">
        <xsl:apply-templates/>
    </fo:list-block>
</xsl:template>

<xsl:template match="ul/li">
    <fo:list-item>
        <fo:list-item-label xsl:use-attribute-sets="list-item"
                            end-indent="label-end()">
            <fo:block font-size="10pt">
                <fo:character character="&#x2022;" />
            </fo:block>
        </fo:list-item-label>
        <fo:list-item-body start-indent="body-start()">
            <fo:block font-size="10pt">
                <xsl:apply-templates/>
            </fo:block>
        </fo:list-item-body>
    </fo:list-item>
</xsl:template>

<xsl:template match="ol">
    <fo:list-block  xsl:use-attribute-sets="list">
        <xsl:apply-templates />
    </fo:list-block>
</xsl:template>

<xsl:template match="ol/li">
    <fo:list-item>
        <fo:list-item-label xsl:use-attribute-sets="list-item" end-indent="label-end()">
            <fo:block font-size="10pt">
                <xsl:number format="1." />
            </fo:block>
        </fo:list-item-label>
        <fo:list-item-body start-indent="body-start()">
            <fo:block font-size="10pt">
                <xsl:apply-templates/>
            </fo:block>
        </fo:list-item-body>
    </fo:list-item>
</xsl:template>


<xsl:template match="dl">
    <fo:block font-size="10pt" xsl:use-attribute-sets="dl">
        <xsl:apply-templates />
    </fo:block>
</xsl:template>


<xsl:template match="dt">
    <fo:block font-size="10pt" xsl:use-attribute-sets="dt">
        <xsl:apply-templates />
    </fo:block>
</xsl:template>

<xsl:template match="dd">
    <fo:block font-size="10pt" xsl:use-attribute-sets="dd">
        <xsl:apply-templates />
    </fo:block>
</xsl:template>

</xsl:stylesheet>
