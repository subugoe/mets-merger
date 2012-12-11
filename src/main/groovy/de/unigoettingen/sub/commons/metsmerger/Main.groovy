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
    static Map params = [:]

    /**
     * Sets options passed on commandline
     */
    static void main(args) {
        //Silence the logger
        log.setLevel(Level.ERROR)
        def cli = new CliBuilder(usage: 'java -jar mets-merger.jar')
        cli.c(longOpt: 'check', 'validate using XML Schema and UGH')
        cli.D(args:2, valueSeparator:'=', argName:'property=value', 'use value for given property for XSLT')
        cli.h(longOpt: 'help', 'usage information')
        cli.i(longOpt: 'input', 'input file', args: 1)
        cli.j(longOpt: 'input-format', 'input file format', args: 1)
        cli.m(longOpt: 'merge', 'file to be merged with input', args: 1)
        cli.o(longOpt: 'output', 'output file', args: 1)
        cli.p(longOpt: 'output-format', 'output file format', args: 1)
        cli.r(longOpt: 'ruleset', 'ruleset file', args: 1)
        cli.v(longOpt: 'verbose', 'verbose output')
        
        def opt = cli.parse(args)
        if(opt.v) {
            verbose = true
            log.setLevel(Level.TRACE)
        }
        
        if(!opt) {
            cli.usage()
            return
        }

        if(opt.h) {
            cli.usage()
            log.trace('Help requested')
            return
        }
        
        if(opt.r) {
            ruleset = new File(opt.r).toURL()
            log.trace('Ruleset: ' + ruleset.toString())
        }
        if(opt.i) {
            input = new File(opt.i).toURL()
            log.trace('Input: ' + input.toString())
            //No input, just ruleset
        } else if(opt.r) {
            log.trace('No input file, assuming ruleset transformation')
            inFormat = FORMAT.RULESET
            outFormat = FORMAT.XSL
        }
        if (opt.o && opt.o != '-') {
            outputFile = new File(opt.o)
            log.trace('Input: ' + outputFile.toString())
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
        
        //Check if there is something to work with
        if (ruleset == null && input == null) {
            println "Neither input file nor ruleset given"
            cli.usage()
            System.exit(1)
        }
        
        //Check formats
        if(opt.j && FORMAT.fromString(opt.j) != null) {
            inFormat = FORMAT.fromString(opt.j)
        } else if (opt.j) {
            log.trace('Can\'t parse input format' + opt.j)
            println 'Format must be one of ' + FORMAT.getFormats()
            System.exit(5)
        }
        if(opt.p && FORMAT.fromString(opt.p) != null) {
            outFormat = FORMAT.fromString(opt.p)
        } else if (opt.p) {
            log.info('Can\'t parse output format' + opt.p)
            println 'Format must be one of ' + FORMAT.getFormats()
            System.exit(5)
        }
        
        //Guess input file type
        if (!opt.j && opt.i) {
            inFormat = guessFormat(input)
            log.trace("No input format given assuming " + inFormat)
            if (inFormat == FORMAT.RULESET) {
                ruleset = input
            }
            //FORMAT.UNKNOWN fails late in start
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
        //parse XSLT params
        if (opt.D) {
            for(i in (0..opt.Ds.size() - 1).step(2)) {
                params[opt.Ds.get(i)] = opt.Ds.get(i + 1)
            }
            params.each() { key,  value ->
                log.trace('XSLT Param ' + key + ' is set to ' + value)
            }
        }
        
        start()
    }	
    
    /**
     * Processes the given files
     */
    static void start () {
        //Open input
        AbstractTransformer converter
        switch (inFormat) {
        case FORMAT.TEI:
            converter = new Tei2Mets(input)
            break
        case FORMAT.DFG:
            converter = new MetsConverter(ruleset, input)
            break
        case FORMAT.RULESET:
            converter = new RulesetConverter(ruleset)
            break
        default:
            log.warn('Format ' + inFormat.name + ' not supported as input or not recognized!')
            System.exit(4) 
        }
        
        //Set the XSLT params
        params.each() { key, value ->
            def method = 'set' + key[0].toUpperCase() + key[1..-1]
            try {
                converter."${method}"(value)
            } catch (groovy.lang.MissingMethodException e) {
                log.warn('Param ' + key + ' not supported (no method called ' + method + ')!')
            }
        }
        
        //transform if no merge file
        if (merge == null) {
            try {
                converter.transform()
            } catch (IllegalStateException ise) {
                println 'Transformation needs additional parameters'
                System.exit(30)
            }
        } else {
            //Test if the file type for merge is right
            if (guessFormat(merge) != FORMAT.GOOBI) {
                println 'The to be merged to needs to be Goobi METS!'
                System.exit(30)
            }
            //TODO : Check for previous results
            if (inFormat != FORMAT.GOOBI) {
                def result = converter.result
            }
            converter = new MetsMerger(ruleset, input)
            converter.transform()
        }
        
        if (inFormat == FORMAT.TEI) {
            def result = converter.result
            //Check which METS should be created
            if (ruleset == null && outFormat == FORMAT.GOOBI) {
                log.warn('No ruleset given, can\'t create Goobi METS !')
                System.exit(3)
            } else if (outFormat == FORMAT.GOOBI) {
                converter = new MetsConverter(ruleset, result)
                converter.transform()
            }
        } else if ((inFormat != FORMAT.RULESET && outFormat != FORMAT.XSL)) {
            log.warn('Format ' + outFormat.name + ' not supported as output!')
            System.exit(3)
        }
        
        //Validate the result if requested
        //Use UGH for goobi METS
        if (check && outFormat == FORMAT.GOOBI) {
            log.trace('Validating using UGH')
            if (!ughValidate(ruleset, converter.result)) {
                log.error('Can\'t validate result, use verbose (-v) to see the logs')
                println 'Validatition failed!'
                System.exit(6)
            }
        }
        //Use XSD
        if (check) {
            if (!converter.validate()) {
                log.info('Can\'t validate result, use verbose to see the logs')
                println 'Validatition failed!'
                System.exit(6)
            } else {
                println 'Result is valid'
            }
        }
        //Write the result
        Util.writeDocument(converter.result, output);
        println "Result written"
    }
    
    /*
     * TODO: finish this, it's currently done by the options j and p 
     */
    static FORMAT guessFormat (URL input) {
        //Check type of input
        def inputNamespace = getRootNamespace(input)
        if (inputNamespace == NamespaceConstants.TEI_NAMESPACE) {
            FORMAT.TEI
        } else if (inputNamespace == NamespaceConstants.METS_NAMESPACE) {
            def namespaces = getNamespaces(input)
            if (namespaces.contains(NamespaceConstants.GOOBI_NAMESPACE)) {
                FORMAT.GOOBI
            } else if (namespaces.contains(NamespaceConstants.DV_NAMESPACE)) {
                FORMAT.DFG
            } else {
                FORMAT.METS
            }
        } else {
            if (getRootElementName(input) == 'Preferences') {
                FORMAT.RULESET
            } else {
                FORMAT.UNKNOWN
            }
        }          
    }
    
}

