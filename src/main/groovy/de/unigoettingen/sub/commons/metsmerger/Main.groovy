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

import org.apache.log4j.Level

import static de.unigoettingen.sub.commons.metsmerger.util.Util.*
import de.unigoettingen.sub.commons.metsmerger.util.NamespaceConstants
import de.unigoettingen.sub.commons.metsmerger.util.Util
import de.unigoettingen.sub.commons.metsmerger.util.Util.FORMAT

/**
 * This is the Main class called if you start the artifact using "java -jar".
 * It only handles the commandline parameters.  
 * @author cmahnke
 */
//@TypeChecked
@Log4j
class Main {
    static Boolean check, verbose
    static URL ruleset, input, outputUrl, merge
    static File outputFile
    static OutputStream output
    static FORMAT inFormat = FORMAT.DFG
    static FORMAT outFormat = FORMAT.GOOBI

    /**
     * Sets options passed on commandline
     */
    static void main(args) {
        //Silence the logger
        log.setLevel(Level.ERROR)
        def cli = new CliBuilder(usage: 'java -jar mets-merger.jar')
        cli.c(longOpt: 'check', 'validate using XML Schema and UGH')
        cli.h(longOpt: 'help', 'usage information')
        cli.i(longOpt: 'input', 'input file', args: 1)
        cli.j(longOpt: 'input-format', 'input file format', args: 1)
        cli.m(longOpt: 'merge', 'file to be merged with input', args: 1)
        cli.o(longOpt: 'output', 'output file', args: 1)
        cli.p(longOpt: 'output-format', 'output file format', args: 1)
        cli.r(longOpt: 'ruleset', 'ruleset file', args: 1)
        cli.v(longOpt: 'verbose', 'verbose output')
        
        def opt = cli.parse(args)
        if(!opt) {
            cli.usage()
            return
        }

        if(opt.h) {
            cli.usage()
            return
        }
        
        if(opt.v) {
            verbose = true
            log.setLevel(Level.INFO)
        }
        if(opt.r) {
            ruleset = new File(opt.r).toURL()
        }
        if(opt.i) {
            input = new File(opt.i).toURL()
            //No input, just ruleset
        } else {
            inFormat = FORMAT.RULESET
            outFormat = FORMAT.XSL
        }
        if (opt.o && opt.o != '-') {
            outputFile = new File(opt.o)
            output = new FileOutputStream(outputFile)
        }

        if(opt.c) {
            if (!opt.r) {
                println 'No ruleset given, can\'t validate results'
            }
            check = true
        }
        if (!output || opt.o == '-') {
            output = System.out
        }
        
        //Check formats
        if(opt.j && FORMAT.fromString(opt.j) != null) {
            inFormat = FORMAT.fromString(opt.j)
        } else {
            log.info('Can\'t parse format' + opt.j)
            println 'Format must be one of ' + FORMAT.getFormats()
            System.exit(5)
        }
        if(opt.p && FORMAT.fromString(opt.p) != null) {
            outFormat = FORMAT.fromString(opt.p)
        } else {
            log.info('Can\'t parse format' + opt.p)
            println 'Format must be one of ' + FORMAT.getFormats()
            System.exit(5)
        }

        //Stuff to merge, check if the file format is right
        if(opt.m) {
            if (inFormat == FORMAT.RULESET || outFormat != FORMAT.GOOBI) {
                println 'Merge mode only woks with Goobi output, and can\'t operate on ruleset input'
                System.exit(10)
            }
            if (ruleset == null) {
                println 'Merge mode needs a ruleset'
                System.exit(11)
            }
            merge = new File(opt.m).toURL()
        }
        
        start()
    }	
    
    /**
     * Processes the given files
     */
    static void start () {
        //Open input
        def inputDoc = loadDocument(input)

        AbstractTransformer converter
        switch (inFormat) {
        case FORMAT.TEI:
            converter = new Tei2Mets(input)
            break
        case FORMAT.DFG:
            converter = new MetsConverter(ruleset, input)
        case FORMAT.RULESET:
            converter = new RulesetConverter(ruleset)
        default:
            log.warn('Format not supported as input!')
            System.exit(4)
                
        }
        /*
         * TODO: finish this, it's currently done by the options j and p 
        //Check type of input
        def inputNamespace = getRootNamespace(inputDoc)
        if (inputNamespace == NamespaceConstants.METS_NAMESPACE) {
        if (merge == null) {
                
        } else {
        converter = new MetsConverter(input)
        }
            
        } else if (inputNamespace == NamespaceConstants.TEI_NAMESPACE) {
        converter = new Tei2Mets(input)
        } else {
        //fail
        println 'Input namespace not supported!'
        System.exit(10)
        }
         */
        
        //transform if no merge file
        if (merge == null) {
            converter.transform()
        } else {
            //otherwise merge
            
        }
        
        if (inFormat == FORMAT.TEI) {
            //Check which METS should be created
            if (outFormat == FORMAT.GOOBI) {
                
            }
        }
        
        switch (outFormat) {
        case FORMAT.GOOBI:
            converter = new MetsConverter(ruleset, input)
            break
        case FORMAT.DFG:
            break

        default:
            log.warn('Format not supported as output!')
            System.exit(3)
                
        }
        
        //Validate the result if requested
        if (check && outFormat == FORMAT.GOOBI) {
            if (!ughValidate(ruleset, converter.result)) {
                log.info('Can\'t validate result, use verbose to see the logs')
                println 'Validatition failed!'
                System.exit(6)
            }
        } else if (check) {
            if (!converter.validate()) {
                log.info('Can\'t validate result, use verbose to see the logs')
                println 'Validatition failed!'
                System.exit(6)
            }
        }
        //Write the result
        Util.writeDocument(converter.result, output);
    }
    
}

