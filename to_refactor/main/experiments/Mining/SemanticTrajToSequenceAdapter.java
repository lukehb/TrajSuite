package onethreeseven.trajsuite.experiments.Mining;


import onethreeseven.spm.data.SPMFItemsetWriter;
import onethreeseven.spm.data.SPMFWriter;
import onethreeseven.trajsuite.osm.model.SemanticPlace;
import onethreeseven.trajsuite.osm.model.SemanticPt;
import onethreeseven.trajsuite.osm.model.SemanticTrajectory;
import onethreeseven.trajsuite.osm.model.tag.OsmTag;
import java.io.File;
import java.util.*;

/**
 * Adapts semantic trajectories into a sequence of places.
 * @author Luke Bermingham
 */
class SemanticTrajToSequenceAdapter {

    private final static String timeCategoryFmt = " (%s)";

    enum ItemExtractor {
        PLACE_KEY_VALUE,
        PLACE_KEY_VALUE_TIME,
        PLACE_NAME,
        PLACE_NAME_TIME
    }

    PlacesMapping adapt(Map<String, SemanticTrajectory> trajs, ItemExtractor itemExtractor){


        PlacesMapping mapping = new PlacesMapping(trajs.size());
        int i = 0;
        for (Map.Entry<String, SemanticTrajectory> entry : trajs.entrySet()) {

            SemanticTrajectory traj = entry.getValue();
            int trajSize = traj.size();
            String[] placeStrings = new String[trajSize];

            for (int j = 0; j < trajSize; j++) {
                SemanticPt semanticPt = traj.get(j);
                String placeString = null;

                switch (itemExtractor){
                    case PLACE_NAME:
                    case PLACE_NAME_TIME:
                        placeString = semanticPt.getTimeAndPlace().getPlace().getPlaceName();
                        if(placeString == null || placeString.equals("null")){
                            placeString = semanticPt.getTimeAndPlace().getPlace().getId();
                        }
                        if(itemExtractor == ItemExtractor.PLACE_NAME_TIME){
                            placeString += String.format(timeCategoryFmt, semanticPt.getTimeAndPlace().getTimeCategory().name);
                        }
                        break;
                    case PLACE_KEY_VALUE:
                    case PLACE_KEY_VALUE_TIME:
                        SemanticPlace place = semanticPt.getTimeAndPlace().getPlace();
                        OsmTag primaryTag = place.getPrimaryTag();
                        if(primaryTag == null){
                            placeString = semanticPt.getTimeAndPlace().getPlace().getId();
                        }
                        else{
                            placeString = semanticPt.getTimeAndPlace().getPlace().getPrimaryTag().toString();
                        }

                        if(itemExtractor == ItemExtractor.PLACE_KEY_VALUE_TIME){
                            placeString += String.format(timeCategoryFmt, semanticPt.getTimeAndPlace().getTimeCategory().name);
                        }
                        break;
                }
                placeStrings[j] = placeString;
            }
            //put place string sequence into the mapper
            mapping.mapTo(i, placeStrings);
            i++;
        }
        return mapping;
    }


    ////////////////////////////
    //PRIVATE CLASSES
    ////////////////////////////

    class PlacesMapping{

        private int[][] placeIntSequences;
        private Map<String, Integer> placeStrToId;
        private Map<Integer, String> placeIdToStr;
        private Integer placeIdCounter = 0;

        PlacesMapping(int nSequences){
            this.placeIntSequences = new int[nSequences][];
            this.placeStrToId = new HashMap<>();
            this.placeIdToStr = new HashMap<>();
        }

        void mapTo(int sequenceIdx, String[] placeStrings){
            //get the sequence that we want to add this place id to
            int[] sequence = new int[placeStrings.length];

            //map from strings to ints
            for (int i = 0; i < placeStrings.length; i++) {
                String placeStr = placeStrings[i];
                //get the id associated with that place string
                Integer placeId = placeStrToId.get(placeStr);
                if(placeId == null){
                    placeId = ++placeIdCounter;
                    placeStrToId.put(placeStr, placeId);
                    placeIdToStr.put(placeId, placeStr);
                }
                //add the place id to the sequence
                sequence[i] = placeId;
            }
            //store the int sequence
            this.placeIntSequences[sequenceIdx] = sequence;
        }

        String[] unMap(int[] placeIntSequence){
            String[] placeStrSequence = new String[placeIntSequence.length];
            for (int i = 0; i < placeIntSequence.length; i++) {
                placeStrSequence[i] = placeIdToStr.get(placeIntSequence[i]);
            }
            return placeStrSequence;
        }

        /**
         * @param outFile The file to write to.
         * Writes the sequences to a file using the SPMF format
         */
        void writeSequencesToFile(File outFile){
            SPMFWriter writer = new SPMFWriter();
            writer.write(outFile, placeIntSequences);
        }

        /**
         * @param outFile The file to write to.
         * Writes the sequences to a file using the SPMF (item-set) format
         */
        void writeItemsetsToFile(File outFile){
            SPMFItemsetWriter writer = new SPMFItemsetWriter();
            writer.write(outFile, placeIntSequences);
        }


    }


}
