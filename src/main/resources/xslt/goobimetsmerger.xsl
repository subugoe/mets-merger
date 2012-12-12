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
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:MODS="http://www.loc.gov/mods/v3" xmlns:DC="http://purl.org/dc/elements/1.1/"
    xmlns:METS="http://www.loc.gov/METS/" xmlns:goobi="http://meta.goobi.org/v1.5.1/"
    xmlns:TEI="http://www.tei-c.org/ns/1.0" xmlns:xlink="http://www.w3.org/1999/xlink"
    xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl" xmlns:xs="http://www.w3.org/2001/XMLSchema"
    version="2.0" exclude-result-prefixes="TEI xd">
    <!--
        This style sheet should be used on the internal Goobi METS file.
        It works the other qay around to, but the results are garbage
    -->
    <xsl:output indent="no" encoding="UTF-8" method="xml"/>
    <xsl:preserve-space elements="*"/>
    <xsl:param name="structFileParam"/>
    <xsl:param name="copyPhysicalStructMapParam" select="false()"/>
    <xsl:param name="fileSectionParam" select="'PRESENTATION'"/>

    <!-- This is used to get the types right -->
    <!--
    <xsl:variable name="structFile" select="'../data/38301-tei-dmd.mets.xml'"/>
    -->
    <xsl:variable name="structFile"
        select="if (doc-available($structFileParam)) then xs:string($structFileParam) else ''"
        as="xs:string"/>
    <xsl:variable name="copyPhysicalStructMap"
        select="if ($copyPhysicalStructMapParam castable as xs:boolean) then xs:boolean($copyPhysicalStructMapParam) else false()"
        as="xs:boolean"/>
    <xsl:variable name="fileSection"
        select="if ($fileSectionParam castable as xs:string) then xs:string($fileSectionParam) else 'PRESENTATION'"
        as="xs:string"/>
    <!--
    TODO:
    * Rewrite and / or check ID links. works for the top element of each struct map 
    * Unify "mets" and "METS" namespace prefix
    * StructLinks can't work since name of structural elements is part of stupid Regelsatz
    * DMD sections are needed for each structural element - not in scope for this stylesheet, is part of rule set converter
    ** ORDERLABEL's aren't copied yet.
    -->
    <xsl:variable name="version">201112-2104</xsl:variable>

    <xsl:template match="/">
        <!--
        <xsl:message>Struct file is '<xsl:value-of select="$structFile"/>' </xsl:message>
        <xsl:message>Struct file param is '<xsl:value-of select="$structFileParam"/>' </xsl:message>
        -->
        <xsl:comment>
            <xsl:text>This file was merged by GoobiMETSMerger Version </xsl:text>
            <xsl:value-of select="$version"/>
        </xsl:comment>
        <xsl:apply-templates/>
    </xsl:template>
    <xsl:template match="METS:structMap">
        <xsl:variable name="TYPE" select="@TYPE"/>
        <xsl:choose>
            <xsl:when test="$TYPE = 'PHYSICAL'">
                <xsl:choose>
                    <xsl:when test="not($copyPhysicalStructMap)">
                        <xsl:call-template name="createPhysicalStructMap"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:copy-of
                            select="document($structFile)//METS:structMap[@TYPE=$TYPE]/*[not(self::METS:fptr[@FILEID=//METS:fileGrp[@USE='LOCAL']//METS:file/@ID])]"
                        />
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:when>
            <xsl:when test="$TYPE = 'LOGICAL'">
                <METS:structMap>
                    <xsl:attribute name="TYPE">
                        <xsl:value-of select="$TYPE"/>
                    </xsl:attribute>
                    <METS:div>
                        <xsl:attribute name="TYPE">
                            <xsl:value-of select="//METS:structMap[@TYPE = $TYPE]/METS:div[1]/@TYPE"
                            />
                        </xsl:attribute>
                        <xsl:if test="//METS:structMap[@TYPE = $TYPE]/METS:div[1]/@DMDID">
                            <xsl:attribute name="DMDID">
                                <xsl:value-of
                                    select="//METS:structMap[@TYPE = $TYPE]/METS:div[1]/@DMDID"/>
                            </xsl:attribute>
                        </xsl:if>
                        <xsl:if
                            test="document($structFile)//METS:structMap[@TYPE=$TYPE]/METS:div[1]/@ID">
                            <xsl:attribute name="ID">
                                <xsl:value-of
                                    select="document($structFile)//METS:structMap[@TYPE=$TYPE]/METS:div[1]/@ID"
                                />
                            </xsl:attribute>
                        </xsl:if>
                        <xsl:copy-of
                            select="document($structFile)//METS:structMap[@TYPE=$TYPE]/METS:div[1]/*"
                        />
                    </METS:div>
                </METS:structMap>
            </xsl:when>
            <xsl:otherwise>
                <xsl:message terminate="yes">Unknown METS:structMap type!</xsl:message>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    <xsl:template match="METS:structLink">
        <!-- Fail if structLink is not empty -->
        <xsl:if test=".//METS:smLink[@xlink:from != '' and @xlink:to != '']">
            <xsl:message terminate="yes">StructLinks of document are not empty!
                Exiting!</xsl:message>
        </xsl:if>
        <xsl:variable name="structLink">
            <xsl:copy-of select="document($structFile)//METS:structLink"/>
        </xsl:variable>
        <!-- TODO: This may needs to be rewritten. Check if IDs will be in target document -->
        <xsl:copy-of select="$structLink"/>
        <!--
        <xsl:copy>
        <xsl:copy-of select="@*"/>
            <xsl:for-each select="$structLink/METS:smLink"></xsl:for-each>
        </xsl:copy> -->
    </xsl:template>
    <xsl:template match="METS:fileSec">
        <xsl:call-template name="copyDmdSec"/>
        <xsl:call-template name="createFileSec"/>
    </xsl:template>
    <xsl:template match="@*|node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
    <!--
    <xsl:template match="comment()|processing-instruction()">
        <xsl:copy-of select="."/>
    </xsl:template>
    -->
    <xsl:template name="createFileSec">
        <xsl:variable name="locationPrefix" select="//goobi:metadata[@name='pathimagefiles']"/>
        <METS:fileSec>
            <METS:fileGrp USE="LOCAL">
                <xsl:for-each
                    select="document($structFile)//METS:fileSec/METS:fileGrp[@USE=$fileSection]/METS:file">
                    <xsl:variable name="id">
                        <xsl:call-template name="getFileID"/>
                    </xsl:variable>
                    <xsl:variable name="fileName">
                        <xsl:value-of select="concat($locationPrefix, '/')"/>
                        <xsl:number format="00000001" value="position()"/>
                        <xsl:text>.tif</xsl:text>
                    </xsl:variable>
                    <METS:file MIMETYPE="image/tiff">
                        <xsl:attribute name="ID">
                            <xsl:value-of select="$id"/>
                        </xsl:attribute>
                        <METS:FLocat LOCTYPE="URL">
                            <xsl:attribute name="xlink:href"
                                namespace="http://www.w3.org/1999/xlink">
                                <xsl:value-of select="$fileName"/>
                            </xsl:attribute>
                        </METS:FLocat>
                    </METS:file>
                </xsl:for-each>
            </METS:fileGrp>
        </METS:fileSec>
    </xsl:template>
    <xsl:template name="getFileID">
        <xsl:text>FILE_</xsl:text>
        <xsl:number format="0001" value="position()"/>
    </xsl:template>
    <xsl:template name="createPhysicalStructMap">
        <METS:structMap TYPE="PHYSICAL">
            <METS:div>
                <xsl:choose>
                    <xsl:when test="//METS:structMap[@TYPE = 'PHYSICAL']/METS:div[1]/@TYPE">
                        <xsl:attribute name="TYPE">
                            <xsl:value-of
                                select="//METS:structMap[@TYPE = 'PHYSICAL']/METS:div[1]/@TYPE"/>
                        </xsl:attribute>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:attribute name="TYPE">
                            <xsl:value-of select="'physSequence'"/>
                        </xsl:attribute>
                    </xsl:otherwise>
                </xsl:choose>
                <xsl:if test="//METS:structMap[@TYPE = 'PHYSICAL']/METS:div[1]/@DMDID">
                    <xsl:attribute name="DMDID">
                        <xsl:value-of
                            select="//METS:structMap[@TYPE = 'PHYSICAL']/METS:div[1]/@DMDID"/>
                    </xsl:attribute>
                </xsl:if>
                <!-- TODO: This ID needs to be from the original -->
                <xsl:attribute name="ID">
                    <!--
                    <xsl:value-of
                        select="document($structFile)//METS:div[@TYPE = 'physSequence']/@ID"/>
                        -->
                    <xsl:value-of select="//METS:structMap[@TYPE = 'PHYSICAL']/METS:div[1]/@ID"/>
                </xsl:attribute>
                <xsl:for-each
                    select="document($structFile)//METS:fileSec/METS:fileGrp[@USE=$fileSection]/METS:file">
                    <xsl:variable name="id">
                        <xsl:call-template name="getFileID"/>
                    </xsl:variable>
                    <xsl:variable name="fileId" select="./@ID"/>
                    <xsl:variable name="order" select="position()"/>
                    <METS:div TYPE="page">
                        <xsl:attribute name="ID">
                            <xsl:value-of
                                select="document($structFile)//METS:fptr[@FILEID = $fileId]/parent::METS:div/@ID"
                            />
                        </xsl:attribute>
                        <xsl:attribute name="ORDER">
                            <xsl:value-of select="position()"/>
                        </xsl:attribute>
                        <xsl:attribute name="ORDERLABEL">
                            <xsl:value-of select="position()"/>
                        </xsl:attribute>
                        <METS:fptr>
                            <xsl:attribute name="FILEID">
                                <xsl:value-of select="$id"/>
                            </xsl:attribute>
                        </METS:fptr>
                    </METS:div>
                </xsl:for-each>
            </METS:div>
        </METS:structMap>
    </xsl:template>
    <xsl:template name="copyDmdSec">
        <xsl:variable name="vetoIDs">
            <xsl:value-of select="document($structFile)//METS:structMap/METS:div[1]/@DMDID "/>
        </xsl:variable>
        <xsl:for-each select="document($structFile)//@DMDID">
            <xsl:variable name="id">
                <xsl:value-of select="."/>
            </xsl:variable>
            <xsl:if test="not($id = $vetoIDs)">
                <xsl:copy-of select="//METS:dmdSec[@ID = $id]"/>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>
