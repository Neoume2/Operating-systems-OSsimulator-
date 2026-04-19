import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProgramLoader {
    
    // Load a program from a text file
    public static String[] loadProgram(String filename) throws IOException {
        List<String> instructions = new ArrayList<>();
        
        File file = new File(filename);
        if (!file.exists()) {
            throw new IOException("File not found: " + file.getAbsolutePath());
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("//")) {
                    instructions.add(line);
                }
            }
        }
        
        return instructions.toArray(new String[0]);
    }
    
    // Load all three programs
    public static String[][] loadAllPrograms(String basePath) throws IOException {
        String[][] programs = new String[3][];
        
        String[] filenames = {
            new File(basePath, "Program 1.txt").getAbsolutePath(),
            new File(basePath, "Program_2.txt").getAbsolutePath(),
            new File(basePath, "Program_3.txt").getAbsolutePath()
        };
        
        for (int i = 0; i < 3; i++) {
            try {
                programs[i] = loadProgram(filenames[i]);
                System.out.println("Loaded " + new File(filenames[i]).getName() + " with " + 
                    programs[i].length + " instructions");
            } catch (IOException e) {
                System.err.println("Warning: Could not load program " + i + " - " + e.getMessage());
                programs[i] = new String[0];
            }
        }
        
        return programs;
    }
}
