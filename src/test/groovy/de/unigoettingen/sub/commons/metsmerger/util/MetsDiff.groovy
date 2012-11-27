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

package de.unigoettingen.sub.commons.metsmerger.util

import groovy.util.logging.Log4j
import groovy.transform.TypeChecked
import groovy.xml.XmlUtil

import org.custommonkey.xmlunit.Diff
import org.custommonkey.xmlunit.DifferenceListener
import org.custommonkey.xmlunit.DifferenceConstants
import org.custommonkey.xmlunit.Difference
import org.custommonkey.xmlunit.ElementQualifier
import org.custommonkey.xmlunit.ElementNameAndAttributeQualifier
import org.custommonkey.xmlunit.NodeDetail
import org.custommonkey.xmlunit.XMLUnit
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.Text

/**
 *
 * @author cmahnke
 */
//@TypeChecked
@Log4j
class MetsDiff implements DifferenceListener {
    //Set up some static settings of XMLUnit
    static {
        XMLUnit.setIgnoreWhitespace(true)
        XMLUnit.setIgnoreComments(true)
        XMLUnit.setIgnoreAttributeOrder(true)
        XMLUnit.setCompareUnmatched(true)
    }
    //Stuff to ignore
    static IGNORED_ATTRIBUTES = ['ID', 'LABEL', 'DMDID', 'AMDID'] 
    static IGNORED_EVENTS = [DifferenceConstants.ELEMENT_NUM_ATTRIBUTES_ID, 
        DifferenceConstants.ATTR_NAME_NOT_FOUND_ID
    ]
    /** These aren't exported and are thus not part of the data that can be reimported */
    static IGNORED_GOOBI_METADATA_ATTRIBUTES = ['TitleDocMainShort', 'CreatorsAllOrigin']
    static IGNORED_GOOBI_METADATA_ELEMENTS = ['identifier']
    static IGNORED_GOOBI_METADATA_CHILD_TYPES = ['person']
    
    MetsDiff () {
        log.trace('The constants are available at http://xmlunit.svn.sourceforge.net/viewvc/xmlunit/tags/XMLUnit-Java-1.3/src/java/org/custommonkey/xmlunit/DifferenceConstants.java?revision=498&view=markup')
    }
    
    private boolean isIgnoredDifference(Difference difference) {
        def diffId = difference.getId()
        Node truthNode = difference.getControlNodeDetail().getNode()
   
        //Not our Namespace, don't ignore!
        def checkNamespace
        if (truthNode.getNodeType() == Node.ELEMENT_NODE) {
            checkNamespace = truthNode.getNamespaceURI()
        } else if (truthNode.getNodeType() == Node.ATTRIBUTE_NODE) {
            checkNamespace = truthNode.getParentNode().getNamespaceURI()
        } else {
            false
        }
        
        if (checkNamespace != NamespaceConstants.METS_NAMESPACE && checkNamespace != NamespaceConstants.GOOBI_NAMESPACE) {
            log.trace('Difference is not in our namespace , don\'t ignore! Namespace is ' + truthNode.getNamespaceURI())
            false
        }
        
        //Accept different count of a Attributes
        if (diffId in IGNORED_EVENTS) {
            log.trace('Ignore DifferenceConstants ' + diffId)
            return true
        }
        
        //Accept if we are dealing with certain attributes
        if (truthNode.getNodeType() == Node.ATTRIBUTE_NODE) {
            log.info('Attr node found')
            if (truthNode.getNodeName() in IGNORED_ATTRIBUTES) {
                log.trace('Ignoring attribute name ' + truthNode.getNodeName())
                return true
            }
            if (truthNode.getNodeValue() in IGNORED_GOOBI_METADATA_ATTRIBUTES) {
                log.trace('Ignoring attribute value ' + truthNode.getNodeValue())
                return true
            }
        }
        
        //Goobi namespace exceptions
        if (truthNode.getNamespaceURI() == NamespaceConstants.GOOBI_NAMESPACE) {
            log.trace('Control node is in the goobi namespace, additional test apply')
            //Accept different node count in Goobi namespace
            if (diffId == DifferenceConstants.CHILD_NODELIST_LENGTH_ID) {
                //Missing data from persons
                def type = truthNode.getAttributes()?.getNamedItem('type')?.getValue()
                if (type in IGNORED_GOOBI_METADATA_CHILD_TYPES) {
                    log.trace('Ignoring different number of childs for person metadata for: ' + truthNode.getNodeName())
                    return true
                }
                //Everything else
                //TODO: Finish this
                log.trace('Ignoring different number of childs for: ' + truthNode.getNodeName())
                return true
            } 
            
            if (diffId == DifferenceConstants.CHILD_NODE_NOT_FOUND_ID) {
                //Accept elements with expected missing @name in Goobi namespace
                def nameAttribute = truthNode.getAttributes()?.getNamedItem('name')?.getValue()
                
                if (nameAttribute in IGNORED_GOOBI_METADATA_ATTRIBUTES) {
                    log.trace('Ignoring missing child for node: ' + truthNode.getNodeName())
                    return true
                }
                //Accept elements that are removed during export to DMS
                def elementName = truthNode.getLocalName()
                if (elementName in IGNORED_GOOBI_METADATA_ELEMENTS) {
                    log.trace('Ignoring missing child for node: ' + truthNode.getNodeName())
                    return true
                }
            }
            //Ignore sequence
            if (diffId == DifferenceConstants.CHILD_NODELIST_SEQUENCE_ID) {
                return true
            }
           
        }        
        log.error('Not accepted difference: ' + difference)
        log.info('XML Unit: ID ' + diffId)
        log.warn('Error node name: ' + truthNode.getNodeName())
        log.warn('Error node namespace: ' + truthNode.getNamespaceURI())
        log.warn('Error node XPath: ' + difference.getControlNodeDetail().getXpathLocation())
        log.trace('Contol node: ' + XmlUtil.serialize(truthNode))
        return false;
    }

    public int differenceFound(Difference difference) {
        log.debug('Checking difference: ' + difference)
        if (isIgnoredDifference(difference)) {
            return DifferenceListener.RETURN_IGNORE_DIFFERENCE_NODES_SIMILAR;
        } else {
            return DifferenceListener.RETURN_ACCEPT_DIFFERENCE;
        }
    }
    
    public void skippedComparison(Node control, Node test) {
    }
    
    public static ElementQualifier getElementQualifier () {

        new ElementNameAndAttributeQualifier() {
            public boolean qualifyForComparison(Element control, Element test) {
                if (super.qualifyForComparison(control, test)) {
                    if (control.getNamespaceURI() == NamespaceConstants.GOOBI_NAMESPACE) {
                        def attributes = areAttributesComparable(control, test)
                        def text = similar(extractText(control), extractText(test));
                        if (attributes && text) {
                            return true
                        }
                        return false
                    }
                    return true
                }
                return false
            }
            
            //Stolen from XMLUnit
            //see http://xmlunit.svn.sourceforge.net/viewvc/xmlunit/tags/XMLUnit-Java-1.3/src/java/org/custommonkey/xmlunit/ElementNameAndTextQualifier.java?revision=498&view=markup
            /**
             * Determine whether the text nodes contain similar values
             * @param control
             * @param test
             * @return true if text nodes are similar, false otherwise
             */
            protected boolean similar(Text control, Text test) {
                if (control == null) {
                    return test == null
                } else if (test == null) {
                    return false
                }
                return control.getNodeValue().equals(test.getNodeValue())
            }
            /**
             * Extract the normalized text from within an element
             * @param fromElement
             * @return extracted Text node (could be null)
             */
            protected Text extractText(Element fromElement) {
                fromElement.normalize();
                NodeList fromNodeList = fromElement.getChildNodes();
                Node currentNode;
                for (int i=0; i < fromNodeList.getLength(); ++i) {
                    currentNode = fromNodeList.item(i);
                    if (currentNode.getNodeType() == Node.TEXT_NODE) {
                        return (Text) currentNode;
                    }
                }
                return null;
            } 
            
        }
    }
    
    static protected Diff getMetsDiff (Document truth, Document generated, DifferenceListener listener, ElementQualifier qualifier) {
        log.info('Called with coustom Difference Listener: ' + listener.getClass())
        def d = new Diff(truth, generated)
        if (listener != null) {
            d.overrideDifferenceListener(listener)
        }
        if (qualifier != null) {
            d.overrideElementQualifier(qualifier)
        }
        return d
    }
    
    static public Diff getMetsDiff (Document truth, Document generated) {
        DifferenceListener differ = new MetsDiff()
        ElementQualifier qualifier = getElementQualifier()
        getMetsDiff(truth, generated, differ, qualifier)
    }
    
}
    


