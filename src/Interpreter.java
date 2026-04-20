import java.util.HashMap;
import java.util.Map;

public class Interpreter {
    private Memory memory;
    private Mutex userInput;
    private Mutex userOutput;
    private Mutex fileMutex;
    private Scheduler scheduler;  // To add unblocked processes back to ready queue
    private Map<String, StringBuilder> fileSystem;  // Simulated file system (shared across all processes)
    private InputProvider inputProvider;  // Provides user input for processes
    
    // Main constructor with shared file system and input provider
    public Interpreter(Memory memory, Mutex userInput, Mutex userOutput, Mutex file, Scheduler scheduler, 
                      Map<String, StringBuilder> sharedFileSystem, InputProvider inputProvider) {
        this.memory = memory;
        this.userInput = userInput;
        this.userOutput = userOutput;
        this.fileMutex = file;
        this.scheduler = scheduler;
        this.fileSystem = sharedFileSystem != null ? sharedFileSystem : new HashMap<>();
        this.inputProvider = inputProvider;
    }
    
    // Constructor without shared file system (creates new one)
    public Interpreter(Memory memory, Mutex userInput, Mutex userOutput, Mutex file, Scheduler scheduler) {
        this(memory, userInput, userOutput, file, scheduler, null, null);
    }
    
    // Constructor with input provider but default file system
    public Interpreter(Memory memory, Mutex userInput, Mutex userOutput, Mutex file, Scheduler scheduler,
                      InputProvider inputProvider) {
        this(memory, userInput, userOutput, file, scheduler, null, inputProvider);
    }
    
    // Legacy constructor for backward compatibility
    public Interpreter(Memory memory, Mutex userInput, Mutex userOutput, Mutex file) {
        this(memory, userInput, userOutput, file, null, null, null);
    }

    // Execute a line of code
    public String execute(String line, ProcessControlBlock pcb) {
        if (line == null || line.trim().isEmpty()) {
            return "OK";
        }
        
        String[] parts = line.trim().split("\\s+", 2);  // Split into max 2 parts for flexibility
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";
        
        try {
            switch (command) {
                case "assign":
                    return executeAssign(args, pcb);
                case "print":
                    return executePrint(args, pcb);
                case "printfromto":
                    return executePrintFromTo(args, pcb);
                case "writefile":
                    return executeWriteFile(args, pcb);
                case "readfile":
                    return executeReadFile(args, pcb);
                case "input":
                    return executeInput(args, pcb);
                case "semwait":
                    return executeSemWait(args, pcb);
                case "semsignal":
                    return executeSemSignal(args, pcb);
                case "compute":
                    return executeCompute(args);
                default:
                    return "Unknown command: " + command;
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // assign x 5  -> assign variable x the value 5
    // assign x input -> read from stdin
    // assign x readFile filename -> read from file
    private String executeAssign(String args, ProcessControlBlock pcb) {
        String[] parts = args.split("\\s+", 3);
        if (parts.length < 2) {
            return "Error: assign requires variable and value";
        }
        
        String varName = parts[0];
        String valueSpec = parts[1];
        
        // Check for special keywords
        if (valueSpec.equals("input")) {
            return readUserInput(varName, pcb);
        } else if (valueSpec.equals("readFile") && parts.length >= 3) {
            String filenameVar = parts[2];
            return readFromFileVar(varName, filenameVar, pcb);
        }
        
        try {
            int numValue = Integer.parseInt(valueSpec);
            pcb.setVariable(varName, numValue);
            return "Assigned " + varName + " = " + numValue;
        } catch (NumberFormatException e) {
            // It's a variable reference or string
            if (valueSpec.startsWith("\"") && valueSpec.endsWith("\"")) {
                String strValue = valueSpec.substring(1, valueSpec.length() - 1);
                pcb.setVariable(varName, strValue);
                return "Assigned " + varName + " = \"" + strValue + "\"";
            } else if (pcb.getVariable(valueSpec) != null) {
                pcb.setVariable(varName, pcb.getVariable(valueSpec));
                return "Assigned " + varName + " = " + pcb.getVariable(valueSpec);
            }
            return "Error: unknown variable or invalid value " + valueSpec;
        }
    }

    private String readUserInput(String varName, ProcessControlBlock pcb) {
        // Note: The userInput mutex should already be held by semWait if this is part of the critical section
        // We don't try to acquire it here - it's already protected
        
        try {
            String inputValue;
            
            // If an input provider is available, use it for real user input
            if (inputProvider != null) {
                inputValue = inputProvider.getInput(pcb.getPid(), "Input for " + varName);
            } else {
                // Fallback: Use default values if no input provider is configured
                inputValue = "1";  // Default input
                if (pcb.getPid() == 1) {
                    // Program 1: wants x and y (range inputs)
                    if (pcb.getVariable("x") == null) {
                        inputValue = "1";  // First input is 1
                    } else {
                        inputValue = "10";  // Second input is 10
                    }
                } else if (pcb.getPid() == 2) {
                    // Program 2: wants a (filename) and b (data)
                    if (pcb.getVariable("a") == null) {
                        inputValue = "output.txt";  // filename
                    } else {
                        inputValue = "Hello World";  // data
                    }
                } else if (pcb.getPid() == 3) {
                    // Program 3: wants a (filename to read)
                    inputValue = "output.txt";  // filename to read
                }
            }
            
            pcb.setVariable(varName, inputValue);
            return "Read input: " + varName + " = " + inputValue;
        } catch (Exception e) {
            return "Error reading input: " + e.getMessage();
        }
    }

    private String readFromFileVar(String varName, String filenameVar, ProcessControlBlock pcb) {
        // Get the actual filename from the variable
        Object filenameObj = pcb.getVariable(filenameVar);
        if (filenameObj == null) {
            return "Error: filename variable '" + filenameVar + "' not found";
        }
        
        String filename = filenameObj.toString();
        return readFromFile(varName, filename, pcb);
    }

    private String readFromFile(String varName, String filename, ProcessControlBlock pcb) {
        // Note: The file mutex should already be held by semWait if this is part of the critical section
        // We don't try to acquire it here - it's already protected
        
        try {
            if (fileSystem.containsKey(filename)) {
                String content = fileSystem.get(filename).toString();
                pcb.setVariable(varName, content);
                return "Read from file: " + varName + " = " + content;
            } else {
                return "Error: file not found: " + filename;
            }
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    // print x -> print variable x
    private String executePrint(String args, ProcessControlBlock pcb) {
        if (args.isEmpty()) {
            return "Error: print requires a variable";
        }
        
        String varName = args.trim();
        Object value = pcb.getVariable(varName);
        if (value == null) {
            return "Error: variable " + varName + " not found";
        }
        
        return "Output: " + value;
    }

    // printFromTo x y -> print variables from x to y
    private String executePrintFromTo(String args, ProcessControlBlock pcb) {
        String[] parts = args.split("\\s+");
        if (parts.length < 2) {
            return "Error: printFromTo requires start and end variables";
        }
        
        String varStart = parts[0];
        String varEnd = parts[1];
        
        Object startVal = pcb.getVariable(varStart);
        Object endVal = pcb.getVariable(varEnd);
        
        if (startVal == null || endVal == null) {
            return "Error: variables not found";
        }
        
        try {
            int start = Integer.parseInt(startVal.toString());
            int end = Integer.parseInt(endVal.toString());
            
            StringBuilder result = new StringBuilder();
            for (int i = start; i <= end; i++) {
                result.append(i).append(" ");
            }
            
            return "Output: " + result.toString();
        } catch (NumberFormatException e) {
            return "Error: variables must be numeric";
        }
    }

    // writeFile filename content -> write to file
    private String executeWriteFile(String args, ProcessControlBlock pcb) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            return "Error: writeFile requires filename and content";
        }
        
        String filenameRef = parts[0];
        String contentRef = parts[1];
        
        // Get the actual filename - check if it's a variable first
        String filename = filenameRef;
        Object filenameVar = pcb.getVariable(filenameRef);
        if (filenameVar != null) {
            filename = filenameVar.toString();  // Use variable value
        }
        
        // Get the actual content (might be a variable)
        Object contentObj = null;
        try {
            contentObj = Integer.parseInt(contentRef);
        } catch (NumberFormatException e) {
            contentObj = pcb.getVariable(contentRef);
        }
        
        if (contentObj == null) {
            return "Error: content variable '" + contentRef + "' not found";
        }
        
        String content = contentObj.toString();
        
        // Note: The file mutex should already be held by semWait if this is part of the critical section
        // We don't try to acquire it here - it's already protected
        try {
            if (!fileSystem.containsKey(filename)) {
                fileSystem.put(filename, new StringBuilder());
            }
            fileSystem.get(filename).append(content);
            return "Wrote to " + filename + " (content: " + content + ")";
        } catch (Exception e) {
            return "Error writing file: " + e.getMessage();
        }
    }

    // readFile filename -> read from file
    private String executeReadFile(String args, ProcessControlBlock pcb) {
        String filename = args.trim();
        
        // System Call: File Read
        if (!fileMutex.tryLock()) {
            // Block process
            fileMutex.block(pcb);
            pcb.setState(ProcessControlBlock.ProcessState.BLOCKED);
            return "Blocked on file read";
        }
        
        try {
            if (fileSystem.containsKey(filename)) {
                String content = fileSystem.get(filename).toString();
                pcb.setVariable("fileContent", content);
                return "Read from " + filename + ": " + content;
            }
            return "Error: file not found " + filename;
        } finally {
            fileMutex.unlock();
        }
    }

    // input x -> read input into variable x
    private String executeInput(String args, ProcessControlBlock pcb) {
        String varName = args.trim();
        
        // System Call: User Input
        if (!userInput.tryLock()) {
            // Block the process
            userInput.block(pcb);
            pcb.setState(ProcessControlBlock.ProcessState.BLOCKED);
            return "Blocked on user input";
        }
        
        // In simulation, just set a default value
        try {
            pcb.setVariable(varName, "simulated_input");
            return "Read input into " + varName;
        } finally {
            userInput.unlock();
        }
    }

    // semWait mutexName -> acquire mutex
    private String executeSemWait(String args, ProcessControlBlock pcb) {
        String mutexName = args.trim().toLowerCase();
        Mutex mutex = getMutexByName(mutexName);
        
        if (mutex == null) {
            return "Error: unknown mutex " + mutexName;
        }
        
        if (mutex.tryLock(pcb)) {
            return "Acquired mutex: " + mutexName;
        } else {
            // Block the process
            mutex.block(pcb);
            pcb.setState(ProcessControlBlock.ProcessState.BLOCKED);
            return "Blocked on mutex: " + mutexName;
        }
    }

    // semSignal mutexName -> release mutex
    private String executeSemSignal(String args, ProcessControlBlock pcb) {
        String mutexName = args.trim().toLowerCase();
        Mutex mutex = getMutexByName(mutexName);
        
        if (mutex == null) {
            return "Error: unknown mutex " + mutexName;
        }
        
        // Signal the mutex - this unblocks one waiting process and transfers the lock to it
        ProcessControlBlock unblockedPcb = mutex.signalAndUnblock(pcb);
        
        if (unblockedPcb != null) {
            unblockedPcb.setState(ProcessControlBlock.ProcessState.READY);
            // Unblock the process in the scheduler
            if (scheduler != null) {
                scheduler.unblockProcess(unblockedPcb);
            }
            return "Released mutex: " + mutexName + ", unblocked PID " + unblockedPcb.getPid();
        }
        
        return "Released mutex: " + mutexName;
    }

    // compute time -> CPU computation for time units
    private String executeCompute(String args) {
        try {
            int time = Integer.parseInt(args.trim());
            return "Computing for " + time + " time units";
        } catch (NumberFormatException e) {
            return "Error: invalid compute time";
        }
    }

    private Mutex getMutexByName(String name) {
        switch (name) {
            case "userinput":
                return userInput;
            case "useroutput":
                return userOutput;
            case "file":
                return fileMutex;
            default:
                return null;
        }
    }
}
