import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;

public class ConsoleSimulator {
    
    public static void main(String[] args) throws Exception {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║         OPERATING SYSTEM SIMULATOR - CONSOLE MODE                ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");
        
        System.out.println("Select Scheduling Algorithm:");
        System.out.println("1. Round Robin (RR)");
        System.out.println("2. HRRN (Highest Response Ratio Next)");
        System.out.println("3. MLFQ (Multi-Level Feedback Queue)");
        System.out.print("\nEnter choice (1-3): ");
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String choice = reader.readLine();
        
        if (choice == null) choice = "1";
        choice = choice.trim();
        
        Scheduler.SchedulingAlgorithm algo;
        switch (choice) {
            case "1":
                algo = Scheduler.SchedulingAlgorithm.ROUND_ROBIN;
                System.out.println("\n>>> Selected: Round Robin");
                break;
            case "2":
                algo = Scheduler.SchedulingAlgorithm.HRRN;
                System.out.println("\n>>> Selected: HRRN");
                break;
            case "3":
                algo = Scheduler.SchedulingAlgorithm.MLFQ;
                System.out.println("\n>>> Selected: MLFQ");
                break;
            default:
                algo = Scheduler.SchedulingAlgorithm.ROUND_ROBIN;
                System.out.println("\n>>> Invalid choice, defaulting to Round Robin");
        }
        
        // Ask for custom configuration
        System.out.print("\nUse custom configuration? (y/n, default: n): ");
        String configChoice = reader.readLine();
        
        int[] arrivalTimes = {0, 1, 4};  // Default
        int rrQuantum = 2;  // Default
        int[] mlfqQuantums = {2, 4, 8, 16};  // Default
        
        if (configChoice != null && configChoice.trim().equalsIgnoreCase("y")) {
            // Get arrival times
            System.out.print("Enter process arrival times (3 values, space-separated, default: 0 1 4): ");
            String arrivalInput = reader.readLine();
            if (arrivalInput != null && !arrivalInput.trim().isEmpty()) {
                try {
                    String[] parts = arrivalInput.trim().split("\\s+");
                    if (parts.length >= 3) {
                        arrivalTimes = new int[3];
                        for (int i = 0; i < 3; i++) {
                            arrivalTimes[i] = Integer.parseInt(parts[i]);
                        }
                        System.out.println("✓ Custom arrival times set: " + java.util.Arrays.toString(arrivalTimes));
                    }
                } catch (NumberFormatException e) {
                    System.out.println("✗ Invalid input, using defaults: " + java.util.Arrays.toString(arrivalTimes));
                }
            }
            
            // Get quantum for RR
            if (algo == Scheduler.SchedulingAlgorithm.ROUND_ROBIN) {
                System.out.print("Enter Round Robin quantum (default: 2): ");
                String rrInput = reader.readLine();
                if (rrInput != null && !rrInput.trim().isEmpty()) {
                    try {
                        rrQuantum = Integer.parseInt(rrInput.trim());
                        System.out.println("✓ RR quantum set to: " + rrQuantum);
                    } catch (NumberFormatException e) {
                        System.out.println("✗ Invalid input, using default: " + rrQuantum);
                    }
                }
            }
            
            // Get quantums for MLFQ
            if (algo == Scheduler.SchedulingAlgorithm.MLFQ) {
                System.out.print("Enter MLFQ quantums for 4 levels (space-separated, default: 2 4 8 16): ");
                String mlfqInput = reader.readLine();
                if (mlfqInput != null && !mlfqInput.trim().isEmpty()) {
                    try {
                        String[] parts = mlfqInput.trim().split("\\s+");
                        if (parts.length >= 4) {
                            mlfqQuantums = new int[4];
                            for (int i = 0; i < 4; i++) {
                                mlfqQuantums[i] = Integer.parseInt(parts[i]);
                            }
                            System.out.println("✓ MLFQ quantums set: " + java.util.Arrays.toString(mlfqQuantums));
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("✗ Invalid input, using defaults: " + java.util.Arrays.toString(mlfqQuantums));
                    }
                }
            }
        } else {
            System.out.println("Using default configuration");
        }
        
        // Find the correct path to program files
        String currentDir = System.getProperty("user.dir");
        String programPath = currentDir;
        
        // Try to find the src directory containing program files
        java.nio.file.Path srcPath = java.nio.file.Paths.get(currentDir, "src");
        if (java.nio.file.Files.exists(srcPath)) {
            programPath = srcPath.toString();
        } else {
            // If we're already in src directory, use it
            if (currentDir.endsWith("src")) {
                programPath = currentDir;
            } else {
                // Try parent directory
                java.nio.file.Path parentSrcPath = java.nio.file.Paths.get(currentDir).getParent().resolve("src");
                if (java.nio.file.Files.exists(parentSrcPath)) {
                    programPath = parentSrcPath.toString();
                }
            }
        }
        
        System.out.println("\nLoading programs from: " + programPath);
        
        // Create simulator with custom configuration
        OSSimulator simulator = new OSSimulator(algo, null, programPath, arrivalTimes, rrQuantum, mlfqQuantums);
        
        System.out.println("\n" + simulator.getSchedulingConfig());
        System.out.println("Starting simulation...\n");
        System.out.println("═════════════════════════════════════════════════════════════════\n");
        
        int maxCycles = 50;
        int cycle = 0;
        boolean done = false;
        
        while (cycle < maxCycles && !done) {
            simulator.stepSimulation();
            
            // Print output
            System.out.println(simulator.getLastOutput());
            System.out.println(simulator.getMemoryMap());
            System.out.println(simulator.getQueueStatus());
            System.out.println();
            
            cycle++;
            
            // Check if done by looking at the output
            String output = simulator.getLastOutput();
            if (output.contains("All processes terminated")) {
                done = true;
            }
        }
        
        System.out.println("═════════════════════════════════════════════════════════════════\n");
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║                     SIMULATION ENDED                            ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
    }
}
