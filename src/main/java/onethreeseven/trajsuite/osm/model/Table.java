package onethreeseven.trajsuite.osm.model;

import java.util.HashMap;
import java.util.Map;

/**
 * A row/column table populated sparsely and accessed
 * under the hood by hash-maps.
 * @author Luke Bermingham
 */
public class Table<K, V> {

    private final HashMap<K, HashMap<K,V>> rows;

    public Table(){
        this.rows = new HashMap<>();
    }

    public void put(K row, K col, V value){
        HashMap<K, V> column = this.rows.get(row);
        if(column == null){
            column = new HashMap<>();
            this.rows.put(row, column);
        }
        column.put(col, value);
    }

    public Map<K, V> getRow(K row){
        return this.rows.get(row);
    }

    public V get(K row, K col){
        HashMap<K, V> column = this.rows.get(row);
        if(column == null){
            return null;
        }
        return column.get(col);
    }

    public V getOrDefault(K row, K col, V defaultValue){
        V value = get(row, col);
        if(value == null){
            return defaultValue;
        }
        return value;
    }

}
