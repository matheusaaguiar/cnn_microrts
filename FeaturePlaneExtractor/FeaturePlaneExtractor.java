package FeaturePlaneExtractor;

import org.jdom.input.SAXBuilder;
import rts.GameState;
import rts.Trace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Vector;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.ZipInputStream;

public class FeaturePlaneExtractor {
    Vector<String> inputDir;
    String outputDir;
    String logFileName;
    int fpsExtractedNum;
    int gamesReadNum;
    int tracesReadNum;
    int wcount;

    public Vector<String> getInputDir() {
        return this.inputDir;
    }

    public String getOutputDir() {
        return this.outputDir;
    }

    public void setInputDir(Vector<String> in) {
        this.inputDir = in;
    }

    public void setOutputDir(String out) {
        this.outputDir = out;
    }

    public int getFpsExtractedNum() {
        return this.fpsExtractedNum;
    }

    public int getGamesReadNum() {
        return this.gamesReadNum;
    }

    public int getTracesReadNum() {
        return this.tracesReadNum;
    }

    public  FeaturePlaneExtractor() {
        this.fpsExtractedNum = 0;
        this.gamesReadNum = 0;
        this.tracesReadNum = 0;
        this.wcount = 0;
    }

    public FeaturePlaneExtractor(Vector<String> input, String output) {
        this.inputDir = input;
        this.outputDir = output;
        this.logFileName = output + "log.txt";
        this.fpsExtractedNum = 0;
        this.gamesReadNum = 0;
        this.tracesReadNum = 0;
        this.wcount = 0;
    }

    private void log(String msg) throws Exception{
        if(!logFileName.isEmpty()) {
            FileWriter w = new FileWriter(logFileName);
            w.write(msg + '\n');
            w.close();
        }
    }

    public ArrayList<GameState> sampleGameStates(Trace gameTrace, Integer sampleNum) {

        ArrayList<GameState> samples = new ArrayList<GameState>();
        int randomNum;
        int min = 0;
        int max = gameTrace.getLength();

        //System.out.println("Sampling Game States...");
        for(Integer i = 0; i < sampleNum; ++i) {
            randomNum = ThreadLocalRandom.current().nextInt(min, max + 1);
            samples.add(gameTrace.getGameStateAtCycle(randomNum));
        }
        return samples;
    }

    public Trace loadTrace(File file) throws Exception {
        Trace currentTrace = null;
        try {
            if (file.getAbsolutePath().endsWith(".zip")) {
                ZipInputStream zip = new ZipInputStream(new FileInputStream(file));
                zip.getNextEntry(); // note: this assumes the zip file contains a single trace!
                currentTrace = new Trace(new SAXBuilder().build(zip).getRootElement());
            } else {
                currentTrace = new Trace(new SAXBuilder().build(file.getAbsolutePath()).getRootElement());
            }
            if(currentTrace == null)
                System.out.println("Trace from file " + file.getName() + " could NOT be created!");
            /*if(currentTrace != null)
                System.out.println("Trace created!");
            else
                System.out.println("Trace NOT created!");*/
        } catch (Exception ex) {
            System.out.println("Error! " + ex.toString());
            throw ex;
        }
        return currentTrace;
    }

    public ArrayList<FeaturePlaneStack> extractRandomSamplesFromFile(File traceFile, int samplesNum) throws Exception {

        ArrayList<FeaturePlaneStack> fpsList = new ArrayList<FeaturePlaneStack>();
        FeaturePlaneStack fps;

        Trace t = this.loadTrace(traceFile);
        int end = t.getLength();
        int w = t.winner();

        if(w != -1) {
            //ArrayList<GameState> samples = this.sampleGameStates(t, samplesNum);
            //for(GameState gs : samples) {
            ++this.gamesReadNum;
            this.wcount += w;
            for (GameState gs : sampleGameStates(t, samplesNum)) {
                fps = new FeaturePlaneStack(8, 8, 25);
                fps.setFromGameState(gs, w, end);
                //System.out.println("Printing feature planes");
                //fps.printStack();
                fpsList.add(fps);
                ++this.fpsExtractedNum;
                //System.out.println("Printing feature planes to File");
            }
        }
        else
            ;//System.out.println("Game was a draw: no samples extracted.");

        return fpsList;
    }

    public void extractRandomSamples(int samplesNum) throws Exception {

        for (String dir : this.inputDir) {

            File input = new File(dir);


            System.out.println("Extracting " + samplesNum + " samples from each game trace in " + input.getAbsolutePath() + " (" + input.listFiles().length + " files)");

            for (File f : input.listFiles()) {
                //System.out.println(f.getName());
                ++this.tracesReadNum;
                int i = 3;
                for (FeaturePlaneStack fps : extractRandomSamplesFromFile(f, samplesNum)) {
                    String fileName = outputDir + "sample" + (this.fpsExtractedNum - i) + ".txt";
                    --i;
                    //System.out.println("Printing feature planes to file " + fileName);
                    fps.toTextFile(new FileWriter(fileName));
                }
            }
        }

        System.out.println("Traces Read: " + String.valueOf(tracesReadNum) + '\n');
        System.out.println("Games used: " + String.valueOf(gamesReadNum) + "(Draws not used: " + String.valueOf(tracesReadNum - gamesReadNum) + ")\n");
        System.out.println("Samples extracted: " + String.valueOf(fpsExtractedNum) + '\n');
        System.out.println("Winner 0: " + String.valueOf(gamesReadNum-wcount) + '\n');
        System.out.println("Winner 1: " + String.valueOf(wcount) + '\n');

        String stats = outputDir + "statistics.txt";
        FileWriter w = new FileWriter(stats);
        w.write("Traces Read: " + String.valueOf(tracesReadNum) + '\n');
        w.write("Games used: " + String.valueOf(gamesReadNum) + "(Draws not used: " + String.valueOf(tracesReadNum - gamesReadNum) + ")\n");
        w.write("Samples extracted: " + String.valueOf(fpsExtractedNum) + '\n');

        w.close();
    }


    public static void main(String args[]) throws Exception {

        FeaturePlaneExtractor fpe = new FeaturePlaneExtractor();
        Vector<String> input = new Vector<String>();

      //ADD INPUT TRACE FOLDERS
        //input.add("/path/to/traces");
        //input.add("/path/to/traces2");


        String output = "";//"/path/to/output/samples";

        int samples = 3;

        fpe.setInputDir(input);
        fpe.setOutputDir(output);

        System.out.println("Initiating");
        fpe.extractRandomSamples(samples);
        System.out.println("Terminating");
    }
}
