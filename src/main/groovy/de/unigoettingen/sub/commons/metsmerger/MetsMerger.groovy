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
import org.w3c.dom.Document

/**
 * This class can be used to merge a existing Goobi process METS Metadata file with
 * a external METS File. The external file will be transformed according to a ruleset first.
 * @author cmahnke
 */
//@TypeChecked
@Log4j
class MetsMerger extends AbstractTransformer {
    /** The MetsConverter to generate the Goobi METS from the external METS file. */
    def MetsConverter converter
    /** The URL of the stylesheet to used */
    def static URL stylesheet = this.getClass().getResource("/xslt/goobimetsmerger.xsl")
    
    //Configuration of Stylesheet
    /** Parameters of the stylesheet */
    def static paramPrototypes = ['structFileParam': '', 'copyPhysicalStructMapParam': 'false', 'fileSectionParam': '', 'overwriteStructLinkParam': 'true']
    
    static {
        paramPrototypes.keySet().each { name ->
            def methodName = name[0].toUpperCase() + name[1..-1]
            MetsMerger.metaClass."set${methodName}" = {String value -> params."${name}" = value }
            MetsMerger.metaClass."get${methodName}" = {-> params."${name}" }
        }
    }
    
    /**
     * Construts a empty MetsMerger and sets the URL of the schema for validation
     * and the parameters of the transformation.
     */
    MetsMerger () {
        this.resultNamespace = NamespaceConstants.METS_NAMESPACE
        this.schemaUrl = new URL(NamespaceConstants.getSchemaLoactionForNamespace(this.resultNamespace))
        //Configuration of Stylesheet
        paramPrototypes.each() { name, value -> params[name] = value }
        /*
         * This is needed to make sure that structLink Elements match the generated
         * structure, otherwise the merge will fail if there is a structLink section
         * in the document the structure will be merged to
         */
        this.setOverwriteStructLinkParam('true')
    }
    
    /**
     * Construts a MetsMerger, sets the URL of the schema for validation
     * and the parameters of the transformation.
     * @param externalMets the {@link java.net.URL URL} of the external METS file to be merged
     * @param goobiMets the {@link java.net.URL URL} of the goobi METS file to be merged
     * @see #MetsMerger()
     */
    MetsMerger(URL externalMets, URL goobiMets) {
        this()
        this.input = goobiMets
        params['structFileParam'] = externalMets.toString()
    }
    
    /**
     * Construts a MetsMerger, sets the URL of the schema for validation
     * and the parameters of the transformation.
     * @param externalDoc the {@link org.w3c.dom.Document Document} of the external METS file to be merged
     * @param goobiMets the {@link java.net.URL URL} of the goobi METS file to be merged
     * @see #MetsMerger()
     */
    MetsMerger(Document externalDoc, URL goobiMets) {
        this()
        this.inputDoc = externalDoc
        params['structFileParam'] = externalMets.toString()
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see de.unigoettingen.sub.commons.metsmerger.AbstractTransformer#transform
     */
    @Override
    void transform () {
        if (!validateParams(params)) {
            throw new IllegalStateException('Params not configured');
        }

        log.debug("Using stylesheet " + stylesheet.toString())
        log.trace('Rewriting location of merge file from ' + this.params['structFileParam'])
        this.params['structFileParam'] = getRelativePath(this.params['structFileParam'])
        log.trace('Rewriting location of merge file to ' + this.params['structFileParam'])
        if (this.inputDoc == null) {
            log.debug("Using ruleset to generate stylesheet " + this.converter.input.toString())
            result = transform(this.input, this.stylesheet, this.params)
        } else {
            log.debug("Transforming using W3C Document")
            result = transform(this.inputDoc, this.stylesheet, this.params)
        }   
    }
    
    /**
     * This method checks if the mandatory parameters for the stylesheet are set.
     * @return Boolean whether the params are valid for the stylesheet 
     */
    protected Boolean validateParams(Map params) {
        log.debug('Params are set to ' + params)
        if (!params['structFileParam'] || params['structFileParam'] == '') {
            log.debug('Param "structFileParam" not det or empty')
            return false
        }
        return true
    }
    
    /**
     * This method checks if a URI is absolute ore relative and returns a relative one always
     * @return URI a relative uri
     */ 
    protected String getRelativePath (String path) {
        URI uri = new URI(path)
        if (uri.isAbsolute()) {
            File cwd = new File(".").getCanonicalFile()
            cwd.toURI().relativize(uri).getPath();
        } else {
            path
        }
    } 
}

