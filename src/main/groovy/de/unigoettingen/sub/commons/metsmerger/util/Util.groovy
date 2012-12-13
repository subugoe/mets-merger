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

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.Transformer
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import javax.xml.namespace.NamespaceContext
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import ugh.dl.DigitalDocument
import ugh.dl.Prefs
import ugh.exceptions.ReadException
import ugh.exceptions.PreferencesException
import ugh.fileformats.mets.MetsMods
import ugh.fileformats.mets.MetsModsImportExport

import de.unigoettingen.sub.commons.metsmerger.util.NamespaceConstants

/**
 * This class contains some static utility methods for DOM handling (like read and write) and for validation.
 * @author cmahnke
 */

//@TypeChecked
@Log4j
class Util {
    /**
     * Writes a DOM {@link org.w3c.dom.Document Document} to the given {@link java.io.OutputStream OutputStream}
     * 
     * @param doc the {@link org.w3c.dom.Document Document} to be written
     * @param out the {@link java.io.OutputStream OutputStream} where the {@link org.w3c.dom.Document Document} will be written to 
     */
    static void writeDocument (Document doc, OutputStream out) {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        def source = new DOMSource(doc);
        def result = new StreamResult(out);
        transformer.transform(source, result);
    }
    
    /**
     * Writes a DOM {@link org.w3c.dom.Document Document} to the given {@link java.io.File File}
     * 
     * @param doc the {@link org.w3c.dom.Document Document} to be written
     * @param file the {@link java.io.File File} where the {@link org.w3c.dom.Document Document} will be written to
     * @see #writeDocument (org.w3c.dom.Document Document,java.io.OutputStream)
     */
    static void writeDocument (Document doc, File file) {
        writeDocument(doc, new FileOutputStream(file))
    }
    
    /**
     * Writes a DOM {@link org.w3c.dom.Document Document} to the given {@link java.net.URL URL}.
     * Please not that this only works for local URLs (with file:// prefix)
     * 
     * @param doc the {@link org.w3c.dom.Document Document} to be written
     * @param url the {@link java.net.URL URL} where the {@link org.w3c.dom.Document Document} will be written to
     * @see #writeDocument (org.w3c.dom.Document Document,java.io.OutputStream)
     * @see #writeDocument (org.w3c.dom.Document Document,java.io.File)
     */
    static void writeDocument (Document doc, URL url) {
        writeDocument(doc, new File(url.toURI()))
    }
    
    /**
     * Loads a DOM {@link org.w3c.dom.Document Document} from an {@link java.net.URL URL}.
     * This is just a wrapper for {@link #loadDocument(InputStream) loadDocument}
     * @param xml the {@link java.net.URL URL} of the document to be loaded
     * @return the loaded DOM {@link org.w3c.dom.Document Document}
     * @see #loadDocument(InputStream)
     */
    static Document loadDocument (URL xml) {
        loadDocument(xml.openStream())
    }
    
    /**
     * Loads a DOM {@link org.w3c.dom.Document Document} from an {@link java.io.InputStream InputStream}
     * @param input the {@link java.io.InputStream InputStream} of the document to be loaded
     * @return the loaded DOM {@link org.w3c.dom.Document Document}
     */
    static Document loadDocument (InputStream input) {
        DocumentBuilder builder = getDocumentBuilder()
        builder.parse(input)
    }
    
    /**
     * Validates a given {@link org.w3c.dom.Document Document} agains a ruleset using UGH.
     * @param ruleset the {@link java.net.URL URL} of the rulset to be used for the validation
     * @param metadataDoc the {@link org.w3c.dom.Document Document} to be validated
     * @return whether the validation was successfull or not
     * @see #ughValidate(java.net.URL,java.io.File)
     */
    static Boolean ughValidate (URL ruleset, Document metadataDoc) {
        //create a temp file
        File out = File.createTempFile('ugh-result-check', '.xml')
        log.trace('Got w3C DOM Document, write File to ' + out.getAbsolutePath())
        writeDocument(metadataDoc, out)
        def valid = ughValidate(ruleset, out)
        //delete the temp file
        out.delete()
        log.trace('Deleted temp file')
        return valid
    }
    
    /**
     * Validates a given {@link java.io.File File} agains a ruleset using UGH.
     * @param ruleset the {@link java.net.URL URL} of the rulset to be used for the validation
     * @param metadataFile the {@link java.io.File File} to be validated
     * @return whether the validation was successfull or not
     * 
     */
    static Boolean ughValidate (URL ruleset, File metadataFile) {
        log.trace('Checking file ' + metadataFile.getAbsolutePath() + ' using ruleset ' + ruleset.toString())
        Prefs preferences = new Prefs();
        preferences.loadPrefs(new File(ruleset.toURI()).getAbsolutePath());
        try {
            //def mets = new MetsModsImportExport(preferences)
            def mets = new MetsMods(preferences)
            mets.read(metadataFile.getAbsolutePath());
            DigitalDocument myDocument = mets.getDigitalDocument();
                                        
        } catch (PreferencesException pe) {
            log.error('Couldn\'t load Ruleset!', pe)
            return false
        } catch (ReadException re) {
            log.error('Couldn\'t read DigitalDocument!', re)
            return false
        }
        return true
    }
    
    /**
     * Gets a {@link javax.xml.parsers.DocumentBuilder DocumentBuilder} 
     * @returns a {@link javax.xml.parsers.DocumentBuilder DocumentBuilder}  instance
     */
    protected static DocumentBuilder getDocumentBuilder () {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
        factory.setNamespaceAware(true)
        return factory.newDocumentBuilder()
    }
    
    /**
     * Transforms a {@Link org.w3c.dom.Node Node} into it's own {@link org.w3c.dom.Document Document}
     * @param node the {@Link org.w3c.dom.Node Node} to be transformed
     * @return a {@link org.w3c.dom.Document Document}
     */
    static Document getDocumentFromNode (Node node) {
        Document docTarget = Util.getDocumentBuilder().newDocument()
        docTarget.appendChild(docTarget.adoptNode(node.cloneNode(true)));
        return docTarget
    }
    
    
    /**
     * Returns the namespace of the root element of the given {@link java.net.URL URL}
     * Make sure that the parser being used is namespace aware.
     * @param xml the {@link java.net.URL URL} of the document which root namespace should be retrived.
     * @returns the namespace URI as String
     * @see #getRootNamespace(org.w3c.dom.Document)
     */
    static String getRootNamespace (URL xml) {
        getRootNamespace(loadDocument(xml))
    }

    /**
     * Returns the namespace of the root element of the given {@link org.w3c.dom.Document Document}#+
     * Make sure that the parser being used is namespace aware.
     * @param doc the {@link org.w3c.dom.Document Document} which root namespace should be retrived.
     * @returns the namespace URI as String 
     */
    static String getRootNamespace (Document doc) {
        //Default namespace set
        if (doc.getDocumentElement().getAttributeNode("xmlns")) {
            doc.getDocumentElement().getAttributeNode("xmlns").getValue()
        } else {
            //No default namespace set, get namespace of root element
            doc.getDocumentElement().getNamespaceURI()
        }
    }
    
    /**
     * Returns al list of the used namespaces inside the provided document
     * @param url the {@link java.net.URL URL} for the document.
     * @see #getNamespaces(org.w3c.dom.Document)
     * @returns the namespace URIs as List<String> 
     */
    static List<String> getNamespaces (URL xml) {
        getNamespaces(loadDocument(xml))
    }
    
    /**
     * Returns a list of the used namespaces inside the provided document
     * @param doc the {@link org.w3c.dom.Document Document} 
     * @returns the namespace URIs as List<String>
     */
    static List<String> getNamespaces (Document doc) {
        List<String> namespaces = []
        def path = '//namespace::*[not(. = following::*/namespace::*)]'
            
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new NamespaceConstants())
        XPathExpression expr = xpath.compile(path);
        
        def nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        
        if (nodes.getLength() > 0) {
            log.info('Got ' + nodes.getLength() + ' distict namespace nodes')
            for (int i = 0; i < nodes.getLength(); i++) {
                Node n = (Node) nodes.item(i);
                def prefix = n.getNodeName()
                def uri = n.getNodeValue()
                namespaces.add(uri)
                log.trace('Got namespace: ' + uri + ' for prefix: ' + prefix)
            }
        } else {
            log.warn('No namespace nodes found') 
        }
        namespaces
    }
    
    /**
     * Returns the name of the root element of the provided document
     * @param url the {@link java.net.URL URL} for the document.
     * @returns the root element name as String 
     */
    static String getRootElementName (URL xml) {
        loadDocument(xml).documentElement.getTagName()
    }
    
    /**
     * Returns the Goobi Identifier from a given Document
     * @param doc the {@link org.w3c.dom.Document Document} to look into.
     * @returns the identifier as String or null
     */
    static String getGoobiIdentifier (Document doc) {
        def path = '//mets:dmdSec[@ID = //mets:structMap[@TYPE=\'LOGICAL\']/mets:div/@DMDID]//goobi:metadata[@name=\'CatalogIDDigital\']'
            
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        xpath.setNamespaceContext(new NamespaceConstants())
        XPathExpression expr = xpath.compile(path);
        
        def nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        
        if (nodes.getLength() != 1) {
            log.info('Search for Goobi Identifier returned no or to many identifiers')
            return null
        }
        Element e = (Element) nodes.item(0)
        return e.getTextContent()
    }
    
    /**
     * This Enum is used to parse the supported input and output formats
     * @author cmahnke
     */
    enum FORMAT {
        TEI('TEI'), DFG('DFG'), GOOBI('GOOBI'), XSL('XSL'), RULESET('RULESET'), METS('METS') ,UNKNOWN('UNKNOWN')
        
        def name
        
        FORMAT(String name) { 
            this.name = name 
        }
        
        /**
         * Get a FORMAT for a given String
         * @returns FORMAT the format or null
         */ 
        public static FORMAT fromString(String format) {
            if (format != null) {
                for (FORMAT f : FORMAT.values()) {
                    if (format.equalsIgnoreCase(f.name)) {
                        return f
                    }
                }
            }
            return null
        }

        /**
         * Get a String representations of all formats
         * @returns String the formats
         */ 
        public static String getFormats() {
            def formats = ""
            for (FORMAT f : FORMAT.values()) {
                formats + f.name + " "
            }
            return formats
        }
        
    }
    
}