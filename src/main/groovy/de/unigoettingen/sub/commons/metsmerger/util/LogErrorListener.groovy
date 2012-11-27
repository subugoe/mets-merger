/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.unigoettingen.sub.commons.metsmerger.util

import groovy.util.logging.Log4j
import groovy.transform.TypeChecked

import javax.xml.transform.TransformerException
import javax.xml.transform.ErrorListener


/**
 * This Helper class is used to redirect output of an XSLT Proccesor to the logger
 * @author cmahnke
 */
@TypeChecked
@Log4j
class LogErrorListener implements ErrorListener {
    def fatal = false
        
    void error(TransformerException exception) {
        log.error("XSLT Message: ", exception)
    }
          
    void fatalError(TransformerException exception) {
        this.fatal = true
        log.error("XSLT Message: ", exception)
    }
         
    void warning(TransformerException exception) {
        log.warn("XSLT Message: ", exception)
    }

}

