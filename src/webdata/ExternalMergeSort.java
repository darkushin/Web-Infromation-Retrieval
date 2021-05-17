package webdata;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ExternalMergeSort {
    private List<String> inv;
    private Comparator<Integer> cmp;
//    private String filePrefix;
    private int numFiles;  // current number of files to merge
    private int pairsInBlock;
    private String dir;
    private int iteration;  // number of merges performed (including current iteration). 1 means we are currently in the first iteration.
    private int savedFiles;  // number of files that were saved in the current iteration.

    private int AVAILABLE_BLOCKS = 5000;

    ExternalMergeSort(Comparator<Integer> cmp, int numFiles, int pairsInBlock, String dir, List<String> inv){
        this.cmp = cmp;
        this.numFiles = numFiles;
//        this.filePrefix = filePrefix;
        this.pairsInBlock = pairsInBlock;
        this.dir = dir;
        this.iteration = 1;
        this.savedFiles = 0;

        this.inv = inv;
    }

    public void sort(){
        // todo: need to handle the case where more than one iteration is needed
        try {
            SingleMerge singleMerge = new SingleMerge(1, numFiles);
            singleMerge.merge();
        } catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }


    }



    /** Holds all the information required for a single iteration of the merge-sort algorithm */
    private class SingleMerge{
        private ArrayList<ObjectInputStream> fileReaders;
        private ArrayList<Deque<int[]>> fileDeques;
        private final int numPairsInDeque;
        private int[] outputBlock;
        private int outputPtr;
        private ObjectOutputStream mergedOutput;


        private SingleMerge(int start, int end) throws IOException {
            // make a new dir for the files of this iteration:
            Files.createDirectories(Path.of(dir + "/iteration_" + (iteration+1)));

            this.numPairsInDeque = ((AVAILABLE_BLOCKS - 1) / (end-start+1)) * pairsInBlock;
            this.mergedOutput = new ObjectOutputStream(new FileOutputStream(dir + "/iteration_" + (iteration+1) + "/" + (savedFiles+1)));
            this.fileReaders = new ArrayList<>(end-start+1);
            this.fileDeques = new ArrayList<>(end-start+1);

            for (int i=start; i<=end; i++){
                FileInputStream fileIn = new FileInputStream(dir + "/iteration_" + iteration + "/" + i);
                this.fileReaders.add(new ObjectInputStream(fileIn));
                this.fileDeques.add(new ArrayDeque<int[]>(this.numPairsInDeque));
            }
        }

        private void merge() throws IOException {
            this.clearOutputBlock();
            this.loadAll();
            while (!this.areAllDequesEmpty()){
                ArrayList<String> heads = getHeads();
                int minIndex = this.getMin();
                this.extractMin(minIndex);
            }
            this.saveOutputBlock();  // needed in case the block wasn't full
            mergedOutput.close();
            // TODO: For some reason, the last few files are not removed
            this.removeDir(dir + "/iteration_" + iteration);  // remove the temp dir in which the files of this iteration were stored
        }

        private ArrayList<String> getHeads() {
            ArrayList<String> heads = new ArrayList<>();
            for (int i=0; i<fileDeques.size(); i++){
                int[] p = fileDeques.get(i).peekFirst();
                if (p == null) {
                    heads.add(null);
                } else {
                    heads.add(inv.get(p[0]));
                }
            }
            return heads;
        }

        /** Add the first element in the deque[minIndex] to the output block.
         * If the block is full, save it to the output file and clear the block.
         * If the deque is empty, load the next elements in the file given in minIndex.
         */
        private void extractMin(int minIndex) throws IOException {
            int[] minPair = fileDeques.get(minIndex).pollFirst();
            this.outputBlock[this.outputPtr] = minPair[0];
            this.outputBlock[this.outputPtr + 1] = minPair[1];
            this.outputPtr += 2;
            if (this.outputPtr == pairsInBlock * 2){
                this.saveOutputBlock();
                this.clearOutputBlock();
            }
            if (fileDeques.get(minIndex).isEmpty() && fileReaders.get(minIndex) != null){
                this.loadData(minIndex, numPairsInDeque);
            }
        }

        /** Return the index of the minimal element of the first elements (smallest elements) in all deques. */
        private int getMin(){
            int minIndex = -1;
            for (int i=0; i<fileDeques.size(); i++){
                if (fileDeques.get(i).size() > 0){
                    if (minIndex == -1) {
                        minIndex = i;
                    } else if (cmp.compare(fileDeques.get(minIndex).getFirst()[0], fileDeques.get(i).getFirst()[0]) > 0){
                        minIndex = i;
                    }
                }
            }
            return minIndex;
        }

        private void loadAll() throws IOException {
            for (int i = 0; i < this.fileReaders.size(); i++){
                this.loadData(i, this.numPairsInDeque);
            }
        }

        /** Load numbBlocks from the file given by index i to the matching deque*/
        private void loadData(int i, int numPairs) throws IOException {
//            // TODO: Code for reading -blocks- (not pairs). Remove if not used
//            int blocksRead = 0;
//            int pairsRead = 0;
//            while (blocksRead < numBlocks) {
//                int[] pair = new int[2];
//                try {
//                    pair[0] = fileReaders.get(i).readInt();
//                    pair[1] = fileReaders.get(i).readInt();
//                } catch (EOFException e){
//                    // Reached end of file.
//                    fileReaders.get(i).close();
//                    fileReaders.set(i, null);
//                    break;
//                }
//                fileDeques.get(i).add(pair);
//                pairsRead++;
//                if (pairsRead == pairsInBlock) {
//                    pairsRead = 0;
//                    blocksRead++;
//                }
//            }

            for (int j = 0; j < numPairs; j++) {
                int[] pair = new int[2];
                try {
                    pair[0] = fileReaders.get(i).readInt();
                    pair[1] = fileReaders.get(i).readInt();
                } catch (EOFException e){
                    // Reached end of file.
                    fileReaders.get(i).close();
                    fileReaders.set(i, null);
                    break;
                }
                fileDeques.get(i).add(pair);
            }
        }

        private boolean areAllDequesEmpty(){
            for (Deque<int[]> d: fileDeques){
                if (!d.isEmpty()){
                    return false;
                }
            }
            return true;
        }

        private void clearOutputBlock(){
            outputBlock = new int[pairsInBlock * 2];
            outputPtr = 0;
        }

        private void saveOutputBlock() throws IOException {
            for (int i = 0; i < this.outputPtr; i++){
                this.mergedOutput.writeInt(this.outputBlock[i]);
            }
        }

        private void removeDir(String dir){
            File dirToRemove = new File(dir);
            File[] contents = dirToRemove.listFiles();
            if (contents != null) {
                for (File file : contents) {
                    file.delete();
                }
            }
            dirToRemove.delete();
        }


    }


}
