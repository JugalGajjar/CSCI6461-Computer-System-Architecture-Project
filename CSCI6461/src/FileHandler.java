import java.io.*;

public class FileHandler {

    // Extract instructions from a file
    public String[] extractInstructions(String filename) {
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            return br.lines().toArray(String[]::new); // Read all lines from file
        } catch (IOException e) {
            e.printStackTrace();
            return new String[0]; // Return empty array in case of error
        }
    }

    // Create a load file with the instruction map
    public void createLoadFile(String[] keys, String[] values, String filename) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            if (keys.length == values.length) {
                // Iterate over keys and values array
                for (int i=0; i < keys.length; i++) {
                    if (keys[i].equals("      ")) {
                        continue;
                    }
                    bw.write(keys[i] + " " + values[i]); // Write each entry to file
                    bw.newLine(); // New line after each entry
                }
            } else {
                System.out.println("Something went wrong!");
            }
        } catch (IOException e) {
            e.printStackTrace(); // Print stack trace in case of error
        }
    }

    // Create a listing file with the instruction map and input
    public void createListingFile(String[] input, String[] keys, String[] values, String outputFile) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile))) {
            if (keys.length == values.length) {
                // Iterate over keys and values array
                for (int i=0; i < keys.length; i++) {
                    bw.write(keys[i] + " " + values[i] + " " + input[i]); // Write each entry to file
                    bw.newLine(); // New line after each entry
                }
            } else {
                System.out.println("Something went wrong!");
            }
        } catch (IOException e) {
            e.printStackTrace(); // Print stack trace in case of error
        }
    }
}