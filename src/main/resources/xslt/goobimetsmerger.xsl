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

    <!-- 
    Development and bug fixes
    Terminology used in Comments and messages
    
    Merge file: A Goobi METS file of a ne created "Vorgang" - Process, that will
                be enriched with structure metadata from external source.
    Structure file: Also called external Document, pseudo Goobi METS file 
                    containing structural metadata to be included in a real Goobi process.
    -->
    <xsl:output indent="yes" encoding="UTF-8" method="xml"/>
    <xsl:preserve-space elements="*"/>
    <!-- 
    The file to take the structure from
    -->
    <xsl:param name="structFileParam"/>
    <xsl:param name="copyPhysicalStructMapParam" select="false()"/>
    <xsl:param name="fileSectionParam" select="'PRESENTATION'"/>
    <!-- 
    Create even if there are errors
    -->
    <xsl:param name="forceParam" select="false()"/>
    <!--
    This is needed to make sure that structLink Elements match the generated
    structure, otherwise the merge will fail if there is a structLink section
    in the document the structure will be merged to
    -->
    <xsl:param name="overwriteStructLinkParam" select="false()"/>

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
        select="if ($fileSectionParam castable as xs:string and xs:string($fileSectionParam) != '') then xs:string($fileSectionParam) else 'PRESENTATION'"
        as="xs:string"/>
    <xsl:variable name="overwriteStructLink"
        select="if ($overwriteStructLinkParam castable as xs:boolean) then xs:boolean($overwriteStructLinkParam) else false()"
        as="xs:boolean"/>
    <xsl:variable name="force"
        select="if ($forceParam castable as xs:boolean) then xs:boolean($forceParam) else false()"
        as="xs:boolean"/>
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
        <!-- For debuging of the given path -->
        <xsl:comment>
            <xsl:text>This file was merged by GoobiMETSMerger Version </xsl:text>
            <xsl:value-of select="$version"/>
            <xsl:text>Struct file is '</xsl:text><xsl:value-of select="$structFile"/><xsl:text>',
                Struct file param is'</xsl:text><xsl:value-of select="$structFileParam"/><xsl:text>' </xsl:text>
            <xsl:text>Params:</xsl:text>
            <xsl:text>copyPhysicalStructMap: </xsl:text><xsl:value-of select="$copyPhysicalStructMap"/>
            <xsl:text>fileSection: </xsl:text><xsl:value-of select="$fileSection"/>
            <xsl:text>overwriteStructLink: </xsl:text><xsl:value-of select="$overwriteStructLink"/>
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
        <xsl:if test=".//METS:smLink[@xlink:from != '' and @xlink:to != '']">
            <xsl:choose>
                <xsl:when test="not($overwriteStructLink)">
                    <!-- Fail if structLink is not empty -->
                    <xsl:message terminate="yes">StructLinks of merge document are not empty!
                        Exiting!</xsl:message>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:message>StructLinks of document are not empty! Ignoring!</xsl:message>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:if>
        <!--
        This builds some variables to do a basic maping for indentifiers to rewrite the 
        structLink part of the METS Document, just get the root of the physical and logical
        structure.
        -->
        <xsl:variable name="myPhysicalRootId">
            <xsl:if test="not(//METS:structMap[@TYPE = 'PHYSICAL']/METS:div[1]/@ID)">
                <xsl:message terminate="yes">Couldn't find DMDID for first physical structure
                    element in merge file!</xsl:message>
            </xsl:if>
            <xsl:value-of select="//METS:structMap[@TYPE = 'PHYSICAL']/METS:div[1]/@ID"/>
        </xsl:variable>
        <xsl:variable name="externalPhysicalRootId">
            <xsl:if
                test="not(document($structFile)//METS:structMap[@TYPE = 'PHYSICAL']/METS:div[1]/@ID)">
                <xsl:message terminate="yes">Couldn't find DMDID for first physical structure
                    element in structure file!</xsl:message>
            </xsl:if>
            <xsl:value-of
                select="document($structFile)//METS:structMap[@TYPE = 'PHYSICAL']/METS:div[1]/@ID"/>
        </xsl:variable>
        <xsl:variable name="myLogicalRootId">
            <xsl:if test="not(//METS:structMap[@TYPE = 'LOGICAL']/METS:div[1]/@ID)">
                <xsl:message terminate="yes">Couldn't find DMDID for first logical structure element
                    in merge file!</xsl:message>
            </xsl:if>
            <xsl:value-of select="//METS:structMap[@TYPE = 'LOGICAL']/METS:div[1]/@ID"/>
        </xsl:variable>
        <xsl:variable name="externalLogicalRootId">
            <xsl:if
                test="not(document($structFile)//METS:structMap[@TYPE = 'LOGICAL']/METS:div[1]/@DMDID)">
                <xsl:message terminate="yes">Couldn't find DMDID for first logical structure element
                    in structure file!</xsl:message>
            </xsl:if>
            <xsl:value-of
                select="document($structFile)//METS:structMap[@TYPE = 'LOGICAL']/METS:div[1]/@DMDID"
            />
        </xsl:variable>
        <!-- Get the structLink section from external document -->
        <xsl:variable name="structLink">
            <xsl:copy-of select="document($structFile)//METS:structLink/*"/>
        </xsl:variable>
        <!-- 
        Copy unknown elements and replace known IDs
        -->
        <xsl:copy copy-namespaces="no">
            <xsl:copy-of select="@*"/>
            <xsl:for-each select="$structLink/*">
                <xsl:choose>
                    <xsl:when test="local-name(.) = 'smLink'">
                        <xsl:copy copy-namespaces="no" exclude-result-prefixes="METS xlink">
                            <!-- copy unknown attributes -->
                            <xsl:copy-of select="@* except @xlink:from except @xlink:to"/>
                            <xsl:if test="not($force) and @xlink:from and not(document($structFile)//@ID[. = @xlink:from])">
                                <xsl:message terminate="yes">No ID found: <xsl:value-of select="@xlink:from"/></xsl:message>
                            </xsl:if>
                            <xsl:choose>
                                <xsl:when
                                    test="@xlink:from and @xlink:from = $externalLogicalRootId">
                                    <xsl:attribute name="xlink:from" select="$myLogicalRootId"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:copy-of select="@xlink:from"/>
                                </xsl:otherwise>
                            </xsl:choose>
                            <xsl:if test="not($force) and @xlink:to and not(document($structFile)//@ID[. = @xlink:to])">
                                <xsl:message terminate="yes">No ID found: <xsl:value-of select="@xlink:to"/></xsl:message>
                            </xsl:if>
                            <xsl:choose>
                                <xsl:when test="@xlink:to and @xlink:to = $externalPhysicalRootId">
                                    <xsl:attribute name="xlink:to" select="$myPhysicalRootId"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:copy-of select="@xlink:to"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </xsl:copy>
                    </xsl:when>
                    <!-- copy unknown elements -->
                    <xsl:otherwise>
                        <xsl:copy-of select="."/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:for-each>
        </xsl:copy>
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
        <!-- Fail if there is no source for the fileSect -->
        <xsl:if test="not(document($structFile)//METS:fileSec/METS:fileGrp[@USE=$fileSection])">
            <xsl:message terminate="yes">
                Given fileGrp '<xsl:value-of select="$fileSection"/>' not found!
            </xsl:message>
        </xsl:if>
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
    <xsl:template name="getFileID">
        <xsl:text>FILE_</xsl:text>
        <xsl:number format="0001" value="position()"/>
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
