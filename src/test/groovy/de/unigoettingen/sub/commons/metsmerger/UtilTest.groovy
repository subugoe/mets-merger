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
import org.jaxen.SimpleNamespaceContext
import org.junit.BeforeClass
import org.junit.Test
import static org.junit.Assert.*

import de.unigoettingen.sub.commons.metsmerger.util.NamespaceConstants
import static de.unigoettingen.sub.commons.metsmerger.util.Util.*

/**
 *
 * @author cmahnke
 */
//@TypeChecked
@Log4j
class UtilTest {
    def static TESTFILES = [this.getClass().getResource('/tei/rom-heyne1798.tei.xml'), 
                            this.getClass().getResource("/dfg-viewer-mets/PPN645063479.mets.xml"),
                            this.getClass().getResource("/processes/24580.goobi.mets.xml")
                           ]
    
    @BeforeClass
    static void testFilesNotNull () {
        for (testFile in TESTFILES) {
            assertNotNull('testFile is null other tests will fail!', testFile)
        }
    }
    
    @Test
    void testWriteDocument() {
        for (testFile in TESTFILES) {
            def out = File.createTempFile('result', '.xml')
            log.info('Created temp file ' + out.getAbsolutePath())
            def doc = loadDocument(testFile)
            log.info('Test document ' + testFile.toString() + ' loaded')
            writeDocument(doc, out)
            log.info('Test document written')
            assertTrue('Written document is zero bytes long!', out.length() != 0)
            out.delete()
            log.info('Test document deleted')
        }
    }
    @Test
    void testLoadDocument() {
        for (testFile in TESTFILES) {
            log.info('Loading test document ' + testFile.toString())
            assertTrue('Loaded document is null!', loadDocument(testFile) != null)
            log.info('Test document loaded')
        }
    }
    
    @Test
    void testGetRootNamespace () {
        for (testFile in TESTFILES) {
            log.info('Checking test document ' + testFile.toString())
            def namespace = getRootNamespace(testFile)
            log.info('Got namespace URI ' + namespace)
            Boolean expected = false
            if (namespace == NamespaceConstants.METS_NAMESPACE || namespace == NamespaceConstants.TEI_NAMESPACE) {
                expected = true
            }
            assertTrue('Namespace not expected ' + namespace, expected)
        }
            
    }
    
    @Test
    void testGetNamespaces () {
        for (testFile in TESTFILES) {
            log.info('Checking test document ' + testFile.toString())
            def namespaces = getNamespaces(testFile)
            log.info('Got namespace URIs: ' + namespaces.toString())
            assertTrue('Unknown namespace detected!', NamespaceConstants.NAMESPACES_WITH_PREFIX.any { key, value ->  namespaces.contains(value) })
        }
    }
    
    @Test
    void testSchemas () {
        NamespaceConstants.SCHEMA_LOCATIONS.each {namespace, schema ->
            log.info('Checking ' + schema) 
            def schemaUrl = new URL(schema)
            assertNotNull('Schema URL couln\'t be created, maybe it\'s malformed?', schemaUrl)
            log.info('URL for ' + schema + ' seems valid')
        }
    }
   
        
}

