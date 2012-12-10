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

import static org.junit.Assert.*
import groovy.util.logging.Log4j
import groovy.transform.TypeChecked

import org.junit.Test;
import org.junit.AfterClass
import org.junit.BeforeClass

import de.unigoettingen.sub.commons.metsmerger.util.XSDValidator
import static de.unigoettingen.sub.commons.metsmerger.util.Util.*
import static de.unigoettingen.sub.commons.metsmerger.AbstractTransformerTest.*

/**
 *
 * @author cmahnke
 */
@Log4j
//@TypeChecked
class MetsMergerTest {
    static Boolean DELETE_TMP_FILES = false
    
    def static TEST_DATA = [new TestData(this.getClass().getResource("/rulesets/archaeo18.xml"), this.getClass().getResource("/processes/rom-38301.goobi.mets.xml"), this.getClass().getResource("/tei/rom-heyne1798.tei.xml")),
        new TestData(this.getClass().getResource("/rulesets/archaeo18.xml"), this.getClass().getResource("/processes/weimar-41874.goobi.mets.xml"), this.getClass().getResource("/tei/weimar-hs-2057.tei.xml"))]

    @BeforeClass
    static void setup () {
        for (testSet in TEST_DATA) {
            //load process to be merged to
            def goobiDoc = loadDocument(testSet.processMets)
            //Get Identifier from METS to be merged
            def identifier = getGoobiIdentifier(goobiDoc)
            log.info('Got identifier ' + identifier + ' from file ' + testSet.processMets.toString())
            //Convert TEI to METS
            def converter = new Tei2Mets(testSet.dfgViewerMets)
            converter.setIdentifier(identifier)
            converter.transform()
            //Save the result METS as input for the merger
            File mets = File.createTempFile('tei2mets-result', '.xml')
            writeDocument(converter.result, mets)
            //set generated file as input
            testSet.dfgViewerMets = mets.toURL()
            log.info('Saved generated file to ' + testSet.dfgViewerMets)
            
            log.info('Setting up MetsMerger with ruleset ' + testSet.ruleset.toString() + ' for file ' + testSet.dfgViewerMets.toString())
            testSet.converter = new MetsMerger(testSet.dfgViewerMets, testSet.processMets)
            testSet.resultFile = File.createTempFile('result', '.xml')
            log.info('Result file will be ' + testSet.resultFile.getAbsolutePath())
        }
    }
    
    @Test
    void testMerge () {
        for (testSet in TEST_DATA) {
            testSet.converter.transform()
            log.trace('Result:\n----------------START OF RESULT\n' + testSet.converter.getXML())
            log.trace('----------------END OF RESULT')
            assertNotNull('Check if Result is not null failed!', testSet.converter.result)
            log.info('METS merged')
        }
    }
    
    @Test
    void testDfg2GoobiMetsXSDValidate () {
        for (testSet in TEST_DATA) {
            testSet.converter.transform()
            log.trace('Check if result is valid using XML Schema ' + testSet.converter.schemaUrl + ':')
            log.trace('Not that this test depends on an external hosted schema, it will fail if you are offline or the schema isn\'t available')
            assertTrue('Validatione failed', testSet.converter.validate())
            log.trace('Result is valid (XML Schema)')
        }
    }
    
    @Test
    void testDfg2GoobiMetsUGHValidate () {
        for (testSet in TEST_DATA) {
            testSet.converter.transform()
            //Use UGH for validation
            writeDocument(testSet.converter.result, testSet.resultFile)
            log.info('Document written to ' + testSet.resultFile.getAbsolutePath())
            log.info('Validate using UGH')
            assertTrue('UGH couln\'t load result!', ughValidate(testSet.ruleset, testSet.resultFile))
            log.trace('Result is valid (UGH)')
        }
    }
    
    @AfterClass
    static void cleanUp () {
        for (testSet in TEST_DATA) {
            if (DELETE_TMP_FILES == true) {
                log.info('Deleting temp file ' + testSet.resultFile.getAbsolutePath())
                new File(testSet.dfgViewerMets).delete()
                testSet.resultFile.delete()
            }
        }
    }
	
}

