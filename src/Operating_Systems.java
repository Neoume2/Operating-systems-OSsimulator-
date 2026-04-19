

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Operating_Systems {
    // Memory instance
    private Memory memory;
    
    // Mutexes
    private Mutex userInput;
    private Mutex userOutput;
    private Mutex file;
    
    // Queues
    private Queue<ProcessControlBlock> readyQueue;
    private Queue<ProcessControlBlock> blockedQueue;
    
    // Process management
    private int nextPid;
    private List<ProcessControlBlock> allProcesses;
    
    // Clock cycle
    private int clockCycle;
    private boolean running;
    
    // Sample program for testing
    private List<String[]> programs;
    
    public Operating_Systems() {
        // Initialize memory
        memory = new Memory();
        
        // Initialize mutexes
        userInput = new Mutex("userInput");
        userOutput = new Mutex("userOutput");
        file = new Mutex("file");
        
        // Initialize queues
        readyQueue = new LinkedList<>();
        blockedQueue = new LinkedList<>();
        
        // Initialize process management
        nextPid = 1;
        allProcesses = new ArrayList<>();
        
        // Initialize clock
        clockCycle = 0;
        running = false;
        
        // Initialize sample programs
        initializePrograms();
    }

    private void initializePrograms() {
        programs = new ArrayList<>();
        
        // Program 1
        programs.add(new String[] {
            "assign x 5",
            "print x",
            "compute 10",
            "assign y 10",
            "print y"
        });
        
        // Program 2
        programs.add(new String[] {
            "input a",
            "semWait userInput",
            "print a",
            "semSignal userInput"
        });
        
        // Program 3
        programs.add(new String[] {
            "assign z 100",
            "compute 20",
            "print z",
            "semWait file",
            "semSignal file"
        });
    }

    // Create a new process
    public ProcessControlBlock createProcess(int programIndex) {
        if (programIndex >= programs.size()) {
            System.out.println("Error: Invalid program index");
            return null;
        }
        
        ProcessControlBlock pcb = new ProcessControlBlock(nextPid++);
        
        // Try to allocate memory
        int memSize = 10; // Default memory size
        int memStart = memory.allocate(pcb.getPid(), memSize);
        
        if (memStart == -1) {
            // No space - need to swap
            System.out.println("Memory full, attempting swap...");
            // For now, return null (in a full system, we'd swap something out)
            return null;
        }
        
        pcb.setMemoryStart(memStart);
        pcb.setMemorySize(memSize);
        pcb.setState(ProcessControlBlock.ProcessState.READY);
        
        allProcesses.add(pcb);
        readyQueue.add(pcb);
        
        return pcb;
    }

    // Main clock cycle loop
    public void run() {
        running = true;
        System.out.println("=== Operating System Starting ===");
        System.out.println("Memory Size: 40 units");
        System.out.println("==================================\n");
        
        // Create initial processes
        System.out.println("Creating initial processes...");
        for (int i = 0; i < programs.size(); i++) {
            ProcessControlBlock pcb = createProcess(i);
            if (pcb != null) {
                System.out.println("Created " + pcb);
            }
        }
        
        System.out.println();
        
        // Main loop
        while (running) {
            System.out.println("--- Clock Cycle " + clockCycle + " ---");
            clockCycle++;
            
            // Execute one instruction from each ready process
            executeReadyProcesses();
            
            // Handle blocked processes (check if they can be unblocked)
            handleBlockedProcesses();
            
            // Print queue status
            printQueueStatus();
            
            // Check if we should stop (all processes done)
            if (readyQueue.isEmpty() && blockedQueue.isEmpty()) {
                System.out.println("\n=== All processes completed ===");
                running = false;
            }
            
            // For demo, stop after 20 cycles
            if (clockCycle >= 20) {
                System.out.println("\n=== Demo complete (20 cycles) ===");
                running = false;
            }
            
            System.out.println();
            
            // Small delay for readability
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
        
        printFinalStats();
    }

    private void executeReadyProcesses() {
        // Create a copy to iterate (since we might move processes to blocked)
        List<ProcessControlBlock> readyList = new ArrayList<>(readyQueue);
        readyQueue.clear();
        
        for (ProcessControlBlock pcb : readyList) {
            if (pcb.getState() == ProcessControlBlock.ProcessState.READY) {
                pcb.setState(ProcessControlBlock.ProcessState.RUNNING);
                
                // Get the program for this process
                int programIndex = (pcb.getPid() - 1) % programs.size();
                String[] program = programs.get(programIndex);
                
                // Execute current instruction
                if (pcb.getProgramCounter() < program.length) {
                    String instruction = program[pcb.getProgramCounter()];
                    System.out.println("PID " + pcb.getPid() + " executing: " + instruction);
                    
                    // Create interpreter and execute
                    Interpreter interpreter = new Interpreter(memory, userInput, userOutput, file);
                    String result = interpreter.execute(instruction, pcb);
                    
                    // Check if blocked
                    if (pcb.getState() == ProcessControlBlock.ProcessState.BLOCKED) {
                        blockedQueue.add(pcb);
                        System.out.println("  -> " + result);
                    } else {
                        pcb.incrementProgramCounter();
                        pcb.setState(ProcessControlBlock.ProcessState.READY);
                        readyQueue.add(pcb);
                        System.out.println("  -> " + result);
                    }
                } else {
                    // Process completed
                    pcb.setState(ProcessControlBlock.ProcessState.TERMINATED);
                    memory.free(pcb.getPid());
                    System.out.println("PID " + pcb.getPid() + " TERMINATED");
                }
            } else {
                readyQueue.add(pcb);
            }
        }
    }

    private void handleBlockedProcesses() {
        // In a real OS, blocked processes would be checked for I/O completion
        // For this simulation, we'll keep them blocked
    }

    private void printQueueStatus() {
        System.out.println("Ready Queue: " + readyQueue.size() + " processes");
        System.out.println("Blocked Queue: " + blockedQueue.size() + " processes");
    }

    private void printFinalStats() {
        System.out.println("\n=== Final Statistics ===");
        System.out.println("Total Clock Cycles: " + clockCycle);
        System.out.println("Processes Created: " + allProcesses.size());
        
        int terminated = 0;
        for (ProcessControlBlock pcb : allProcesses) {
            if (pcb.getState() == ProcessControlBlock.ProcessState.TERMINATED) {
                terminated++;
            }
        }
        System.out.println("Processes Terminated: " + terminated);
        System.out.println("Memory Available: " + memory.getAvailableSpace() + " units");
    }

    public static void main(String[] args) {
        Operating_Systems os = new Operating_Systems();
        os.run();
    }
}
