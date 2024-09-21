import java.math.BigInteger;
import java.util.*;

public class Assembler {
    // Constructor
    public Assembler() {}

    // Filter out comments from instructions
    public String[] handleComments(String[] instructions) {
        return Arrays.stream(instructions)
                .filter(str -> !str.startsWith(";"))
                .map(str -> str.split(";")[0].trim())
                .toArray(String[]::new);
    }

    private String currentLocation = "000000"; // Default location
    ArrayList<String> keys = new ArrayList<>();
    ArrayList<String> values = new ArrayList<>();

    // Assemble instruction based on opcode and params
    public void assembleInstruction(String input, String targetLocation) {
        String[] parts = input.split(" ", 2); // Split instruction by space
        if (parts.length < 2) return; // Skip if format is invalid

        String opcode = parts[0]; // Get opcode
        String params = parts[1]; // Get parameters (registers, addresses, etc.)

        if (opcode.equals("LOC")) {
            handleLocationChange(params, targetLocation);
        } else {
            String instruction = createInstruction(opcode, params, targetLocation);
            if (instruction != null && !instruction.isEmpty()) {
                String octalInstruction = Integer.toOctalString(Integer.parseInt(instruction, 2));
                keys.add(currentLocation);
                values.add(formatWithLeadingZeros(octalInstruction, 6)); // Pad octal to 6 digits
                updateLocation(); // Move to next location
            }
        }
    }

    // Handle changes in location (LOC)
    private void handleLocationChange(String params, String targetLocation) {
        currentLocation = formatWithLeadingZeros(convertDecimalToOctal(params), 6);
        keys.add("      ");
        values.add("      ");
    }

    // Create binary instruction based on opcode and parameters
    private String createInstruction(String opcode, String params, String targetLocation) {
        StringBuilder instructionBuilder = new StringBuilder();
        String[] paramArray = params.split(",");

        // Create binary for opcode
        String opcodeBinary = getOpcodeBinary(opcode);

        switch (opcode) {
            case "LDR", "STR", "LDA", "JZ", "JNE", "SOB", "JGE", "AMR", "SMR":
                instructionBuilder.append(encodeRegister(paramArray[0])) // Encode register
                        .append(encodeIndexRegister(paramArray[1]))      // Encode index register
                        .append(paramArray.length == 4 ? encodeIndirectAddressing(paramArray[3]) : "0") // Optional indirect
                        .append(encodeAddress(paramArray[2]));           // Encode address
                break;
            case "LDX", "STX":
                instructionBuilder.append("00")
                        .append(encodeIndexRegister(paramArray[0]))      // Encode index register
                        .append(paramArray.length == 3 ? encodeIndirectAddressing(paramArray[2]) : "0") // Optional indirect
                        .append(encodeAddress(paramArray[1]));           // Encode address
                break;
            case "JCC", "JMA", "JSR":
                instructionBuilder.append(encodeConditionCode(paramArray[0])) // Encode condition code
                        .append(encodeIndexRegister(paramArray[1]))       // Encode index register
                        .append(paramArray.length == 4 ? encodeIndirectAddressing(paramArray[3]) : "0") // Optional indirect
                        .append(encodeAddress(paramArray[2]));            // Encode address
                break;
            case "RFS":
                instructionBuilder.append(encodeAddress(paramArray[0]));  // Encode address
                break;
            case "AIR", "SIR":
                instructionBuilder.append(encodeRegister(paramArray[0])) // Encode register
                        .append(encodeAddress(paramArray[1]));           // Encode address
                break;
            case "MLT", "DVD", "TRR", "AND", "ORR":
                instructionBuilder.append(encodeRegister(paramArray[0])) // Encode first register
                        .append(encodeRegister(paramArray[1]))           // Encode second register
                        .append("000000");                               // Padding
                break;
            case "NOT":
                instructionBuilder.append(encodeRegister(paramArray[0])) // Encode register
                        .append("000000");                               // Padding
                break;
            case "SRC", "RRC":
                instructionBuilder.append(encodeRegister(paramArray[0])) // Encode register
                        .append(encodeShiftRotate(paramArray[2]))        // Encode shift/rotate
                        .append(encodeShiftRotate(paramArray[1]))        // Encode shift/rotate
                        .append("00")                                    // Padding
                        .append(encodeCount(paramArray[3]));             // Encode count
                break;
            case "IN", "OUT", "CHK":
                instructionBuilder.append(encodeRegister(paramArray[0])) // Encode register
                        .append("000")                                   // Padding
                        .append(encodeDevice(paramArray[1]));            // Encode device
                break;
            case "Data":
                instructionBuilder.append(encodeData(params.equals("End") ? targetLocation : params)); // Encode data
                break;
            default:
                return "000000"; // Unknown opcode
        }

        return formatWithLeadingZeros(opcodeBinary + instructionBuilder.toString(), 16); // Ensure 16-bit output
    }

    // Return opcode binary values for each instruction
    private String getOpcodeBinary(String opcode) {
        switch (opcode) {
            case "LDR": return convertOctalToBinary("01");
            case "STR": return convertOctalToBinary("02");
            case "LDA": return convertOctalToBinary("03");
            case "LDX": return convertOctalToBinary("41");
            case "STX": return convertOctalToBinary("42");
            case "JZ":  return convertOctalToBinary("10");
            case "JNE": return convertOctalToBinary("11");
            case "JCC": return convertOctalToBinary("12");
            case "JMA": return convertOctalToBinary("13");
            case "JSR": return convertOctalToBinary("14");
            case "RFS": return convertOctalToBinary("15");
            case "SOB": return convertOctalToBinary("16");
            case "JGE": return convertOctalToBinary("17");
            case "AMR": return convertOctalToBinary("04");
            case "SMR": return convertOctalToBinary("05");
            case "AIR": return convertOctalToBinary("06");
            case "SIR": return convertOctalToBinary("07");
            case "MLT": return convertOctalToBinary("70");
            case "DVD": return convertOctalToBinary("71");
            case "TRR": return convertOctalToBinary("72");
            case "AND": return convertOctalToBinary("73");
            case "ORR": return convertOctalToBinary("74");
            case "NOT": return convertOctalToBinary("75");
            case "SRC": return convertOctalToBinary("31");
            case "RRC": return convertOctalToBinary("32");
            case "IN":  return convertOctalToBinary("61");
            case "OUT": return convertOctalToBinary("62");
            case "CHK": return convertOctalToBinary("63");
            default: return "000000";
        }
    }

    // Update location by incrementing octal value
    private void updateLocation() {
        int location = Integer.parseInt(currentLocation, 8);
        currentLocation = formatWithLeadingZeros(Integer.toOctalString(location + 1), 6); // Increment location
    }

    // Helper methods for encoding registers, addresses, and other components
    private String encodeConditionCode(String cc) { return formatWithLeadingZeros(convertDecimalToBinary(cc), 2); }
    private String encodeRegister(String reg) { return formatWithLeadingZeros(convertDecimalToBinary(reg), 2); }
    private String encodeIndexRegister(String ix) { return formatWithLeadingZeros(convertDecimalToBinary(ix), 2); }
    private String encodeIndirectAddressing(String i) { return i.equals("1") ? "1" : "0"; }
    private String encodeAddress(String addr) { return formatWithLeadingZeros(convertDecimalToBinary(addr), 5); }
    private String encodeShiftRotate(String param) { return formatWithLeadingZeros(convertDecimalToBinary(param), 2); }
    private String encodeCount(String count) { return formatWithLeadingZeros(convertDecimalToBinary(count), 4); }
    private String encodeDevice(String device) { return formatWithLeadingZeros(convertDecimalToBinary(device), 5); }
    private String encodeData(String data) { return formatWithLeadingZeros(convertDecimalToBinary(data), 6); }

    // Conversion methods: Decimal to binary, octal, etc.
    private String convertDecimalToOctal(String decimal) { return Integer.toOctalString(Integer.parseInt(decimal)); }
    private String convertOctalToBinary(String octal) { return new BigInteger(octal, 8).toString(2); }
    private String convertDecimalToBinary(String decimal) { return Integer.toBinaryString(Integer.parseInt(decimal)); }

    // Pad with leading zeros
    private String formatWithLeadingZeros(String value, int length) {
        while (value.length() < length) value = "0" + value;
        return value;
    }

    // Get instruction map keys and values
    public String[] getInstructionMapKeys() {
        return keys.toArray(new String[keys.size()]);
    }
    public String[] getInstructionMapValues() {
        return values.toArray(new String[values.size()]);
    }
}