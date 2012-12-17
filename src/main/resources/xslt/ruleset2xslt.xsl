<?xml version="1.0" encoding="UTF-8"?>
<!--
  This file is part of the METS Merger, Copyright 2011, 2012 SUB Göttingen

  This program is free software; you can redistribute it and/or modify it under
  the terms of the GNU Affero General Public License version 3 as published by
  the Free Software Foundation.
  
  This program is distributed in the hope that it will be useful, but WITHOUT
  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
  FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.
 
  You should have received a copy of the GNU Affero General Public License
  along with this program; if not, see http://www.gnu.org/licenses or write to
  the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
  MA 02110-1301 USA.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:goobi="http://meta.goobi.org/v1.5.1/" xmlns:gdz="http://gdz.sub.uni-goettingen.de/" xmlns:mods="http://www.loc.gov/mods/v3"
    xmlns:mets="http://www.loc.gov/METS/" xmlns:xslo="http://www.w3.org/1999/XSL/TransformAlias" xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl" xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" exclude-result-prefixes="xd" version="2.0">
    <xd:doc scope="stylesheet">
        <xd:desc>
            <xd:p>
                <xd:b>Created on:</xd:b> Dec 1, 2011 </xd:p>
            <xd:p>
                <xd:b>Author:</xd:b> cmahnke </xd:p>
            <xd:p/>
        </xd:desc>
    </xd:doc>
    <!-- TODO:
       * try to fix the # stuff in the XPath expressions
       * validation doesn't work yet
    -->
    <xsl:output encoding="UTF-8" method="xml" indent="yes"/>
    <xsl:namespace-alias stylesheet-prefix="xslo" result-prefix="xsl"/>
    <xsl:param name="createDMDSectsParam" select="true()"/>
    <xsl:param name="addOrderLabelParam" select="true()"/>
    <!-- Currently the result stylesheet will be configurable if createDMDSects is set
    <xsl:param name="configurableResultParam" select="true()"/>
    -->
    <!-- This is a stupid method to use params via JAXP, which are typed as String.
    Pass an empty tring to set the variables to  -->
    <xsl:variable name="createDMDSects" select="if ($createDMDSectsParam castable as xs:boolean) then xs:boolean($createDMDSectsParam) else true()" as="xs:boolean"/>
    <xsl:variable name="addOrderLabel" select="if ($addOrderLabelParam castable as xs:boolean) then xs:boolean($addOrderLabelParam) else false()" as="xs:boolean"/>
    <!--
    <xsl:variable name="configurableResult" select="if ($configurableResultParam castable as xs:boolean) then xs:boolean($configurableResultParam) else false()" as="xs:boolean"/>
    -->
    <xsl:template match="/">
        <xslo:stylesheet>
            <xsl:for-each select="//NamespaceDefinition">
                <xsl:variable name="ns">
                    <xsl:element name="{./prefix}ns" namespace="{./URI}"/>
                </xsl:variable>
                <!-- This can be used with XSLT 1.0 using the EXSL Functions (xmlns:exsl="http://exslt.org/common") -->
                <!--
                    <xsl:copy-of select="exsl:node-set($ns)//namespace::*"/>
                -->
                <xsl:copy-of select="$ns//namespace::*"/>
            </xsl:for-each>
            <xsl:attribute name="version">
                <xsl:text>2.0</xsl:text>
            </xsl:attribute>
            <xslo:output encoding="UTF-8" method="xml" indent="yes"/>

            <xsl:comment>Should @LABEL attributes be copied?</xsl:comment>
            <xslo:param name="copyLabelParam" select="false()"/>
            <xsl:comment>Should references to administrative metadata (ADMID attributes) be copied?</xsl:comment>
            <xslo:param name="copyADMParam" select="false()"/>
            <xslo:variable name="copyLabel" select="if ($copyLabelParam castable as xs:boolean) then xs:boolean($copyLabelParam) else false()" as="xs:boolean"/>
            <xslo:variable name="copyADM" select="if ($copyADMParam castable as xs:boolean) then xs:boolean($copyADMParam) else false()" as="xs:boolean"/>
            <xsl:if test="$createDMDSects">
                <xslo:param name="createGoobiMETSParam" select="true()"/>
                <xslo:variable name="createGoobiMETS" select="if ($createGoobiMETSParam castable as xs:boolean) then xs:boolean($createGoobiMETSParam) else false()" as="xs:boolean"/>
            </xsl:if>
            <xslo:variable name="addSchemaLocation" select="true()"/>
            <xslo:preserve-space elements="*"/>
            <xslo:template match="/">
                <xslo:apply-templates/>
            </xslo:template>
            <xsl:apply-templates/>
            <xslo:template match="@* | node()">
                <xslo:copy>
                    <xslo:apply-templates select="@* | node()"/>
                </xslo:copy>
            </xslo:template>
            <xslo:template match="processing-instruction()">
                <xslo:copy-of select="."/>
            </xslo:template>
            <xsl:if test="$createDMDSects">
                <xsl:variable name="InternalName">
                    <xsl:value-of select="//WriteXPath[text() ='./mods:mods/mods:titleInfo/#mods:title']/../InternalName/text()"/>
                </xsl:variable>
                <xslo:template name="generateDMDID">
                    <xslo:param name="node" select="."/>
                    <xslo:text>DMDLOG_</xslo:text>
                    <xslo:value-of select="generate-id($node)"/>
                </xslo:template>
                <xslo:template match="mets:dmdSec">
                    <xslo:copy>
                        <!-- Rewire the IDs, there might be collisions fom the generated DMD sections -->
                        <xslo:if test="@DMDID">
                            <xslo:attribute name="DMDID">
                                <xslo:call-template name="generateDMDID">
                                    <xslo:with-param name="node" select="//mets:dmdSec[@ID = @DMDID]"/>
                                </xslo:call-template>
                            </xslo:attribute>
                        </xslo:if>
                        <xslo:apply-templates select="@* | node()"/>
                    </xslo:copy>
                </xslo:template>
                <xslo:template match="mets:div">
                    <xslo:choose>
                        <xslo:when test="./ancestor::mets:structMap[@TYPE = 'LOGICAL']">
                            <xslo:copy>
                                <xslo:choose>
                                    <xslo:when test="not(@DMDID) and @LABEL">
                                        <xslo:attribute name="DMDID">
                                            <xslo:call-template name="generateDMDID">
                                                <xslo:with-param name="node" select="."/>
                                            </xslo:call-template>
                                        </xslo:attribute>
                                    </xslo:when>
                                    <xslo:when test="@DMDID">
                                        <!-- Rewrite DMDIDs to avoid collisions with generated IDs -->
                                        <xslo:attribute name="DMDID">
                                            <xslo:call-template name="generateDMDID">
                                                <xslo:with-param name="node" select="//mets:dmdSec[@ID = @DMDID]"/>
                                            </xslo:call-template>
                                        </xslo:attribute>
                                    </xslo:when>
                                </xslo:choose>
                                <xslo:apply-templates select="@* | node()"/>
                            </xslo:copy>
                        </xslo:when>
                        <xsl:if test="$addOrderLabel">
                            <xslo:when test="./ancestor::mets:structMap[@TYPE = 'PHYSICAL'] and @ORDER and not(@ORDERLABEL)">
                                <xslo:copy>
                                    <xslo:attribute name="ORDERLABEL">
                                        <xslo:value-of select="@ORDER"/>
                                    </xslo:attribute>
                                    <xslo:apply-templates select="@* | node()"/>
                                </xslo:copy>
                            </xslo:when>
                        </xsl:if>
                        <xslo:otherwise>
                            <xslo:copy>
                                <xslo:apply-templates select="@* | node()"/>
                            </xslo:copy>
                        </xslo:otherwise>
                    </xslo:choose>
                </xslo:template>
            </xsl:if>
            <xsl:if test="$addOrderLabel and not($createDMDSects)">
                <xslo:template match="mets:div">
                    <xslo:choose>
                        <xslo:when test="./ancestor::mets:structMap[@TYPE = 'PHYSICAL'] and @ORDER and not(@ORDERLABEL)">
                            <xslo:copy>
                                <xslo:attribute name="ORDERLABEL">
                                    <xslo:value-of select="@ORDER"/>
                                </xslo:attribute>
                                <xslo:apply-templates select="@* | node()"/>
                            </xslo:copy>
                        </xslo:when>
                        <xslo:otherwise>
                            <xslo:copy>
                                <xslo:apply-templates select="@* | node()"/>
                            </xslo:copy>
                        </xslo:otherwise>
                    </xslo:choose>
                </xslo:template>
            </xsl:if>
        </xslo:stylesheet>
    </xsl:template>
    <xsl:template match="METS">
        <xsl:comment>Filter Stuff out which is generated by Goobi if requested.</xsl:comment>
        <xslo:template match="mets:div/@LABEL">
            <xslo:if test="$copyLabel = true()">
                <xslo:copy-of select="."/>
            </xslo:if>
        </xslo:template>
        <xslo:template match="mets:div/@ADMID">
            <xslo:if test="$copyADM = true()">
                <xslo:copy-of select="."/>
            </xslo:if>
        </xslo:template>
        <xslo:template match="mets:div/@TYPE">
            <xslo:attribute name="TYPE">
                <xslo:choose>
                    <xsl:for-each select="DocStruct">
                        <xslo:when>
                            <xsl:attribute name="test">
                                <xsl:text>.='</xsl:text>
                                <xsl:value-of select="MetsType"/>
                                <xsl:text>'</xsl:text>
                            </xsl:attribute>
                            <xslo:text>
                                <xsl:value-of select="InternalName"/>
                            </xslo:text>
                        </xslo:when>
                    </xsl:for-each>
                    <xslo:otherwise>
                        <xslo:message terminate="yes">No Mapping found!</xslo:message>
                    </xslo:otherwise>
                </xslo:choose>
            </xslo:attribute>
        </xslo:template>
        <!-- Match the root -->
        <xslo:template match="mets:mets">
            <xslo:copy>
                <xslo:if test="$addSchemaLocation and not(@xsi:schemaLocation)">
                    <xslo:attribute name="xsi:schemaLocation"
                        select="'http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-3.xsd http://www.loc.gov/METS/ http://www.loc.gov/standards/mets/version17/mets.v1-7.xsd'"/>
                </xslo:if>
                <xslo:apply-templates select="@*"/>
                <xslo:apply-templates select="//mets:dmdSec"/>
                <xsl:if test="$createDMDSects">
                    <xslo:call-template name="convertLabels"/>
                </xsl:if>
                <xslo:apply-templates select="*[not(self::mets:dmdSec)]"/>
            </xslo:copy>
        </xslo:template>
        <xsl:if test="$createDMDSects">
            <xslo:template name="convertLabels">
                <xsl:variable name="InternalName">
                    <xsl:value-of select="//WriteXPath[text() ='./mods:mods/mods:titleInfo/#mods:title']/../InternalName/text()"/>
                </xsl:variable>
                <xslo:for-each select="//mets:structMap[@TYPE = 'LOGICAL']/mets:div/descendant::mets:div">
                    <xslo:if test="not(@DMDID) and @LABEL">
                        <xslo:variable name="id">
                            <xslo:call-template name="generateDMDID">
                                <xslo:with-param name="node" select="."/>
                            </xslo:call-template>
                        </xslo:variable>
                        <mets:dmdSec>
                            <xslo:attribute name="ID">
                                <xslo:value-of select="$id"/>
                            </xslo:attribute>
                            <mets:mdWrap MDTYPE="MODS">
                                <mets:xmlData>
                                    <mods:mods>
                                        <xslo:choose>
                                            <xslo:when test="$createGoobiMETS = true()">
                                                <mods:extension>
                                                    <goobi:goobi>
                                                        <goobi:metadata>
                                                            <xsl:attribute name="name">
                                                                <xsl:value-of select="$InternalName"/>
                                                            </xsl:attribute>
                                                            <xslo:value-of select="@LABEL"/>
                                                        </goobi:metadata>
                                                    </goobi:goobi>
                                                </mods:extension>
                                            </xslo:when>
                                            <xslo:otherwise>
                                                <mods:titleInfo>
                                                    <mods:title>
                                                        <xslo:value-of select="@LABEL"/>
                                                    </mods:title>
                                                </mods:titleInfo>
                                            </xslo:otherwise>
                                        </xslo:choose>
                                    </mods:mods>
                                </mets:xmlData>
                            </mets:mdWrap>
                        </mets:dmdSec>
                    </xslo:if>
                </xslo:for-each>
            </xslo:template>
        </xsl:if>
        <xslo:template match="mods:mods">
            <xslo:copy>
                <mods:extension>
                    <goobi:goobi>
                        <xsl:for-each select="Metadata">
                            <xsl:variable name="path">
                                <xsl:choose>
                                    <!-- The Path should be used in a loop -->
                                    <xsl:when test="contains(concat('.', substring(WriteXPath, string-length('./mods:mods') + 1)), '#')">
                                        <xsl:value-of select="translate(concat('.', substring(WriteXPath, string-length('./mods:mods') + 1)), '#', '')"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="concat('.', substring(WriteXPath, string-length('./mods:mods') + 1))"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:variable>
                            <xsl:variable name="loop">
                                <xsl:choose>
                                    <xsl:when test="contains(concat('.', substring(WriteXPath, string-length('./mods:mods') + 1)), '#')">
                                        <xsl:value-of select="true()"/>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:value-of select="false()"/>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:variable>
                            <xslo:if>
                                <xsl:attribute name="test">
                                    <xsl:call-template name="selectorParser">
                                        <xsl:with-param name="str">
                                            <xsl:call-template name="sanitisePath">
                                                <xsl:with-param name="path" select="$path"/>
                                            </xsl:call-template>
                                        </xsl:with-param>
                                    </xsl:call-template>
                                    <!-- This doesn't work with XSLT 2.0
                                <xsl:text> != false()</xsl:text>
                                -->
                                </xsl:attribute>
                                <xsl:choose>
                                    <xsl:when test="not(FirstnameXPath) and not(LastnameXPath) and not(DisplayNameXPath) and not(IdentifierXPath)">
                                        <xsl:choose>
                                            <xsl:when test="$loop = true()">
                                                <xslo:for-each>
                                                    <xsl:attribute name="select">
                                                        <xsl:call-template name="sanitisePath">
                                                            <xsl:with-param name="path" select="$path"/>
                                                        </xsl:call-template>
                                                    </xsl:attribute>
                                                    <xsl:call-template name="goobiMetadata">
                                                        <xsl:with-param name="name" select="InternalName"/>
                                                        <xsl:with-param name="path" select="'.'"/>
                                                    </xsl:call-template>
                                                </xslo:for-each>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:call-template name="goobiMetadata">
                                                    <xsl:with-param name="name" select="InternalName"/>
                                                    <xsl:with-param name="path" select="$path"/>
                                                </xsl:call-template>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <xsl:choose>
                                            <xsl:when test="$loop = true()">
                                                <xslo:for-each>
                                                    <xsl:attribute name="select">
                                                        <xsl:call-template name="selectorParser">
                                                            <xsl:with-param name="str">
                                                                <xsl:call-template name="sanitisePath">
                                                                    <xsl:with-param name="path" select="$path"/>
                                                                </xsl:call-template>
                                                            </xsl:with-param>
                                                        </xsl:call-template>
                                                    </xsl:attribute>
                                                    <xsl:call-template name="personMetadata"/>
                                                </xslo:for-each>
                                            </xsl:when>
                                            <xsl:otherwise>
                                                <xsl:call-template name="personMetadata"/>
                                            </xsl:otherwise>
                                        </xsl:choose>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xslo:if>
                        </xsl:for-each>
                    </goobi:goobi>
                </mods:extension>
            </xslo:copy>
        </xslo:template>
    </xsl:template>
    <xsl:template name="personMetadata">
        <xsl:element name="goobi:metadata">
            <xsl:attribute name="name">
                <xsl:value-of select="InternalName"/>
            </xsl:attribute>
            <xsl:attribute name="type">
                <xsl:text>person</xsl:text>
            </xsl:attribute>
            <xsl:call-template name="goobiMetadata">
                <xsl:with-param name="element" select="'goobi:firstName'"/>
                <xsl:with-param name="path" select="FirstnameXPath"/>
            </xsl:call-template>
            <xsl:call-template name="goobiMetadata">
                <xsl:with-param name="element" select="'goobi:lastName'"/>
                <xsl:with-param name="path" select="LastnameXPath"/>
            </xsl:call-template>
            <xslo:if>
                <xsl:attribute name="test">
                    <xsl:call-template name="selectorParser">
                        <xsl:with-param name="str">
                            <xsl:call-template name="sanitisePath">
                                <xsl:with-param name="path" select="IdentifierXPath"/>
                            </xsl:call-template>
                        </xsl:with-param>
                    </xsl:call-template>
                    <xsl:text> != false()</xsl:text>
                </xsl:attribute>
                <xsl:call-template name="goobiMetadata">
                    <xsl:with-param name="element" select="'goobi:identifier'"/>
                    <xsl:with-param name="path" select="IdentifierXPath"/>
                </xsl:call-template>
            </xslo:if>
            <xsl:call-template name="goobiMetadata">
                <xsl:with-param name="element" select="'goobi:displayName'"/>
                <xsl:with-param name="path" select="DisplayNameXPath"/>
            </xsl:call-template>
            <xsl:message terminate="no">Complex Mapping detected!</xsl:message>
        </xsl:element>
    </xsl:template>
    <xsl:template name="goobiMetadata">
        <xsl:param name="element" select="'goobi:metadata'"/>
        <xsl:param name="name" select="''"/>
        <xsl:param name="path"/>
        <xsl:element name="{$element}">
            <xsl:if test="$name != ''">
                <xsl:attribute name="name">
                    <xsl:value-of select="$name"/>
                </xsl:attribute>
            </xsl:if>
            <xslo:value-of>
                <xsl:attribute name="select">
                    <xsl:call-template name="sanitisePath">
                        <xsl:with-param name="path" select="$path"/>
                    </xsl:call-template>
                </xsl:attribute>
            </xslo:value-of>
        </xsl:element>
    </xsl:template>
    <xsl:template name="sanitisePath">
        <xsl:param name="path"/>
        <!-- remove trailing slashes -->
        <xsl:variable name="returnPath">
            <xsl:choose>
                <xsl:when test="substring($path, string-length($path)) = '/'">
                    <xsl:value-of select="substring($path, 1, string-length($path) - 1)"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$path"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <!-- Replace " with ' -->
        <xsl:variable name="apos">'</xsl:variable>
        <xsl:choose>
            <xsl:when test="contains($returnPath, '&#34;')">
                <xsl:value-of select="translate($returnPath, '&#34;', $apos)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$returnPath"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template name="selectorParser">
        <xsl:param name="str"/>
        <xsl:param name="chars" select="'[]'"/>
        <xsl:param name="nestingLevel" select="0"/>
        <xsl:param name="maxNesting" select="1"/>
        <xsl:variable name="charWidth" select="string-length($chars) div 2"/>
        <xsl:variable name="startChar" select="substring($chars, 1, $charWidth)"/>
        <xsl:variable name="endChar" select="substring($chars, $charWidth + 1, $charWidth)"/>
        <xsl:variable name="containsAny">
            <xsl:call-template name="containsAny">
                <xsl:with-param name="str" select="$str"/>
                <xsl:with-param name="pattern" select="$chars"/>
            </xsl:call-template>
        </xsl:variable>
        <!-- Get the first match -->
        <xsl:variable name="token">
            <xsl:choose>
                <xsl:when test="string($containsAny) = 'true'">
                    <xsl:choose>
                        <!-- The order of the whe clauses is very important here! -->
                        <xsl:when test="starts-with($str, $startChar) or starts-with($str, $endChar)">
                            <xsl:value-of select="substring($str, 1, $charWidth)"/>
                        </xsl:when>
                        <xsl:when test="not(contains($str, $startChar))">
                            <xsl:value-of select="substring-before($str, $endChar)"/>
                        </xsl:when>
                        <xsl:when test="string-length(substring-before($str,$startChar)) &lt; string-length(substring-before($str,$endChar))">
                            <xsl:value-of select="substring-before($str, $startChar)"/>
                        </xsl:when>
                        <xsl:when test="string-length(substring-before($str,$startChar)) &gt; string-length(substring-before($str,$endChar))">
                            <xsl:value-of select="substring-before($str, $endChar)"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:message terminate="yes"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$str"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="nesting">
            <xsl:choose>
                <xsl:when test="contains($token, $startChar)">
                    <xsl:value-of select="$nestingLevel + 1"/>
                </xsl:when>
                <xsl:when test="contains($token, $endChar)">
                    <xsl:value-of select="$nestingLevel - 1"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$nestingLevel"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:if test="not($nesting &gt; $maxNesting) and not($nestingLevel &gt; $maxNesting)">
            <xsl:value-of select="$token"/>
        </xsl:if>
        <xsl:variable name="remainder" select="substring($str, string-length($token) + $charWidth)"/>
        <xsl:if test="string-length($remainder) &gt; 0">
            <xsl:call-template name="selectorParser">
                <xsl:with-param name="str" select="$remainder"/>
                <xsl:with-param name="nestingLevel" select="$nesting"/>
                <xsl:with-param name="maxNesting" select="$maxNesting"/>
            </xsl:call-template>
        </xsl:if>
    </xsl:template>
    <xsl:template name="containsAny">
        <xsl:param name="str"/>
        <xsl:param name="pattern"/>
        <xsl:param name="position" select="1"/>
        <xsl:variable name="char" select="substring($pattern, $position, 1)"/>
        <xsl:choose>
            <xsl:when test="$position &gt; string-length($pattern)">
                <xsl:value-of select="false()"/>
            </xsl:when>
            <xsl:when test="contains($str, $char)">
                <xsl:value-of select="true()"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:call-template name="containsAny">
                    <xsl:with-param name="str" select="$str"/>
                    <xsl:with-param name="pattern" select="$pattern"/>
                    <xsl:with-param name="position" select="$position + 1"/>
                </xsl:call-template>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="text()"/>
</xsl:stylesheet>
