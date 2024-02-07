/**
 * COMP-10205 - Starting code for Assignment # 3
 * @author Young Sang Kwon, 000847777
 *
 * Performance Analysis:
 * In this program, the main task is to identify words in a novel that are not found in a dictionary.
 * To achieve this, three distinct methods were utilized:
 * 1) Linear search using ArrayList.contains()
 * 2) Binary search using Collections.binarySearch()
 * 3) Hash table lookup using a SimpleHashSet data structure.
 *
 * From the results:
 * Part A-5) The # of word that are not contained in the dictionary and performance
 *  ArrayList contains: 3296, time: 603000
 *  binarySearch      : 3296, time: 166700
 *  SimpleHashSet     : 3296, time: 126200
 *
 * Observations:
 * - All three methods found the same number of words not present in the dictionary: 3296 words.
 * - The linear search using ArrayList.contains() is the slowest. This is expected as the worst-case time complexity for a linear search in an ArrayList is O(n).
 * - The binary search with Collections.binarySearch() is faster than linear search but slower than hash table lookup. For binary search to work, the ArrayList must be sorted, and its time complexity is O(log n).
 * - The hash table lookup using SimpleHashSet was the fastest among the three. This is anticipated because, in an ideal scenario, the hash table provides a constant time complexity of O(1) for search operations.
 *
 * Conclusions:
 * For searching operations in a large dataset like this novel, hash table lookups provide superior performance compared to linear and binary searches.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Assignment3 {
    // For Part A variables
    private static ArrayList<BookWord> novelWords = new ArrayList<>();
    private static ArrayList<BookWord> dictionaryWords = new ArrayList<>();
    private static SimpleHashSet<BookWord> hashSetDictionary = new SimpleHashSet<>();

    // For Part B variables
    private static List<WordPosition> wordPositionsList = new ArrayList<>();
    private static final String RING = "ring";
    private static final int PROXIMITY_DISTANCE = 42;
    private static List<String> characters = Arrays.asList("frodo", "sam", "bilbo", "gandalf", "boromir", "aragorn",
                                                           "legolas", "gollum", "pippin", "merry", "gimli", "sauron",
                                                           "saruman", "faramir", "denethor", "treebeard", "elrond", "galadriel");

    public static void main(String[] args) {
        // For Part A procedures
        loadDictionary();
        loadNovel();

        System.out.println("Part A-1) Total # of words in the novel: " + novelWords.stream().mapToInt(BookWord::getCount).sum());
        System.out.println("\nPart A-2) Total # of unique words in the file: " + novelWords.size());

        displayTop10FrequentWords();
        displayWordsOccurring64Times();
        checkMisspelledWords();

        // For Part B procedures
        loadPositions();

        long startTime = System.nanoTime();
        List<CharacterProximity> results = computeClosenessFactor();
        long endTime = System.nanoTime();
        long timeElapsed = endTime - startTime;

        Collections.sort(results);

        if (!results.isEmpty()) {
            System.out.println("\nPart B-1) Who is the real Lord of the Rings: " + results.get(0).characterName);
        } else {
            System.out.println("\nPart B-1) Who is the real Lord of the Rings: No character found close to Ring");
        }

        for (CharacterProximity cp : results) {
            System.out.println("  " + cp);
        }

        System.out.println("\nPart B-2) Execution time(ms): " + timeElapsed / 1000000);
    }
    // For Part A
    /**
     * Description: Represents a word from the book.
     */
    public static class BookWord {
        private String text;
        private Integer count;

        /**
         * Constructor for the BookWord class.
         * @param: wordText
         */
        public BookWord(String wordText) {
            this.text = wordText.toLowerCase();
            this.count = 1;
        }

        public String getText() {
            return text;
        }

        public Integer getCount() {
            return count;
        }

        public void incrementCount() {
            this.count++;
        }

        /**
         * Description: Checks equality of two BookWord objects.
         * @param: wordToCompare
         * @return: Returns true if objects are equal, false otherwise.
         */
        @Override
        public boolean equals(Object wordToCompare) {
            if (this == wordToCompare) return true;
            if (wordToCompare == null || getClass() != wordToCompare.getClass()) return false;
            BookWord bookWord = (BookWord) wordToCompare;
            return text.equals(bookWord.text);
        }

        /**
         * Description: A hash code for the BookWord.
         * @return: hash code.
         * Using FNV-1a hashing algorithm - https://en.wikipedia.org/wiki/Fowler%E2%80%93Noll%E2%80%93Vo_hash_function
         * Constants for 32-bit FNV-1a hash
         */
        @Override
        public int hashCode() {
            final int OFFSET_BASIS = 0x811c9dc5;
            final int PRIME = 0x01000193;

            int hash = OFFSET_BASIS;
            for (char c : text.toCharArray()) {
                hash ^= (int) c;
                hash *= PRIME;
            }
            return hash;
        }

        @Override
        public String toString() {
            return "  BookWord{" + "text='" + text + '\'' + ", count=" + count + '}';
        }
    }

    /**
     * Description: A basic implementation of a hash set.(Code given in assignment)
     */
    public static class SimpleHashSet<T> {

        public ArrayList<T>[] buckets;
        public int numberOfBuckets = 10;
        public int size = 0;
        public static final double AVERAGE_BUCKET_SIZE = 3;

        public SimpleHashSet() {
            // Create buckets.
            buckets = new ArrayList[numberOfBuckets];
            for (int i = 0; i < numberOfBuckets; i++) {
                buckets[i] = new ArrayList<T>();
            }
            size = 0;
        }

        public int getHash(T x, int hashSize) {
            // Use modulus as hash function.
            return Math.abs(x.hashCode() % hashSize);
        }

        public void resize() {
            // Double number of buckets.
            int newBucketsSize = numberOfBuckets * 2;
            ArrayList<T>[] newBuckets = new ArrayList[newBucketsSize];

            // Create new buckets.
            for (int i = 0; i < newBucketsSize; i++) {
                newBuckets[i] = new ArrayList<T>();
            }

            // Copy elements over and use new hashes.
            for (int i = 0; i < numberOfBuckets; i++) {
                for (T y : buckets[i]) {
                    int hash = getHash(y, newBucketsSize);
                    newBuckets[hash].add(y);
                }
            }

            // Set new buckets.
            buckets = newBuckets;
            numberOfBuckets = newBucketsSize;
        }

        public boolean insert(T x) {
            // Get hash of x.
            int hash = Math.abs(getHash(x, numberOfBuckets));

            // Get current bucket from hash.
            ArrayList<T> curBucket = buckets[hash];

            // Stop, if current bucket already has x.
            if (curBucket.contains(x)) {
                return false;
            }

            // Otherwise, add x to the bucket.
            curBucket.add(x);

            // Resize if the collision chance is higher than threshold.
            if ((float) size / numberOfBuckets > AVERAGE_BUCKET_SIZE) {
                resize();
            }
            size++;
            return true;
        }

        public boolean contains(T x) {
            // Get hash of x.
            int hash = getHash(x, numberOfBuckets);

            // Get current bucket from hash.
            ArrayList<T> curBucket = buckets[hash];

            // Return if bucket contains x.
            return curBucket.contains(x);
        }

        public boolean remove(T x) {
            // Get hash of x.
            int hash = getHash(x, numberOfBuckets);

            // Get bucket from hash.
            ArrayList<T> curBucket = buckets[hash];

            // Remove x from bucket and return if operation successful.
            return curBucket.remove(x);
        }

        /**
         * Utility method that will return the current number of buckets
         * @return
         */

        public int getNumberofBuckets()
        {
            return numberOfBuckets;
        }

        public int getNumberofEmptyBuckets()
        {
            int empty = 0;
            for (ArrayList bucket : buckets)
                if (bucket.size() == 0)
                    empty++;
            return empty;
        }

        public int size()
        {
            return size;
        }

        public int getLargestBucketSize()
        {
            int maximumSize = 0;
            for (ArrayList bucket : buckets)
                if (bucket.size() > maximumSize)
                    maximumSize = bucket.size();
            return maximumSize;
        }
    }

    /**
     * Description: Loads the dictionary words.
     */
    public static void loadDictionary() {
        try (Scanner fin = new Scanner(new File("src/US.txt"))) {
            while (fin.hasNext()) {
                String word = fin.next().toLowerCase();
                BookWord bookWord = new BookWord(word);
                dictionaryWords.add(bookWord);
                hashSetDictionary.insert(bookWord);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
        Collections.sort(dictionaryWords, Comparator.comparing(BookWord::getText));
    }

    /**
     * Description: Loads the novel words.
     */
    public static void loadNovel() {
        try (Scanner fin = new Scanner(new File("src/TheLordOfTheRings.txt"))) {
            fin.useDelimiter("\\s|\"|\\(|\\)|\\.|\\,|\\?|\\!|\\_|\\-|\\:|\\;|\\n");
            while (fin.hasNext()) {
                String word = fin.next().toLowerCase();
                if (word.length() > 0) {
                    int index = novelWords.indexOf(new BookWord(word));
                    if (index == -1) {
                        novelWords.add(new BookWord(word));
                    } else {
                        novelWords.get(index).incrementCount();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
        Collections.sort(novelWords, (a, b) -> b.getCount().compareTo(a.getCount()) == 0 ? a.getText().compareTo(b.getText()) : b.getCount().compareTo(a.getCount()));
    }

    /**
     * Description: Displays the top 10 most frequent words from the novel.
     */
    public static void displayTop10FrequentWords() {
        System.out.println("\nPart A-3) Top 10 Frequent Words:");

        Collections.sort(novelWords, (w1, w2) -> {
            if (w1.getCount() == w2.getCount()) {
                return w1.getText().compareTo(w2.getText());
            }
            return w2.getCount() - w1.getCount();
        });

        for (int i = 0; i < 10 && i < novelWords.size(); i++) {
            System.out.println(novelWords.get(i));
        }
    }

    /**
     * Description: Displays words that occur 64 times in the novel.
     */
    public static void displayWordsOccurring64Times() {
        System.out.println("\nPart A-4) Words Occurring 64 Times:");
        novelWords.stream().filter(b -> b.getCount() == 64).sorted(Comparator.comparing(BookWord::getText)).forEach(System.out::println);
    }

    /**
     * Description: Checks and displays the misspelled words from the novel using various methods.
     */
    public static void checkMisspelledWords() {
        int count1 = 0, count2 = 0, count3 = 0;
        long time1 = 0, time2 = 0, time3 = 0;
        for (BookWord novelWord : novelWords) {
            if (!dictionaryWords.contains(novelWord)) {
                long start1 = System.nanoTime();
                count1++;
                long end1 = System.nanoTime();
                time1 += (end1-start1);
            }
            if (Collections.binarySearch(dictionaryWords, novelWord, Comparator.comparing(BookWord::getText)) < 0) {
                long start2 = System.nanoTime();
                count2++;
                long end2 = System.nanoTime();
                time2 += (end2-start2);
            }
            if (!hashSetDictionary.contains(novelWord)) {
                long start3 = System.nanoTime();
                count3++;
                long end3 = System.nanoTime();
                time3 += (end3-start3);
            }
        }
        System.out.println("\nPart A-5) The # of word that are not contained in the dictoinary and performance");
        System.out.println("  ArrayList contains: " + count1 + ", time: " + time1);
        System.out.println("  binarySearch      : " + count2 + ", time: " + time2);
        System.out.println("  SimpleHashSet     : " + count3 + ", time: " + time3);
    }

    // For Part B
    private static List<Integer> ringPositions = new ArrayList<>(); // A list to store the positions of the word "ring" in the text.

    /**
     * Description: Represents a word and its positions within a text.
     */
    public static class WordPosition {
        private String word;
        private List<Integer> positions;

        /**
         * Description: Constructor for the WordPosition class.
         * @param: word
         */
        public WordPosition(String word) {
            this.word = word;
            this.positions = new ArrayList<>();
        }

        /**
         * Description: Retrieves the word represented by the WordPosition object.
         * @return: word
         */
        public String getWord() {
            return word;
        }

        /**
         * Description: Retrieves the list of positions where the word appears in the text.
         * @return: positions
         */
        public List<Integer> getPositions() {
            return positions;
        }

        /**
         * Description: Adds a new position to the list of positions for the word.
         * @param: position
         */
        public void addPosition(int position) {
            this.positions.add(position);
        }
    }

    /**
     * Description: Represents a character's proximity to the word "ring".
     */
    public static class CharacterProximity implements Comparable<CharacterProximity> {
        String characterName;
        int occurrencesCount;
        int proximityCount;
        double closenessFactor;

        /**
         * Description: Constructor for CharacterProximity class.
         * @param: characterName
         * @param: proximityCount
         * @param: closenessFactor
         */
        public CharacterProximity(String characterName, int occurrencesCount, int proximityCount, double closenessFactor) {
            this.characterName = characterName;
            this.occurrencesCount = occurrencesCount;
            this.proximityCount = proximityCount;
            this.closenessFactor = closenessFactor;
        }

        /**
         * Description: Compares two CharacterProximity objects based on closeness factor.
         * @param: o
         * @return: The comparison result
         */
        @Override
        public int compareTo(CharacterProximity o) {
            return Double.compare(o.closenessFactor, this.closenessFactor);
        }

        @Override
        public String toString() {
            return "[" + characterName + ", " + occurrencesCount + "] Close to Ring " + proximityCount + " Closeness Factor " + String.format("%.4f", closenessFactor);
        }
    }

    /**
     * Description: Loads positions of each character's name and the word "ring" from a text file.
     */
    private static void loadPositions() {
        int wordPosition = 0;
        try (Scanner fin = new Scanner(new File("src/TheLordOfTheRings.txt"))) {
            fin.useDelimiter("\\s|\"|\\(|\\)|\\.|\\,|\\?|\\!|\\_|\\-|\\:|\\;|\\n");
            while (fin.hasNext()) {
                String word = fin.next().toLowerCase();
                wordPosition++;
                if (RING.equals(word)) {
                    ringPositions.add(wordPosition);
                } else if (characters.contains(word)) {
                    WordPosition wp = findOrCreateWordPosition(word);
                    wp.addPosition(wordPosition);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }

    /**
     * Description: Finds the WordPosition object for the given word in the wordPositionsList.
     * If it does not exist, it creates a new WordPosition object for the word and adds it to the list.
     *
     * @param: word
     * @return: WordPosition
     */
    private static WordPosition findOrCreateWordPosition(String word) {
        for (WordPosition wp : wordPositionsList) {
            if (wp.getWord().equals(word)) {
                return wp;
            }
        }
        WordPosition newWp = new WordPosition(word);
        wordPositionsList.add(newWp);
        return newWp;
    }

    /**
     * Description: Computes how close each character's name is to the word "ring".
     * @return: results
     */
    private static List<CharacterProximity> computeClosenessFactor() {
        List<CharacterProximity> results = new ArrayList<>();
        for (String characterName : characters) {
            int proximityCount = 0;
            List<Integer> positions = getPositionsForWord(characterName);

            if (positions != null) {
                for (int position : positions) {
                    for (int ringPosition : ringPositions) {
                        if (Math.abs(ringPosition - position) <= PROXIMITY_DISTANCE) {
                            proximityCount++;
                        }
                    }
                }
                double closenessFactor = (double) proximityCount / positions.size();
                results.add(new CharacterProximity(characterName, positions.size(), proximityCount, closenessFactor));
            }
        }
        return results;
    }

    /**
     * Description: Retrieves the positions list for the given word from the WordPosition object in the wordPositionsList.
     *
     * @param: word
     * @return: wp.getPositions()
     */
    private static List<Integer> getPositionsForWord(String word) {
        for (WordPosition wp : wordPositionsList) {
            if (wp.getWord().equals(word)) {
                return wp.getPositions();
            }
        }
        return null;
    }
}