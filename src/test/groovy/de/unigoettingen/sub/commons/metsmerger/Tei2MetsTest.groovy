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
import org.junit.Ignore
import org.junit.Test
import static org.junit.Assert.*

import static de.unigoettingen.sub.commons.metsmerger.util.Util.*
import de.unigoettingen.sub.commons.metsmerger.util.XSDValidator

/**
 *
 * @author cmahnke
 */
//@TypeChecked
@Log4j
class Tei2MetsTest extends AbstractTransformerTest {
    
    def static TEIFILES = [this.getClass().getResource('/tei/rom-heyne1798.tei.xml'), 
        this.getClass().getResource('/tei/weimar-hs-2057.tei.xml')]
    
    @Test
    void testGetInstance () {
        def converter = new Tei2Mets()
        log.trace('Stylesheet is ' + converter.stylesheet.toString())
    }

    @Test
    void testTei2MetsTransform () {
        for (tei in TEIFILES) {
            log.info('Transforming TEI File ' + tei.toString())
            def converter = new Tei2Mets(tei)
            converter.setIdentifier('1234')
            log.info('Identifier is set to ' +  converter.getIdentifier())
            converter.transform()
            log.trace('Result:\n----------------START OF RESULT\n' + converter.getXML())
            log.trace('----------------END OF RESULT\n')
            assertNotNull('Check if Result is not null failed!', converter.result)
            log.info('TEI transformed')
        }
    }
    
    @Test
    void checkStructLink () {
        log.info('Entering checkStructLink:')
        for (tei in TEIFILES) {
            log.info('Transforming TEI File ' + tei.toString())
            def converter = new Tei2Mets(tei)
            converter.setIdentifier('1234')
            log.info('Identifier is set to ' +  converter.getIdentifier())
            converter.transform()
            log.info('Checking if Ids in structLink section are resolvable for result of TEI to METS for ' + tei.toString())
            assertTrue('XPathes for ID Links check failed!', checkStructLink(converter.result))
        }
    }

    @Test
    void testTei2MetsXSDValidate () {
        for (tei in TEIFILES) {
            log.info('Validating result for TEI File ' + tei.toString())
            def converter = new Tei2Mets(tei)
            converter.setIdentifier('1234')
            converter.transform()
            log.info('Identifier is set to ' +  converter.getIdentifier())
            log.trace('Check if result is valid using XML Schema:')
            log.trace('Not that this test depends on an external hosted schema, it will fail if you are offline or the schema isn\'t available')
            assertTrue('Validatione failed', converter.validate())
            log.trace('Result is valid (XML Schema)')
            
        }
    }
        
    @Test
    void testTei2MetsUniqueIDs () {
        for (tei in TEIFILES) {
            log.info('Validating generated IDs for TEI File ' + tei.toString())
            def converter = new Tei2Mets(tei)
            converter.setIdentifier('1234')
            converter.transform()
            def doc = converter.result
            assertTrue('XPath for ID check failed!', checkUniquePath (doc, '//mets:*[@ID = following::mets:*/@ID]'))
        }
    }
    
}

