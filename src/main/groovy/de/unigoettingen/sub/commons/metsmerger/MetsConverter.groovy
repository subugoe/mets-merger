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

import groovy.transform.TypeChecked
import groovy.util.logging.Log4j

import javax.xml.transform.dom.DOMSource
import javax.xml.transform.Source

import de.unigoettingen.sub.commons.metsmerger.util.NamespaceConstants

/**
 * This class an be used to convert external (like in 
 * <a href="http://dfg-viewer.de/ueber-das-projekt/">DFG-Viewer</a> compatible) <a href="http://www.loc.gov/standards/mets/">METS</a>
 * into a Goobi Internal representation based on a UGH ruleset. This class uses 
 * a generated stylesheet and is jus a wrapper around {@link RulesetConverter RulesetConverter}
 * @author cmahnke
 */
//@TypeChecked
@Log4j
class MetsConverter extends AbstractTransformer {
    /** The RulesetConverter to generate the XSLT from the ruleset. */
    def RulesetConverter converter
    /** The XSLT to be used for the transformation, this is the result of the RulesetConverter */
    Source xslt
    //Configuration of Stylesheet
    /** Parameters of the stylesheet */
    def static paramPrototypes = ['createGoobiMETSParam': 'true', 'copyLabelParam': 'false']
    
    static {
        paramPrototypes.keySet().each { name ->
            def methodName = name[0].toUpperCase() + name[1..-1]
            MetsConverter.metaClass."set${methodName}" = {String value -> params."${name}" = value }
            MetsConverter.metaClass."get${methodName}" = {-> params."${name}" }
        }
    }
    
    /**
     * Construts a empty MetsConverter and sets the {@link java.net.URL URL} of the schema for validation
     * and the parameters of the transformation
     */
    MetsConverter () {
        this.resultNamespace = NamespaceConstants.METS_NAMESPACE
        this.schemaUrl = new URL(NamespaceConstants.getSchemaLoactionForNamespace(this.resultNamespace))
        //Configuration of Stylesheet
        paramPrototypes.each() { name, value -> params[name] = value }
    }
    
    /**
     * Construts a MetsConverter, sets the {@link java.net.URL URL} of the schema for validation
     * and the parameters of the transformation and sets the given input.
     * @param ruleset the {@link java.net.URL URL} of the ruleset used to generate the transformation
     * @param metsfile the {@link java.net.URL URL} of the METS file to be transformed
     * @see #MetsConverter()
     */
    MetsConverter (URL ruleset, URL metsFile) {
        this()
        this.converter = new RulesetConverter(ruleset)
        this.converter.transform()
        this.xslt = new DOMSource(this.converter.result)
        this.input = metsFile
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see de.unigoettingen.sub.commons.metsmerger.AbstractTransformer#transform()
     */
    @Override
    void transform () {
        log.debug("Using ruleset to generate stylesheet " + this.converter.input.toString())
        result = transform(input, xslt, params)
    }
	
}