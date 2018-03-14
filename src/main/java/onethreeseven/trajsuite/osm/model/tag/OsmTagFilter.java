package onethreeseven.trajsuite.osm.model.tag;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * A black-list of tags to accept.
 * @author Luke Bermingham
 */
public class OsmTagFilter {

    private static final Logger log = Logger.getLogger(OsmTagFilter.class.getSimpleName());

    private final HashSet<String> blacklistedTags;
    private static final String packagePrefix = "trajsuite.osm.model.tag.";

    public OsmTagFilter(){
        this.blacklistedTags = new HashSet<>();
    }

    public boolean isBlacklisted(OsmTag tag){
        return this.blacklistedTags.contains(tag.getName());
    }

    /**
     * @param tagKey The string to resolve into an {@link OsmTag}.
     * @param value The value associated with the tag.
     * @return The resolved tag
     */
    public OsmTag resolve(String tagKey, String value){
        tagKey = tagKey.trim().toLowerCase();
        boolean blacklisted = blacklistedTags.contains(tagKey);
        if(blacklisted){
            return null;
        }

        String capitalised = tagKey.substring(0, 1).toUpperCase() + tagKey.substring(1);
        String className = packagePrefix + capitalised;

        try {
            Class<?> tagClass = Class.forName(className);
            Constructor<?> constructor = tagClass.getConstructor(String.class);
            Object inst = constructor.newInstance(value);
            return (OsmTag) inst;
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            //log.info(capitalised + " OSM tag is unsupported.");
            return null;
        }
    }

    private OsmTagFilter blacklist(OsmTag blacklisted){
        this.blacklistedTags.add(blacklisted.getName());
        return this;
    }

    /**
     * A filter with all supported tags.
     */
    public static final OsmTagFilter ALL = new OsmTagFilter();


}
