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

import de.unigoettingen.sub.commons.metsmerger.util.NamespaceConstants

/**
 * This is just a simple Wrapper for XSLT, to convert {@link http://www.tei-c.org/index.xml TEI} to mets using the TEI 
 * structural elements to create the logical structMap and the provided pagebreaks
 * to create the physical StructMap.
 * @author cmahnke
 */
//@TypeChecked
@Log4j
class Tei2Mets extends AbstractTransformer {
    /** The URL of the stylesheet to used */
    def static URL stylesheet = this.getClass().getResource("/xslt/tei2mets.xsl")
    //Configuration of Stylesheet
    /** Parameters of the stylesheet */
    def static paramPrototypes = ['identifier': '', 'locationPrefix': '', 'localPrefix': '', 'locationSuffix': '']
            
    /*
     * This generates accessor methods for the parameters which live inside of the params Map 
     */
    static {
        paramPrototypes.keySet().each { name ->
            def methodName = name[0].toUpperCase() + name[1..-1]
            Tei2Mets.metaClass."set${methodName}" = {String value -> params."${name}" = value }
            Tei2Mets.metaClass."get${methodName}" = {-> params."${name}" }
        }
    }
    
    /**
     * Construts a empty Tei2Mets and sets the URL of the schema for validation
     * and the parameters of the transformation.
     */
    Tei2Mets () {
        this.resultNamespace = NamespaceConstants.METS_NAMESPACE
        //this.schemaUrl = new URL('http://www.loc.gov/standards/mets/version17/mets.v1-7.xsd')
        this.schemaUrl = new URL(NamespaceConstants.getSchemaLoactionForNamespace(NamespaceConstants.METS_NAMESPACE))
        // Fill the map with the names of the params
        paramPrototypes.each() { name, value -> params[name] = value }
    }
    
    /**
     * Construts a Tei2Mets, sets the {@link java.net.URL URL} of the schema for validation
     * and the parameters of the transformation and sets the given input.
     * @param input the {@link java.net.URL URL} of the document to be transformed
     * @see #Tei2Mets()
     */
    Tei2Mets (URL input) {
        this()
        this.input = input
    }
    
    /**
     * Checks if the required parameters are set and performes the transformation
     * @throws IllegalStateException if the paramters are empty or not set
     * @see de.unigoettingen.sub.commons.metsmerger.AbstractTransformer#transform()
     */
    @Override
    void transform () {
        if (!validateParams(params)) {
            throw new IllegalStateException('Params not configured');
        }
        log.debug("Using stylesheet " + stylesheet.toString())
        result = transform(this.input, this.stylesheet, this.params)
    }
    
    /**
     * This method checks if the mandatory parameters for the stylesheet are set.
     * @return Boolean whether the params are valid for the stylesheet 
     */
    protected Boolean validateParams(Map params) {
        log.debug('Params are set to ' + params)
        if (!params['identifier'] || params['identifier'] == '') {
            log.debug('Param "identifier" not set or empty')
            return false
        }
        return true
    }
    
}

