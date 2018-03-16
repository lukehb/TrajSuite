package onethreeseven.trajsuite.osm.algorithm;

import onethreeseven.spm.data.SequentialPatternWriter;
import onethreeseven.spm.model.SequentialPattern;
import java.io.File;
import java.util.*;

/**
 * Given in input of sequences find the top-k most frequent items in the sequences.
 * I.e This finds the count of each item (with a maximum of one occurrence being counted per sequence.)
 * @author Luke Bermingham
 */
public class TopKItems {


    public void run(int[][] sequences, File outputFile, int k){

        //<item, support>
        HashMap<Integer, Integer> itemTally = new HashMap<>();

        for (int[] sequence : sequences) {
            HashSet<Integer> uniqueItems = new HashSet<>();
            for (int item : sequence) {
                uniqueItems.add(item);
            }

            //only increment the unique items (no duplicated per person)
            for (Integer item : uniqueItems) {
                itemTally.put(item, itemTally.getOrDefault(item, 0) + 1);
            }
        }

        //get top-k
        PriorityQueue<SequentialPattern> output = new PriorityQueue<>(k, new Comparator<SequentialPattern>() {
            @Override
            public int compare(SequentialPattern o1, SequentialPattern o2) {
                return o1.getSupport() - o2.getSupport();
            }
        });

        //maintain a queue of size k
        for (Map.Entry<Integer, Integer> entry : itemTally.entrySet()) {
            SequentialPattern p = new SequentialPattern(new int[]{entry.getKey()}, entry.getValue());
            output.add(p);
            if(output.size() > k){
                output.poll();
            }
        }

        SequentialPatternWriter writer = new SequentialPatternWriter(outputFile);
        for (SequentialPattern sequentialPattern : output) {
            writer.write(sequentialPattern);
        }
        writer.close();
    }


}
