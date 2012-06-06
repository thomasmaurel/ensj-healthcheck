/**
 * ChecksumDatabase
 * 
 * @author dstaines
 * @author $Author$
 * @version $Revision$
 */
package org.ensembl.healthcheck.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.ensembl.healthcheck.DatabaseRegistryEntry;

/**
 * Utility to compare checksums from a set of database tables to a persistent
 * file on disk
 * 
 * @author dstaines
 */
public class ChecksumDatabase {

	protected final static String CHECKSUM_SQL = "CHECKSUM TABLE %s EXTENDED";

	protected final String databaseName;
	protected final SqlTemplate templ;
	protected final File checksumFile;
	protected final Collection<String> tables;

	public ChecksumDatabase(DatabaseRegistryEntry dbre, File directory,
			Collection<String> tables) {
		this(dbre.getName(), DBUtils.getSqlTemplate(dbre), directory, tables);
	}

	public ChecksumDatabase(String databaseName, SqlTemplate templ,
			File directory, Collection<String> tables) {
		this.databaseName = databaseName;
		this.templ = templ;
		this.tables = tables;
		directory.mkdirs();
		checksumFile = new File(directory, databaseName + ".chk");
	}

	protected Properties getChecksumFromFile() {
		Properties fileSum = new Properties();
		if (checksumFile.exists()) {
			try {
				fileSum.load(new FileInputStream(checksumFile));
			} catch (IOException e) {
				throw new RuntimeException("Cannot read table properties from "
						+ checksumFile, e);
			}
		}
		return fileSum;
	}

	protected Properties getChecksumFromDatabase() {
		Properties dbSum = new Properties();
		for (final String table : tables) {
			dbSum.putAll(templ.queryForMap(
					CHECKSUM_SQL.replaceFirst("%s", table),
					new MapRowMapper<String, String>() {

						@Override
						public String mapRow(ResultSet resultSet, int position)
								throws SQLException {
							return resultSet.getString(2);
						}

						@Override
						public Map<String, String> getMap() {
							return CollectionUtils.createHashMap(1);
						}

						@Override
						public String getKey(ResultSet resultSet)
								throws SQLException {
							return resultSet.getString(1);
						}

						@Override
						public void existingObject(String currentValue,
								ResultSet resultSet, int position)
								throws SQLException {
							throw new RuntimeException();
						}
					}));
		}
		return dbSum;
	}

	public boolean isUpdated() {
		boolean updated = false;
		Properties db = getChecksumFromDatabase();
		Properties fs = getChecksumFromFile();
		for (Entry<Object, Object> e : db.entrySet()) {
			Object fsVal = fs.get(e.getKey());
			if (!e.getValue().equals(fsVal)) {
				updated = true;
				break;
			}
		}
		return updated;
	}

	public void setRead() {
		// Write properties file
		try {
			getChecksumFromDatabase().store(new FileOutputStream(checksumFile),
					null);
		} catch (IOException e) {
			throw new RuntimeException("Cannot write table properties to "
					+ checksumFile, e);
		}
	}

	public void reset() {
		checksumFile.delete();
	}

}
