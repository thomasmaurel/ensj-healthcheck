package org.ensembl.healthcheck.test;

import java.io.File;

import org.ensembl.healthcheck.util.Utils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @version $Revision$
 * @author glenn
 */
public class UtilsTest {

    // -----------------------------------------------------------------

    /**
     * Test of readPropertiesFile method, of class
     * org.ensembl.healthcheck.util.Utils.
     */
    @Test
    public void testReadPropertiesFile() {

        Assert.assertNotNull(Utils.readSimplePropertiesFile("database.properties"));

    }

    // -----------------------------------------------------------------
    @Test
    public void testgetSubDirs() {

        String startDir = System.getProperty("user.dir");
        System.out.println("Checking subdirectories of " + startDir);
        String[] subdirs = Utils.getSubDirs(startDir);
        Assert.assertNotNull(subdirs);
        Assert.assertTrue(subdirs.length > 3, "Fewer than expected subdirectories");
        for (int i = 0; i < subdirs.length; i++) {
            Assert.assertNotNull(subdirs[i]);
            File f = new File(subdirs[i]);
            Assert.assertTrue(f.isDirectory(), "Got a file where we should have only got directories");
            System.out.println("\t" + subdirs[i]);
        }

        subdirs = Utils.getSubDirs("/some/madeup/dir");
        Assert.assertNotNull(subdirs);
        Assert.assertEquals(0, subdirs.length);

    }

    // -----------------------------------------------------------------
    @Test
    public void testFilterArray() {

        String[] source = {"a", "b", "c", "d", "e", "f"};
        String[] remove = {"c", "e"};
        Object[] filtered = Utils.filterArray(source, remove);
        Assert.assertEquals(filtered.length, 4);

    }

    // -----------------------------------------------------------------

}