package onethreeseven.trajsuite.core.model;

/**
 * Basic tuple class, useful in many situations for storing two types.
 * @author Luke Bermingham.
 */
public class Tuple<T1, T2> {

    private T1 value1;
    private T2 value2;

    public Tuple(T1 value1, T2 value2) {
        this.value1 = value1;
        this.value2 = value2;
    }

    public T1 getValue1() {
        return value1;
    }

    public T2 getValue2() {
        return value2;
    }

    public void setValue1(T1 value1) {
        this.value1 = value1;
    }

    public void setValue2(T2 value2) {
        this.value2 = value2;
    }

    /**
     * Swaps the order of the tuple elements
     *
     * @return The swapped tuple
     */
    public Tuple<T2, T1> swap() {
        return new Tuple<>(value2, value1);
    }

    @SuppressWarnings("unchecked")
    public <T> T not(T value){
        if(!value1.equals(value)){
            return (T) value1;
        }else if(!value2.equals(value)){
            return (T) value2;
        }else{
            throw new IllegalArgumentException("Neither value 1 nor value 2 were equal to: " + value);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tuple tuple = (Tuple) o;
        return value1.equals(tuple.value1) && value2.equals(tuple.value2);
    }

    @Override
    public int hashCode() {
        int result = value1.hashCode();
        result = 31 * result + value2.hashCode();
        return result;
    }
}
