public class Main {
    public static void main(String[] args) {
        // Instantiate FileHandler to manage file operations
        FileHandler fileHandler = new FileHandler();
        // Instantiate Assembler to handle assembly of instructions
        Assembler assembler = new Assembler();

        // Extract instructions from the input file
        String[] instructions = fileHandler.extractInstructions("test_input.txt");

        // Remove comments from the instructions
        String[] cleanInstructions = assembler.handleComments(instructions);

        // Get the last location from the instructions
        String finalLocation = findLastLocation(cleanInstructions);

        // Assemble each instruction using the Assembler
        for (String line : cleanInstructions) {
            assembler.assembleInstruction(line, finalLocation);
        }

        // Get instruction map keys and values
        String[] keys = assembler.getInstructionMapKeys();
        String[] values = assembler.getInstructionMapValues();

        // Create a load file from the instruction map
        fileHandler.createLoadFile(keys, values, "test_load.txt");

        // Create a listing file
        fileHandler.createListingFile(instructions, keys, values, "test_listing.txt");
    }

    // Find the last location from LOC instructions
    private static String findLastLocation(String[] instructions) {
        String lastLocation = "";
        for (String instruction : instructions) {
            String[] parts = instruction.split(" ");
            if ("LOC".equals(parts[0])) {
                lastLocation = parts[1];
            }
        }
        return lastLocation;
    }
}