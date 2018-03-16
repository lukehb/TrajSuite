package onethreeseven.trajsuite.osm.model;

import com.graphhopper.coll.GHBitSet;
import com.graphhopper.util.*;
import java.util.Iterator;

/**
 * Does a breath first search on GraphHopper graph
 * except it uses an iterator.
 *
 * @author Luke Bermingham
 */
public class BreadthFirstSearchIterator extends XFirstSearch implements Iterator<Integer> {

    private final SimpleIntDeque fifo = new SimpleIntDeque();
    private final GHBitSet visited = createBitSet();
    private final EdgeExplorer explorer;
    private int current = -1;

    public BreadthFirstSearchIterator(EdgeExplorer explorer, int startNode){
        this.explorer = explorer;
        addNodeToProcess(startNode);
    }

    @Override
    public boolean hasNext() {
        if(current == -1){
            while(!fifo.isEmpty()){
                int nextNode = fifo.pop();
                //if go further, break out we need to process this node
                if(goFurther(nextNode)){
                    current = nextNode;
                    break;
                }
                //else continue this loop and just discard this node
            }
            return current != -1;
        }
        else{
            return true;
        }
    }

    @Override
    public Integer next() {

        if(hasNext()){
            EdgeIterator iter = explorer.setBaseNode(current);
            while (iter.next())
            {
                int connectedId = iter.getAdjNode();
                if (checkAdjacent(iter) && !visited.contains(connectedId))
                {
                    addNodeToProcess(connectedId);
                }
            }
            int result = current;
            current = -1;
            return result;
        }
        else{
            throw new IllegalStateException("Not more nodes to process.");
        }
    }

    private void addNodeToProcess(int node){
        visited.add(node);
        fifo.push(node);
    }

    @Override
    public void start(EdgeExplorer explorer, int startNode) {
        throw new UnsupportedOperationException("This is an iterator, no need to start it.");
    }

}
