package onethreeseven.trajsuite.osm.model.tag;

/**
 * OSM tags are a key-value pair of place type and value.
 * For example the tag may be "leisure" and the value may be "pitch".
 * @author Luke Bermingham
 */
public abstract class OsmTag {

    /**
     * The levels of specificity a single tag describes.
     * The more specific a tag it the more likely it is the primary tag.
     */
    public enum Specificity{
        EXTREMELY_SPECIFIC(0.8f),
        SPECIFIC(0.6f), //i.e leisure = pitch
        GENERIC(0.4f),
        BETTER_THAN_NOTHING(0.2f), //i.e building = yes
        USELESS(0f); //no tag

        public final float level;
        Specificity(float level){
            this.level = level;
        }
    }

    protected final String value;

    OsmTag(String value){
        this.value = value.trim().toLowerCase();
    }

    abstract Specificity getSpecificityImpl();

    public abstract String getName();

    public Specificity getSpecificity(){
        if(value.equals("no")){
            return Specificity.USELESS;
        }
        return getSpecificityImpl();
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getName() + "=" + getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OsmTag)) return false;
        OsmTag osmTag = (OsmTag) o;
        return this.getClass().equals(o.getClass()) &&
                (value != null ? value.equals(osmTag.value) : osmTag.value == null);

    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}

