import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class OSSimulator {
    private Memory memory;
    private Scheduler scheduler;
    private Mutex userInput;
    private Mutex userOutput;
    private Mutex fileMutex;
    
    private int clock = 0;
    private int nextPid = 1;
    private List<ProcessControlBlock> allProcesses;
    private List<Integer> processArrivalTimes;
    private String[][] PROCESS_PROGRAMS;
    private String lastOutput = "";
    
    private int processCreationIndex = 0;
    private static final int[] DEFAULT_ARRIVAL_TIMES = {0, 1, 4};
    private int[] arrivalTimes;
    private int rrQuantum = 2;
    private int[] mlfqQuantums = {2, 4, 8, 16};
    
    // Shared file system for all processes
    private Map<String, StringBuilder> sharedFileSystem;
    private InputProvider inputProvider;  // Provides user input for processes
    
    public OSSimulator(Scheduler.SchedulingAlgorithm algorithm, OSSimulatorGUI gui) {
        this(algorithm, gui, "./src", DEFAULT_ARRIVAL_TIMES, 2, new int[]{2, 4, 8, 16});
    }
    
    public OSSimulator(Scheduler.SchedulingAlgorithm algorithm, OSSimulatorGUI gui, String programPath) {
        this(algorithm, gui, programPath, DEFAULT_ARRIVAL_TIMES, 2, new int[]{2, 4, 8, 16});
    }
    
    public OSSimulator(Scheduler.SchedulingAlgorithm algorithm, OSSimulatorGUI gui, String programPath, 
                       int[] customArrivalTimes, int rrQuantum, int[] mlfqQuantums) {
        // Initialize components
        memory = new Memory();
        scheduler = new Scheduler(algorithm, rrQuantum, mlfqQuantums);
        userInput = new Mutex("userInput");
        userOutput = new Mutex("userOutput");
        fileMutex = new Mutex("file");
        
        // Initialize shared file system
        sharedFileSystem = new HashMap<>();
        
        // Initialize input provider (for console/user input)
        inputProvider = new ConsoleInputProvider();
        
        this.arrivalTimes = customArrivalTimes;
        this.rrQuantum = rrQuantum;
        this.mlfqQuantums = mlfqQuantums;
        
        allProcesses = new ArrayList<>();
        processArrivalTimes = new ArrayList<>();
        
        // Load programs from files
        try {
            PROCESS_PROGRAMS = ProgramLoader.loadAllPrograms(programPath);
            // Check if all programs failed to load (empty arrays)
            boolean allEmpty = true;
            for (String[] program : PROCESS_PROGRAMS) {
                if (program != null && program.length > 0) {
                    allEmpty = false;
                    break;
                }
            }
            if (allEmpty) {
                throw new Exception("All programs are empty");
            }
        } catch (Exception e) {
            System.err.println("Failed to load programs: " + e.getMessage());
            // Fallback to default programs
            PROCESS_PROGRAMS = getDefaultPrograms();
        }
        
        // Load processes with custom arrival times
        for (int i = 0; i < PROCESS_PROGRAMS.length && i < arrivalTimes.length; i++) {
            processArrivalTimes.add(arrivalTimes[i]);
        }
    }
    
    /**
     * Set the InputProvider for this simulator (useful for switching between console and GUI input)
     */
    public void setInputProvider(InputProvider provider) {
        if (provider != null) {
            this.inputProvider = provider;
        }
    }
    
    /**
     * Get the current InputProvider
     */
    public InputProvider getInputProvider() {
        return inputProvider;
    }
    
    private static String[][] getDefaultPrograms() {
        return new String[][] {
            {  // Process 1 - arrives at time 0
                "semWait userInput",
                "assign x input",
                "assign y input",
                "semSignal userInput",
                "semWait userOutput",
                "printFromTo x y",
                "semSignal userOutput"
            },
            {  // Process 2 - arrives at time 1
                "semWait userInput",
                "assign a input",
                "assign b input",
                "semSignal userInput",
                "semWait file",
                "writeFile a b",
                "semSignal file"
            },
            {  // Process 3 - arrives at time 4
                "semWait userInput",
                "assign a input",
                "semSignal userInput",
                "semWait file",
                "assign b readFile a",
                "semSignal file",
                "semWait userOutput",
                "print b",
                "semSignal userOutput"
            }
        };
    }
    
    // Execute one simulation step (one clock cycle)
    public void stepSimulation() {
        // Check if new processes should arrive BEFORE incrementing clock (first cycle catches arrival 0)
        String arrivalMessages = "";
        if (processCreationIndex < PROCESS_PROGRAMS.length) {
            int arrivalTime = processArrivalTimes.get(processCreationIndex);
            
            if (clock == arrivalTime) {
                ProcessControlBlock pcb = createProcess(processCreationIndex);
                if (pcb != null) {
                    arrivalMessages = String.format("[Clock %d] P%d ARRIVED\n", clock, pcb.getPid());
                    arrivalMessages += String.format("[Clock %d] STATE CHANGE: P%d -> READY\n", clock, pcb.getPid());
                }
                processCreationIndex++;
            }
        }
        
        clock++;
        lastOutput = "";
        
        // Update scheduler's clock (for HRRN)
        scheduler.updateClock(clock);
        
        // Add arrival messages if any
        lastOutput = arrivalMessages;
        
        ExecutionTrace trace = new ExecutionTrace(clock);
        
        // Get next process to run
        ProcessControlBlock currentProcess = scheduler.getNextProcess();
        
        // Check if a process was just preempted
        ProcessControlBlock preemptedProcess = scheduler.getLastPreemptedProcess();
        if (preemptedProcess != null) {
            trace.setStateChange(ProcessControlBlock.ProcessState.READY, preemptedProcess.getPid());
            lastOutput += String.format("[Clock %d] STATE CHANGE: P%d -> READY (Preempted due to quantum)\n", 
                                      clock, preemptedProcess.getPid());
            scheduler.clearLastPreempted();
        }
        
        if (currentProcess != null) {
            trace.setCurrentProcess(currentProcess);
            executeInstruction(currentProcess, trace);
        } else {
            lastOutput += String.format("[Clock %d] No process ready to run\n", clock);
        }
        
        // Check if any processes were unblocked during instruction execution
        List<ProcessControlBlock> unblockedProcesses = scheduler.getLastUnblockedProcesses();
        for (ProcessControlBlock pcb : unblockedProcesses) {
            lastOutput += String.format("[Clock %d] STATE CHANGE: P%d -> READY (Unblocked)\n", clock, pcb.getPid());
        }
        scheduler.clearLastUnblocked();
        
        // Populate execution trace with current queue states
        trace.setReadyQueue(scheduler.getReadyQueueAsList());
        trace.setBlockedQueue(scheduler.getBlockedQueueAsList());
        trace.setMemoryMap(memory.getMemoryMap());
        
        // Add detailed trace output
        lastOutput = trace.getDetailedTrace() + lastOutput;
        
        // Check if all processes are done
        if (allProcessesDone()) {
            lastOutput += "\n*** SIMULATION COMPLETE - All processes terminated ***";
        }
    }
    
    private void checkForUnblockedProcesses() {
        // After executing an instruction, some processes might have been unblocked (state changed to READY)
        // but they're not in the scheduler's queue yet. We need to add them.
        // However, this is tricky because we don't know which processes were just unblocked.
        // A better approach would be to track this in the Interpreter or use a different mechanism.
        // For now, this is a placeholder.
    }
    
    private ProcessControlBlock createProcess(int programIndex) {
        ProcessControlBlock pcb = new ProcessControlBlock(nextPid++);
        pcb.setArrivalTime(clock);
        
        // Set program
        if (programIndex < PROCESS_PROGRAMS.length) {
            pcb.setProgram(PROCESS_PROGRAMS[programIndex]);
        }
        
        // Allocate memory (10 words per process)
        int memStart = memory.allocate(pcb.getPid(), 10);
        
        if (memStart == -1) {
            // Memory full - need to swap something
            lastOutput += "\nMEMORY FULL - Swapping required (not implemented)\n";
            return null;
        }
        
        pcb.setMemoryStart(memStart);
        pcb.setMemorySize(10);
        
        // Add to scheduler
        scheduler.addProcess(pcb);
        allProcesses.add(pcb);
        
        return pcb;
    }
    
    private void executeInstruction(ProcessControlBlock pcb, ExecutionTrace trace) {
        if (pcb.getProgram() == null || pcb.getProgramCounter() >= pcb.getProgram().length) {
            // Process finished
            pcb.setState(ProcessControlBlock.ProcessState.TERMINATED);
            memory.free(pcb.getPid());
            scheduler.processTerminated(pcb);  // Notify scheduler
            trace.setStateChange(ProcessControlBlock.ProcessState.TERMINATED, pcb.getPid());
            lastOutput = String.format("[Clock %d] P%d TERMINATED\n", clock, pcb.getPid());
            return;
        }
        
        String instruction = pcb.getProgram()[pcb.getProgramCounter()];
        trace.setInstruction(instruction);
        
        // Execute instruction with shared file system and scheduler
        Interpreter interpreter = new Interpreter(memory, userInput, userOutput, fileMutex, scheduler, sharedFileSystem, inputProvider);
        String result = interpreter.execute(instruction, pcb);
        trace.setResult(result);
        
        // Check if process got blocked
        if (pcb.getState() == ProcessControlBlock.ProcessState.BLOCKED) {
            // Add to scheduler's blocked queue
            scheduler.blockProcess(pcb);
            trace.setStateChange(ProcessControlBlock.ProcessState.BLOCKED, pcb.getPid());
            lastOutput += String.format("[Clock %d] P%d BLOCKED: %s\n", clock, pcb.getPid(), result);
        } else {
            // Process executed successfully and was not blocked
            pcb.incrementProgramCounter();
            // NOTE: Do NOT add to scheduler here! The scheduler manages the ready queue
            // After this instruction, the next getNextProcess() call will determine if the process:
            // - Continues running (quantum not exceeded)
            // - Gets preempted and moved to back of queue (quantum exceeded)
            pcb.setState(ProcessControlBlock.ProcessState.READY);
            lastOutput += String.format("[Clock %d] P%d executed OK (PC now %d)\n", clock, pcb.getPid(), pcb.getProgramCounter());
        }
    }
    
    public boolean allProcessesDone() {
        for (ProcessControlBlock pcb : allProcesses) {
            if (pcb.getState() != ProcessControlBlock.ProcessState.TERMINATED) {
                return false;
            }
        }
        return processCreationIndex >= PROCESS_PROGRAMS.length;
    }
    
    public String getLastOutput() {
        return lastOutput;
    }
    
    public String getMemoryMap() {
        return memory.getMemoryMap();
    }
    
    public String getQueueStatus() {
        return scheduler.getQueueStatus();
    }
    
    public ProcessControlBlock getCurrentProcess() {
        return scheduler.getCurrentProcess();
    }
    
    public int getClock() {
        return clock;
    }
    
    public int[] getArrivalTimes() {
        return arrivalTimes;
    }
    
    public String getArrivalTimesInfo() {
        StringBuilder sb = new StringBuilder("Process Arrival Times: ");
        for (int i = 0; i < arrivalTimes.length && i < 3; i++) {
            sb.append(String.format("P%d@%d ", i+1, arrivalTimes[i]));
        }
        return sb.toString();
    }
    
    public String getSchedulingConfig() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== SCHEDULING CONFIGURATION ===\n");
        sb.append(scheduler.getQuantumInfo()).append("\n");
        sb.append(getArrivalTimesInfo()).append("\n");
        return sb.toString();
    }
}