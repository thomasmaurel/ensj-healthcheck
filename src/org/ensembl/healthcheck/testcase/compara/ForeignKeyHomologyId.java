/*
 Copyright (C) 2004 EBI, GRL
 
 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.
 
 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.
 
 You should have received a copy of the GNU Lesser General Public
 License along with this library; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.ensembl.healthcheck.testcase.compara;

import java.sql.Connection;

import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.ReportManager;
import org.ensembl.healthcheck.testcase.SingleDatabaseTestCase;

/**
 * An EnsEMBL Healthcheck test case that looks for broken foreign-key
 * relationships.
 */

public class ForeignKeyHomologyId extends SingleDatabaseTestCase {

    /**
     * Create an OrphanTestCase that applies to a specific set of databases.
     */
    public ForeignKeyHomologyId() {

        addToGroup("compara_db_constraints");
        setDescription("Check for broken foreign-key relationships in ensembl_compara databases.");

    }

    /**
     * Run the test.
     * 
     * @param dbre
     *          The database to use.
     * @return true if the test pased.
     *  
     */
    public boolean run(DatabaseRegistryEntry dbre) {

        boolean result = true;

        int orphans = 0;

        Connection con = dbre.getConnection();
        // 1 test to check gene_relationship_id used as foreign key

        if (tableHasRows(con, "homology")) {

            orphans = countOrphans(con, "homology_member", "homology_id", "homology", "homology_id", true);
            fillReportManager(con, orphans,"homology_member","homology","homology_id");

            orphans = countOrphans(con, "homology", "homology_id", "homology_member", "homology_id", true);
            fillReportManager(con, orphans,"homology","homology_member","homology_id");
        } else {
            ReportManager.correct(this, con, "NO ENTRIES in homology table, so nothing to test IGNORED");
        }

        result &= (orphans == 0);

        return result;

    }

    public int fillReportManager(Connection con, int orphans, String table1, String table2, String fk) {
        
        if (orphans == 0) {
            ReportManager.correct(this, con, "PASSED " + table1 + " -> " + table2 + " using FK " + fk + " relationships");
        } else if (orphans > 0) {
            ReportManager.problem(this, con, "FAILED " + table1 + " -> " + table2 + " using FK " + fk + " relationships");
            ReportManager.problem(this, con, "FAILURE DETAILS: " + orphans + " " + table1 + " entries have unlinked entries in " + table2);
        } else {
            ReportManager.problem(this, con, "TEST NOT COMPLETED " + table1 + " -> " + table2 + " using FK " + fk + ", look at the StackTrace if any");
        }

        return 1;
    } //fillReportManager

} // OrphanTestCase
