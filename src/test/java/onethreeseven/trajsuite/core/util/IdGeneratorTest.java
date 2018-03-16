package onethreeseven.trajsuite.core.util;

import onethreeseven.trajsuitePlugin.util.IdGenerator;
import org.junit.Assert;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;

/**
 * Testing the id generator.
 * @see IdGenerator
 * @author Luke Bermingham
 */
public class IdGeneratorTest {

    @Test
    public void testNextId() throws Exception {

        Set<String> ids = new HashSet<>();

        //generate 1k ids, check for uniqueness
        for (int i = 0; i < 1000; i++) {
            String id = IdGenerator.nextId();
            Assert.assertTrue(ids.add(id));
            System.out.println(id);
        }

    }
}