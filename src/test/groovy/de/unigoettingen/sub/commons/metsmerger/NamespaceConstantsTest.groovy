/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.unigoettingen.sub.commons.metsmerger

import static org.junit.Assert.*
import groovy.util.logging.Log4j
import groovy.transform.TypeChecked

import org.junit.Test;
import org.junit.AfterClass
import org.junit.BeforeClass

import de.unigoettingen.sub.commons.metsmerger.util.NamespaceConstants
import javax.xml.namespace.NamespaceContext

/**
 *
 * @author cmahnke
 */
@TypeChecked
@Log4j
class NamespaceConstantsTest {
    @Test
    void testNamespaceContext () {
        NamespaceContext nsc = new NamespaceConstants()
        def metsNamespace = nsc.getNamespaceURI('mets')
        log.info('Got Namespace URI for prefix \'mets\': ' + metsNamespace)
        assertTrue('METS Namespace not known!', metsNamespace == NamespaceConstants.METS_NAMESPACE)
        
    }
}

