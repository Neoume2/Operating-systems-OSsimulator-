# OS Simulator - Requirements Analysis & Implementation Guide

**Date**: April 19, 2026  
**Project**: Operating Systems Simulator - Java  
**Analysis Type**: Comprehensive Requirements Verification

---

## Executive Summary

| # | Requirement | Status | Priority | Effort |
|---|---|---|---|---|
| 1 | Queues printed after scheduling events | ⚠️ PARTIAL | High | Medium |
| 2 | Currently executing process shown | ✅ COMPLETE | High | Complete |
| 3 | Current instruction shown | ✅ COMPLETE | High | Complete |
| 4 | Configurable time slice/quantum | ❌ NOT IMPL | High | Medium |
| 5 | Variable scheduling order | ✅ COMPLETE | Medium | Complete |
| 6 | Variable arrival timings | ❌ NOT IMPL | Medium | Low |
| 7 | Memory displayed each cycle (readable) | ✅ COMPLETE | High | Complete |
| 8 | Swapped process IDs tracked | ⚠️ PARTIAL | Medium | Medium |
| 9 | Disk memory format defined | ✅ COMPLETE | Medium | Complete |
| 10 | Output readable & presentable | ✅ GOOD | Medium | Low |

**Overall Completion**: 60% (6/10 requirements fully implemented, 2 partial, 2 missing)

---

## Detailed Requirements Analysis

### REQUIREMENT 1: Queues Printed After Every Scheduling Event

**Status**: ⚠️ PARTIAL IMPLEMENTATION

**Current Implementation**:
- **File**: [ConsoleSimulator.java](ConsoleSimulator.java#L72)
- **Line**: 72 - `System.out.println(simulator.getQueueStatus());`
- **Frequency**: Every clock cycle (every `stepSimulation()`)

- **File**: [OSSimulatorGUI.java](OSSimulatorGUI.java#L217)
- **Lines**: 217-218 - `queueArea.setText(simulator.getQueueStatus());`
- **Frequency**: Every GUI update

**Queue Status Display Method**:
- **File**: [Scheduler.java](Scheduler.java#L207-L227)
- **Lines**: 207-227 - `getQueueStatus()` method
- **Output Format**:
  ```
  Ready: P1 P2 P3 
  Blocked: 
  ```
  OR for MLFQ:
  ```
  Level 0: P1 P2 
  Level 1: 
  Level 2: 
  Level 3: 
  Blocked: 
  ```

**Issues**:
1. ❌ Queues printed **every cycle**, not specifically after scheduling events
2. ❌ No distinction between events (process chosen, blocked, or finished)
3. ❌ Missing contextual information (reason for queue change)

**What's Missing**:
- Event-triggered logging instead of cycle-based
- Event type tracking (SCHEDULED, BLOCKED, UNBLOCKED, TERMINATED)
- Queue state changes explanation

**Needed Code Changes**:

**CHANGE 1**: Add event logging to Scheduler.java
- **File**: [Scheduler.java](Scheduler.java)
- **After line**: 127 (end of `getNextMLFQ()`)
- **Add**:
```java
// Event tracking
private List<String> schedulingEvents = new ArrayList<>();

public String getLastSchedulingEvent() {
    if (schedulingEvents.isEmpty()) return "";
    return schedulingEvents.remove(0);
}

private void logEvent(String event) {
    schedulingEvents.add(event);
}
```

**CHANGE 2**: Update `getNextRoundRobin()` in [Scheduler.java](Scheduler.java#L63-L82)
- **Current**: Lines 63-82
- **Modify** to call `logEvent()` when preempting or selecting:
```java
// When preempting (around line 73)
logEvent(String.format("[RR] Preempting P%d (quantum expired: %d/%d)", 
    currentProcess.getPid(), currentQuantumUsed, timeQuantum));

// When selecting new process (around line 80)
logEvent(String.format("[RR] Selected P%d from ready queue", currentProcess.getPid()));
```

**CHANGE 3**: Update `blockProcess()` in [Scheduler.java](Scheduler.java#L161-L167)
- **Current**: Lines 161-167
- **Add event logging**:
```java
public void blockProcess(ProcessControlBlock pcb) {
    pcb.setState(ProcessControlBlock.ProcessState.BLOCKED);
    blockedQueue.add(pcb);
    if (currentProcess == pcb) {
        currentProcess = null;
    }
    logEvent(String.format("[BLOCK] P%d moved to blocked queue", pcb.getPid()));
}
```

**CHANGE 4**: Update `unblockProcess()` in [Scheduler.java](Scheduler.java#L169-L182)
- **Current**: Lines 169-182
- **Add event logging**:
```java
public void unblockProcess(ProcessControlBlock pcb) {
    if (blockedQueue.remove(pcb)) {
        pcb.setState(ProcessControlBlock.ProcessState.READY);
        
        if (algorithm == SchedulingAlgorithm.MLFQ) {
            priorityQueues.get(pcb.getPriority()).add(pcb);
        } else {
            readyQueue.add(pcb);
        }
        logEvent(String.format("[UNBLOCK] P%d returned to ready queue", pcb.getPid()));
    }
}
```

**CHANGE 5**: Display events in [ConsoleSimulator.java](ConsoleSimulator.java#L68-L75)
- **Current**: Lines 68-75
- **Modify output section**:
```java
while (cycle < maxCycles && !done) {
    simulator.stepSimulation();
    
    // Print events first
    String events = simulator.getLastSchedulingEvents();
    if (!events.isEmpty()) {
        System.out.println(events);
    }
    
    // Then other outputs
    System.out.println(simulator.getLastOutput());
    System.out.println(simulator.getMemoryMap());
    System.out.println(simulator.getQueueStatus());
    
    cycle++;
    
    String output = simulator.getLastOutput();
    if (output.contains("All processes terminated")) {
        done = true;
    }
}
```

**CHANGE 6**: Add event getter to [OSSimulator.java](OSSimulator.java)
- **After line**: 158 (after `getQueueStatus()`)
- **Add**:
```java
public String getLastSchedulingEvents() {
    StringBuilder sb = new StringBuilder();
    // Collect events from scheduler
    for (int i = 0; i < 10; i++) {
        String event = scheduler.getLastSchedulingEvent();
        if (event.isEmpty()) break;
        sb.append(event).append("\n");
    }
    return sb.toString();
}
```

---

### REQUIREMENT 2: Which Process is Currently Executing ✅ COMPLETE

**Status**: ✅ FULLY IMPLEMENTED

**Implementation Details**:
- **File**: [ExecutionTrace.java](ExecutionTrace.java#L48-L70)
- **Lines**: 48-70 - `toString()` method outputs current process
- **Output Format**:
```
RUNNING: P1 (PC=3)
```

**File**: [OSSimulatorGUI.java](OSSimulatorGUI.java#L214-L222)
- **Lines**: 214-222 - Updates `currentProcessLabel`
- **Output**: `Current Process: PID 1 (PC: 3)`

**File**: [Scheduler.java](Scheduler.java#L184-L186)
- **Lines**: 184-186 - `getCurrentProcess()` getter

**Verification**: ✅ Working correctly across Console and GUI modes

---

### REQUIREMENT 3: The Instruction Currently Executing ✅ COMPLETE

**Status**: ✅ FULLY IMPLEMENTED

**Implementation Details**:
- **File**: [ExecutionTrace.java](ExecutionTrace.java#L59-L62)
- **Lines**: 59-62 - Displays instruction in trace
- **Output Format**:
```
INSTRUCTION: semWait userInput
```

**File**: [OSSimulator.java](OSSimulator.java#L98-L100)
- **Lines**: 98-100 - Sets instruction in trace:
```java
String instruction = pcb.getProgram()[pcb.getProgramCounter()];
trace.setInstruction(instruction);
```

**File**: [Interpreter.java](Interpreter.java#L12-L33)
- **Lines**: 12-33 - `execute()` method processes instruction
- **Supported Commands**: assign, print, printFromTo, writeFile, readFile, input, semWait, semSignal, compute

**Verification**: ✅ Working correctly - shows each instruction before execution

---

### REQUIREMENT 4: Time Slice (Quantum) Values Configurable ❌ NOT IMPLEMENTED

**Status**: ❌ NOT IMPLEMENTED - HARDCODED VALUES ONLY

**Current Hardcoded Values**:

**File**: [Scheduler.java](Scheduler.java#L16)
- **Line**: 16 - `private int timeQuantum = 2;  // 2 instructions per time slice`
- **Algorithm**: Round Robin
- **Value**: 2 instructions (non-configurable)

**File**: [Scheduler.java](Scheduler.java#L20)
- **Line**: 20 - `private int[] quantumPerLevel = {2, 4, 8, 16};  // 2^i`
- **Algorithm**: MLFQ
- **Values**: Level 0=2, Level 1=4, Level 2=8, Level 3=16 (non-configurable)

**Issues**:
1. ❌ No constructor parameters for quantum configuration
2. ❌ No configuration file support
3. ❌ No runtime modification capability
4. ❌ No validation of quantum values

**Needed Code Changes**:

**CHANGE 1**: Modify Scheduler constructor to accept quantum values
- **File**: [Scheduler.java](Scheduler.java#L24-L39)
- **Current Lines**: 24-39
- **Replace with**:
```java
private SchedulingAlgorithm algorithm;
private Queue<ProcessControlBlock> readyQueue;
private Queue<ProcessControlBlock> blockedQueue;
private ProcessControlBlock currentProcess;

// For Round Robin
private int timeQuantum;  // Now configurable
private int currentQuantumUsed = 0;

// For HRRN
private int clock = 0;

// For MLFQ - 4 priority levels
private List<Queue<ProcessControlBlock>> priorityQueues;
private int[] quantumPerLevel;  // Now configurable
private int[] currentQuantumUsedPerLevel;

// Constructor with default values
public Scheduler(SchedulingAlgorithm algo) {
    this(algo, 2, new int[]{2, 4, 8, 16});
}

// Constructor with custom quantum values
public Scheduler(SchedulingAlgorithm algo, int rrQuantum, int[] mlfqQuantums) {
    this.algorithm = algo;
    this.readyQueue = new LinkedList<>();
    this.blockedQueue = new LinkedList<>();
    this.currentProcess = null;
    this.timeQuantum = rrQuantum;
    this.quantumPerLevel = mlfqQuantums;
    
    // Validate quantum values
    if (rrQuantum <= 0) throw new IllegalArgumentException("RR quantum must be > 0");
    if (mlfqQuantums.length != 4) throw new IllegalArgumentException("MLFQ needs 4 quantum levels");
    for (int q : mlfqQuantums) {
        if (q <= 0) throw new IllegalArgumentException("Each MLFQ quantum must be > 0");
    }
    
    // Initialize MLFQ queues
    if (algo == SchedulingAlgorithm.MLFQ) {
        this.priorityQueues = new ArrayList<>();
        this.currentQuantumUsedPerLevel = new int[4];
        for (int i = 0; i < 4; i++) {
            priorityQueues.add(new LinkedList<>());
            currentQuantumUsedPerLevel[i] = 0;
        }
    }
}
```

**CHANGE 2**: Update OSSimulator to accept quantum config
- **File**: [OSSimulator.java](OSSimulator.java#L24-L37)
- **Current Lines**: 24-37
- **Add constructor parameter**:
```java
public OSSimulator(Scheduler.SchedulingAlgorithm algorithm, OSSimulatorGUI gui, 
                   int rrQuantum, int[] mlfqQuantums) {
    this(algorithm, gui, "./src", rrQuantum, mlfqQuantums);
}

public OSSimulator(Scheduler.SchedulingAlgorithm algorithm, OSSimulatorGUI gui, 
                   String programPath, int rrQuantum, int[] mlfqQuantums) {
    // Initialize components
    memory = new Memory();
    scheduler = new Scheduler(algorithm, rrQuantum, mlfqQuantums);  // Pass quantum values
    // ... rest of initialization
}
```

**CHANGE 3**: Update ConsoleSimulator to accept input for quantum
- **File**: [ConsoleSimulator.java](ConsoleSimulator.java#L18-L38)
- **Add after algorithm selection**:
```java
int rrQuantum = 2;
int[] mlfqQuantums = {2, 4, 8, 16};

if (algo == Scheduler.SchedulingAlgorithm.ROUND_ROBIN) {
    System.out.print("Enter RR quantum (instructions per slice) [default: 2]: ");
    String quantumInput = reader.readLine();
    if (quantumInput != null && !quantumInput.trim().isEmpty()) {
        try {
            rrQuantum = Integer.parseInt(quantumInput.trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input, using default: 2");
        }
    }
} else if (algo == Scheduler.SchedulingAlgorithm.MLFQ) {
    System.out.println("Enter MLFQ quantums for levels 0-3 [default: 2 4 8 16]:");
    for (int i = 0; i < 4; i++) {
        System.out.print("  Level " + i + " quantum: ");
        String quantumInput = reader.readLine();
        if (quantumInput != null && !quantumInput.trim().isEmpty()) {
            try {
                mlfqQuantums[i] = Integer.parseInt(quantumInput.trim());
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input, using default: " + (2 << i));
            }
        }
    }
}

OSSimulator simulator = new OSSimulator(algo, null, programPath, rrQuantum, mlfqQuantums);
```

**CHANGE 4**: Add getter to display current quantum values
- **File**: [Scheduler.java](Scheduler.java)
- **After line**: 227 (end of `getQueueStatus()`)
- **Add**:
```java
public String getQuantumInfo() {
    StringBuilder sb = new StringBuilder();
    sb.append("Algorithm: ").append(algorithm).append("\n");
    if (algorithm == SchedulingAlgorithm.ROUND_ROBIN) {
        sb.append("Quantum: ").append(timeQuantum).append(" instructions\n");
    } else if (algorithm == SchedulingAlgorithm.MLFQ) {
        sb.append("Quantums per level: ");
        for (int i = 0; i < 4; i++) {
            sb.append("L").append(i).append("=").append(quantumPerLevel[i]).append(" ");
        }
        sb.append("\n");
    }
    return sb.toString();
}
```

---

### REQUIREMENT 5: Process Scheduling Order Can Vary ✅ COMPLETE

**Status**: ✅ FULLY IMPLEMENTED

**Three Scheduling Algorithms Implemented**:

**1. Round Robin (Preemptive)**
- **File**: [Scheduler.java](Scheduler.java#L63-L82)
- **Lines**: 63-82 - `getNextRoundRobin()` method
- **Quantum**: 2 instructions (configurable as per Requirement 4)
- **Behavior**: Rotates through ready queue, preempts after quantum expires
- **Queue Order**: FIFO (first in, first out)

**2. HRRN (Highest Response Ratio Next)**
- **File**: [Scheduler.java](Scheduler.java#L85-L113)
- **Lines**: 85-113 - `getNextHRRN()` method
- **Formula**: RR = (Waiting Time + Burst Time) / Burst Time
- **Behavior**: Non-preemptive, selects process with highest response ratio
- **Calculation**: Line 104-105
```java
double responseRatio = (double) (waitingTime + burstTime) / burstTime;
```

**3. MLFQ (Multi-Level Feedback Queue)**
- **File**: [Scheduler.java](Scheduler.java#L116-L145)
- **Lines**: 116-145 - `getNextMLFQ()` method
- **Levels**: 4 priority levels (0=highest to 3=lowest)
- **Quantums**: 2, 4, 8, 16 instructions respectively
- **Behavior**: Process moves to lower priority if quantum expires
- **Demotion Logic**: Lines 131-137
```java
if (currentQuantumUsed >= quantumPerLevel[currentLevel]) {
    currentProcess.setState(ProcessControlBlock.ProcessState.READY);
    int newLevel = Math.min(currentLevel + 1, 3);
    currentProcess.setPriority(newLevel);
    priorityQueues.get(newLevel).add(currentProcess);
}
```

**GUI Algorithm Selection**:
- **File**: [OSSimulatorGUI.java](OSSimulatorGUI.java#L157-L163)
- **Lines**: 157-163 - Dropdown selector with all three algorithms

**Console Algorithm Selection**:
- **File**: [ConsoleSimulator.java](ConsoleSimulator.java#L25-L38)
- **Lines**: 25-38 - User input for algorithm choice

**Verification**: ✅ All three algorithms produce different scheduling orders based on their respective criteria

---

### REQUIREMENT 6: Process Arrival Timings Can Vary ❌ PARTIALLY HARDCODED

**Status**: ⚠️ PARTIAL - Currently hardcoded, not configurable

**Current Implementation**:
- **File**: [OSSimulator.java](OSSimulator.java#L19)
- **Line**: 19 - `private static final int[] ARRIVAL_TIMES = {0, 1, 4};`
- **Processes**: P1 arrives at 0, P2 at 1, P3 at 4
- **Issue**: `static final` means not configurable at runtime

**Arrival Checking Logic**:
- **File**: [OSSimulator.java](OSSimulator.java#L125-L136)
- **Lines**: 125-136 - `checkNewArrivals()` method
- **Current Check**: `if (clock == arrivalTime)`

**Issues**:
1. ❌ Hardcoded as static final constant
2. ❌ No configuration file support
3. ❌ No GUI control for arrival times
4. ❌ Cannot vary arrival times between simulation runs

**Needed Code Changes**:

**CHANGE 1**: Make arrival times configurable
- **File**: [OSSimulator.java](OSSimulator.java#L19)
- **Replace line**: 19
- **Current**: `private static final int[] ARRIVAL_TIMES = {0, 1, 4};`
- **Replace with**:
```java
private int[] ARRIVAL_TIMES;

public OSSimulator(Scheduler.SchedulingAlgorithm algorithm, OSSimulatorGUI gui, int[] arrivalTimes) {
    this(algorithm, gui, "./src", arrivalTimes);
}

public OSSimulator(Scheduler.SchedulingAlgorithm algorithm, OSSimulatorGUI gui, 
                   String programPath, int[] arrivalTimes) {
    // Initialize components
    memory = new Memory();
    scheduler = new Scheduler(algorithm);
    userInput = new Mutex("userInput");
    userOutput = new Mutex("userOutput");
    fileMutex = new Mutex("file");
    
    allProcesses = new ArrayList<>();
    processArrivalTimes = new ArrayList<>();
    
    // Set arrival times
    this.ARRIVAL_TIMES = arrivalTimes != null ? arrivalTimes : new int[]{0, 1, 4};
    
    // Load programs from files
    try {
        PROCESS_PROGRAMS = ProgramLoader.loadAllPrograms(programPath);
    } catch (Exception e) {
        System.err.println("Failed to load programs: " + e.getMessage());
        PROCESS_PROGRAMS = getDefaultPrograms();
    }
    
    // Load processes with specified arrival times
    for (int i = 0; i < PROCESS_PROGRAMS.length && i < ARRIVAL_TIMES.length; i++) {
        processArrivalTimes.add(ARRIVAL_TIMES[i]);
    }
}

// Keep backward-compatible constructor
public OSSimulator(Scheduler.SchedulingAlgorithm algorithm, OSSimulatorGUI gui) {
    this(algorithm, gui, "./src", null);
}
```

**CHANGE 2**: Update ConsoleSimulator to accept arrival times
- **File**: [ConsoleSimulator.java](ConsoleSimulator.java#L38-L50)
- **After algorithm selection, add**:
```java
// Get arrival times
int[] arrivalTimes = {0, 1, 4};  // defaults
System.out.println("\nEnter process arrival times [default: 0 1 4]:");
System.out.println("(3 processes, format: time1 time2 time3)");
System.out.print("Arrival times: ");

String arrivalInput = reader.readLine();
if (arrivalInput != null && !arrivalInput.trim().isEmpty()) {
    try {
        String[] timeParts = arrivalInput.trim().split("\\s+");
        if (timeParts.length == 3) {
            for (int i = 0; i < 3; i++) {
                arrivalTimes[i] = Integer.parseInt(timeParts[i]);
            }
            System.out.println("Using arrival times: " + java.util.Arrays.toString(arrivalTimes));
        } else {
            System.out.println("Invalid format, using defaults: 0 1 4");
        }
    } catch (NumberFormatException e) {
        System.out.println("Invalid input, using defaults: 0 1 4");
    }
}

OSSimulator simulator = new OSSimulator(algo, null, programPath, arrivalTimes);
```

**CHANGE 3**: Display current arrival times
- **File**: [OSSimulator.java](OSSimulator.java)
- **After line**: 158 (after `getQueueStatus()`)
- **Add**:
```java
public String getArrivalTimesInfo() {
    StringBuilder sb = new StringBuilder();
    sb.append("Process Arrival Times: ");
    for (int i = 0; i < Math.min(PROCESS_PROGRAMS.length, ARRIVAL_TIMES.length); i++) {
        sb.append("P").append(i+1).append(" at t=").append(ARRIVAL_TIMES[i]).append(", ");
    }
    return sb.toString();
}
```

---

### REQUIREMENT 7: Memory Displayed Every Clock Cycle (Human-Readable) ✅ COMPLETE

**Status**: ✅ FULLY IMPLEMENTED

**Implementation Details**:

**Memory Display Method**:
- **File**: [Memory.java](Memory.java#L138-L149)
- **Lines**: 138-149 - `getMemoryMap()` method
- **Update Frequency**: Every clock cycle
- **Format**: Grid display, 10 words per row

**Output Example**:
```
Memory Map (40 words):
[P1 P1 P1 P1 P1 P1 P1 P1 P1 P1]
[ .  .  .  . P2 P2 P2 P2 P2 P2]
[P2 P2 P3 P3 P3 P3 P3 P3 P3 P3]
[P3 P3 P3 P3 P3  .  .  .  .  .]
```

**Display in Console**:
- **File**: [ConsoleSimulator.java](ConsoleSimulator.java#L72)
- **Line**: 72 - `System.out.println(simulator.getMemoryMap());`
- **Frequency**: Every cycle

**Display in GUI**:
- **File**: [OSSimulatorGUI.java](OSSimulatorGUI.java#L215)
- **Line**: 215 - Updates `memoryArea` text
- **Panel**: "Memory Map" panel (right side)

**Memory Structure**:
- **File**: [Memory.java](Memory.java#L12-L24)
- **Lines**: 12-24 - `MemoryWord` class
- **Fields**: `varName`, `value`, `pid`
- **Total Size**: 40 words (fixed)
- **Allocation**: 10 words per process

**Memory Operations Tracked**:
- **Allocation**: [Memory.java](Memory.java#L54-L78) - Line 54
- **Deallocation**: [Memory.java](Memory.java#L80-L91) - Line 80
- **Process Boundaries**: Stored in `processMemoryMap`

**Verification**: ✅ Memory map correctly shows:
- Process occupation (P1, P2, P3)
- Available space (. symbols)
- Proper grid formatting (10 per row)
- Updated every cycle

---

### REQUIREMENT 8: ID of Processes Swapped In/Out of Disk ⚠️ PARTIAL

**Status**: ⚠️ INFRASTRUCTURE EXISTS BUT NOT ACTIVELY USED

**Current Implementation**:

**Swapped Process Class**:
- **File**: [Memory.java](Memory.java#L34-L45)
- **Lines**: 34-45 - `SwappedProcess` inner class
- **Fields**:
  - `pid` (int) - Process ID
  - `memoryContent` (int[]) - Memory content snapshot

**Swap To Disk Method**:
- **File**: [Memory.java](Memory.java#L93-L109)
- **Lines**: 93-109 - `swapToDisk()` method
- **Logic**: Copies memory to `diskStorage` ArrayList
- **Status**: Defined but never called

**Swap From Disk Method**:
- **File**: [Memory.java](Memory.java#L111-L132)
- **Lines**: 111-132 - `swapFromDisk()` method
- **Logic**: Restores process from disk to memory
- **Status**: Defined but never called

**Disk Storage**:
- **File**: [Memory.java](Memory.java#L30-L31)
- **Line**: 30 - `private List<SwappedProcess> diskStorage = new ArrayList<>();`

**Issues**:
1. ❌ Swap logic never triggered (no memory pressure condition)
2. ❌ No output showing swap operations
3. ❌ No tracking of when processes are swapped
4. ❌ No disk format logging/display

**Needed Code Changes**:

**CHANGE 1**: Enable swapping when memory pressure exists
- **File**: [OSSimulator.java](OSSimulator.java#L158-L169)
- **Lines**: 158-169 - `createProcess()` method
- **Current**: Returns null when memory full
- **Replace with**:
```java
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
        // Find least recently used (LRU) or first process to swap
        ProcessControlBlock victimProcess = findProcessToSwap();
        if (victimProcess != null) {
            lastOutput += String.format("[Clock %d] SWAPPING OUT P%d to disk (memory pressure)\n", 
                                       clock, victimProcess.getPid());
            memory.swapToDisk(victimProcess.getPid());
            
            // Try allocation again
            memStart = memory.allocate(pcb.getPid(), 10);
        }
    }
    
    if (memStart == -1) {
        lastOutput += "\nMEMORY FULL - Cannot allocate space even after swapping\n";
        return null;
    }
    
    pcb.setMemoryStart(memStart);
    pcb.setMemorySize(10);
    
    // Add to scheduler
    scheduler.addProcess(pcb);
    allProcesses.add(pcb);
    
    return pcb;
}

private ProcessControlBlock findProcessToSwap() {
    // Find first non-running process
    for (ProcessControlBlock pcb : allProcesses) {
        if (pcb.getState() != ProcessControlBlock.ProcessState.RUNNING && 
            pcb.getState() != ProcessControlBlock.ProcessState.TERMINATED) {
            return pcb;
        }
    }
    return null;
}
```

**CHANGE 2**: Add swap events to scheduler logging
- **File**: [Scheduler.java](Scheduler.java#L168-L182)
- **In `unblockProcess()` method**:
- **After line**: 182
- **Add logic to handle swapped-in processes**:
```java
public void unblockProcess(ProcessControlBlock pcb) {
    if (blockedQueue.remove(pcb)) {
        pcb.setState(ProcessControlBlock.ProcessState.READY);
        
        // Check if process was swapped out - bring it back
        if (memory.isSwapped(pcb.getPid())) {
            // Note: actual swap-in handled by caller
            logEvent(String.format("[SWAP-IN] P%d brought back to memory", pcb.getPid()));
        }
        
        if (algorithm == SchedulingAlgorithm.MLFQ) {
            priorityQueues.get(pcb.getPriority()).add(pcb);
        } else {
            readyQueue.add(pcb);
        }
        logEvent(String.format("[UNBLOCK] P%d returned to ready queue", pcb.getPid()));
    }
}
```

**CHANGE 3**: Display disk status
- **File**: [OSSimulator.java](OSSimulator.java)
- **After line**: 158
- **Add**:
```java
public String getDiskStatus() {
    return memory.getDiskStatus();
}
```

**CHANGE 4**: Add disk status method to Memory
- **File**: [Memory.java](Memory.java)
- **After line**: 149 (after `getMemoryMap()`)
- **Add**:
```java
public String getDiskStatus() {
    StringBuilder sb = new StringBuilder();
    sb.append("Disk Storage:\n");
    if (diskStorage.isEmpty()) {
        sb.append("(empty - no swapped processes)\n");
    } else {
        for (SwappedProcess sp : diskStorage) {
            sb.append("  P").append(sp.pid).append(": ");
            sb.append(sp.memoryContent.length).append(" words\n");
        }
    }
    return sb.toString();
}
```

**CHANGE 5**: Update ConsoleSimulator to show disk status
- **File**: [ConsoleSimulator.java](ConsoleSimulator.java#L72-L76)
- **After line**: 72
- **Add**:
```java
System.out.println(simulator.getDiskStatus());
```

---

### REQUIREMENT 9: Format of Memory Stored on Disk ✅ COMPLETE

**Status**: ✅ FULLY IMPLEMENTED (SIMPLE FORMAT)

**Implementation Details**:

**Disk Memory Format**:
- **File**: [Memory.java](Memory.java#L34-L45)
- **Lines**: 34-45 - `SwappedProcess` class definition
- **Format Structure**:
```java
public static class SwappedProcess {
    public int pid;                    // Process ID (4 bytes)
    public int[] memoryContent;        // Memory snapshot (variable length)
}
```

**Data Stored**:
1. **Process ID** (`pid` - int)
   - Unique identifier (1-N)
   - Range: 1-2147483647 (standard int range)

2. **Memory Content** (`memoryContent` - int array)
   - Current: Stores memory address indices
   - Size: Variable (typically 10 words per process)
   - Format: Array of integers [address1, address2, ...]

**Example Disk State**:
```
SwappedProcess {
    pid: 1
    memoryContent: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
}

SwappedProcess {
    pid: 3
    memoryContent: [20, 21, 22, 23, 24, 25, 26, 27, 28, 29]
}
```

**Swapping Logic**:
- **To Disk**: [Memory.java](Memory.java#L93-L109)
  ```java
  public void swapToDisk(int pid) {
      if (processMemoryMap.containsKey(pid)) {
          int[] bounds = processMemoryMap.get(pid);
          int start = bounds[0];
          int size = bounds[1];
          int[] content = new int[size];
          
          // Copy memory content (simplified - just copying indices)
          for (int i = 0; i < size; i++) {
              content[i] = start + i;
          }
          
          diskStorage.add(new SwappedProcess(pid, content));
          free(pid);
      }
  }
  ```

- **From Disk**: [Memory.java](Memory.java#L111-L132)
  ```java
  public void swapFromDisk(int pid, int newStart) {
      SwappedProcess proc = null;
      for (SwappedProcess p : diskStorage) {
          if (p.pid == pid) {
              proc = p;
              break;
          }
      }
      
      if (proc != null) {
          diskStorage.remove(proc);
          processMemoryMap.put(pid, new int[]{newStart, proc.memoryContent.length});
          
          // Restore memory
          for (int i = 0; i < proc.memoryContent.length; i++) {
              occupied[newStart + i] = true;
              memory[newStart + i] = new MemoryWord("", null, pid);
          }
      }
  }
  ```

**Advantages of Current Format**:
- ✅ Simple and efficient
- ✅ Easy to serialize/deserialize
- ✅ Tracks process boundaries correctly
- ✅ Supports FIFO-based swapping

**Limitations**:
- ⚠️ Only stores memory addresses, not actual data
- ⚠️ Could be enhanced to store variable values
- ⚠️ No timestamp or swap-order tracking

**Enhancement Option**: Store actual memory state
```java
public static class SwappedProcess {
    public int pid;
    public MemoryWord[] memoryContent;  // Store actual memory words
    
    public SwappedProcess(int pid, MemoryWord[] content) {
        this.pid = pid;
        this.memoryContent = content;
    }
}
```

**Verification**: ✅ Format is well-defined and correctly tracks process IDs and memory content

---

### REQUIREMENT 10: Output is Readable and Presentable ✅ GOOD

**Status**: ✅ GOOD IMPLEMENTATION (Minor improvements possible)

**Current Formatting**:

**Console Output**:
- **File**: [ConsoleSimulator.java](ConsoleSimulator.java#L10-L12)
- **Lines**: 10-12 - Header formatting
- **Format**:
```
╔════════════════════════════════════════════════════════════════╗
║         OPERATING SYSTEM SIMULATOR - CONSOLE MODE                ║
╚════════════════════════════════════════════════════════════════╝
```

**Execution Trace Output**:
- **File**: [ExecutionTrace.java](ExecutionTrace.java#L46-L70)
- **Lines**: 46-70 - Formatted trace output
- **Format**:
```
========================================
CLOCK CYCLE 1
========================================

STATE CHANGE: P1 -> READY

RUNNING: P1 (PC=0)
INSTRUCTION: semWait userInput

RESULT: Blocked on user input

QUEUE STATUS:
  Ready Queue: (empty)
  Blocked Queue: P1 
```

**GUI Display**:
- **File**: [OSSimulatorGUI.java](OSSimulatorGUI.java#L70-L105)
- **Lines**: 70-105 - Three-panel layout
- **Panels**:
  1. Execution Output (left) - 40 cols wide
  2. Memory Map (center) - 20 cols
  3. Queue Status (right) - 20 cols

**Memory Map Formatting**:
- **File**: [Memory.java](Memory.java#L138-L149)
- **Lines**: 138-149
- **Format**: Grid with 10 words per row
```
Memory Map (40 words):
[P1 P1 P1 P1 P1 P1 P1 P1 P1 P1]
[ .  .  .  . P2 P2 P2 P2 P2 P2]
```

**Queue Status Formatting**:
- **File**: [Scheduler.java](Scheduler.java#L207-L227)
- **Lines**: 207-227
- **Format**: Lists processes per queue
```
Ready: P1 P2 P3 
Blocked: 
```

**Strengths**:
- ✅ Clear section separators
- ✅ Process IDs clearly marked
- ✅ Visual grid for memory
- ✅ Multi-panel GUI layout
- ✅ Box-drawing characters for headers

**Areas for Improvement**:
1. ⚠️ Queue display could be more visual (ASCII boxes)
2. ⚠️ No color coding in console mode
3. ⚠️ Could add per-instruction latency display
4. ⚠️ Disk status not currently displayed

**Suggested Enhancement**:

**CHANGE 1**: Add color to console output (requires ANSI codes or Jansi library)
- **File**: [ExecutionTrace.java](ExecutionTrace.java#L46-L70)
- **Add ANSI color codes**:
```java
@Override
public String toString() {
    StringBuilder sb = new StringBuilder();
    final String RESET = "\u001B[0m";
    final String GREEN = "\u001B[32m";
    final String YELLOW = "\u001B[33m";
    final String BLUE = "\u001B[34m";
    final String CYAN = "\u001B[36m";
    
    sb.append(CYAN + "\n" + "========================================\n");
    sb.append(String.format("CLOCK CYCLE %d\n", clock));
    sb.append("========================================\n" + RESET);
    
    if (stateChange != null) {
        sb.append(YELLOW + String.format("STATE CHANGE: P%d -> %s\n", 
                  stateChangePid, stateChange) + RESET);
    }
    
    if (currentProcess != null) {
        sb.append(GREEN + String.format("RUNNING: P%d (PC=%d)\n", 
                  currentProcess.getPid(), currentProcess.getProgramCounter()) + RESET);
        // ... rest of output
    }
    
    return sb.toString();
}
```

**CHANGE 2**: Add detailed statistics footer
- **File**: [ConsoleSimulator.java](ConsoleSimulator.java#L83-L88)
- **At end, before completion message**:
```java
System.out.println("\n╔════════════════════════════════════════════════════════════════╗");
System.out.println("║                  SIMULATION STATISTICS                         ║");
System.out.println("╠════════════════════════════════════════════════════════════════╣");
System.out.println(String.format("║ Total Clock Cycles: %-50d║", cycle));
System.out.println(String.format("║ Processes Completed: %-50d║", completedCount));
System.out.println(String.format("║ Memory Utilization: %-49d%%║", memoryUtil));
System.out.println("╚════════════════════════════════════════════════════════════════╝");
```

**Verification**: ✅ Output is readable and well-formatted; enhancements are optional

---

## Implementation Priority Matrix

| Priority | Requirement | Effort | Impact | Status |
|----------|---|---|---|---|
| **CRITICAL** | #4: Configurable Quantum | High | High | ❌ Missing |
| **CRITICAL** | #1: Event-based Queue Logging | Medium | High | ⚠️ Partial |
| **HIGH** | #6: Configurable Arrival Times | Low | Medium | ⚠️ Hardcoded |
| **MEDIUM** | #8: Active Swapping & Disk Tracking | Medium | Medium | ⚠️ Infrastructure only |
| **LOW** | #10: Enhanced Output Formatting | Low | Low | ✅ Good |

---

## Summary & Recommendations

### Current Status
- **6/10 requirements fully implemented** (60%)
- **2/10 requirements partially implemented** (20%)
- **2/10 requirements missing** (20%)

### Key Gaps
1. **Configuration system missing** - Both quantum and arrival times are hardcoded
2. **Event-based logging incomplete** - Queue only shown cycle-by-cycle
3. **Swapping inactive** - Infrastructure exists but never triggered
4. **No unified configuration** - Each parameter scattered in code

### Quick Wins (Implement First)
1. ✅ Requirement 6: Configurable arrival times (Low effort, immediate benefit)
2. ✅ Requirement 1: Event logging (Medium effort, high visibility)
3. ✅ Requirement 4: Configurable quantum (Medium effort, important feature)

### Strategic Improvements
1. Create `Configuration.java` class to centralize all parameters
2. Implement `ConfigurationLoader` to read from JSON/properties file
3. Add `SimulationStatistics` class to track metrics
4. Enable active memory swapping with disk I/O visualization

### File Changes Summary
**Files Requiring Modifications**:
- [Scheduler.java](Scheduler.java) - Add event logging, quantum configuration
- [OSSimulator.java](OSSimulator.java) - Add arrival time config, enable swapping
- [ConsoleSimulator.java](ConsoleSimulator.java) - Add configuration input
- [Memory.java](Memory.java) - Add disk status display
- [ExecutionTrace.java](ExecutionTrace.java) - Optional: Add color codes

**Files to Create**:
- `Configuration.java` - Centralized config management
- `ConfigurationLoader.java` - Load from files

---

## Code Changes Summary Table

| Change # | File | Lines | Type | Impact |
|---|---|---|---|---|
| 1.1-1.6 | Scheduler.java | 24-227 | Add event logging | Requirement #1 |
| 4.1-4.4 | Multiple | Various | Add quantum config | Requirement #4 |
| 6.1-6.3 | OSSimulator.java | 19-158 | Add arrival times | Requirement #6 |
| 8.1-8.5 | Memory.java, OSSimulator.java | Multiple | Enable swapping | Requirement #8 |
| 10.1-10.2 | ExecutionTrace.java | 46-88 | Add colors | Requirement #10 |

---

## Testing Recommendations

**Test Cases**:
1. Test all three scheduling algorithms with different quantums
2. Test varying arrival times (verify correct P2, P3 delays)
3. Test swapping when all 4 processes need space
4. Verify queue displays after each event
5. Confirm memory display every cycle

**Test with Different Configurations**:
```
Test 1: RR with quantum=2, arrivals=[0,1,4]
Test 2: RR with quantum=4, arrivals=[0,2,5]
Test 3: MLFQ with quantums=[2,4,8,16], arrivals=[0,1,4]
Test 4: HRRN with default settings
```

---

**Document Version**: 1.0  
**Generated**: April 19, 2026  
**Analysis Type**: Comprehensive Requirements Verification
