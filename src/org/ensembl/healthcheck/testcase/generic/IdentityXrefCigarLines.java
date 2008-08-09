/*
 * Copyright (C) 2006 WTSI & EBI
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 */

package org.ensembl.healthcheck.testcase.generic;

import java.sql.Connection;

import org.ensembl.healthcheck.DatabaseRegistryEntry;
import org.ensembl.healthcheck.DatabaseType;
import org.ensembl.healthcheck.ReportManager;
import org.ensembl.healthcheck.testcase.SingleDatabaseTestCase;

/**
 * Check that cigar lines in the identity_xref table are in the same format as
 * they are in the alignment tables, i.e. start with a number rather than a
 * letter.
 */

public class IdentityXrefCigarLines extends SingleDatabaseTestCase {

	/**
	 * Creates a new instance of Ditag.
	 */
	public IdentityXrefCigarLines() {

		addToGroup("post_genebuild");
		addToGroup("release");
		addToGroup("core_xrefs");
		setDescription("Check that cigar lines in the identity_xref table are in the same format, as they are in the alignment tables, i.e. start with a number rather than a letter");

	}

	/**
	 * This test only applies to core databases.
	 */
	public void types() {

		removeAppliesToType(DatabaseType.OTHERFEATURES);
		removeAppliesToType(DatabaseType.ESTGENE);
		removeAppliesToType(DatabaseType.VEGA);
		removeAppliesToType(DatabaseType.CDNA);

	}

	/**
	 * Test various things about ditag features.
	 * 
	 * @param dbre
	 *          The database to use.
	 * @return Result.
	 */
	public boolean run(DatabaseRegistryEntry dbre) {

		boolean result = true;

		Connection con = dbre.getConnection();

		int rows = getRowCount(con, "SELECT COUNT(*) FROM identity_xref WHERE cigar_line REGEXP '^[MDI]'");

		if (rows > 0) {

			ReportManager.problem(this, con, rows + " cigar lines in identity_xref appear to be in the wrong format (number first)");
			result = false;

		} else {

			ReportManager.correct(this, con, "All cigar lines in identity_xref are in the correct format");
		}

		return result;

	}

	// ----------------------------------------------------------------------

}
