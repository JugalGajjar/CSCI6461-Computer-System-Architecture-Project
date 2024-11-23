import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.*;
import java.util.regex.Pattern;

public class Simulator {
    // Memory and Registers
    private static final int MEMORY_SIZE = 2048;  // 2048 words of memory
    private short[] memory = new short[MEMORY_SIZE];  // Changed to short for 16-bit memory

    // Registers
    private String PC = "";  // Program Counter
    private String IR = "";  // Instruction Register
    private String X1 = "", X2 = "", X3 = "";  // Index Registers
    private String R0 = "", R1 = "", R2 = "", R3 = "";  // General-purpose registers

    private String MAR = "";  // Memory Address Register
    private String MBR = "";  // Memory Buffer Register

    // Declare JTextFields and JLabels as instance variables for access across methods
    private JTextField[] gprFields = new JTextField[4];  // GPR TextFields
    private JButton[] gprButtons = new JButton[4];  // GPR Buttons
    private JTextField[] ixrFields = new JTextField[3];  // IXR TextFields
    private JButton[] ixrButtons = new JButton[3];  // IXR Buttons
    private JTextField[] textFields = new JTextField[6]; // PC, MAR, MBR, IR, CC, MFR TextFields
    private JButton[] otherButtons = new JButton[6]; // PC, MAR, MBR, IR, CC, MFR Load Button
    private JTextField[] cacheFields = new JTextField[16]; // Cache TextFields
    private JTextField octalField;  // Octal input field
    private JTextField binaryField; // Binary output field
    private JTextField programFileField; // Program file path field
    private JTextArea inputTextArea;
    private JTextArea outputTextArea;
    int[] cc = new int[4];
    ArrayList<String> sentences = new ArrayList<>();
    String searchWord = "";
    Map<Character, Integer> charToNumMap = new HashMap<>();
    ArrayList<String> loadFile = new ArrayList<>();

    public Simulator() {
        Arrays.fill(memory, (short) 0);  // Clear the memory, set to short

        //Mapping
        for (char ch = 'a'; ch <= 'z'; ch++) {
            charToNumMap.put(ch, ch - 'a');
        }
        for (int i = 0; i <= 9; i++) {
            charToNumMap.put(Integer.toString(i).charAt(0), i+26);
        }
        int counter = 36;
        char[] symbols = {'+','-','*','/','%','=','^','.',',','?','(',')','@','$','!','&','_','#','\'','\"','<','>',':',';','~','{','}','[',']'};
        for (char c : symbols) {
            charToNumMap.put(c,counter);
            counter++;
        }
        charToNumMap.put(' ', 65); // Space is mapped to 30
        charToNumMap.put('\n', 100); // End of sentence marker

        // Create the frame
        JFrame frame = new JFrame("CSCI 6461 Simulator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Create a main panel with padding and spacing
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create a panel for the registers
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Labels for GPR
        JLabel gprLabel = new JLabel("GPR");
        c.gridx = 0;
        c.gridy = 1;
        panel.add(gprLabel, c);

        // GPR Registers
        for (int i = 0; i < 4; i++) {
            c.gridx = 0;
            c.gridy = i + 2;
            panel.add(new JLabel(String.valueOf(i)), c);
            gprFields[i] = new JTextField(17);
            gprFields[i].setPreferredSize(new Dimension(100, 30));
            c.gridx = 1;
            panel.add(gprFields[i], c);
            gprButtons[i] = new JButton("Load");
            gprButtons[i].setPreferredSize(new Dimension(80, 30));
            c.gridx = 2;
            panel.add(gprButtons[i], c);

            int index = i;  // Capture the current index
            gprButtons[i].addActionListener( e -> {
                String bin = binaryField.getText();
                gprFields[index].setText(bin);
            });
        }

        // Labels for IXR
        JLabel ixrLabel = new JLabel("IXR");
        c.gridx = 3;
        c.gridy = 1;
        panel.add(ixrLabel, c);

        // IXR Registers
        for (int i = 0; i < 3; i++) {
            c.gridx = 3;
            c.gridy = i + 2;
            panel.add(new JLabel(String.valueOf(i+1)), c);
            ixrFields[i] = new JTextField(17);
            ixrFields[i].setPreferredSize(new Dimension(100, 30));
            c.gridx = 4;
            panel.add(ixrFields[i], c);
            ixrButtons[i] = new JButton("Load");
            ixrButtons[i].setPreferredSize(new Dimension(80, 30));
            c.gridx = 5;
            panel.add(ixrButtons[i], c);

            int index = i;  // Capture the current index
            ixrButtons[i].addActionListener(e -> {
                String bin = binaryField.getText();
                ixrFields[index].setText(bin);
            });
        }

        // PC, MAR, MBR, IR, CC, MFR Fields
        String[] labels = {"PC", "MAR", "MBR", "IR", "CC", "MFR"};
        for (int i = 0; i < labels.length; i++) {
            c.gridx = 6;
            c.gridy = i+1;
            panel.add(new JLabel(labels[i]), c);
            textFields[i] = new JTextField(17);
            textFields[i].setPreferredSize(new Dimension(100, 30));
            c.gridx = 7;
            panel.add(textFields[i], c);
            otherButtons[i] = new JButton("Load");
            otherButtons[i].setPreferredSize(new Dimension(80, 30));
            c.gridx = 8;
            panel.add(otherButtons[i], c);

            int index = i;  // Capture the current index
            otherButtons[i].addActionListener(e -> {
                String bin = binaryField.getText();
                if (index == 1 || index == 0) {
                    textFields[index].setText(bin.substring(4));
                } else {
                    textFields[index].setText(bin);
                }
            });
        }

        // Label for Cache
        JLabel cacheLabel = new JLabel("CACHE");
        c.gridx = 9;
        c.gridy = 0;
        panel.add(cacheLabel, c);

        // Cache text fields
        for (int i = 0; i < 8; i++) {
            c.gridx = 9;
            c.gridy = i + 1;
            cacheFields[i*2] = new JTextField(17);
            cacheFields[i*2].setPreferredSize(new Dimension(100, 30));
            panel.add(cacheFields[i*2], c);
            c.gridx = 10;
            cacheFields[i*2 + 1] = new JTextField(17);
            cacheFields[i*2 + 1].setPreferredSize(new Dimension(100, 30));
            panel.add(cacheFields[i*2 + 1], c);
        }

        // Printer
        JLabel printer = new JLabel("Console Output");
        c.gridx = 1;
        c.gridy = 7;
        panel.add(printer, c);

        outputTextArea = new JTextArea(2, 16);
        outputTextArea.setLineWrap(true);
        c.gridy = 8;
        c.gridwidth = 7;
        c.gridheight = 2;
        panel.add(outputTextArea, c);
        c.gridwidth = 1;
        c.gridheight = 1;

        // Buttons for "Load", "Store", "Load+", "Store+", "Reset", "Halt", "Run", "Step", "Add", "Subtract", "Multiply", "Divide"
        JPanel buttonPanel1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        String[] buttonLabels1 = {"Load", "Store", "Load+", "Store+", "Reset", "Halt", "Run", "Step", "Add", "Subtract", "Multiply", "Divide"};
        for (String label : buttonLabels1) {
            JButton button = new JButton(label);
            button.setPreferredSize(new Dimension(90, 30));

            switch (label) {
                case "Reset" -> button.addActionListener(e -> resetSimulator());
                case "Halt" -> button.addActionListener(e -> haltSimulator());
                case "Run" -> button.addActionListener(e -> runSimulator());
                case "Step" -> button.addActionListener(e -> stepSimulator());
                case "Load" -> button.addActionListener(e -> loadOperation());
                case "Store" -> button.addActionListener(e -> storeOperation());
                case "Load+" -> button.addActionListener(e -> loadIncrementOperation());
                case "Store+" -> button.addActionListener(e -> storeIncrementOperation());
                case "Add" -> button.addActionListener(e -> addOperation());
                case "Subtract" -> button.addActionListener(e -> subtractOperation());
                case "Multiply" -> button.addActionListener(e -> multiplyOperation());
                case "Divide" -> button.addActionListener(e -> divideOperation());
            }

            buttonPanel1.add(button);
        }

        // Buttons for "Equal", "AND", "OR", "NOT", "Shift", "Rotate", "JZ/JNZ", "JCC", "JMA", "JSR", "JGE", "RFS"
        JPanel buttonPanel2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        String[] buttonLabels2 = {"Equal", "AND", "OR", "NOT", "Shift", "Rotate", "JZ/JNZ", "JCC", "JMA", "JSR", "JGE", "RFS"};
        for (String label : buttonLabels2) {
            JButton button = new JButton(label);
            button.setPreferredSize(new Dimension(90, 30));

            switch (label) {
                case "Equal" -> button.addActionListener(e -> equalOperation());
                case "AND" -> button.addActionListener(e -> andOperation());
                case "OR" -> button.addActionListener(e -> orOperation());
                case "NOT" -> button.addActionListener(e -> notOperation());
                case "Shift" -> button.addActionListener(e -> shiftOperation());
                case "Rotate" -> button.addActionListener(e -> rotateOperation());
                case "JZ/JNZ" -> button.addActionListener(e -> jzjnzOperation());
                case "JCC" -> button.addActionListener(e -> jccOperation());
                case "JMA" -> button.addActionListener(e -> jmaOperation());
                case "JSR" -> button.addActionListener(e -> jsrOperation());
                case "JGE" -> button.addActionListener(e -> jgeOperation());
                case "RFS" -> button.addActionListener(e -> rfsOperation());
            }

            buttonPanel2.add(button);
        }

        // Octal/Binary Fields and Convert Button
        JPanel convertPanel = new JPanel(new GridBagLayout());
        GridBagConstraints cc = new GridBagConstraints();
        cc.insets = new Insets(5, 5, 5, 5);
        cc.fill = GridBagConstraints.HORIZONTAL;

        cc.gridx = 0;
        cc.gridy = 0;
        JButton startProgram2Button = new JButton("Start Program 2");
        startProgram2Button.setPreferredSize(new Dimension(135, 30));
        startProgram2Button.addActionListener(e -> startProgram2());
        convertPanel.add(startProgram2Button, cc);

        cc.gridy = 1;
        JButton nextButton = new JButton("Enter Next Sentence/Word");
        nextButton.setPreferredSize(new Dimension(80, 30));
        nextButton.addActionListener(e -> next());
        cc.gridwidth = 2;
        convertPanel.add(nextButton, cc);
        cc.gridwidth = 1;

        cc.gridx = 1;
        cc.gridy = 0;
        convertPanel.add(new JLabel("Console Input"), cc);

        cc.gridx = 2;
        inputTextArea = new JTextArea(2, 25);
        inputTextArea.setPreferredSize(new Dimension(200, 70));
        inputTextArea.setLineWrap(true);
        cc.gridheight = 2; // Span across two rows
        convertPanel.add(inputTextArea, cc);
        cc.gridheight = 1; // Reset gridheight for subsequent components

        cc.gridx = 3;
        convertPanel.add(new JLabel("Octal"), cc);

        octalField = new JTextField(8);
        octalField.setPreferredSize(new Dimension(100, 30));
        cc.gridx = 4;
        convertPanel.add(octalField, cc);

        cc.gridx = 5;
        convertPanel.add(new JLabel("Binary"), cc);

        binaryField = new JTextField(16);
        binaryField.setPreferredSize(new Dimension(100, 30));
        cc.gridx = 6;
        convertPanel.add(binaryField, cc);

        JButton convertButton = new JButton("To binary");
        convertButton.setPreferredSize(new Dimension(80, 30));
        convertButton.addActionListener(e -> convertOctalToBinary());
        cc.gridx = 7;
        convertPanel.add(convertButton, cc);

        JButton clearCacheButton = new JButton("Clear Cache");
        clearCacheButton.setPreferredSize(new Dimension(132, 30));
        cc.gridx = 8;
        clearCacheButton.addActionListener(e -> clearCacheFunction());
        convertPanel.add(clearCacheButton, cc);

        JButton searchButton = new JButton("Search");
        searchButton.setPreferredSize(new Dimension(80, 30));
        cc.gridx = 9;
        searchButton.addActionListener(e -> search());
        convertPanel.add(searchButton, cc);

        // Program File Loader
        cc.gridx = 3;
        cc.gridy = 1;
        convertPanel.add(new JLabel("Program File"), cc);

        // Elongated Program File text field spanning across three columns
        programFileField = new JTextField(30); // Width of 30 columns
        programFileField.setPreferredSize(new Dimension(300, 30));
        cc.gridx = 4;
        cc.gridwidth = 3; // Span across three columns
        convertPanel.add(programFileField, cc);
        cc.gridwidth = 1; // Reset gridwidth for subsequent components

        JButton fileButton = new JButton("Select File");
        fileButton.setPreferredSize(new Dimension(100, 30));
        cc.gridx = 7;
        convertPanel.add(fileButton, cc);

        JButton convertLoadFileButton = new JButton("Create Load File");
        convertLoadFileButton.setPreferredSize(new Dimension(132, 30));
        cc.gridx = 8;
        convertLoadFileButton.addActionListener(e -> readAndConvertInputFile(""));
        convertPanel.add(convertLoadFileButton, cc);

        JButton iplButton = new JButton("IPL");
        iplButton.setPreferredSize(new Dimension(80, 30));
        cc.gridx = 9;
        iplButton.addActionListener(e -> initializeProgramLoader());
        convertPanel.add(iplButton, cc);

        // Add action listener to the file button
        fileButton.addActionListener(e -> selectProgramFile());

        // Add panels to the main panel
        mainPanel.add(panel);
        mainPanel.add(buttonPanel1);
        mainPanel.add(buttonPanel2);
        mainPanel.add(convertPanel);

        // Add the main panel to the frame and make it visible
        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private void next() {
        if (inputTextArea.getText().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter a valid sentence/word.");
        } else {
            String sentence = inputTextArea.getText();
            if (sentences.size() < 6) {
                if (sentence.endsWith(".")) {
                    sentences.add(sentence+"\n");
                } else {
                    sentences.add(sentence+"."+"\n");
                }
            } else if (sentences.size() == 6) {
                searchWord = sentence.split(" ")[0];
            } else {
                JOptionPane.showMessageDialog(null, "You cannot enter more data.");
            }
            inputTextArea.setText("");
        }
    }

    private void startProgram2() {
        JOptionPane.showMessageDialog(null, "Enter the first sentence in the console input.");
    }

    private void clearCacheFunction() {
    }

    private void subtractOperation() {
        String params = inputTextArea.getText();
        if (params.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter parameters for SUB in console input.\nSyntax: R,IX,Address,I");
        } else {
            String[] parts = params.split(",");
            for (int i = 0; i < parts.length; i++) {
                parts[i] = parts[i].trim();
            }
            int r = Integer.parseInt(parts[0]);
            if (r < 0 || r > 3) {
                JOptionPane.showMessageDialog(null, "Enter a valid register number.");
            } else {
                if (parts.length > 2 && parts.length <= 4) {
                    int ix = Integer.parseInt(parts[1]);
                    if (ix < 0 || ix > 2) {
                        JOptionPane.showMessageDialog(null, "Enter a valid index register number.");
                    } else {
                        int EA;
                        if (parts.length == 3) {
                            String addr = Integer.toBinaryString(Integer.parseInt(parts[2]));
                            EA = computeEffectiveAddress(Integer.toBinaryString(ix), addr, "0");
                        } else {
                            String addr = Integer.toBinaryString(Integer.parseInt(parts[2]));
                            String i = Integer.toBinaryString(Integer.parseInt(parts[3]));
                            EA = computeEffectiveAddress(String.valueOf(ix), addr, i);
                        }
                        gprFields[r].setText(Integer.toBinaryString(Integer.parseInt(gprFields[r].getText(), 2) - memory[EA]));
                    }
                } else if (parts.length == 2) {
                    int immed = Integer.parseInt(parts[1]);
                    gprFields[r].setText(Integer.toBinaryString(Integer.parseInt(gprFields[r].getText(), 2) - immed));
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid parameters.");
                }
            }
        }
    }

    private void addOperation() {
        String params = inputTextArea.getText();
        if (params.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter parameters for ADD in console input.\nSyntax: R,IX,Address,I");
        } else {
            String[] parts = params.split(",");
            for (int i = 0; i < parts.length; i++) {
                parts[i] = parts[i].trim();
            }
            int r = Integer.parseInt(parts[0]);
            if (r < 0 || r > 3) {
                JOptionPane.showMessageDialog(null, "Enter a valid register number.");
            } else {
                if (parts.length > 2 && parts.length <= 4) {
                    int ix = Integer.parseInt(parts[1]);
                    if (ix < 0 || ix > 2) {
                        JOptionPane.showMessageDialog(null, "Enter a valid index register number.");
                    } else {
                        int EA;
                        if (parts.length == 3) {
                            String addr = Integer.toBinaryString(Integer.parseInt(parts[2]));
                            EA = computeEffectiveAddress(Integer.toBinaryString(ix), addr, "0");
                        } else {
                            String addr = Integer.toBinaryString(Integer.parseInt(parts[2]));
                            String i = Integer.toBinaryString(Integer.parseInt(parts[3]));
                            EA = computeEffectiveAddress(String.valueOf(ix), addr, i);
                        }
                        gprFields[r].setText(Integer.toBinaryString(Integer.parseInt(gprFields[r].getText(), 2) + memory[EA]));
                    }
                } else if (parts.length == 2) {
                    int immed = Integer.parseInt(parts[1]);
                    gprFields[r].setText(Integer.toBinaryString(Integer.parseInt(gprFields[r].getText(), 2) + immed));
                } else {
                    JOptionPane.showMessageDialog(null, "Invalid parameters.");
                }
            }
        }
    }

    private void multiplyOperation() {
        String params = inputTextArea.getText().trim();
        if (params.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter parameters for MLT (Multiply) operation.\nSyntax: rx, ry");
            return;
        }

        String[] parts = params.split(",");
        if (parts.length != 2) {
            JOptionPane.showMessageDialog(null, "Invalid parameters for MLT operation. Syntax: rx, ry");
            return;
        }

        try {
            int rx = Integer.parseInt(parts[0].trim());
            int ry = Integer.parseInt(parts[1].trim());

            if (isValidMultiplyRegister(rx) && isValidMultiplyRegister(ry)) {
                int rxValue = Integer.parseInt(gprFields[rx].getText(), 2);
                int ryValue = Integer.parseInt(gprFields[ry].getText(), 2);
                long result = (long) rxValue * ryValue;

                String resultBinary = Long.toBinaryString(result);
                if (resultBinary.length() > 32) {
                    // Overflow occurred
                    JOptionPane.showMessageDialog(null, "Overflow occurred in multiplication.");
                    // Set overflow flag as needed (example: cc[0] = 1;)
                }

                // Store the high order bits in rx and low order bits in rx+1
                String highOrderBits = resultBinary.length() > 16 ? resultBinary.substring(0, resultBinary.length() - 16) : "0";
                String lowOrderBits = resultBinary.length() > 16 ? resultBinary.substring(resultBinary.length() - 16) : resultBinary;

                gprFields[rx].setText(highOrderBits);
                gprFields[rx + 1].setText(lowOrderBits);
            } else {
                JOptionPane.showMessageDialog(null, "Invalid register numbers for multiplication. Only registers 0 and 2 are allowed.");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid parameter format. Ensure register numbers are integers.");
        }
    }

    private void divideOperation() {
        String params = inputTextArea.getText().trim();
        if (params.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter parameters for DVD (Divide) operation.\nSyntax: rx, ry");
            return;
        }

        String[] parts = params.split(",");
        if (parts.length != 2) {
            JOptionPane.showMessageDialog(null, "Invalid parameters for DVD operation. Syntax: rx, ry");
            return;
        }

        try {
            int rx = Integer.parseInt(parts[0].trim());
            int ry = Integer.parseInt(parts[1].trim());

            if (isValidMultiplyRegister(rx) && isValidMultiplyRegister(ry)) {
                int rxValue = Integer.parseInt(gprFields[rx].getText(), 2);
                int ryValue = Integer.parseInt(gprFields[ry].getText(), 2);

                if (ryValue == 0) {
                    JOptionPane.showMessageDialog(null, "Division by zero error.");
                    // Set DIVZERO flag as needed (example: cc[3] = 1;)
                    return;
                }

                int quotient = rxValue / ryValue;
                int remainder = rxValue % ryValue;

                gprFields[rx].setText(Integer.toBinaryString(quotient));
                gprFields[rx + 1].setText(Integer.toBinaryString(remainder));
            } else {
                JOptionPane.showMessageDialog(null, "Invalid register numbers for division. Only registers 0 and 2 are allowed.");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid parameter format. Ensure register numbers are integers.");
        }
    }

    private void search() {
        readAndConvertInputFile("program2.txt");
        for (int i = 0; i < 6; i++) {
            String[] words = sentences.get(i).split(" ");
            for (int j = 0; j < words.length; j++) {
                if (words[j].toLowerCase().equals(searchWord.toLowerCase())) {
                    outputTextArea.setText("Sentence: " + Integer.toString(i+1) + ", Word: " + Integer.toString(j+1));
                    break;
                }
            }
        }
    }

    // Helper method to validate register numbers for multiply and divide operations
    private boolean isValidMultiplyRegister(int register) {
        return register == 0 || register == 2;
    }

    private void equalOperation() {
        String params = inputTextArea.getText().trim();
        if (params.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter parameters for TRR (Test Equality) operation.\nSyntax: rx, ry");
            return;
        }

        String[] parts = params.split(",");
        if (parts.length != 2) {
            JOptionPane.showMessageDialog(null, "Invalid parameters for TRR operation. Syntax: rx, ry");
            return;
        }

        try {
            int rx = Integer.parseInt(parts[0].trim());
            int ry = Integer.parseInt(parts[1].trim());

            if (isValidRegister(rx) && isValidRegister(ry)) {
                int rxValue = Integer.parseInt(gprFields[rx].getText(), 2);
                int ryValue = Integer.parseInt(gprFields[ry].getText(), 2);

                // Set the condition code based on equality
                if (rxValue == ryValue) {
                    cc[3] = 1;  // Registers are equal
                    JOptionPane.showMessageDialog(null, "Registers are equal. cc[3] set to 1.");
                } else {
                    cc[3] = 0;  // Registers are not equal
                    JOptionPane.showMessageDialog(null, "Registers are not equal. cc[3] set to 0.");
                }
            } else {
                JOptionPane.showMessageDialog(null, "Invalid register numbers. Valid range: 0 to 3.");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid parameter format. Ensure register numbers are integers.");
        }
    }

    private void andOperation() {
        String params = inputTextArea.getText().trim();
        if (params.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter parameters for AND operation.\nSyntax: rx, ry");
            return;
        }

        String[] parts = params.split(",");
        if (parts.length != 2) {
            JOptionPane.showMessageDialog(null, "Invalid parameters for AND operation. Syntax: rx, ry");
            return;
        }

        try {
            int rx = Integer.parseInt(parts[0].trim());
            int ry = Integer.parseInt(parts[1].trim());

            if (isValidRegister(rx) && isValidRegister(ry)) {
                int rxValue = Integer.parseInt(gprFields[rx].getText(), 2);
                int ryValue = Integer.parseInt(gprFields[ry].getText(), 2);
                int result = rxValue & ryValue;
                gprFields[rx].setText(Integer.toBinaryString(result));
            } else {
                JOptionPane.showMessageDialog(null, "Invalid register numbers. Valid range: 0 to 3.");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid parameter format. Ensure register numbers are integers.");
        }
    }

    private void orOperation() {
        String params = inputTextArea.getText().trim();
        if (params.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter parameters for OR operation.\nSyntax: rx, ry");
            return;
        }

        String[] parts = params.split(",");
        if (parts.length != 2) {
            JOptionPane.showMessageDialog(null, "Invalid parameters for OR operation. Syntax: rx, ry");
            return;
        }

        try {
            int rx = Integer.parseInt(parts[0].trim());
            int ry = Integer.parseInt(parts[1].trim());

            if (isValidRegister(rx) && isValidRegister(ry)) {
                int rxValue = Integer.parseInt(gprFields[rx].getText(), 2);
                int ryValue = Integer.parseInt(gprFields[ry].getText(), 2);
                int result = rxValue | ryValue;
                gprFields[rx].setText(Integer.toBinaryString(result));
            } else {
                JOptionPane.showMessageDialog(null, "Invalid register numbers. Valid range: 0 to 3.");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid parameter format. Ensure register numbers are integers.");
        }
    }

    private void notOperation() {
        String params = inputTextArea.getText().trim();
        if (params.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter parameter for NOT operation.\nSyntax: rx");
            return;
        }

        String[] parts = params.split(",");
        if (parts.length != 1) {
            JOptionPane.showMessageDialog(null, "Invalid parameter for NOT operation. Syntax: rx");
            return;
        }

        try {
            int rx = Integer.parseInt(parts[0].trim());

            if (isValidRegister(rx)) {
                int rxValue = Integer.parseInt(gprFields[rx].getText(), 2);
                int result = ~rxValue;  // Logical NOT operation
                gprFields[rx].setText(Integer.toBinaryString(result & 0xFFFF)); // Limit to 16-bit result if necessary
            } else {
                JOptionPane.showMessageDialog(null, "Invalid register number. Valid range: 0 to 3.");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid parameter format. Ensure register number is an integer.");
        }
    }

    // Helper method to validate register numbers
    private boolean isValidRegister(int register) {
        return register >= 0 && register <= 3;
    }

    // JSR (Jump and Save Return Address) Operation
    private void jsrOperation() {
        String params = inputTextArea.getText().trim();
        if (params.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter parameters for JSR operation.\nSyntax: x, address[, I]");
            return;
        }

        String[] parts = params.split(",");
        if (parts.length < 2 || parts.length > 3) {
            JOptionPane.showMessageDialog(null, "Invalid parameters for JSR operation. Syntax: x, address[, I]");
            return;
        }

        try {
            int x = Integer.parseInt(parts[0].trim());
            int address = Integer.parseInt(parts[1].trim());
            int i = (parts.length == 3) ? Integer.parseInt(parts[2].trim()) : 0;

            int effectiveAddress = computeEffectiveAddress(Integer.toBinaryString(x), Integer.toBinaryString(address), Integer.toBinaryString(i));

            // Save return address
            gprFields[3].setText(Integer.toBinaryString(Integer.parseInt(PC,2) + 1));
            // Jump to effective address
            PC = Integer.toBinaryString(effectiveAddress);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid parameter format. Ensure x and address are integers.");
        }
    }

    // JMA (Unconditional Jump to Address) Operation
    private void jmaOperation() {
        String params = inputTextArea.getText().trim();
        if (params.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter parameters for JMA operation.\nSyntax: x, address[, I]");
            return;
        }

        String[] parts = params.split(",");
        if (parts.length < 2 || parts.length > 3) {
            JOptionPane.showMessageDialog(null, "Invalid parameters for JMA operation. Syntax: x, address[, I]");
            return;
        }

        try {
            int x = Integer.parseInt(parts[0].trim());
            int address = Integer.parseInt(parts[1].trim());
            int i = (parts.length == 3) ? Integer.parseInt(parts[2].trim()) : 0;

            int effectiveAddress = computeEffectiveAddress(Integer.toBinaryString(x), Integer.toBinaryString(address), Integer.toBinaryString(i));

            // Unconditional jump to effective address
            PC = Integer.toBinaryString(effectiveAddress);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid parameter format. Ensure x and address are integers.");
        }
    }

    // JGE (Jump Greater Than or Equal) Operation
    private void jgeOperation() {
        String params = inputTextArea.getText().trim();
        if (params.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter parameters for JGE operation.\nSyntax: r, x, address[, I]");
            return;
        }

        String[] parts = params.split(",");
        if (parts.length < 3 || parts.length > 4) {
            JOptionPane.showMessageDialog(null, "Invalid parameters for JGE operation. Syntax: r, x, address[, I]");
            return;
        }

        try {
            int r = Integer.parseInt(parts[0].trim());
            int x = Integer.parseInt(parts[1].trim());
            int address = Integer.parseInt(parts[2].trim());
            int i = (parts.length == 4) ? Integer.parseInt(parts[3].trim()) : 0;

            int effectiveAddress = computeEffectiveAddress(Integer.toBinaryString(x), Integer.toBinaryString(address), Integer.toBinaryString(i));

            int regValue = Integer.parseInt(gprFields[r].getText(), 2);
            if (regValue >= 0) {
                PC = Integer.toBinaryString(effectiveAddress);
            } else {
                PC = Integer.toBinaryString(Integer.parseInt(PC, 2) + 1);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid parameter format. Ensure r, x, and address are integers.");
        }
    }

    // RFS (Return From Subroutine with Return Code) Operation
    private void rfsOperation() {
        String params = inputTextArea.getText().trim();
        if (params.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter parameters for RFS operation.\nSyntax: Immed");
            return;
        }

        try {
            int immed = Integer.parseInt(params.trim());

            // Set R0 to the immediate return code
            gprFields[0].setText(Integer.toBinaryString(immed));
            // Restore PC from R3
            PC = gprFields[3].getText();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid parameter format. Ensure Immed is an integer.");
        }
    }

    // JCC (Jump If Condition Code) Operation
    private void jccOperation() {
        String params = inputTextArea.getText().trim();
        if (params.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter parameters for JCC operation.\nSyntax: cc, x, address[, I]");
            return;
        }

        String[] parts = params.split(",");
        if (parts.length < 3 || parts.length > 4) {
            JOptionPane.showMessageDialog(null, "Invalid parameters for JCC operation. Syntax: cc, x, address[, I]");
            return;
        }

        try {
            int ccIndex = Integer.parseInt(parts[0].trim());
            int x = Integer.parseInt(parts[1].trim());
            int address = Integer.parseInt(parts[2].trim());
            int i = (parts.length == 3) ? Integer.parseInt(parts[3].trim()) : 0;

            int effectiveAddress = computeEffectiveAddress(Integer.toBinaryString(x), Integer.toBinaryString(address), Integer.toBinaryString(i));

            // Check the condition code
            if (cc[ccIndex] == 1) {
                PC = Integer.toBinaryString(effectiveAddress);
            } else {
                PC = Integer.toBinaryString(Integer.parseInt(PC, 2) + 1);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid parameter format. Ensure cc, x, and address are integers.");
        }
    }

    // JZJNZ (Jump If Zero / Jump If Not Equal) Operation
    private void jzjnzOperation() {
        String params = inputTextArea.getText().trim();
        if (params.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter parameters for JZ/JNZ operation.\nSyntax: r, x, address[, I]");
            return;
        }

        String[] parts = params.split(",");
        if (parts.length < 3 || parts.length > 4) {
            JOptionPane.showMessageDialog(null, "Invalid parameters for JZ/JNZ operation. Syntax: r, x, address[, I]");
            return;
        }

        try {
            int r = Integer.parseInt(parts[0].trim());
            int x = Integer.parseInt(parts[1].trim());
            int address = Integer.parseInt(parts[2].trim());
            int i = (parts.length == 4) ? Integer.parseInt(parts[3].trim()) : 0;

            int effectiveAddress = computeEffectiveAddress(Integer.toBinaryString(x), Integer.toBinaryString(address), Integer.toBinaryString(i));

            int regValue = Integer.parseInt(gprFields[r].getText(), 2);

            if (regValue == 0) {  // JZ operation
                PC = Integer.toBinaryString(effectiveAddress);
            } else if (regValue != 0) {  // JNZ operation (treated as negative jump)
                PC = Integer.toBinaryString(-1*effectiveAddress);
            } else {
                PC = Integer.toBinaryString(Integer.parseInt(PC, 2) + 1);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid parameter format. Ensure r, x, and address are integers.");
        }
    }

    private void shiftOperation() {
        String params = inputTextArea.getText().trim();
        if (params.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter parameters for SRC (Shift) operation.\nSyntax: r, count, L/R, A/L");
            return;
        }

        String[] parts = params.split(",");
        if (parts.length != 4) {
            JOptionPane.showMessageDialog(null, "Invalid parameters for SRC operation. Syntax: r, count, L/R, A/L");
            return;
        }

        try {
            int r = Integer.parseInt(parts[0].trim());
            int count = Integer.parseInt(parts[1].trim());
            int lr = Integer.parseInt(parts[2].trim());  // Left/Right
            int al = Integer.parseInt(parts[3].trim());  // Arithmetic/Logical

            if (isValidRegister(r) && count >= 0 && count <= 15) {
                int regValue = Integer.parseInt(gprFields[r].getText(), 2);

                if (count == 0) return;  // No shift if count is zero

                if (lr == 1) {  // Left shift
                    if (al == 1) {  // Logical left shift
                        regValue = regValue << count;
                    } else {  // Arithmetic left shift is the same as logical left shift in most cases
                        regValue = regValue << count;
                    }
                } else {  // Right shift
                    if (al == 1) {  // Logical right shift
                        regValue = regValue >>> count;  // Unsigned shift
                    } else {  // Arithmetic right shift
                        regValue = regValue >> count;   // Signed shift to preserve sign bit
                    }
                }

                // Store the result back in the register field, limited to 16 bits
                gprFields[r].setText(Integer.toBinaryString(regValue & 0xFFFF));
            } else {
                JOptionPane.showMessageDialog(null, "Invalid register number or count. Register range: 0 to 3, Count range: 0 to 15.");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid parameter format. Ensure register numbers and count are integers.");
        }
    }

    private void rotateOperation() {
        String params = inputTextArea.getText().trim();
        if (params.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter parameters for RRC (Rotate) operation.\nSyntax: r, count, L/R, A/L");
            return;
        }

        String[] parts = params.split(",");
        if (parts.length != 4) {
            JOptionPane.showMessageDialog(null, "Invalid parameters for RRC operation. Syntax: r, count, L/R, A/L");
            return;
        }

        try {
            int r = Integer.parseInt(parts[0].trim());
            int count = Integer.parseInt(parts[1].trim());
            int lr = Integer.parseInt(parts[2].trim());  // Left/Right
            int al = Integer.parseInt(parts[3].trim());  // Arithmetic/Logical

            if (isValidRegister(r) && count >= 0 && count <= 15) {
                int regValue = Integer.parseInt(gprFields[r].getText(), 2);

                if (count == 0) return;  // No rotation if count is zero

                // Mask to ensure 16-bit rotation
                regValue &= 0xFFFF;

                if (lr == 1) {  // Rotate left
                    for (int i = 0; i < count; i++) {
                        int msb = (regValue >> 15) & 1;  // Get the most significant bit
                        regValue = ((regValue << 1) & 0xFFFF) | msb;  // Rotate and place msb at the lsb position
                    }
                } else {  // Rotate right
                    for (int i = 0; i < count; i++) {
                        int lsb = regValue & 1;  // Get the least significant bit
                        regValue = (regValue >>> 1) | (lsb << 15);  // Rotate and place lsb at the msb position
                    }
                }

                // Store the result back in the register field, limited to 16 bits
                gprFields[r].setText(Integer.toBinaryString(regValue));
            } else {
                JOptionPane.showMessageDialog(null, "Invalid register number or count. Register range: 0 to 3, Count range: 0 to 15.");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid parameter format. Ensure register numbers and count are integers.");
        }
    }

    private void readAndConvertInputFile(String name) {
        // Instantiate FileHandler to manage file operations
        FileHandler fileHandler = new FileHandler();
        // Instantiate Assembler to handle assembly of instructions
        Assembler assembler = new Assembler();

        // Extract instructions from the input file
        String[] instructions = new String[500];
        if (name.isEmpty()) {
            instructions = fileHandler.extractInstructions(programFileField.getText());
        } else {
            instructions = fileHandler.extractInstructions(name);
        }

        // Remove comments from the instructions
        String[] cleanInstructions = assembler.handleComments(instructions);

        boolean hasAlphabets = Pattern.compile("[a-zA-Z]").matcher(cleanInstructions[0]).find();

        if (hasAlphabets) {
            if (sentences.size() == 6 && !searchWord.isEmpty()) {
                // Process each sentence
                List<String> instruct = new ArrayList<>();
                for (String sent : sentences) {
                    for (char ch : sent.toCharArray()) {
                        if (charToNumMap.containsKey(ch)) {
                            instruct.add("Data " + charToNumMap.get(ch));
                        }
                    }
                }
                List<String> searchInstruct = new ArrayList<>();
                for (char ch : searchWord.toCharArray()) {
                    if (charToNumMap.containsKey(ch)) {
                        searchInstruct.add("Data " + charToNumMap.get(ch));
                    }
                }
                // Replace the placeholder with the instructions
                List<String> resultList = new ArrayList<>();
                for (String str : cleanInstructions) {
                    if (str.contains("<TEXT>")) {
                        resultList.addAll(instruct); // Add all instructions where <TEXT> is found
                    } else if (str.contains("<SEARCH>")) {
                        resultList.addAll(searchInstruct); // Add all instructions where <SEARCH> is found
                    } else {
                        resultList.add(str);
                    }
                }
                // Convert the result back to an array
                cleanInstructions = resultList.toArray(new String[0]);

            }

            // Get the last location from the instructions
            String finalLocation = findLastLocation(cleanInstructions);

            // Assemble each instruction using the Assembler
            for (String line : cleanInstructions) {
                assembler.assembleInstruction(line, finalLocation);
            }

            // Get instruction map keys and values
            String[] keys = assembler.getInstructionMapKeys();
            String[] values = assembler.getInstructionMapValues();

            // Create load file
            fileHandler.createLoadFile(keys, values, "load.txt");
            JOptionPane.showMessageDialog(null, "Load file saved in the current directory.");
        } else {
            JOptionPane.showMessageDialog(null, "Chosen file is already a load file.");
        }
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

    private void storeIncrementOperation() {
        storeOperation();
        int mar_incr = Integer.parseInt(textFields[1].getText(), 2) + 1;
        String binaryString = Integer.toBinaryString(mar_incr);
        textFields[1].setText(binaryString.length() < 12 ? "0".repeat(12 - binaryString.length()) + binaryString : binaryString);
    }

    private void loadIncrementOperation() {
        loadOperation();
        int mar_incr = Integer.parseInt(textFields[1].getText(), 2) + 1;
        String binaryString = Integer.toBinaryString(mar_incr);
        textFields[1].setText(binaryString.length() < 12 ? "0".repeat(12 - binaryString.length()) + binaryString : binaryString);
    }

    private void storeOperation() {
        memory[Integer.parseInt(textFields[1].getText(), 2)] = (short) Integer.parseInt(textFields[2].getText(), 2);
    }

    private void loadOperation() {
        String binaryString = Integer.toBinaryString(memory[Integer.parseInt(textFields[1].getText(), 2)]);
        textFields[2].setText(binaryString.length() < 16 ? "0".repeat(16 - binaryString.length()) + binaryString : binaryString);
    }

    private void resetSimulator() {
        System.out.println("Resetting simulator...");
        Arrays.fill(memory, (short) 0);  // Clear the memory, set to short
        PC = MAR = MBR = IR = X1 = X2 = X3 = R0 = R1 = R2 = R3 = "";  // Reset registers

        for (JTextField field : gprFields) {
            field.setText("");
        }
        for (JTextField field : ixrFields) {
            field.setText("");
        }
        for (JTextField field : textFields) {
            field.setText("");
        }
        octalField.setText("");
        binaryField.setText("");
        programFileField.setText("");
        inputTextArea.setText("");
        outputTextArea.setText("");
        JOptionPane.showMessageDialog(null, "Simulator reset.");
    }

    private void haltSimulator() {
        System.out.println("Halting simulator...");
        Arrays.fill(memory, (short) 0);  // Clear the memory, set to short
        MAR = MBR = IR = X1 = X2 = X3 = R0 = R1 = R2 = R3 = "";  // Reset registers

        for (JTextField field : gprFields) {
            field.setText("");
        }
        for (JTextField field : ixrFields) {
            field.setText("");
        }
        for (JTextField field : textFields) {
            field.setText("");
        }

        PC = "000000000110";
        textFields[0].setText(PC);
        octalField.setText("");
        binaryField.setText("");
        programFileField.setText("");
        JOptionPane.showMessageDialog(null, "Simulator Halted.");
    }

    private void runSimulator() {
        System.out.println("Running simulator...");
        JOptionPane.showMessageDialog(null, "Simulator Starting...");
        for (String content : loadFile) {
            String[] parts = content.split(" ");
            String addr = octalToBinary(parts[0]);  // Address part
            String instr = octalToBinary(parts[1]); // Instruction part

            // Update Program Counter (PC)
            PC = addr;
            textFields[0].setText(PC);  // Update PC in the GUI

            // Fetch the instruction and update Instruction Register (IR)
            IR = instr;
            textFields[3].setText(IR);  // Update IR in the GUI

            // Decode the instruction
            String opcode = instr.substring(0, 6);  // First 6 bits for opcode
            String reg = instr.substring(6, 8);     // Next 2 bits for register
            String ix = instr.substring(8, 10);     // Next 2 bits for index register
            String indirect = instr.substring(10, 11);  // 1 bit for indirect addressing
            String address = instr.substring(11);   // Last 5 bits for address or immediate data

            if (Integer.parseInt(instr, 2) == Integer.parseInt(loadFile.get(loadFile.size()-1).split(" ")[0], 8)) {
                // Address in `parts[0]` is the memory location
                int addr_int = binaryToDecimal(addr);  // Convert binary address to decimal index
                // The `address` part in this case is the actual data to store (interpreted as immediate data)
                int data = Integer.parseInt(instr, 2);

                // Store data in memory at the location specified by `addr_int`
                memory[addr_int] = (short) data;

                // Update MAR and MBR to reflect this data operation
                MAR = addr;  // The memory address where data is stored
                MBR = String.valueOf(data);  // The data stored in memory
                textFields[1].setText(MAR);  // Update MAR in the GUI
                textFields[2].setText(MBR);  // Update MBR in the GUI
            }
            // Handle Opcode `000000`: Add Data to the Current Location
            else if (opcode.equals("000000")) {
                // Address in `parts[0]` is the memory location
                int addr_int = Integer.parseInt(addr, 2);  // Convert binary address to decimal index
                // The `address` part in this case is the actual data to store (interpreted as immediate data)
                int data = Integer.parseInt(address, 2);

                // Store data in memory at the location specified by `addr_int`
                memory[addr_int] = (short) data;

                // Update MAR and MBR to reflect this data operation
                MAR = addr;  // The memory address where data is stored
                MBR = String.valueOf(data);  // The data stored in memory
                textFields[1].setText(MAR);  // Update MAR in the GUI
                textFields[2].setText(MBR);  // Update MBR in the GUI
            }
            // Other opcodes (load/store, arithmetic, etc.)
            else if (opcode.startsWith("000") || opcode.equals("100001") || opcode.equals("100010")) {
                executeLoadStoreInstructions(opcode, reg, ix, indirect, address);
            } else if (opcode.startsWith("001")) {
                executeTransferInstructions(opcode, reg, ix, indirect, address);
            } else if (opcode.startsWith("01")) {
                executeArithmeticInstructions(opcode, reg, ix, indirect, address);
            } else if (opcode.startsWith("110")) {
                executeShiftRotateInstructions(opcode, reg, address.substring(0, 4), address.substring(4, 5), indirect);
            } else if (opcode.startsWith("111")) {
                executeIOInstructions(opcode, reg, address);
            }

            // Move to the next instruction in the program by incrementing the Program Counter (PC)
            PC = String.valueOf(Long.parseLong(PC) + 1);
            textFields[0].setText(PC);  // Update PC field in the GUI
        }

        JOptionPane.showMessageDialog(null, "Simulation Finished...");
    }

    private void executeShiftRotateInstructions(String opcode, String reg, String count, String direction, String type) {
        int regNum = binaryToDecimal(reg);
        int shiftCount = binaryToDecimal(count);
        int value = Integer.parseInt(gprFields[regNum].getText());

        if (opcode.equals("110001")) {  // SRC: Shift Register by Count
            if (direction.equals("1")) {  // Left shift
                value <<= shiftCount;
            } else {  // Right shift
                value >>= shiftCount;
            }
            gprFields[regNum].setText(String.valueOf(value));
        }
    }

    private void executeIOInstructions(String opcode, String reg, String devid) {
        int regNum = binaryToDecimal(reg);

        switch (opcode) {
            case "111101":  // IN: Input from Device to Register
                String input = JOptionPane.showInputDialog("Enter input for device " + devid + ":");
                gprFields[regNum].setText(input);
                break;
            case "111110":  // OUT: Output to Device from Register
                JOptionPane.showMessageDialog(null, "Output to device " + devid + ": " + gprFields[regNum].getText());
                break;
        }
    }

    int line_num = 0;
    private void stepSimulator() {
        System.out.println("Stepping through the simulator...");
        int  upper_bound = loadFile.size();
        if (!programFileField.getText().isEmpty()) {
            if (line_num < upper_bound) {
                String content = loadFile.get(line_num);
                String[] parts = content.split(" ");
                String addr = octalToBinary(parts[0]);  // Address part
                String instr = octalToBinary(parts[1]); // Instruction part
                System.out.println(addr+" "+instr);

                // Update Program Counter (PC)
                PC = addr;
                textFields[0].setText(PC.length() < 12 ? "0".repeat(12 - PC.length()) + PC : PC);  // Update PC in the GUI

                // Fetch the instruction and update Instruction Register (IR)
                IR = instr;
                textFields[3].setText(IR);  // Update IR in the GUI

                // Decode the instruction
                String opcode = instr.substring(0, 6);  // First 6 bits for opcode
                String reg = instr.substring(6, 8);     // Next 2 bits for register
                String ix = instr.substring(8, 10);     // Next 2 bits for index register
                String indirect = instr.substring(10, 11);  // 1 bit for indirect addressing
                String address = instr.substring(11);   // Last 5 bits for address or immediate data

                if (Integer.parseInt(instr, 2) == Integer.parseInt(loadFile.get(loadFile.size()-1).split(" ")[0], 8)) {
                    // Address in `parts[0]` is the memory location
                    int addr_int = binaryToDecimal(addr);  // Convert binary address to decimal index
                    // The `address` part in this case is the actual data to store (interpreted as immediate data)
                    int data = Integer.parseInt(instr, 2);

                    // Store data in memory at the location specified by `addr_int`
                    memory[addr_int] = (short) data;

                    // Update MAR and MBR to reflect this data operation
                    MAR = addr;  // The memory address where data is stored
                    MBR = String.valueOf(data);  // The data stored in memory
                    textFields[1].setText(MAR.length() < 12 ? "0".repeat(12 - MAR.length()) + MAR : MAR);  // Update MAR in the GUI
                    textFields[2].setText(MBR.length() < 16 ? "0".repeat(16 - MBR.length()) + MBR : MBR);  // Update MBR in the GUI
                }
                // Handle Opcode `000000`: Add Data to the Current Location
                else if (opcode.equals("000000")) {
                    // Address in `parts[0]` is the memory location
                    int addr_int = Integer.parseInt(addr, 2);  // Convert binary address to decimal index
                    // The `address` part in this case is the actual data to store (interpreted as immediate data)
                    int data = Integer.parseInt(address, 2);

                    // Store data in memory at the location specified by `addr_int`
                    memory[addr_int] = (short) data;

                    // Update MAR and MBR to reflect this data operation
                    MAR = addr;  // The memory address where data is stored
                    MBR = String.valueOf(data);  // The data stored in memory
                    textFields[1].setText(MAR);  // Update MAR in the GUI
                    textFields[2].setText(MBR);  // Update MBR in the GUI
                }
                // Other opcodes (load/store, arithmetic, etc.)
                else if (opcode.startsWith("000") || opcode.equals("100001") || opcode.equals("100010")) {
                    executeLoadStoreInstructions(opcode, reg, ix, indirect, address);
                } else if (opcode.startsWith("001")) {
                    executeTransferInstructions(opcode, reg, ix, indirect, address);
                } else if (opcode.startsWith("01")) {
                    executeArithmeticInstructions(opcode, reg, ix, indirect, address);
                } else if (opcode.startsWith("110")) {
                    executeShiftRotateInstructions(opcode, reg, address.substring(0, 4), address.substring(4, 5), indirect);
                } else if (opcode.startsWith("111")) {
                    executeIOInstructions(opcode, reg, address);
                }

                // Move to the next instruction in the program by incrementing the Program Counter (PC)
                PC = String.valueOf(Long.parseLong(PC) + 1);
                textFields[0].setText(PC);  // Update PC field in the GUI
                line_num++;
            }

            if (line_num >= upper_bound) {
                JOptionPane.showMessageDialog(null, "All Instructions Executed...");
            }
        }

        JOptionPane.showMessageDialog(null, "Step Execution Finished...");
    }

    private void executeLoadStoreInstructions(String opcode, String reg, String ix, String indirect, String address) {
        int EA = computeEffectiveAddress(ix, address, indirect);  // Get effective address (EA)

        // Update MAR and MBR for every memory access
        MAR = String.valueOf(EA);  // Effective Address (EA) is placed in MAR
        MBR = String.valueOf(memory[EA]);  // Content of memory[EA] is placed in MBR
        textFields[1].setText(MAR);  // Update MAR in the GUI
        textFields[2].setText(MBR);  // Update MBR in the GUI

        switch (opcode) {
            case "000001":  // LDR: Load Register From Memory
                int regNum = binaryToDecimal(reg);
                gprFields[regNum].setText(String.valueOf(memory[EA]));  // Load memory content into the register
                break;
            case "000010":  // STR: Store Register To Memory
                regNum = binaryToDecimal(reg);
                memory[EA] = (short) Integer.parseInt(gprFields[regNum].getText());
                break;
            case "000011":  // LDA: Load Register with Address
                regNum = binaryToDecimal(reg);
                gprFields[regNum].setText(String.valueOf(EA));  // Store address directly in the register
                break;
            case "100001":  // LDX: Load Index Register from Memory
                int ixNum = binaryToDecimal(ix);
                ixrFields[ixNum - 1].setText(String.valueOf(memory[EA]));  // Load memory content into index register
                break;
            case "100010":  // STX: Store Index Register to Memory
                ixNum = binaryToDecimal(ix);
                memory[EA] = (short) Integer.parseInt(ixrFields[ixNum - 1].getText());  // Store index register value in memory
                break;
        }
    }

    private void executeArithmeticInstructions(String opcode, String reg, String ix, String indirect, String address) {
        int EA = computeEffectiveAddress(ix, address, indirect);  // Get effective address
        int regNum = binaryToDecimal(reg);

        // Update MAR and MBR for arithmetic instructions
        MAR = String.valueOf(EA);  // Address involved in the operation
        MBR = String.valueOf(memory[EA]);  // Content of memory[EA]
        textFields[1].setText(MAR);  // Update MAR in the GUI
        textFields[2].setText(MBR);  // Update MBR in the GUI

        switch (opcode) {
            case "000100":  // AMR: Add Memory to Register
                gprFields[regNum].setText(String.valueOf(Integer.parseInt(gprFields[regNum].getText()) + memory[EA]));
                break;
            case "000101":  // SMR: Subtract Memory from Register
                gprFields[regNum].setText(String.valueOf(Integer.parseInt(gprFields[regNum].getText()) - memory[EA]));
                break;
            case "000110":  // AIR: Add Immediate to Register
                gprFields[regNum].setText(String.valueOf(Integer.parseInt(gprFields[regNum].getText()) + Integer.parseInt(address)));
                break;
            case "000111":  // SIR: Subtract Immediate from Register
                gprFields[regNum].setText(String.valueOf(Integer.parseInt(gprFields[regNum].getText()) - Integer.parseInt(address)));
                break;
        }
    }

    private void executeTransferInstructions(String opcode, String reg, String ix, String indirect, String address) {
        int EA = computeEffectiveAddress(ix, address, indirect);
        int regNum = binaryToDecimal(reg);

        // Handle conditional jumps
        switch (opcode) {
            case "001000":  // JZ: Jump if Zero
                if (Integer.parseInt(gprFields[regNum].getText()) == 0) {
                    PC = String.valueOf(EA);
                }
                break;
            case "001001":  // JNE: Jump if Not Equal
                if (Integer.parseInt(gprFields[regNum].getText()) != 0) {
                    PC = String.valueOf(EA);
                }
                break;
            case "001011":  // JMA: Unconditional Jump
                PC = String.valueOf(EA);
                break;
            case "001100":  // JSR: Jump to Subroutine
                gprFields[3].setText(String.valueOf(Integer.parseInt(PC) + 1));  // Store return address in R3
                PC = String.valueOf(EA);
                break;
            case "001101":  // RFS: Return from Subroutine
                gprFields[0].setText(address);  // Store return code in R0
                PC = gprFields[3].getText();  // Return to the saved PC in R3
                break;
        }

        // Update PC after transfer
        textFields[0].setText(PC);  // Update
    }

        private int computeEffectiveAddress(String ix, String address, String indirect) {
        int EA = binaryToDecimal(address);

        // Handle indexing
        if (!ix.equals("00") || !ix.equals("0")) {
            int ixNum = binaryToDecimal(ix);
            EA += Integer.parseInt(ixrFields[ixNum - 1].getText());
        }

        // Handle indirect addressing
        if (indirect.equals("1")) {
            EA = memory[EA];
        }

        return EA;
    }

    private void convertOctalToBinary() {
        String octal = octalField.getText();
        String binary = octalToBinary(octal);
        binaryField.setText(binary);
    }

    private void selectProgramFile() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(null);
        if (option == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            programFileField.setText(selectedFile.getAbsolutePath());
            readFileContents(selectedFile);
        }
    }

    private void readUsingFileName(String name) {
        try (BufferedReader reader = new BufferedReader(new FileReader(name))) {
            String line;
            ArrayList<String> fileContents = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                fileContents.add(line);
            }

            loadFile = fileContents;
            programFileField.setText(name);
            JOptionPane.showMessageDialog(null, "File loaded successfully.");
            // Perform operations with fileContents if needed
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Failed to load file.");
        }
    }

    private void readFileContents(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            ArrayList<String> fileContents = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                fileContents.add(line);
            }

            loadFile = fileContents;
            JOptionPane.showMessageDialog(null, "File loaded successfully.");
            // Perform operations with fileContents if needed
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Failed to load file.");
        }
    }

    private int binaryToDecimal(String binaryString) {
        // Remove leading/trailing whitespace
        binaryString = binaryString.trim();

        // Validate input
        for (char c : binaryString.toCharArray()) {
            if (c != '0' && c != '1') {
                throw new IllegalArgumentException("Invalid binary digit: " + c);
            }
        }
        // Parse the binary string as an integer with base 2
        int decimalValue = Integer.parseInt(binaryString, 2);

        return decimalValue;
    }

    private String octalToBinary(String octalString) {
        // Remove leading/trailing whitespace
        octalString = octalString.trim();

        // Validate input
        for (char c : octalString.toCharArray()) {
            if (c < '0' || c > '7') {
                JOptionPane.showMessageDialog(null, "Invalid octal number");
                throw new IllegalArgumentException("Invalid octal digit: " + c);
            }
        }
        int octalValue = Integer.parseInt(octalString, 8);
        String binaryString = Integer.toBinaryString(octalValue);
        return binaryString.length() < 16 ? "0".repeat(16 - binaryString.length()) + binaryString : binaryString;
    }

    private void initializeProgramLoader() {
        // Implementation for Initial Program Loader (IPL)
        PC = "000000000110";
        IR = R0 = R1 = R2 = R3 = X1 = X2 = X3 = MAR = MBR = "0";

        for(JTextField field : gprFields) {
            field.setText("0");
        }

        for(JTextField field : ixrFields) {
            field.setText("0");
        }

        byte ind = 0;
        for(JTextField field : textFields) {
            if (ind == 0) {
                field.setText(PC);
                ind++;
            }
            else {
                field.setText("0");
            }
        }

        JOptionPane.showMessageDialog(null, "Simulator Initialized.");
    }
}