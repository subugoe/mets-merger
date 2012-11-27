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

import groovy.util.logging.*

import de.unigoettingen.sub.commons.metsmerger.util.NamespaceConstants

/**
 * This class can be used to transform a ruleset into a XSLT stylesheet that is 
 * able to transform a METS File exported by Goobi or crated by a DFG-Viewer 
 * capable repository into an Goobi internal representation
 * @author cmahnke
 */

@Log4j
class RulesetConverter extends AbstractTransformer {
    /** The URL of the stylesheet to used */
    def static URL stylesheet = this.getClass().getResource("/xslt/ruleset2xslt.xsl")
    
    /**
     * Construts a empty RulesetConverter and sets the {@link java.net.URL URL} of the schema for validation
     * and the parameters of the transformation
     */
    RulesetConverter () {
        //Configuration of Stylesheet
        this.params = ['createDMSsectsParam': 'true', 'addOrderLabelParam': 'true']
        this.resultNamespace = NamespaceConstants.XSLT_NAMESPACE
        this.schemaUrl = new URL(NamespaceConstants.getSchemaLoactionForNamespace(this.resultNamespace))
    }
    
    /**
     * Construts a RulesetConverter, sets the {@link java.net.URL URL} of the schema for validation
     * and the parameters of the transformation and sets the given input.
     * @param input the {@link java.net.URL URL} of the document to be transformed
     * @see #RulesetConverter()
     */
    RulesetConverter (URL input) {
        this()
        this.input = input
    }
    
    /**
     * Construts a RulesetConverter and sets the {@link java.net.URL URL} of the schema for validation.
     * The stylesheet and it's parameters are overwritten by this contructor. 
     * @param stylesheet the {@link java.net.URL URL} of the stylesheet to be used during the transformation
     * @param input the {@link java.net.URL URL} of the document to be transformed
     * @param params of parameters for the given stylesheet
     * @see #RulesetConverter(java.net.URL)
     */
    RulesetConverter (URL stylesheet, URL input, Map params) {
        this(input)
        this.stylesheet = stylesheet
        this.params = params
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see de.unigoettingen.sub.commons.metsmerger.AbstractTransformer#transform
     */
    @Override
    void transform () {
        log.debug("Using stylesheet " + stylesheet.toString())
        result = transform(input, stylesheet, this.params)
    }
    
}
