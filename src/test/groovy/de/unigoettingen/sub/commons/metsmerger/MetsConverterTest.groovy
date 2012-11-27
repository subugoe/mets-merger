/*
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

import static org.junit.Assert.*
import org.jaxen.dom.DOMXPath
import org.jaxen.SimpleNamespaceContext
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test
import org.w3c.dom.Document

import de.unigoettingen.sub.commons.metsmerger.util.MetsDiff
import de.unigoettingen.sub.commons.metsmerger.util.NamespaceConstants
import de.unigoettingen.sub.commons.metsmerger.util.XSDValidator
import static de.unigoettingen.sub.commons.metsmerger.util.Util.*

/**
 *
 * @author cmahnke
 */
//@TypeChecked
@Log4j
class MetsConverterTest extends AbstractTransformerTest {
    static Boolean DELETE_TMP_FILES = false
   
    def static TEST_DATA = [new TestData(this.getClass().getResource("/rulesets/archaeo18.xml"), this.getClass().getResource("/processes/24580.goobi.mets.xml"), this.getClass().getResource("/dfg-viewer-mets/PPN645063479.mets.xml"))]
    
    def static TEST_PATHS = ['//mets:structMap[@TYPE="LOGICAL"]', 
        //'//mets:structMap[@TYPE="PHYSICAL"]', - This doesn't work since the file sections are different
                             '//mets:dmdSec[@ID =//mets:structMap[@TYPE="LOGICAL"]/mets:div/@DMDID]']
    
    @BeforeClass
    static void setup () {
        for (testSet in TEST_DATA) {
            log.info('Setting up MetsConverter with ruleset ' + testSet.ruleset.toString() + ' for file ' + testSet.dfgViewerMets.toString())
            testSet.converter = new MetsConverter(testSet.ruleset, testSet.dfgViewerMets)
            //TODO: Add a test if this really works
            //testSet.converter.setCopyLabelParam('true')
            testSet.resultFile = File.createTempFile('result', '.xml')
            log.info('Result file will be ' + testSet.resultFile.getAbsolutePath())
        }
        
    }
        
    @Test
    void testDfg2GoobiMetsTransform () {
        for (testSet in TEST_DATA) {
            log.info('Processing ruleset ' + testSet.ruleset)
            
            testSet.converter.transform()
            log.trace('Result:\n----------------START OF RESULT\n' + testSet.converter.getXML())
            log.trace('----------------END OF RESULT')
            assertNotNull('Check if Result is not null failed!', testSet.converter.result)
            log.info('Ruleset transformed')
        }
    }
    
    @Test
    void testDfg2GoobiMetsXSDValidate () {
        for (testSet in TEST_DATA) {
            log.trace('Check if result is valid using XML Schema ' + testSet.converter.schemaUrl + ':')
            log.trace('Not that this test depends on an external hosted schema, it will fail if you are offline or the schema isn\'t available')
            assertTrue('Validatione failed', testSet.converter.validate())
            log.trace('Result is valid (XML Schema)')
        }
    }
    
    @Test
    void testDfg2GoobiMetsUGHValidate () {
        for (testSet in TEST_DATA) {
            //Use UGH for validation
            writeDocument(testSet.converter.result, testSet.resultFile)
            log.info('Document written to ' + testSet.resultFile.getAbsolutePath())
            log.info('Validate using UGH')
            assertTrue('UGH couln\'t load result!', ughValidate(testSet.ruleset, testSet.resultFile))
            log.trace('Result is valid (UGH)')
        }
    }
    
    @Test
    void testDfg2GoobiMetsCompareMetadata () {
        for (testSet in TEST_DATA) {
            //load the truth
            log.info('Loading Document ' + testSet.processMets.toString() + 'as truth')
            log.info('Using ruleset ' + testSet.ruleset)
            log.info('Using transformation result for ' + testSet.dfgViewerMets)
            
            def truth = loadDocument(testSet.processMets);
            
            def original = loadDocument(testSet.dfgViewerMets);
            for (xpath in TEST_PATHS) {
                log.info('Creating fragments for xpath ' + xpath)
                //this is for debugging
                def originalFragment = getDocumentFromXPath(xpath, original, NamespaceConstants.NAMESPACES_WITH_PREFIX)
                log.trace('Original Fragment:\n----------------START OF RESULT\n' + XmlUtil.serialize(originalFragment.documentElement))
                log.trace('----------------END OF RESULT')
                
                //This is the real test
                def truthFragment = getDocumentFromXPath(xpath, truth, NamespaceConstants.NAMESPACES_WITH_PREFIX)
                def resultFragment = getDocumentFromXPath(xpath, testSet.converter.result, NamespaceConstants.NAMESPACES_WITH_PREFIX)
                //.documentElement
                log.trace('Truth Fragment:\n----------------START OF RESULT\n' + XmlUtil.serialize(truthFragment.documentElement))
                log.trace('----------------END OF RESULT')
                log.trace('Generated Fragment:\n----------------START OF RESULT\n' + XmlUtil.serialize(resultFragment.documentElement))
                log.trace('----------------END OF RESULT')
                
                //assertTrue('Fragments for path ' + xpath + ' are not similar', diffDocsSimilar(truthFragment, resultFragment, new MetsDiff()))
                assertTrue('Fragments for path ' + xpath + ' are not similar', diffMetsDocsSimilar(truthFragment, resultFragment))
            }

            log.info('Document ' + testSet.processMets + 'checked')
        }
    }
    
    //TODO: Remove this or add working comparision
    @Ignore
    @Test
    void testDfg2GoobiMetsCompare () {
        for (testSet in TEST_DATA) {
            //load the truth
            log.info('Loading Document ' + testSet.processMets.toString())
            def truth = loadDocument(testSet.processMets);
            log.info('Checking simulariry')
            assertTrue('Documents are not similar', diffDocsSimilar(truth, testSet.converter.result))
            log.info('Documents are similar')
        }
    }
     
    def static getDocumentFromXPath (String path, Document doc, Map namespaces) {
        def xpath = new DOMXPath(path)
        log.info('Checking XPath \'' + path + '\'')
        def nsContext = new SimpleNamespaceContext(namespaces)
        log.info('Set up namespace context')
        xpath.setNamespaceContext(nsContext)
        def node = xpath.selectSingleNode(doc)
        return getDocumentFromNode(node)
    }
        
    @AfterClass
    static void cleanUp () {
        for (testSet in TEST_DATA) {
            if (DELETE_TMP_FILES == true) {
                log.info('Deleting temp file ' + testSet.resultFile.getAbsolutePath())
                testSet.resultFile.delete()
            }
        }
    }
    
}

