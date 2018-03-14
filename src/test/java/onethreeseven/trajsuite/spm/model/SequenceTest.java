package onethreeseven.trajsuite.spm.model;

import org.junit.Assert;
import org.junit.Test;

/**
 * Testing extraction of sub-sequences resolve parent sequences.
 * @see Sequence
 * @author Luke Bermingham
 */
public class SequenceTest {

    @Test
    public void testIndexOfSequence() throws Exception {
        Sequence sequence = new Sequence("ABCDEFGHIJK");
        int idx = sequence.indexOfSequence('D', 'E', 'F');
        int idx2 = sequence.indexOfSequence('D', 'E', 'J');
        Assert.assertTrue(idx == 5 && idx2 == 9);
    }
}