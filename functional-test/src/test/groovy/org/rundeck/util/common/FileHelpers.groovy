package org.rundeck.util.common

import java.nio.file.Files
import java.nio.file.Path

class FileHelpers {
    /**
     * Given a list with expected lines, when we provide an actual list with entries,
     * the method asserts that THE ENTRIES contains ALL LINES.
     *
     * @param lines
     * @param entries
     * @return
     */
    static def assertLinesInsideEntries(List<String> lines, List<String> entries){
        def assertion = true
        lines.each { el -> {
            if( !entries.contains(el) ){
                assertion = false
            }
        }}
        return assertion
    }

    /**
     * Read files, provide list of Strings with each line in file
     *
     * @param filePath
     * @return
     */
    static def readFile(final Path filePath){
        try{
            return Files.readAllLines(filePath)
        }catch(Exception e){
            e.printStackTrace()
        }
    }

    /**
     * Writes a file in filesystem
     *
     * @param fileContent
     * @param file
     * @return
     */
    static def writeFile(String fileContent, final File file){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(fileContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
