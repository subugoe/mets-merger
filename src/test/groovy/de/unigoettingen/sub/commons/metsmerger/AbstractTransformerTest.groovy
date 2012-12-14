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

import org.apache.log4j.Logger
import org.custommonkey.xmlunit.Diff
import org.custommonkey.xmlunit.DifferenceListener
import org.custommonkey.xmlunit.ElementQualifier
import org.custommonkey.xmlunit.XMLUnit
import static org.junit.Assert.*
import org.jaxen.dom.DOMXPath
import org.jaxen.SimpleNamespaceContext
import org.w3c.dom.Element
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import javax.xml.transform.dom.DOMSource
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory

import de.unigoettingen.sub.commons.metsmerger.util.MetsDiff
import de.unigoettingen.sub.commons.metsmerger.util.Util
import static de.unigoettingen.sub.commons.metsmerger.util.Util.*
import de.unigoettingen.sub.commons.metsmerger.util.NamespaceConstants

/**
 *
 * @author cmahnke
 */
//@TypeChecked
@Log4j
abstract class AbstractTransformerTest {
    
    protected static Boolean assertEmptyXPathResult (String path, Document doc) {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new NamespaceConstants())
        XPathExpression expr = xpath.compile(path);
        
        def nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        
        if (nodes.getLength() != 0) {
            log.info('Result not empty for XPath ' + path + ' matches: ' + nodes.getLength())
            for (int i = 0; i < nodes.getLength(); i++) {
                log.error('Unexpected Match: ' + XmlUtil.serialize(nodes.item(i)))
            }
            return false
        }
        return true
    }
    
    static Boolean checkUniquePath (Document doc, String path) {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new NamespaceConstants())
        XPathExpression expr = xpath.compile(path);
        log.info('Checking XPath \'' + path + '\'')
        def nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        
        if (nodes.getLength() > 0) {
            log.warn('Got ' + nodes.getLength() + ' unepected results for XPath ' + path)
            //log.info('Result not empty for XPath ' + path + ' matches: ' + nodes.getLength())
            for (int i = 0; i < nodes.getLength(); i++) {
                log.error('Duplicate Match: ' + XmlUtil.serialize(nodes.item(i)))
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
    
    static Boolean dmdCheck (Document doc) {
        //get all labels and see if the content is also part of the linked dmd sects
        //fail first if there is a label but no linked ID, this should be assured 
        //and done by the result of the ruleset converter. 
        log.info('some complex XPath test, check the source code to see whats going on')
        def labelWithoutDMDIDPath = '//mets:div[@LABEL][not(@DMDID)]'   
        assertTrue(assertEmptyXPathResult(labelWithoutDMDIDPath, doc))
        
        /*
        This doesn't need to be true, some structural (?) elements don't have names
        def logicalDivsWithoutDMDIdPath = '//mets:structMap[@TYPE=\'LOGICAL\']//mets:div[not(@DMDID)]'
        assertTrue(assertEmptyXPathResult(logicalDivsWithoutDMDIdPath, domResult))
         */
        
        //This looks if there are any div's with DMD IDs that aren't resoveable
        def labelAndDMDSectDoesntMatchPath = '//mets:div[@DMDID[not(//mets:dmdSec/@ID = .)]]'
        assertTrue(assertEmptyXPathResult(labelAndDMDSectDoesntMatchPath, doc))
        
        //This checks if there are any empty DMDID Attributes
        def emptyDMDIdsPath = '//mets:div[@DMDID = \'\']'
        assertTrue(assertEmptyXPathResult(emptyDMDIdsPath, doc))
        
        return true
    }
    
    static Boolean checkStructLink (Document doc) {
        //test if there are no IDREFs in the structLink section that aren't resolvable 
        //This tests the logical structural elements
        def fromIDsNotResoveablePath = '//mets:smLink[@xlink:from[not(//mets:div/@ID = .)]]'
        assertTrue('XPath ' + fromIDsNotResoveablePath + ' failed', assertEmptyXPathResult(fromIDsNotResoveablePath, doc))
            
        //This tests the physical structural elements
        def toIDsNotResoveablePath = '//mets:smLink[@xlink:to[not(//mets:div/@ID = .)]]'
        assertTrue('XPath ' + toIDsNotResoveablePath + ' failed', assertEmptyXPathResult(toIDsNotResoveablePath, doc))
        log.info('All Ids in structLink section are resolvable')
        
        return true
    }
    
    //TODO: get rid of Jaxen
    def static getDocumentFromXPath (String path, Document doc, Map namespaces) {
        def xpath = new DOMXPath(path)
        log.info('Checking XPath \'' + path + '\'')
        def nsContext = new SimpleNamespaceContext(namespaces)
        log.info('Set up namespace context')
        xpath.setNamespaceContext(nsContext)
        def node = xpath.selectSingleNode(doc)
        return getDocumentFromNode(node)
    }
    
}

