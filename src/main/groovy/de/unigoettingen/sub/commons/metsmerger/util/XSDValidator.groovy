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


package de.unigoettingen.sub.commons.metsmerger.util

import groovy.util.logging.Log4j
import groovy.transform.TypeChecked

import javax.xml.XMLConstants
import javax.xml.transform.dom.DOMSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory

import org.w3c.dom.Document

/**
     * This class is a wrapper around the Java validation API. It's main purpose
     * is the caching of Schema instances, mainly to speed up the unit tests a bit.
     * @author cmahnke
     */
//@TypeChecked
@Log4j   
class XSDValidator {
        /** The map that contains the {@link javax.xml.validation.Schema Schemas} for a given {@link java.net.URL URL}, it's our cache. */
        static Map<URL, Schema> schemaCache =[:]
        /** The SchemaFactory for XSD Schemas */
        static SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)

        /**
         * This method is only used do dump the status of the Schema cache
         */
        private static debug() {
            def urls = schemaCache.keySet().each({it.toString() + ' '})
            log.trace('Cache contains Schemas for URLs: ' + urls)
        }
        
        /**
         * This static method is used to validate a Documant agains a given schema.
         * The resulting Schema will be cached
         * @param schemaUrl the {@link java.net.URL URL} of the schema
         * @param the DOM Document that wil be validated
         */
        static Boolean validate (URL schemaUrl, Document doc) {
            //Check if there is an Schema in the cache
            Schema s
            if (schemaCache.containsKey(schemaUrl)) {
                s = schemaCache.get(schemaUrl)
            } else {
                s = schemaFactory.newSchema(schemaUrl)
                schemaCache.put(schemaUrl, s)
            }
            debug()
            def validator = s.newValidator()
            try {
                validator.validate(new DOMSource(doc))
            } catch (IllegalArgumentException e) {
                log.error('Not valid ' + e.getMessage())
                log.error('Can not valide document', e)
                return false
            }
            return true 
        } 
    }

