package onethreeseven.trajsuite.osm.model;

import onethreeseven.trajsuite.osm.model.tag.OsmTag;

/**
 * A place that is visited, has some contextual information in the tag map.
 * @author Luke Bermingham
 */
public class SemanticPlace {
    final String id;
    final String placeName;
    final OsmTag primaryTag;

    public SemanticPlace(String id, String placeName, OsmTag primaryTag) {
        this.id = id;
        this.placeName = placeName;
        this.primaryTag = primaryTag;
    }

    public OsmTag getPrimaryTag() {
        return primaryTag;
    }

    /**
     * @return A number expressing how specific this place is. Lower is more specific.
     * The number is bounded between 0 and 1.
     */
    public float getSpecificity(){
        float tagSpecificity = (primaryTag != null) ? primaryTag.getSpecificity().level : OsmTag.Specificity.USELESS.level;
        float nameSpecificity = (placeName != null) ? 0.2f : 0;
        return tagSpecificity + nameSpecificity;
    }

    @Override
    public String toString() {
        return "id=" + id +
                ", name=" + ((placeName == null) ? "null" : placeName) +
                ", primaryTag=" + ((this.primaryTag == null) ? "none" : primaryTag.toString());
    }

    public String getId() {
        return id;
    }

    public String getPlaceName() {
        return placeName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SemanticPlace)) return false;
        SemanticPlace that = (SemanticPlace) o;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (placeName != null ? !placeName.equals(that.placeName) : that.placeName != null) return false;
        return primaryTag != null ? primaryTag.equals(that.primaryTag) : that.primaryTag == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (placeName != null ? placeName.hashCode() : 0);
        result = 31 * result + (primaryTag != null ? primaryTag.hashCode() : 0);
        return result;
    }

    public String print(String delimiter) {
        return id + delimiter +
                ((placeName == null) ? "null" : placeName) + delimiter +
                ((this.primaryTag == null) ? "Unknown=null" : primaryTag.toString());
    }
}
