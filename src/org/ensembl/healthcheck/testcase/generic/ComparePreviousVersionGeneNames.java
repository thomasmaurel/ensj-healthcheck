/*
 * Copyright (C) 2003 EBI, GRL
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
package org.ensembl.healthcheck.testcase.generic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.text.DecimalFormat;

import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.DatabaseType;
import org.ensembl.healthcheck.ReportManager;
import org.ensembl.healthcheck.Team;
import org.ensembl.healthcheck.testcase.SingleDatabaseTestCase;
import org.ensembl.healthcheck.Species;
import org.ensembl.healthcheck.util.Utils;

/**
 * Compare the gene names in the current database with those from the equivalent database on the secondary server.
 */

public class ComparePreviousVersionGeneNames extends SingleDatabaseTestCase {

	/**
	 * Create a new testcase.
	 */
	public ComparePreviousVersionGeneNames() {

		addToGroup("release");
		addToGroup("core_xrefs");
		
		setDescription("Compare gene names in the current database with those from the equivalent database on the secondary server.");
		setTeamResponsible(Team.GENEBUILD);
		setSecondTeamResponsible(Team.CORE);

	}

	/**
	 * This only applies to core and Vega databases.
	 */
	public void types() {

		removeAppliesToType(DatabaseType.OTHERFEATURES);
		removeAppliesToType(DatabaseType.ESTGENE);
		removeAppliesToType(DatabaseType.CDNA);
		removeAppliesToType(DatabaseType.RNASEQ);

	}
	
	// ----------------------------------------------------------------------

	public boolean run(DatabaseRegistryEntry dbre) {

		boolean result = true;

		//this test only applies to the human db
		if ( !dbre.getSpecies().equals(Species.HOMO_SAPIENS) ) {
			return result;
		}
		
		Connection currentCon = dbre.getConnection();

		DatabaseRegistryEntry previous = getEquivalentFromSecondaryServer(dbre);
		if (previous == null) {
			return result;
		}
		Connection previousCon = previous.getConnection();

		// Get data from previous database, compare each one with equivalent on
		// current
		float displayXrefCount = new Integer(getRowCount(currentCon, "SELECT COUNT(1) FROM gene WHERE display_xref_id IS NOT NULL" ) );
		float displayXrefPreviousCount = new Integer(getRowCount(previousCon, "SELECT COUNT(1) FROM gene WHERE display_xref_id IS NOT NULL" ) );
		
		if (displayXrefCount == 0 || displayXrefPreviousCount == 0 ) {
			ReportManager.problem(this, currentCon, "display xref count is 0 in the current or previous database");
			result = false;
			return result;
		}
		
		String previousSQL = "SELECT stable_id, db_name, dbprimary_acc FROM gene JOIN gene_stable_id USING(gene_id) LEFT JOIN xref ON display_xref_id = xref_id LEFT JOIN external_db USING(external_db_id)";
		String currentSQL = "SELECT stable_id, db_name, dbprimary_acc FROM gene JOIN gene_stable_id USING(gene_id) LEFT JOIN xref ON display_xref_id = xref_id LEFT JOIN external_db USING(external_db_id) WHERE stable_id = ?";
		
		int missingIds = 0;
		int accessionsChanged = 0;

		HashMap < String, Integer > changeCounts = new HashMap < String, Integer >();

		HashMap < String, String > exampleStableIds = new HashMap < String, String >();
		
		try {

			PreparedStatement previousStmt = previousCon.prepareStatement(previousSQL);
			PreparedStatement currentStmt = currentCon.prepareStatement(currentSQL);
			
			ResultSet previousRS = previousStmt.executeQuery();

			while (previousRS.next()) {

				String stableId = previousRS.getString(1);
				String previousDbName = previousRS.getString(2);
				String previousAccession = previousRS.getString(3);
				
				currentStmt.setString(1, stableId);
				ResultSet currentRS = currentStmt.executeQuery();			
							
				if (currentRS == null) {
					missingIds ++;
					currentRS.close();
					continue;
				}
				
				if (!currentRS.next()) {
					missingIds ++;
					currentRS.close();
					continue;
				}

				String currentDbName = currentRS.getString(2);
				String currentAccession = currentRS.getString(3);
			
				if (previousDbName == null) {
				    previousDbName = "null";
				}
				if (previousAccession == null) {
				    previousAccession = "null";
				}

				if (currentDbName == null) {
				    currentDbName = "null";
				}
				if (currentAccession == null) {
				    currentAccession = "null";
				}

					
				if (!currentAccession.equals(previousAccession) && currentDbName.equals(previousDbName) ) { 
				    //store counts of display xrefs where accession changed - same source
				    accessionsChanged ++;
				}
				if (!currentDbName.equals(previousDbName) ) {
				    //store counts of display xrefs where source changed
										
				    String dbNames =  previousDbName + " to " + currentDbName; 
						
				    if (changeCounts.containsKey(dbNames) ) {
					 int changeCount = changeCounts.get(dbNames);
					 changeCount ++;
					 changeCounts.put(dbNames, changeCount);
					 if (changeCount <= 3) {
					     String exampleSt = exampleStableIds.get(dbNames);
					     exampleSt += " " + stableId;
					     exampleStableIds.put(dbNames, exampleSt);
					 }
													
				    } else {
					 changeCounts.put(dbNames,1); 
					 exampleStableIds.put(dbNames, ", e.g. " + stableId);
				    }
				}

				currentRS.close();

			}
			previousRS.close();

			currentStmt.close();
			previousStmt.close();
			

		} catch (SQLException e) {

			System.err.println("Error executing SQL");
			e.printStackTrace();

		}
		float changedSource = 0;
		float totalCount = 0;
		float percentageChange = 0;
		if ( changeCounts.size() > 0  || accessionsChanged > 0 ) {
			Iterator<String> iter = changeCounts.keySet().iterator();

			while(iter.hasNext()) {
				String key = iter.next();
				int changeCount = changeCounts.get(key);
				changedSource += changeCount;
			}	
			totalCount = changedSource + accessionsChanged;
	   		percentageChange = totalCount/displayXrefPreviousCount * 100 ;			
			if (percentageChange > 1) {
				result = false;
			}
		}
		
		if (!result) {
           	DecimalFormat twoDForm = new DecimalFormat("#.##");
           	
			float percentage = missingIds/displayXrefPreviousCount * 100;
			percentage = Float.valueOf(twoDForm.format(percentage));
			
			if (missingIds > 0 ) {	
        		ReportManager.problem(this, currentCon, missingIds + "(" + percentage + "%) stable ids missing from the current database ");
        	}
           	
		percentage = accessionsChanged/displayXrefPreviousCount * 100;
		percentage = Float.valueOf(twoDForm.format(percentage));
			
           	if (accessionsChanged > 0 ) {	
        		ReportManager.problem(this, currentCon, accessionsChanged + "(" +percentage + "%) display xref primary accessions changed for the same source ");
        	}
           	percentageChange = changedSource/displayXrefPreviousCount * 100 ;	
           	percentageChange = Float.valueOf(twoDForm.format(percentageChange));
		
    		ReportManager.problem(this, currentCon, percentageChange + "% of gene display xrefs changed source: (from [previous source] to [current source] )");
			//print out counts and percentages of changes
			Iterator<String> iter = changeCounts.keySet().iterator();
			while(iter.hasNext()) {
				String key = iter.next();
				int changeCount = changeCounts.get(key);
				percentage = changeCount/displayXrefPreviousCount * 100;
				percentage = Float.valueOf(twoDForm.format(percentage));
				ReportManager.problem(this, currentCon, changeCount +"("+ percentage +"%) gene display xrefs changed source from " + key + exampleStableIds.get(key) );
			}
	
		}
		return result;

	}


	// ----------------------------------------------------------------------

} // ComparePreviousVersionGeneNames
