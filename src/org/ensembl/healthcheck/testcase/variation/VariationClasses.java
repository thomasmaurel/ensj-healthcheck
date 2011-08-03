/*
 * Copyright (C) 2004 EBI, GRL
 * 
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.ensembl.healthcheck.testcase.variation;

import java.sql.Connection;

import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.DatabaseType;
import org.ensembl.healthcheck.ReportManager;
import org.ensembl.healthcheck.Species;
import org.ensembl.healthcheck.Team;
import org.ensembl.healthcheck.testcase.SingleDatabaseTestCase;

/**
 * Sanity check variation classes
 */
public class VariationClasses extends SingleDatabaseTestCase {

	/**
	 * Creates a new instance of VariationClasses
	 */
	public VariationClasses() {

		addToGroup("variation");
		addToGroup("variation-release");

		setDescription("Sanity check variation classes");
		setTeamResponsible(Team.VARIATION);

	}

	// ---------------------------------------------------------------------

	/**
	 * Sanity check the variation classes.
	 * 
	 * @param dbre
	 *          The database to check.
	 * @return true if the test passed.
	 */
	public boolean run(DatabaseRegistryEntry dbre) {

		boolean result = true;

		Species species = dbre.getSpecies();

		Connection con = dbre.getConnection();

        // at the moment we only check human
        
        if (species == Species.HOMO_SAPIENS) {
            
            try {
                
                // and we only check that no HGMD mutation is ever classed as 'sequence_alteration'

                String query =  "SELECT COUNT(*) "+
                                "FROM variation v, source s, attrib a, attrib_type t "+
                                "WHERE t.code = 'SO_term' "+
                                "AND a.attrib_type_id = t.attrib_type_id "+
                                "AND a.value = 'sequence_alteration' "+
                                "AND a.attrib_id = v.class_attrib_id "+
                                "AND s.name = 'HGMD-PUBLIC' "+
                                "AND s.source_id = v.source_id ";

			    result &= (getRowCount(con, query) == 0);

		    } 
            catch (Exception e) {
			    ReportManager.problem(this, con, "HealthCheck caused an exception: " + e.getMessage());
		    	result = false;
		    }
        }

		if (result) {
			ReportManager.correct(this, con, "Variation classes look sane");
		}

		return result;

	} // run

	// -----------------------------------------------------------------

	/**
	 * This only applies to variation databases.
	 */
	public void types() {

		removeAppliesToType(DatabaseType.OTHERFEATURES);
		removeAppliesToType(DatabaseType.CDNA);
		removeAppliesToType(DatabaseType.CORE);
		removeAppliesToType(DatabaseType.VEGA);

	}

} // VariationClasses
