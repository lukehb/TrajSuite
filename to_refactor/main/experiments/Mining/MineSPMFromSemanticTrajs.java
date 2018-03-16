package onethreeseven.trajsuite.experiments.Mining;

import ca.pfv.spmf.algorithms.frequentpatterns.dci_closed_optimized.AlgoDCI_Closed_Optimized;
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPGrowth;
import ca.pfv.spmf.algorithms.frequentpatterns.fpgrowth.AlgoFPMax;
import ca.pfv.spmf.algorithms.sequentialpatterns.clasp_AGP.AlgoCM_ClaSP;
import ca.pfv.spmf.algorithms.sequentialpatterns.clasp_AGP.dataStructures.creators.AbstractionCreator_Qualitative;
import ca.pfv.spmf.algorithms.sequentialpatterns.clasp_AGP.dataStructures.database.SequenceDatabase;
import ca.pfv.spmf.algorithms.sequentialpatterns.clasp_AGP.idlists.creators.IdListCreator;
import ca.pfv.spmf.algorithms.sequentialpatterns.clasp_AGP.idlists.creators.IdListCreatorStandard_Map;
import ca.pfv.spmf.algorithms.sequentialpatterns.spam.AlgoCMSPAM;
import ca.pfv.spmf.algorithms.sequentialpatterns.spam.AlgoTKS;
import ca.pfv.spmf.algorithms.sequentialpatterns.spam.AlgoVMSP;
import onethreeseven.common.data.AbstractWriter;
import onethreeseven.common.model.TimeCategoryPool;
import onethreeseven.common.util.FileUtil;
import onethreeseven.datastructures.data.resolver.IdFieldResolver;
import onethreeseven.datastructures.data.resolver.NumericFieldsResolver;
import onethreeseven.datastructures.data.resolver.TemporalFieldResolver;
import onethreeseven.geo.projection.ProjectionMercator;
import onethreeseven.spm.algorithm.CCSpan;
import onethreeseven.spm.algorithm.DCSpan;
import onethreeseven.spm.data.SPMFParser;
import onethreeseven.spm.model.SequentialPattern;
import onethreeseven.trajsuite.osm.algorithm.HomeEstimator;
import onethreeseven.trajsuite.osm.algorithm.TopKItems;
import onethreeseven.trajsuite.osm.data.SemanticPlaceFieldsResolver;
import onethreeseven.trajsuite.osm.data.SemanticTrajectoryParser;
import onethreeseven.trajsuite.osm.model.SemanticTrajectory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Read in semantic trajectories process them using sequential pattern mining.
 * @author Luke Bermingham
 */
public class MineSPMFromSemanticTrajs {

    private static final File semanticTrajFile = new File(FileUtil.makeAppDir("semantic"), "geolife_179_semantic.txt");

    private static final int minAbsSup = 15;
    private static final int topK = 20;

    private static final SemanticTrajToSequenceAdapter.ItemExtractor extractor =
            SemanticTrajToSequenceAdapter.ItemExtractor.PLACE_KEY_VALUE_TIME;

    private static final MODE miningMode = MODE.MAX;

    private static final boolean guessHomes = true;

    private enum MODE {
        ALL,
        CLOSED,
        MAX,
        CLOSED_CONTIG,
        TOP_K_SEQUENCES,
        TOP_K_ITEMS,
        DISTINCT_CONTIG,
        ITEMSET,
        MAX_ITEMSET,
        CLOSED_ITEMSET
    }

    //073, 40.00095082959428, 116.26871413333338, 2008-05-24T02:59:42, SMALL_HOURS, 29228773, Summer Palace, leisure=park

    public static void main(String[] args) throws IOException {

        SemanticTrajectoryParser parser = new SemanticTrajectoryParser(
                new ProjectionMercator(),
                new IdFieldResolver(0),
                new NumericFieldsResolver(1,2),
                new TemporalFieldResolver(3),
                new SemanticPlaceFieldsResolver(5,6,7),
                TimeCategoryPool.TIMES_OF_DAY,
                false
        );

        Map<String, SemanticTrajectory> trajs = parser.parse(semanticTrajFile);

        if(guessHomes){
            System.out.println("Guessing homes...");
            HomeEstimator homeEstimator = new HomeEstimator();
            for (SemanticTrajectory semanticTrajectory : trajs.values()) {
                homeEstimator.run(semanticTrajectory);
            }
        }

        //convert semantic trajectories into spmf integer sequence
        File spmfFile = createOutputFile("spmf", null, extractor.name(), 0);
        if(spmfFile.exists() && spmfFile.delete()){
            System.out.println("SPMF already existed, had to delete.");
        }

        SemanticTrajToSequenceAdapter adapter = new SemanticTrajToSequenceAdapter();
        SemanticTrajToSequenceAdapter.PlacesMapping mapping = adapter.adapt(trajs, extractor);

        //write in SPMF format (either item-sets or sequences).
        if(miningMode == MODE.ITEMSET || miningMode == MODE.CLOSED_ITEMSET || miningMode == MODE.MAX_ITEMSET){
            mapping.writeItemsetsToFile(spmfFile);
        }else{
            mapping.writeSequencesToFile(spmfFile);
        }


        System.out.println("Output raw integer sequences (for mining) are here: " + spmfFile.getAbsolutePath());

        //mine the spmf file
        File patternsFile = createOutputFile("spm", miningMode, extractor.name(),
                miningMode == MODE.TOP_K_ITEMS || miningMode == MODE.TOP_K_SEQUENCES ? topK : minAbsSup);

        if(patternsFile.exists()){
            System.out.println("This exact pattern mining output already exists, skipping mining step.");
        }
        else{
            switch (miningMode){
                case ALL:
                    runCMSPAM(spmfFile, patternsFile, trajs.size());
                    break;
                case CLOSED:
                    runClosedSPM(spmfFile, patternsFile, trajs.size());
                    break;
                case TOP_K_SEQUENCES:
                    runTKS(spmfFile, patternsFile);
                    break;
                case CLOSED_CONTIG:
                    runCCSPAN(spmfFile, patternsFile);
                    break;
                case MAX:
                    runVMSP(spmfFile, patternsFile, trajs.size());
                    break;
                case DISTINCT_CONTIG:
                    runDCSPAN(spmfFile, patternsFile, trajs.size());
                    break;
                case TOP_K_ITEMS:
                    runTOPKItems(spmfFile, patternsFile);
                    break;
                case ITEMSET:
                    runFrequentItemsetMining(spmfFile, patternsFile, trajs.size());
                    break;
                case MAX_ITEMSET:
                    runMaxItemsetMining(spmfFile, patternsFile, trajs.size());
                    break;
                case CLOSED_ITEMSET:
                    runClosedItemsetMining(spmfFile, patternsFile);
                    break;
            }
        }
        System.out.println("Output integer sequential patterns at: " + patternsFile.getAbsolutePath());

        //convert the patterns file into human readable strings using the place mapping
        File readableOutput = new File(FileUtil.makeAppDir("patterns"), patternsFile.getName());
        if(readableOutput.exists()){
            System.out.println("Readble output already exists, skipping the un-mapping step.");
        }else{
            createHumanReadablePatterns(patternsFile, readableOutput, mapping);
        }
        System.out.println("Get readble output at: " + readableOutput.getAbsolutePath());

    }

    private static void createHumanReadablePatterns(File patternsFile, File outputFile, SemanticTrajToSequenceAdapter.PlacesMapping mapping){

        SPMFParser parser = new SPMFParser();
        final List<SequentialPattern> seqs = parser.parsePatterns(patternsFile);

        class PatternWriter extends AbstractWriter<String[][]>{
            @Override
            protected boolean write(BufferedWriter bufferedWriter, String[][] strings) throws IOException {
                for (int sequenceIdx = 0; sequenceIdx < strings.length; sequenceIdx++) {
                    String[] stringSeq = strings[sequenceIdx];
                    int lastIdx = stringSeq.length - 1;
                    for (int i = 0; i <= lastIdx; i++) {
                        bufferedWriter.write(stringSeq[i]);
                        if (i < lastIdx) {
                            bufferedWriter.write(delimiter);
                        }
                    }
                    int support = seqs.get(sequenceIdx).getSupport();
                    bufferedWriter.write(" #SUP:" + support);
                    bufferedWriter.newLine();
                }
                return true;
            }
        }

        PatternWriter writer = new PatternWriter();

        int nSequences = seqs.size();
        String[][] stringSeqs = new String[nSequences][];

        for (int i = 0; i < nSequences; i++) {
            SequentialPattern sequentialPattern = seqs.get(i);
            String[] placesSeq = mapping.unMap(sequentialPattern.getSequence());
            stringSeqs[i] = placesSeq;
        }

        writer.write(outputFile, stringSeqs);
    }

    private static File createOutputFile(String dir, MODE mode, String extra, int param){
        String outFilename = semanticTrajFile.getName().split("\\.")[0];
        if(guessHomes){
            outFilename += "_homes";
        }
        if(mode != null){
            outFilename += "_mode_" + mode.name();
        }
        if(param > 0 && mode != null){
            outFilename += (mode == MODE.TOP_K_ITEMS || mode == MODE.TOP_K_SEQUENCES) ? "_k=" + topK : "_minSup=" + param;
        }
        if(extra != null){
            outFilename += "_" + extra;
        }
        outFilename += ".txt";
        return new File(FileUtil.makeAppDir(dir), outFilename);
    }

    private static void runClosedSPM(File inputFile, File outputFile, int nTrajs) throws IOException {
        double minSupRel = minAbsSup / (double)nTrajs;
        AbstractionCreator_Qualitative abstractionCreator = AbstractionCreator_Qualitative.getInstance();
        IdListCreator idListCreator = IdListCreatorStandard_Map.getInstance();
        SequenceDatabase sequenceDatabase = new SequenceDatabase(abstractionCreator, idListCreator);
        double relativeSupport = sequenceDatabase.loadFile(inputFile.getAbsolutePath(), minSupRel);
        AlgoCM_ClaSP algo = new AlgoCM_ClaSP(relativeSupport, abstractionCreator, true, true);
        algo.runAlgorithm(sequenceDatabase, true, false, outputFile.getAbsolutePath(), false);
        algo.printStatistics();
    }

    private static void runTOPKItems(File inputFile, File outputFile){
        TopKItems algo = new TopKItems();
        SPMFParser parser = new SPMFParser();
        System.out.println("Running Top-K item mining...");
        algo.run(parser.parseSequences(inputFile), outputFile, topK);
    }

    private static void runCCSPAN(File inputFile, File outputFile){
        CCSpan algo = new CCSpan();
        SPMFParser parser = new SPMFParser();
        System.out.println("Running CC-SPAN...");
        algo.run(parser.parseSequences(inputFile), minAbsSup, outputFile);
    }

    private static void runTKS(File inputFile, File outputFile) throws IOException {
        System.out.println("Running TKS...");
        AlgoTKS algo = new AlgoTKS();
        algo.runAlgorithm(inputFile.getAbsolutePath(), null, topK);
        algo.writeResultTofile(outputFile.getAbsolutePath());
        algo.printStatistics();
    }

    private static void runVMSP(File inputFile, File outputFile, int nTrajs) throws IOException {
        System.out.println("Running VMSP...");
        AlgoVMSP algo = new AlgoVMSP();
        double minSupRel = minAbsSup / (double)nTrajs;
        algo.runAlgorithm(inputFile.getAbsolutePath(), outputFile.getAbsolutePath(), minSupRel);
        algo.printStatistics();
    }

    private static void runCMSPAM(File inputFile, File outputFile, int nTrajs) throws IOException {
        System.out.println("Running CMSPAM...");
        AlgoCMSPAM algo = new AlgoCMSPAM();
        double minSupRel = minAbsSup / (double)nTrajs;
        algo.runAlgorithm(inputFile.getAbsolutePath(), outputFile.getAbsolutePath(), minSupRel, false);
        algo.printStatistics();
    }

    private static void runMaxItemsetMining(File inputFile, File outputFile, int nTrajs) throws IOException {
        AlgoFPMax algo = new AlgoFPMax();
        double minSupRel = minAbsSup / (double)nTrajs;
        algo.runAlgorithm(inputFile.getAbsolutePath(), outputFile.getAbsolutePath(), minSupRel);
        algo.printStats();
    }

    private static void runFrequentItemsetMining(File inputFile, File outputFile, int nTrajs) throws IOException {
        AlgoFPGrowth algo = new AlgoFPGrowth();
        double minSupRel = minAbsSup / (double)nTrajs;
        algo.runAlgorithm(inputFile.getAbsolutePath(), outputFile.getAbsolutePath(), minSupRel);
        algo.printStats();
    }

    private static void runClosedItemsetMining(File inputFile, File outputFile) throws IOException {
        AlgoDCI_Closed_Optimized algo = new AlgoDCI_Closed_Optimized();
        algo.setShowTransactionIdentifiers(true);
        algo.runAlgorithm(inputFile.getAbsolutePath(), outputFile.getAbsolutePath(), minAbsSup);
    }

    private static void runDCSPAN(File inputFile, File outputFile, int nTrajs) throws IOException {
        File tmpFile = new File(FileUtil.makeAppDir("tmp"), "temp_patterns.txt");

        runCMSPAM(inputFile, tmpFile, nTrajs);

        System.out.println("Running DC-SPAN...");

        DCSpan algo = new DCSpan();
        SPMFParser parser = new SPMFParser();
        List<SequentialPattern> patterns = parser.parsePatterns(tmpFile);
        int[][] seqDb = parser.parseSequences(inputFile);

        algo.run(seqDb, patterns, 0, outputFile);

        if(tmpFile.delete()){
            System.out.println("Deleted temporary pattern file...");
        }

    }

}
