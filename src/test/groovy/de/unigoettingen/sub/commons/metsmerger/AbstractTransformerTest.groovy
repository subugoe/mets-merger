/*
 * This file is part of the METS Merger, Copyright 2011, 2012 SUB Göttingen
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see http://www.gnu.org/licenses or write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301 USA.
 */

package de.unigoettingen.sub.commons.metsmerger

import groovy.util.logging.Log4j
import groovy.transform.TypeChecked
import groovy.xml.XmlUtil

import org.custommonkey.xmlunit.Diff
import org.custommonkey.xmlunit.DifferenceListener
import org.custommonkey.xmlunit.ElementQualifier
import org.custommonkey.xmlunit.XMLUnit
import org.jaxen.dom.DOMXPath
import org.jaxen.SimpleNamespaceContext
import org.w3c.dom.Element
import org.w3c.dom.Document
import javax.xml.transform.dom.DOMSource

import de.unigoettingen.sub.commons.metsmerger.util.MetsDiff
import de.unigoettingen.sub.commons.metsmerger.util.NamespaceConstants
import org.apache.log4j.Logger

/**
 *
 * @author cmahnke
 */
//@TypeChecked
@Log4j
abstract class AbstractTransformerTest {
    
    static String getGoobiIdentifier (Document doc) {
        def xpath = new DOMXPath('//mets:dmdSec[@ID = //mets:structMap[@TYPE=\'LOGICAL\']/mets:div/@DMDID]//goobi:metadata[@name=\'CatalogIDDigital\']')
        def nsContext = new SimpleNamespaceContext()
        nsContext.addNamespace("goobi", NamespaceConstants.GOOBI_NAMESPACE)
        nsContext.addNamespace("mets", NamespaceConstants.METS_NAMESPACE)
        xpath.setNamespaceContext(nsContext)
        def nodes = xpath.selectNodes(doc)
        if (nodes.size() != 1 ) {
            log.info('Search for Goobi Identifier returned no or to many identifiers')
            return null
        }
        Element e = (Element) nodes.get(0)
        return e.getTextContent()
    }
    
    static Boolean checkUniquePath (Document doc, String path) {
        //TODO: Pass a map with prefix and URL for namespaces
        //Checks matches that occure more then once
        def xpath = new DOMXPath(path)
        log.info('Checking XPath \'' + path + '\'')
        def nsContext = new SimpleNamespaceContext()
        nsContext.addNamespace("xsl", NamespaceConstants.XSLT_NAMESPACE)
        xpath.setNamespaceContext(nsContext)
        def nodes = xpath.selectNodes(doc)
        
        if (nodes.size() > 0) {
            log.warn('Got ' + nodes.size() + ' unepected results')
            for (int i = 0; i < nodes.size(); i++) {
                Element e = (Element) nodes.get(i)
                log.error('Duplicate match attribute: ' + e.getAttribute("match"))
            }
            return false
        }
        return true
    }

    static Boolean diffMetsDocsSimilar (Document truth, Document generated) {
        MetsDiff.getMetsDiff(truth, generated).similar()

    }
    
    static logXmlFragment (Logger logger, String source, Document doc) {
        logger.trace('----------------START OF RESULT for ' + source + '\n' + XmlUtil.serialize(doc.documentElement))
        logger.trace('----------------END OF RESULT\n')
    }
    
    
	
}

