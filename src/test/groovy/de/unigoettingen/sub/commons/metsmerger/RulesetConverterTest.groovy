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
import static org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import javax.xml.transform.stream.StreamSource
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMResult

import de.unigoettingen.sub.commons.metsmerger.util.LogErrorListener
import de.unigoettingen.sub.commons.metsmerger.util.NamespaceConstants

/**
 *
 * @author cmahnke
 */
//  @TypeChecked
@Log4j
class RulesetConverterTest extends AbstractTransformerTest {
    
    def static RULESETS = [this.getClass().getResource('/rulesets/archaeo18.xml'), 
        this.getClass().getResource('/rulesets/gdz.xml')]
    
    def static TESTFILE_DMD = this.getClass().getResource('/dfg-viewer-mets/PPN645063479.mets.xml')
    def static TESTFILE_ID = this.getClass().getResource('/tei/weimar-hs-2056.tei.xml')
    
    def static TEMPLATE_PATH = '//xsl:template[@match = following-sibling::xsl:template/@match]'
                      
    @Test
    void testTransformRulesets () {
        assertNotNull('List of rulesets shouldn\'t be null', RULESETS)
        log.info('Base path: ' + this.getClass().getResource('/'))
        for (ruleset in RULESETS) {
            log.info('Processing: '+ ruleset.toString())
            assertNotNull('Ruleset shouldn\'t be null', ruleset)
            def converter = new RulesetConverter(ruleset)
            converter.transform()
            log.trace('Result:\n----------------START OF RESULT for ' + ruleset.toString() + '\n' + converter.getXML())
            log.trace('----------------END OF RESULT\n')
            log.trace('Check for unique matches in template:')
            assertTrue('Check for unique matches in template failed', checkUniquePath(converter.result, TEMPLATE_PATH))
        }
    }
    
    @Test
    void testValidateRulesetsResultXSD () {
        assertNotNull('List of rulesets shouldn\'t be null', RULESETS)
        log.info('Base path: ' + this.getClass().getResource('/'))
        for (ruleset in RULESETS) {
            log.info('Processing: '+ ruleset.toString())
            assertNotNull('Ruleset shouldn\'t be null', ruleset)
            def converter = new RulesetConverter(ruleset)
            converter.transform()
            log.trace('Check if result is valid using ' + converter.schemaUrl + ':')
            assertTrue('Validatione failed', converter.validate())
            log.trace('Result seems valid')
        }
    }
    
    @Test
    void testDMDSects () {
        //This should test if for every label a DMD Sect is created
        //First get the stylesheet from the ruleset
        def RulesetConverter converter
        converter = new RulesetConverter(RULESETS.get(0))
        //set option for DMD sections
        converter.setCreateDMDSectsParam('true')
        converter.transform()
        //Use the result to transform
        //def protected static Document transform (Source input, Source xslt, Map params) {
        def factory = TransformerFactory.newInstance()
        def transformer = factory.newTransformer(new DOMSource(converter.result))
        def listener = new LogErrorListener()
        transformer.setErrorListener(listener)
        def result = new DOMResult()
        //Pass params to the stylesheet
        //Preserve the labels
        transformer.setParameter('copyLabelParam', 'true')

        try {
            transformer.transform(new StreamSource(TESTFILE_DMD.openStream()), result)
        } catch (TransformerException te) {
            log.error("Transformation failed ", te)
        }
        if (listener.fatal) {
            log.error('Transformation failed, check the log!')
        }
        def doc = (Document) result.getNode()
        assertTrue('XPathes for DMD check failed!', dmdCheck(doc))
        
    }
    
    @Test
    void testUniqueIDs () {
        //First get the stylesheet from the ruleset
        def RulesetConverter converter
        converter = new RulesetConverter(RULESETS.get(0))
        converter.transform()
        //Use the result to transform
        //def protected static Document transform (Source input, Source xslt, Map params) {
        def factory = TransformerFactory.newInstance()
        def transformer = factory.newTransformer(new DOMSource(converter.result))
        def listener = new LogErrorListener()
        transformer.setErrorListener(listener)
        def result = new DOMResult()
        //Set up TEI converter
        def teiConverter = new Tei2Mets(TESTFILE_ID)
        teiConverter.setIdentifier('1234')
            
        teiConverter.transform()
        try {
            transformer.transform(new DOMSource(teiConverter.result), result)
        } catch (TransformerException te) {
            log.error("Transformation failed ", te)
        }
        if (listener.fatal) {
            log.error('Transformation failed, check the log!')
        }
        def doc = (Document) result.getNode()
        assertTrue('XPath for ID check failed!', checkUniquePath (doc, '//mets:*[@ID = following::mets:*/@ID]'))
    }
                
}
