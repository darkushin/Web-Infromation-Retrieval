package webdata;

import java.util.List;

public class ExternalMergeSort {
    private List<String> invertedTokenDict;
    private String filePrefix;
    int numFiles;
    int blockSize;

    ExternalMergeSort(List<String> invertedTokenDict, int numFiles, String filePrefix, int blockSize){
        this.invertedTokenDict = invertedTokenDict;
        this.numFiles = numFiles;
        this.filePrefix = filePrefix;
        this.blockSize = blockSize;
    }

    /**
     * Merge all files in the given range
     */
    private void mergeFiles(int start, int end){
        // todo: iterate over all files in the range and take every time the smallest element.
        // when a block is full, save it to the new file until done with all files
    }
}
