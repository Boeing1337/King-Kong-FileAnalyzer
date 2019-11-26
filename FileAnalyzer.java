import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public final class FileAnalyzer {
    private final String DIRECTORY;
    private final PatternsStorage PATTERNS;

    private List<Callable<String>> operationList;
    private List<Future<String>> futuresList;
    private ExecutorService mainExecutor;
    private File[] fileName;


    private FileAnalyzer(final PatternsStorage PATTERNS, String DIRECTORY) {
        this.DIRECTORY = DIRECTORY;
        this.PATTERNS = PATTERNS;
    }

    public static void main(final String[] args) {

        PatternsStorage patternsStorage = new PatternsStorage(args[1]);
        patternsStorage.archiveAll();

        new FileAnalyzer(patternsStorage, args[0]).process();
    }

    private void process() {
        takeFilesFromDirectory();

        if (!isFilesExists()) {
            return;
        }

        createExecutors();
        createOperationList();
        parseFilesToExecutors();
        outputResult();
        mainExecutor.shutdownNow();
    }

    private void takeFilesFromDirectory() {
        fileName = new File(DIRECTORY).listFiles(File::isFile);
    }

    private boolean isFilesExists() {
        return fileName.length > 0;
    }

    private void createExecutors() {
        mainExecutor = Executors.newFixedThreadPool(fileName.length);
    }

    private void createOperationList() {
        operationList = new ArrayList<>();
        for (File e : fileName) {
            operationList.add(() -> new Analyzer(
                    DIRECTORY + File.separator + e.getName())
                    .process()
            );
        }
    }

    private void parseFilesToExecutors() {
        try {
            futuresList = mainExecutor.invokeAll(operationList);
        } catch (Exception ignored) {

        }
    }

    private void outputResult() {
        final int CROPDIRECTORY = DIRECTORY.length();

        for (Future<String> e : futuresList) {
            try {
                String result = e.get().substring(CROPDIRECTORY);
                System.out.println(result);
            } catch (Exception ignored) {
            }


        }
    }


    final class Analyzer {
        private final File FILEPATH;


        private int directoryIndex;
        private String pattern;
        private int[] prefix;
        private byte[] contentFromFile;
        private boolean isFound = false;

        Analyzer(final String FILEPATH) {
            this.FILEPATH = new File(FILEPATH);
        }

        String process() {
            readFIle();
            search();
            return result();
        }

        private void readFIle() {
            try {
                final InputStream streamFilePath =
                        new BufferedInputStream(new FileInputStream(FILEPATH));
                contentFromFile = streamFilePath.readAllBytes();
                streamFilePath.close();
            } catch (Exception ignored) {

            }
        }

        private void search() {
            directoryIndex = PATTERNS.getSize() - 1;
            for (; directoryIndex >= 0 && !isFound; directoryIndex--) {

                pattern = PATTERNS.get(directoryIndex).getPattern();
                prefixFunc();
                KMPAlgorithm();

            }
        }

        private void prefixFunc() {
            prefix = new int[pattern.length()];

            for (int end = 1; end < pattern.length(); end++) {
                int suff = prefix[end - 1];

                while (suff > 0 && pattern.charAt(end) != pattern.charAt(suff)) {
                    suff = prefix[suff - 1];
                }

                if (pattern.charAt(end) == pattern.charAt(suff)) {
                    suff++;
                }
                prefix[end] = suff;
            }

        }

        private void KMPAlgorithm() {
            int count = 0;

            for (byte b : contentFromFile) {
                while (count > 0 && b != pattern.charAt(count)) {
                    count = prefix[count - 1];
                }

                if (b == pattern.charAt(count)) {
                    count++;
                }

                if (count == pattern.length()) {
                    isFound = true;
                    return;
                }
            }

        }

        private String result() {
            String output = FILEPATH + ": ";
            if (isFound) {
                return output + PATTERNS.get(directoryIndex + 1).getName();
            } else {
                return output + "Unknown file type";
            }


        }

    }

}

final class PatternsStorage {
    private final String DBNAME;

    private volatile List<Pattern> patternList = new ArrayList<>();
    private List<String> DBLines = new ArrayList<>();

    PatternsStorage(final String DBNAME) {
        this.DBNAME = DBNAME;
    }

    int getSize() {
        return patternList.size();
    }

    Pattern get(final int index) {
        if (getSize() <= index || index < 0) {
            return new Pattern("emptyPattern");
        } else {
            return patternList.get(index);
        }
    }

    void archiveAll() {
        readFIle();
        archive();
    }

    private void readFIle() {
        try {
            DBLines = Files.readAllLines(Paths.get(DBNAME));
        } catch (Exception e) {
            System.out.println(e.toString() + " PatternsStorage FromDBName!");
        }
    }

    private void archive() {
        for (String e : DBLines) {
            final String[] temp = e.trim().replaceAll("\"", "").split(";");
            patternList.add(new Pattern(temp[1], temp[2]));
        }
    }


}

final class Pattern {
    private final String pattern;
    private final String name;

    Pattern(final String pattern, String name) {
        this.pattern = pattern;
        this.name = name;
    }

    Pattern(final String emptyPattern) {
        this.pattern = "pattern";
        this.name = "name";
    }

    String getName() {
        return name;
    }

    String getPattern() {
        return pattern;
    }
}


///Timer was need for first and second stage

final class Timer {
    private long startTime;
    private long endTime;

    void start() {
        startTime = System.currentTimeMillis();
    }

    void stop() {
        endTime = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        int elapsed = (int) (endTime - startTime);
        return elapsed / 1000 + "." + (elapsed - elapsed / 1000);
    }

}