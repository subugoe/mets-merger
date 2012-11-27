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
import org.junit.Ignore;
import org.junit.Test;
import javax.xml.transform.stream.StreamSource

/**
 *
 * @author cmahnke
 */
//  @TypeChecked
@Log4j
class RulesetConverterTest extends AbstractTransformerTest {
    
    def static RULESETS = [this.getClass().getResource('/rulesets/archaeo18.xml'), 
                          this.getClass().getResource('/rulesets/gdz.xml')]
                      
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
        
}

