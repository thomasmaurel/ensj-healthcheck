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
 * Check that all tables have data.
 */
public class EmptyVariationTables extends SingleDatabaseTestCase {

	/**
	 * Creates a new instance of EmptyVariationTablesTestCase
	 */
	public EmptyVariationTables() {

		addToGroup("variation");
		addToGroup("variation-release");

		setDescription("Checks that all tables have data");
		setTeamResponsible(Team.VARIATION);

	}

	// ---------------------------------------------------------------------

	/**
	 * Define what tables are to be checked.
	 */
	private String[] getTablesToCheck(final DatabaseRegistryEntry dbre) {

		String[] tables = getTableNames(dbre.getConnection());
		Species species = dbre.getSpecies();

        String[] unusedTables           = { "allele_group", "allele_group_allele", "httag", "variation_group", "variation_group_variation", "variation_group_feature" };
        String[] humanOnlyTables        = { "protein_function_predictions", "phenotype", "tagged_variation_feature" };
        String[] svTables               = { "structural_variation", "structural_variation_feature" };
		String[] sampleTables           = { "population_genotype", "population_structure", "sample_synonym" };
        String[] setTables              = { "variation_set", "variation_set_structure", "variation_set_variation" };
        String[] genotypeTables         = { "individual_genotype_multiple_bp", "compressed_genotype_single_bp" };
        String[] coverageTables         = { "read_coverage" };

        // first drop the unused tables

        tables = remove(tables, unusedTables);

        // then human specific ones unless we're running on human

        if (species != Species.HOMO_SAPIENS) {
            tables = remove(tables, humanOnlyTables);
        }

        // only these species have coverage data

		if (species != Species.RATTUS_NORVEGICUS && species != Species.MUS_MUSCULUS && species != Species.PONGO_ABELII && species != Species.HOMO_SAPIENS) {
			tables = remove(tables, coverageTables);
		}

        // only these species have structural variation data

		if (species != Species.HOMO_SAPIENS && species != Species.MUS_MUSCULUS && species != Species.CANIS_FAMILIARIS && species != Species.SUS_SCROFA) {
            tables = remove(tables, svTables);
	    }
        
        // only these species do not have sample data
	
        if (species == Species.ANOPHELES_GAMBIAE || species == Species.ORNITHORHYNCHUS_ANATINUS || species == Species.PONGO_ABELII || species == Species.TETRAODON_NIGROVIRIDIS) {
			tables = remove(tables, sampleTables);
		}
		
        return tables;
	}

	// ---------------------------------------------------------------------

	/**
	 * Check that every table has more than 0 rows.
	 * 
	 * @param dbre
	 *          The database to check.
	 * @return true if the test passed.
	 */
	public boolean run(DatabaseRegistryEntry dbre) {

		boolean result = true;

		String[] tables = getTablesToCheck(dbre);
		Connection con = dbre.getConnection();

		for (int i = 0; i < tables.length; i++) {

			String table = tables[i];
			// logger.finest("Checking that " + table + " has rows");

			if (!tableHasRows(con, table)) {

				ReportManager.problem(this, con, table + " has zero rows");
				result = false;

			}
		}

		if (result) {
			ReportManager.correct(this, con, "All required tables have data");
		}

		return result;

	} // run

	// -----------------------------------------------------------------

	private String[] remove(final String[] tables, final String table) {

		String[] result = new String[tables.length - 1];
		int j = 0;
		for (int i = 0; i < tables.length; i++) {
			if (!tables[i].equalsIgnoreCase(table)) {
				if (j < result.length) {
					result[j++] = tables[i];
				} else {
					logger.severe("Cannot remove " + table + " since it's not in the list!");
				}
			}
		}

		return result;

	}

	// -----------------------------------------------------------------

	private String[] remove(final String[] src, final String[] tablesToRemove) {

		String[] result = src;

		for (int i = 0; i < tablesToRemove.length; i++) {
			result = remove(result, tablesToRemove[i]);
		}

		return result;

	}

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

} // EmptyVariationTablesTestCase
