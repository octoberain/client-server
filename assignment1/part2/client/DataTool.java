import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class DataTool {

    private BufferedWriter bw;
    private static final int NINETYNINE = 99;
    private static final int ABNORMAL_ADJ = 0;
    private static final String SEPARATOR = ",";
    private static final String filePath = System.getProperty("user.dir") + "/output/records.csv";
    private List<Integer> latencies;
    long wallTime;

    public DataTool() {
        createFile();
        init();
    }

    public void init() {
        try {
            this.bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void createFile() {
        File f = new File(filePath);
        try {
            f.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Thread-safe writing to file
     */
    synchronized public void writeToCSV(Record r) {
        StringBuffer sb = new StringBuffer();
        sb.append(r.getStartTime());
        sb.append(SEPARATOR);
        sb.append(r.getLatency());
        sb.append(SEPARATOR);
        sb.append(r.getRequestType());
        sb.append(SEPARATOR);
        sb.append(r.getResponseCode());
        try {
            bw.write(sb.toString());
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static List<Integer> readFromCSV() {
        List<Integer> latencies = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(SEPARATOR);
                latencies.add(Integer.parseInt(values[1]));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return latencies;
    }

    public void close() {
        try {
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private long getMin() {
        return latencies.stream().min(Integer::compare).get();
    }

    private long getMax() {
        return latencies.stream().max(Integer::compare).get();
    }

    private double getMean() {
        return Math.round(latencies.subList(ABNORMAL_ADJ, latencies.size()).stream().mapToInt(i -> i).average().getAsDouble());
    }

    private double getMedian() {
        DoubleStream sorted = latencies.subList(ABNORMAL_ADJ, latencies.size()).stream().mapToDouble(i -> i).sorted();
        double median = latencies.size() % 2 == 0 ?
                sorted.skip(latencies.size() / 2 - 1).limit(2).average().getAsDouble() :
                sorted.skip(latencies.size() / 2).findFirst().getAsDouble();
        return Math.round(median);
    }

    private double getThroughput() {
        // calculate total response time
        return Math.round((latencies.size()) * 1000 / this.wallTime);
    }

    private int get99percentile() {
        // save sorted to a different list, not modifing original list
        List<Integer> sorted = latencies.stream().collect(Collectors.toList());
        Collections.sort(sorted);
        int index = (int) Math.ceil(NINETYNINE / 100.0 * latencies.size());
        return sorted.get(index - 1);
    }

    public void printStats() {
        if (latencies == null)
            this.latencies = readFromCSV();
        System.out.println("\n ------ Latency Analysis ------");
        Utils.printMsg("Min Latency", this.getMin(), "milliseconds");
        Utils.printMsg("Max Latency", this.getMax(), "milliseconds");
        Utils.printMsg("Mean Latency", this.getMean(), "milliseconds");
        Utils.printMsg("Median Latency", this.getMedian(), "milliseconds");
        Utils.printMsg("Throughput", this.getThroughput(), "per second");
        Utils.printMsg("99% Percentile", this.get99percentile(), "milliseconds");
    }

}
