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

import org.junit.Ignore

/**
 *
 * @author cmahnke
 */
@TypeChecked
@Ignore
class TestData {
    URL ruleset
    URL processMets
    URL dfgViewerMets
    AbstractTransformer converter
    File resultFile
        
    def TestData (URL ruleset, URL processMets, URL dfgViewerMets) {
        this.ruleset = ruleset
        this.processMets = processMets
        this.dfgViewerMets = dfgViewerMets
    }
}

