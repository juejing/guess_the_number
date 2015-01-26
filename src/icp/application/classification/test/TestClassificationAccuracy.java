package icp.application.classification.test;

import icp.Const;
import icp.online.app.OnlineDetection;
import icp.application.classification.FilterFeatureExtraction;
import icp.application.classification.IERPClassifier;
import icp.application.classification.IFeatureExtraction;
import icp.application.classification.MLPClassifier;
import icp.online.app.DataObjects.MessageType;
import icp.online.app.DataObjects.ObserverMessage;
import icp.online.app.OffLineDataProvider;
import icp.online.gui.*;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * Created by stebjan on 20.1.2015.
 */
public class TestClassificationAccuracy implements Observer {

    private String dir = "data/numbers/Horazdovice";
    private final String infoFileName = "info.txt";

    private Map<String, Integer> results;
    private Integer[] result;
    private String filename;
    private boolean end;
    private Map<String, Statistics> stats;

    public static void main(String[] args) throws InterruptedException {
        TestClassificationAccuracy testClassificationAccuracy = new TestClassificationAccuracy();
    }

    public TestClassificationAccuracy() throws InterruptedException {
        stats = new HashMap<>();
        results = new HashMap<>();
        
        try {
            File directory;
            Map<String, Integer> map;
            for (String dirName : Const.DIRECTORIES) {
                directory = new File(dirName);
                if (directory.exists() && directory.isDirectory()) {
                    map = loadExpectedResults(infoFileName, dirName);
                    results.putAll(map);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        File directory;
        File f;
        for (String dirName : Const.DIRECTORIES) {
            directory = new File(dirName);
            if (directory.exists() && directory.isDirectory()) {
                for (Entry<String, Integer> entry : results.entrySet()) {
                    f = new File(directory, entry.getKey());
                    if (f.exists() && f.isFile()) {
                        end = false;
                        filename = f.getName();
                        IERPClassifier classifier = new MLPClassifier();
                        classifier.load(Const.TRAINING_FILE_NAME);
                        IFeatureExtraction fe = new FilterFeatureExtraction();
                        classifier.setFeatureExtraction(fe);
                        OnlineDetection detection = new OnlineDetection(classifier, this);
                        OffLineDataProvider offLineData = new OffLineDataProvider(f, detection);
                        Thread t = new Thread(offLineData);
                        t.start();
                        while (!end) {
                            Thread.sleep(500);
                        }
                    }
                }
            }
        }

        printStats();
    }

    private void printStats() {
        System.out.println("----------------------------------------------");
        System.out.println("Statistics: ");
        System.out.println();
        int okNumber = 0;
        for (Map.Entry<String, Statistics> entry : stats.entrySet()) {
            if (entry.getValue().getRank() == 1) {
                okNumber++;
            }
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
            System.out.println();
        }
        System.out.println("Total points: " + Statistics.getTotalPts() + " of " + Statistics.MAX_POINT * stats.size());
        System.out.println("Perfect guess: " + okNumber);
        double percent = ((double) okNumber / stats.size()) * 100;
        System.out.println("Accuracy: " + percent + " %");

    }

    private Map<String, Integer> loadExpectedResults(String filename, String dir) throws IOException {
        Map<String, Integer> res = new HashMap<>();
        File file = new File(dir + File.separator + filename);
        FileInputStream fis = new FileInputStream(file);

        //Construct BufferedReader from InputStreamReader
        BufferedReader br = new BufferedReader(new InputStreamReader(fis));

        String line;
        int num;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(" ");
            if (parts.length > 1) {
                try {
                    num = Integer.parseInt(parts[1]);
                    res.put(parts[0], num);
                } catch (NumberFormatException ex) {
                }
            }
        }

        br.close();
        return res;
    }

    private Integer[] initProbabilities(double[] probabilities) {
        Integer[] ranks = new Integer[probabilities.length];
        for (int i = 0; i < ranks.length; ++i) {
            ranks[i] = i;
        }
        Comparator<Integer> gc = new ProbabilityComparator(probabilities);
        Arrays.sort(ranks, gc);
        return ranks;

    }

    private int getExpectedResult(String filename) {
        return results.get(filename);
    }

    @Override
    public void update(Observable o, Object message) {
        if (message instanceof OnlineDetection) {
            double[] probabilities = ((OnlineDetection) message).getWeightedResults();

            result = initProbabilities(probabilities);

        }
        if (message instanceof ObserverMessage) {
            ObserverMessage msg = (ObserverMessage) message;
            if (msg.getMsgType() == MessageType.END) {
                System.out.println(filename);
                int winner = (result[0] + 1);
                Statistics st = new Statistics();
                st.setExpectedResult(getExpectedResult(filename));
                st.setThoughtResult(winner);

                for (int i = 0; i < result.length; i++) {
                    if ((result[i] + 1) == getExpectedResult(filename)) {
                        st.setRank(i + 1);
                        break;
                    }
                }
                stats.put(filename, st);
                end = true;

            }
        }
    }
}
