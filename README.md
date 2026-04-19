src/
├── Memory.java              # 40-word memory management with swapping
├── ProcessControlBlock.java # PCB data structure with process metadata
├── Mutex.java               # Mutual exclusion with blocked queue
├── Scheduler.java           # RR, HRRN, and MLFQ scheduling algorithms
├── Interpreter.java         # Command execution and system calls
├── OSSimulator.java         # Simulation orchestrator
├── OSSimulatorGUI.java      # Java Swing GUI for visualization
└── Operating_Systems.java   # Legacy main class

# Operating Systems Simulator

A comprehensive Java-based simulator that models a real operating system with process scheduling, memory management, mutual exclusion, and file I/O operations.

## Table of Contents
1. [Project Overview](#project-overview)
2. [Execution Pipeline](#execution-pipeline)
3. [Architecture & Class Structure](#architecture--class-structure)
4. [Detailed Method Explanations](#detailed-method-explanations)
5. [How to Run](#how-to-run)
6. [Scheduling Algorithms](#scheduling-algorithms)
7. [System Design](#system-design)

---

## Project Overview

This Operating System Simulator implements core OS concepts:
- **Process Scheduling**: Three scheduling algorithms (Round Robin, HRRN, MLFQ)
- **Memory Management**: Fixed 40-word memory array with process allocation
- **Mutual Exclusion**: Semaphore-based mutex implementation with three resources
- **Program Execution**: Load and execute programs with system calls
- **I/O Operations**: File read/write and user input/output

### Key Features
- Real-time process visualization
- Detailed execution tracing
- Mutex blocking/unblocking
- Memory swapping simulation
- Three distinct programs executing concurrently

---

## Execution Pipeline

### High-Level Flow: Input → Processing → Output

```
┌─────────────────────────────────────────────────────────────────────┐
│  INPUT: Program Selection & Process Creation                        │
│  ├─ User selects scheduling algorithm (RR, HRRN, or MLFQ)          │
│  └─ Programs loaded from disk (Program_1.txt, Program_2.txt, etc)  │
└────────────────────┬────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────────────────────┐
│  INITIALIZATION: OSSimulator Setup                                   │
│  ├─ Create Memory (40-word array)                                   │
│  ├─ Initialize Scheduler (with selected algorithm)                  │
│  ├─ Create three Mutexes (userInput, userOutput, file)             │
│  └─ Load processes from files (ProgramLoader)                       │
└────────────────────┬────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────────────────────┐
│  SIMULATION LOOP: Clock Cycles (50 max)                             │
│  For each clock cycle:                                              │
│  ├─ Check new process arrivals (OSSimulator.checkNewArrivals)      │
│  ├─ Select next process (Scheduler.getNextProcess)                 │
│  ├─ Execute instruction (OSSimulator.executeInstruction)           │
│  │  ├─ Retrieve instruction from Process Control Block             │
│  │  ├─ Pass to Interpreter for execution                           │
│  │  │  ├─ Parse command                                            │
│  │  │  ├─ Execute (assign, semWait, print, etc.)                   │
│  │  │  └─ Handle mutex/resource access                            │
│  │  ├─ Check if blocked (state = BLOCKED)                         │
│  │  └─ If not blocked, increment PC and return to ready queue     │
│  ├─ Update process state in scheduler                              │
│  └─ Create execution trace (ExecutionTrace)                        │
└────────────────────┬────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────────────────────┐
│  OUTPUT: Real-time Display & Logging                                │
│  ├─ Print clock cycle number                                        │
│  ├─ Show running process & instruction                              │
│  ├─ Display queue status (ready/blocked)                            │
│  ├─ Show memory map (40 words)                                      │
│  ├─ Log execution trace                                             │
│  └─ Output: "Output: 1 2 3 4 5 6 7 8 9 10" (Program 1 result)     │
└────────────────────┬────────────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────────────────────┐
│  TERMINATION: Cleanup & Summary                                      │
│  ├─ All processes reach PC >= program length                        │
│  ├─ Memory freed for terminated processes                           │
│  └─ Display: "All processes terminated"                             │
└─────────────────────────────────────────────────────────────────────┘
```

### Detailed Step-by-Step Execution Example

**Clock Cycle 1:**
```
1. OSSimulator.checkNewArrivals() → Detects P1 arrives at time 0
  └─ OSSimulator.createProcess(0) → Creates ProcessControlBlock with Program 1
    └─ Memory.allocate(1, 10) → Allocates 10 words in memory for P1
    └─ Scheduler.addProcess(pcb) → Adds P1 to ready queue

2. Scheduler.getNextProcess() → Returns P1 from ready queue
  └─ Uses Round Robin algorithm (if selected)

3. OSSimulator.executeInstruction(P1, trace)
  └─ Retrieves instruction: "semWait userInput"
  └─ Interpreter.execute("semWait userInput", P1)
    └─ Interpreter.executeSemWait("userInput", P1)
      └─ userInput.tryLock(P1) → Returns true (mutex free)
      └─ Returns "Acquired mutex: userinput"
  └─ P1 not blocked → increment PC (0 → 1)
  └─ Scheduler.addProcess(P1) → Return P1 to ready queue

4. ExecutionTrace.getDetailedTrace()
  └─ Formats output with clock, process, instruction, result

5. Console output:
  ```
  CLOCK CYCLE 1
  RUNNING: P1 (PC=1)
  INSTRUCTION: semWait userInput
  RESULT: Acquired mutex: userinput
  [Clock 1] P1 executed OK (PC now 1)
  ```
```

**Clock Cycle 2:**
```
1. Scheduler.getNextProcess() → Returns P1 again (2-instruction quantum for RR)

2. OSSimulator.executeInstruction(P1, trace)
  └─ Retrieves instruction: "assign x input"
  └─ Interpreter.execute("assign x input", P1)
    └─ Interpreter.executeAssign("x input", P1)
      └─ Interpreter.readUserInput("x", P1)
        └─ Generates input value based on P1: "1"
        └─ P1.setVariable("x", 1)
        └─ Returns "Read input: x = 1"
  └─ P1 not blocked → increment PC (1 → 2)
  └─ Scheduler.addProcess(P1) → Return to ready queue

3. Console output shows P1 successfully executed "assign x input"
```

---

## Architecture & Class Structure

### Class Overview & Responsibilities

#### 1. **ConsoleSimulator.java** - User Interface & Entry Point
**Purpose**: Provides a console-based interface for users to select scheduling algorithm and run the simulation.

**Key Methods**:
- `main(String[] args)`: Entry point; prompts user for scheduling choice, initializes OSSimulator
- `run()`: Loads programs, starts simulation loop, displays output
- `displayMenu()`: Shows algorithm selection options (1=RR, 2=HRRN, 3=MLFQ)
- `parseSchedulerChoice(int choice)`: Returns scheduler algorithm name as string

**Data Flow**:
```
User Input (1, 2, or 3)
    ↓
ConsoleSimulator.main() 
    ↓
OSSimulator initialization with choice
    ↓
Simulation runs for 50 clock cycles
    ↓
Console output with execution traces
```

---

#### 2. **OSSimulator.java** - Simulation Orchestrator (Core Engine)
**Purpose**: Main simulation loop that coordinates all OS components and manages clock cycles.

**Key Methods**:

- **`OSSimulator(String schedulerName)`** - Constructor
  - Input: Scheduler algorithm name ("RoundRobin", "HRRN", "MLFQ")
  - Creates: Memory, Scheduler, Mutexes (userInput, userOutput, file)
  - Initializes: Process arrival queue, execution traces

- **`stepSimulation()`** - Executes one clock cycle (called 50 times)
  - **Process Flow**:
    ```
    1. checkNewArrivals() → Create new processes if arrival time reached
    2. Scheduler.getNextProcess() → Get next process to run
    3. executeInstruction(pcb) → Run one instruction of selected process
    4. Store execution trace for output
    5. Clock increments
    ```
  - **Output**: ExecutionTrace object with clock, process, instruction, result

- **`checkNewArrivals()`** - Creates processes when they arrive
  - Checks current clock against process arrival times (0, 1, 4)
  - Calls `createProcess(programIndex)` for each arriving process
  - **Flow**:
    ```
    if (clock == 0) createProcess(0)  // P1 arrives
    if (clock == 1) createProcess(1)  // P2 arrives
    if (clock == 4) createProcess(2)  // P3 arrives
    ```

- **`createProcess(int programIndex)`** - Allocates resources for new process
  - **Input**: Program index (0, 1, or 2)
  - **Processing**:
    1. `Memory.allocate(processIndex, 10)` → Reserve 10 words for process
    2. Create ProcessControlBlock with program loaded from disk
    3. Set initial state to READY
    4. `Scheduler.addProcess(pcb)` → Add to ready queue
  - **Output**: Process ready in scheduler queue

- **`executeInstruction(ProcessControlBlock pcb, ExecutionTrace trace)`** - Runs one instruction
  - **Input**: Process Control Block, execution trace object
  - **Processing**:
    1. Retrieve current instruction at `pcb.pc` (program counter)
    2. Call `Interpreter.execute(instruction, pcb)` to execute
    3. Check if process got blocked (state == BLOCKED)
    4. If not blocked: increment PC and add back to ready queue
    5. If blocked: stay in blocked queue until unblocked
  - **Output**: ExecutionTrace with instruction result, or BLOCKED state

- **`allProcessesDone()`** - Termination check
  - Returns true if all processes have terminated
  - Checked in simulation loop to end simulation

**Data Structures**:
- `Memory memory` - 40-word memory array
- `Scheduler scheduler` - Process queue manager
- `Mutex userInput, userOutput, fileAccess` - Resource locks
- `List<ExecutionTrace> traces` - Execution history

**Execution Sequence Diagram**:
```
Clock 0: 
  ├─ checkNewArrivals() → Create P1
  ├─ getNextProcess() → Select P1
  ├─ executeInstruction(P1) → Run first instruction
  └─ Output trace

Clock 1:
  ├─ checkNewArrivals() → Create P2
  ├─ getNextProcess() → Select next process
  ├─ executeInstruction() → Run instruction
  └─ Output trace
```

---

#### 3. **Interpreter.java** - Instruction Executor & System Call Handler
**Purpose**: Parses and executes all program instructions, handles system calls and I/O.

**Key Methods**:

- **`execute(String line, ProcessControlBlock pcb)`** - Main dispatcher
  - **Input**: Instruction string (e.g., "semWait userInput"), Process Control Block
  - **Processing**: Parses instruction type and calls appropriate method
  - **Instruction Types**:
    ```
    "semWait <mutex>" → executeSemWait()
    "semSignal <mutex>" → executeSemSignal()
    "assign <var> <value/input>" → executeAssign()
    "print <var>" → executePrint()
    "readFile <var>" → executeReadFile()
    "writeFile <filename>" → executeWriteFile()
    "printFromTo <var1> <var2>" → executePrintFromTo()
    ```
  - **Output**: Result string describing execution

- **`executeSemWait(String mutexName, ProcessControlBlock pcb)`** - Mutex acquisition
  - **Input**: Mutex name ("userInput", "userOutput", "file"), Process
  - **Processing**:
    1. Get mutex object by name
    2. Call `mutex.tryLock(pcb)` to acquire
    3. If acquired: return success message
    4. If not: `pcb.setState(BLOCKED)`, add to mutex queue, process blocks
  - **Output**: "Acquired mutex: {name}" or blocks process

- **`executeSemSignal(String mutexName, ProcessControlBlock pcb)`** - Mutex release
  - **Input**: Mutex name, current Process (lock holder)
  - **Processing**:
    1. Get mutex object by name
    2. Call `mutex.signalAndUnblock(pcb)` to release and transfer lock
    3. If process waiting: unblock it and transfer lock ownership
    4. Call `scheduler.unblockProcess()` to move from blocked to ready
  - **Output**: "Released mutex: {name}" + unblocked process name

- **`executeAssign(String args, ProcessControlBlock pcb)`** - Variable assignment
  - **Input**: "x 5" or "x input", Process
  - **Processing**:
    1. Parse variable name and value
    2. If value is "input": call `readUserInput(varName, pcb)`
    3. Otherwise: set variable to literal value
    4. `pcb.setVariable(varName, value)`
  - **Output**: "Assigned {var} = {value}"

- **`readUserInput(String varName, ProcessControlBlock pcb)`** - Simulated input
  - **Input**: Variable name, Process
  - **Processing**: Generates input based on process and variable position
    - For P1: returns sequential values 1, 2, 3, ... (for range printing)
    - For P2: returns "Hello World" (for file writing)
    - For P3: returns parsed values from file
  - **Output**: Generated input value
  - **Note**: This does NOT block; blocking happens at semWait level

- **`executePrint(String args, ProcessControlBlock pcb)`** - Single variable output
  - **Input**: Variable name, Process
  - **Processing**:
    1. Retrieve variable value from `pcb.getVariable(varName)`
    2. Send to output stream
  - **Output**: Displays value + stores in output buffer

- **`executePrintFromTo(String args, ProcessControlBlock pcb)`** - Range output
  - **Input**: "1 10", Process
  - **Processing**:
    1. Parse start and end values
    2. Loop from start to end
    3. For each iteration: call `readUserInput()` and print value
    4. Generates: "Output: 1 2 3 4 5 6 7 8 9 10"
  - **Output**: Formatted range output

- **`executeReadFile(String args, ProcessControlBlock pcb)`** - File input
  - **Input**: File name, variable name, Process
  - **Processing**:
    1. Read from file (e.g., "a")
    2. Parse content
    3. Store in process variable
  - **Output**: File content stored in variable

- **`executeWriteFile(String args, ProcessControlBlock pcb)`** - File output
  - **Input**: File name, content, Process
  - **Processing**:
    1. Write content to file
    2. Verify write successful
  - **Output**: File updated with data

**Data Flow**:
```
Instruction String (e.g., "semWait userInput")
    ↓
execute() → Parse instruction type
    ↓
Call specific handler (executeSemWait, executeAssign, etc.)
    ↓
Interact with Mutex/Memory/Scheduler as needed
    ↓
Return result string
    ↓
OSSimulator logs result in ExecutionTrace
```

---

#### 4. **Scheduler.java** - Process Queue Manager
**Purpose**: Manages process queues (ready/blocked) and implements scheduling algorithms.

**Scheduling Algorithms**:

1. **Round Robin (RR)**
   - Quantum: 2 instructions
   - Process: Execute process for 2 instructions, then move to back of queue
   - Method: `getNextRoundRobin()`
   - Fair allocation of CPU time

2. **HRRN (Highest Response Ratio Next)**
   - Formula: Response Ratio = (Wait Time + Burst Time) / Burst Time
   - Process: Calculate ratio for all processes, select highest
   - Method: `getNextHRRN()`
   - Minimizes average wait time

3. **MLFQ (Multi-Level Feedback Queue)**
   - Levels: 4 priority queues (0=highest, 3=lowest)
   - Quantum per level: 2, 4, 8, 16 instructions
   - Demotion: If process uses full quantum, move to lower priority
   - Method: `getNextMLFQ()`
   - Adapts to process behavior

**Key Methods**:

- **`getNextProcess()`** - Main dispatcher
  - Determines which scheduling algorithm to use
  - Calls appropriate method (Round Robin, HRRN, or MLFQ)
  - **Output**: Next ProcessControlBlock to run

- **`getNextRoundRobin()`** - Round Robin scheduling
  - **Processing**:
    1. Get first process from ready queue
    2. Increment instruction counter
    3. If counter == 2: move to back of queue, reset counter
    4. Return process
  - **Output**: Next process (or rotated same process)

- **`getNextHRRN()`** - HRRN scheduling
  - **Processing**:
    1. Calculate response ratio for all ready processes
    2. Select process with highest ratio
    3. Remove from queue permanently (non-preemptive)
  - **Output**: Selected process

- **`getNextMLFQ()`** - MLFQ scheduling
  - **Processing**:
    1. Check priority level 0; if has process, return it
    2. Otherwise check level 1, 2, 3 in order
    3. Track instruction count for each process
    4. If instructions exceed quantum for level: demote to next level
  - **Output**: Next process from highest priority non-empty queue

- **`addProcess(ProcessControlBlock pcb)`** - Add to ready queue
  - Input: Process to add
  - Processing: Add to ready queue, set state to READY
  - Output: Process now available for scheduling

- **`blockProcess(ProcessControlBlock pcb)`** - Move to blocked queue
  - Input: Process to block
  - Processing: Remove from ready, add to blocked queue, set state to BLOCKED

- **`unblockProcess(ProcessControlBlock pcb)`** - Move back to ready queue
  - Input: Process to unblock
  - Processing: Remove from blocked queue, add to ready queue, set state to READY
  - Called when: Mutex is released, I/O completes

**Queue Management**:
```
Ready Queue: [P1, P2, P3, P1, P3, ...]  (processes waiting to run)
Blocked Queue: [P2, P3]                  (processes waiting for resources)
```

---

#### 5. **ProcessControlBlock (PCB).java** - Process Metadata Container
**Purpose**: Stores all metadata and state for a process.

**Key Attributes**:
- `int processId` - Process number (1, 2, 3)
- `ProcessState state` - Current state (READY, RUNNING, BLOCKED, TERMINATED)
- `int pc` (Program Counter) - Current instruction index
- `List<String> program` - Instructions loaded from file
- `HashMap<String, Integer> variables` - Process variables (x, y, a, b, etc.)
- `int memoryStartAddress` - Start of allocated memory
- `int memorySize` - Size of allocated memory (10 words)
- `int creationTime` - Clock cycle when process created
- `int waitTime` - Time spent waiting

**Key Methods**:

- **`ProcessControlBlock(int id, List<String> program, int memStart, int memSize)`** - Constructor
  - Initializes process with program and memory allocation

- **`setState(ProcessState state)`** - Update process state
  - Sets state: READY → RUNNING → BLOCKED → TERMINATED

- **`setVariable(String name, int value)`** - Store variable value
  - Stores in `variables` HashMap

- **`getVariable(String name)`** - Retrieve variable value
  - Returns value or error if not found

- **`nextInstruction()`** - Get current instruction and increment PC
  - Returns instruction at current PC
  - Increments PC for next cycle

- **`terminate()`** - Mark process as complete
  - Sets state to TERMINATED
  - Flags for memory deallocation

---

#### 6. **Mutex.java** - Mutual Exclusion Lock
**Purpose**: Implements semaphore-style locking with process blocking/unblocking.

**Key Attributes**:
- `ProcessControlBlock lockHolder` - Which process currently holds lock (null if free)
- `Queue<ProcessControlBlock> waitQueue` - Processes waiting for lock
- `String name` - Mutex identifier ("userInput", "userOutput", "file")

**Key Methods**:

- **`Mutex(String name)`** - Constructor
  - Initializes empty lock and wait queue

- **`tryLock(ProcessControlBlock pcb)`** - Attempt to acquire lock
  - **Input**: Process attempting to lock
  - **Processing**:
    1. If `lockHolder == null`: 
       - Set `lockHolder = pcb`
       - Return true (lock acquired)
    2. Else: 
       - Return false (lock unavailable)
  - **Output**: boolean (true=acquired, false=unavailable)
  - **Note**: Caller is responsible for blocking process if false

- **`signalAndUnblock(ProcessControlBlock releaser)`** - Release lock and unblock waiting process
  - **Input**: Current lock holder (process releasing lock)
  - **Processing**:
    1. Verify releaser holds lock (lockHolder == releaser)
    2. If wait queue is empty:
       - Set `lockHolder = null` (lock now free)
    3. If wait queue has processes:
       - Remove first process from queue
       - Transfer ownership: `lockHolder = nextProcess`
       - Return nextProcess to signal caller for unblocking
  - **Output**: Next process to unblock (or null if queue empty)
  - **Critical**: Ensures lock always has owner if processes waiting

- **`block(ProcessControlBlock pcb)`** - Add process to wait queue
  - Input: Process to block
  - Processing: Add to `waitQueue`
  - Output: Process waiting for lock

**Blocking/Unblocking Sequence**:
```
Process P1 tries to acquire mutex:
  tryLock(P1) → false (already held by P2)
  ├─ P1 state = BLOCKED
  ├─ block(P1) → Add P1 to waitQueue
  └─ P1 moves to Scheduler blocked queue

Later, P2 releases mutex:
  signalAndUnblock(P2) → Returns P1
  ├─ P1 state = READY
  ├─ lockHolder = P1 (ownership transferred)
  └─ P1 moves to Scheduler ready queue

Next, P1 can execute semWait again:
  tryLock(P1) → true (P1 now holds lock)
  └─ P1 continues execution
```

---

#### 7. **Memory.java** - Memory Management
**Purpose**: Manages 40-word fixed memory array with allocation/deallocation.

**Key Attributes**:
- `int[] memory` - 40-word array (40 locations × 1 word each)
- `boolean[] allocated` - Track which words are in use
- `int[] processMap` - Track which process owns each word

**Key Methods**:

- **`allocate(int processId, int size)`** - Reserve memory for process
  - **Input**: Process ID, size (10 words per process)
  - **Processing**:
    1. Find first contiguous free block of size 10
    2. Mark as allocated for this process
    3. Store process ID mapping
  - **Output**: Start address of allocation (0, 10, 20, or 30)

- **`deallocate(int processId)`** - Free process memory
  - **Input**: Process ID
  - **Processing**: 
    1. Find all memory locations owned by process
    2. Mark as free
    3. Clear process map
  - **Output**: Memory freed

- **`write(int address, int value)`** - Store value at address
  - Input: Address (0-39), value to store
  - Processing: `memory[address] = value`

- **`read(int address)`** - Retrieve value at address
  - Input: Address
  - Output: Value at that address

- **`isFree(int address)`** - Check if address is available
  - Input: Address
  - Output: boolean (true if free)

**Memory Map Example**:
```
Words 0-9:   P1 memory (process 1)
Words 10-19: P2 memory (process 2)
Words 20-29: P3 memory (process 3)
Words 30-39: Free/Unallocated
```

---

#### 8. **ExecutionTrace.java** - Execution History Logging
**Purpose**: Records details of each instruction execution for output and debugging.

**Key Attributes**:
- `int clockCycle` - When instruction executed
- `ProcessControlBlock process` - Which process
- `String instruction` - What instruction
- `String result` - Execution result
- `ProcessState stateAfter` - Process state after execution

**Key Methods**:

- **`ExecutionTrace(int clock, ProcessControlBlock pcb, String instr, String result, ProcessState state)`** - Constructor
  - Stores all execution details

- **`getDetailedTrace()`** - Format for output display
  - Returns formatted string with all trace information
  - Used by ConsoleSimulator for display

**Example Output**:
```
CLOCK CYCLE 5
RUNNING: P2 (PC=2)
INSTRUCTION: semWait userOutput
RESULT: Acquired mutex: userOutput
[Clock 5] P2 executed OK (PC now 2)
```

---

## Detailed Method Call Flows

### Flow 1: Process Creation at Time 0

```
ConsoleSimulator.run()
  ├─ OSSimulator.stepSimulation()
  │   ├─ checkNewArrivals()
  │   │   ├─ if (clock == 0) true
  │   │   ├─ createProcess(0)
  │   │   │   ├─ Memory.allocate(1, 10) → returns 0 (address 0-9)
  │   │   │   ├─ new ProcessControlBlock(1, program1, 0, 10)
  │   │   │   │   └─ Load "Program_1.txt" into program list
  │   │   │   ├─ pcb.setState(READY)
  │   │   │   └─ Scheduler.addProcess(pcb)
  │   │   │       └─ Add P1 to ready queue
  │   │   └─ Display: "P1 created at time 0"
```

### Flow 2: Mutex Acquisition (semWait)

```
OSSimulator.executeInstruction(P1, trace)
  ├─ instruction = "semWait userInput"
  ├─ Interpreter.execute("semWait userInput", P1)
  │   ├─ Parse: command="semWait", arg="userInput"
  │   ├─ executeSemWait("userInput", P1)
  │   │   ├─ mutex = userInput
  │   │   ├─ mutex.tryLock(P1)
  │   │   │   ├─ if (lockHolder == null) true
  │   │   │   ├─ lockHolder = P1
  │   │   │   └─ return true
  │   │   ├─ (true → lock acquired)
  │   │   ├─ result = "Acquired mutex: userInput"
  │   │   └─ P1 continues (NOT blocked)
  │   └─ return result
  ├─ if (P1.state != BLOCKED) true
  ├─ pcb.incrementPC() → PC: 0→1
  └─ Scheduler.addProcess(P1) → Back to ready queue
```

### Flow 3: Mutex Release (semSignal) & Unblocking

```
OSSimulator.executeInstruction(P2, trace)
  ├─ instruction = "semSignal userInput"
  ├─ Interpreter.execute("semSignal userInput", P2)
  │   ├─ executeSemSignal("userInput", P2)
  │   │   ├─ mutex = userInput
  │   │   ├─ nextToUnblock = mutex.signalAndUnblock(P2)
  │   │   │   ├─ verify P2 holds lock ✓
  │   │   │   ├─ if (waitQueue not empty) true
  │   │   │   ├─ remove first from waitQueue → P1
  │   │   │   ├─ lockHolder = P1 (ownership transfer)
  │   │   │   └─ return P1
  │   │   ├─ (P1 returned → unblock this process)
  │   │   ├─ P1.setState(READY)
  │   │   ├─ Scheduler.unblockProcess(P1)
  │   │   │   └─ Move P1 from blocked to ready queue
  │   │   ├─ result = "Released mutex: userInput, Unblocked P1"
  │   │   └─ return result
  │   └─ return result
  ├─ P2.incrementPC()
  └─ Scheduler.addProcess(P2)
```

### Flow 4: Variable Assignment with Input

```
OSSimulator.executeInstruction(P1, trace)
  ├─ instruction = "assign x input"
  ├─ Interpreter.execute("assign x input", P1)
  │   ├─ executeAssign("x input", P1)
  │   │   ├─ parse: var="x", value="input"
  │   │   ├─ if (value == "input") true
  │   │   ├─ inputVal = readUserInput("x", P1)
  │   │   │   ├─ (simulated based on P1 and iteration)
  │   │   │   ├─ Generate: 1 (first input for P1)
  │   │   │   └─ return 1
  │   │   ├─ P1.setVariable("x", 1)
  │   │   │   └─ variables["x"] = 1
  │   │   ├─ result = "Assigned x = 1"
  │   │   └─ return result
  │   └─ return result
  ├─ P1.incrementPC()
  └─ Scheduler.addProcess(P1)
```

### Flow 5: Range Output (printFromTo)

```
OSSimulator.executeInstruction(P1, trace)
  ├─ instruction = "printFromTo 1 10"
  ├─ Interpreter.execute("printFromTo 1 10", P1)
  │   ├─ executePrintFromTo("1 10", P1)
  │   │   ├─ start=1, end=10
  │   │   ├─ output = ""
  │   │   ├─ for (i=1; i<=10; i++)
  │   │   │   ├─ val = readUserInput("i", P1)
  │   │   │   │   └─ generates: 1, 2, 3, ..., 10
  │   │   │   └─ output += val + " "
  │   │   ├─ result = "Output: 1 2 3 4 5 6 7 8 9 10 "
  │   │   └─ Store in output buffer
  │   └─ return result
  ├─ P1.incrementPC()
  └─ Scheduler.addProcess(P1)
```

### Flow 6: Process Termination

```
OSSimulator.stepSimulation()
  ├─ executeInstruction(P1, trace)
  │   ├─ P1.pc = 14 (last instruction index)
  │   ├─ Execute last instruction
  │   ├─ P1.incrementPC() → pc=15
  │   ├─ if (P1.pc >= P1.program.length) true (15 >= 15)
  │   │   ├─ P1.setState(TERMINATED)
  │   │   ├─ Memory.deallocate(1)
  │   │   │   └─ Free memory addresses 0-9
  │   │   └─ Do NOT add back to ready queue
  │   └─ ExecutionTrace shows: "P1 TERMINATED"
  │
  ├─ Later: allProcessesDone() check
  │   ├─ All P1, P2, P3 states == TERMINATED
  │   └─ return true → End simulation
  │
  └─ Output: "All processes terminated - Simulation complete"
```

---

## How to Run

### Step 1: Compilation
```bash
cd src/
javac -d ../bin *.java
```

### Step 2: Execution
```bash
cd ../
java -cp bin ConsoleSimulator
```

### Step 3: User Interaction
```
Select Scheduling Algorithm:
1. Round Robin (2-instruction quantum)
2. HRRN (Highest Response Ratio Next)
3. MLFQ (Multi-Level Feedback Queue)

Enter choice (1-3): 1
```

### Step 4: Simulation Output
The simulator runs for up to 50 clock cycles and displays:
- Clock cycle number
- Currently running process and instruction
- Execution result
- Queue status (ready/blocked processes)
- Memory map (40 words)
- Final output and termination message

---

## Scheduling Algorithms

### 1. Round Robin (RR)
```
Algorithm: Rotate through ready queue, each process gets 2 instructions
Quantum: 2

Example with P1, P2, P3:
Clock 0: Execute P1 (Inst 1)
Clock 1: Execute P1 (Inst 2)     ← Used full quantum
Clock 2: Execute P2 (Inst 1)     ← P1 to back of queue
Clock 3: Execute P2 (Inst 2)
Clock 4: Execute P3 (Inst 1)
Clock 5: Execute P1 (Inst 3)     ← P1 rotated back

Pros: Fair CPU allocation, prevents starvation
Cons: Higher context switch overhead
```

### 2. HRRN (Highest Response Ratio Next)
```
Algorithm: Select process with highest response ratio
Formula: Response Ratio = (Waiting Time + Service Time) / Service Time

Example:
Process A: Waiting 5ms, Service 2ms → Ratio = (5+2)/2 = 3.5
Process B: Waiting 3ms, Service 3ms → Ratio = (3+3)/3 = 2.0
→ Select Process A (higher ratio)

Pros: Minimizes average wait time, favors shorter jobs
Cons: Non-preemptive, requires service time estimation
```

### 3. MLFQ (Multi-Level Feedback Queue)
```
Algorithm: 4 priority levels, processes promoted/demoted based on behavior

Level 0: Quantum = 2 instructions  (Highest priority)
Level 1: Quantum = 4 instructions
Level 2: Quantum = 8 instructions
Level 3: Quantum = 16 instructions (Lowest priority)

Promotion: If process doesn't use full quantum → stay at same level
Demotion: If process uses full quantum → move to lower priority level

Example:
Clock 0: P1 at Level 0, uses 1 instr → stays Level 0
Clock 1: P1 at Level 0, uses 2 instr → demotes to Level 1
Clock 2: P2 at Level 0, uses 2 instr → demotes to Level 1
...

Pros: Adapts to process behavior, balances interactive/batch jobs
Cons: Complex implementation, potential starvation at Level 3
```

---

## System Design & Data Flow

### Complete Data Flow Diagram

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         INPUT LAYER                                      │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ User selects scheduling algorithm (ConsoleSimulator)            │    │
│  │ Input: 1 (RR), 2 (HRRN), or 3 (MLFQ)                           │    │
│  └────────────────────────┬────────────────────────────────────────┘    │
└─────────────────────────────┼────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                    INITIALIZATION LAYER                                  │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ OSSimulator created with scheduler algorithm                    │    │
│  │ ├─ Memory: Allocate 40-word array                              │    │
│  │ ├─ Scheduler: Initialize with RR/HRRN/MLFQ                    │    │
│  │ ├─ Mutexes: Create 3 (userInput, userOutput, file)            │    │
│  │ └─ Traces: Empty execution history                            │    │
│  └────────────────────────┬────────────────────────────────────────┘    │
└─────────────────────────────┼────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                    PROCESS CREATION (Time 0)                             │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ Clock 0: Process P1 arrives                                     │    │
│  │ ├─ createProcess(0)                                            │    │
│  │ │  ├─ Memory.allocate(1, 10) → address 0-9                     │    │
│  │ │  ├─ Load Program_1.txt                                       │    │
│  │ │  └─ Create PCB with state=READY                              │    │
│  │ └─ Scheduler.addProcess(P1) → Ready Queue: [P1]               │    │
│  └────────────────────────┬────────────────────────────────────────┘    │
└─────────────────────────────┼────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                  MAIN SIMULATION LOOP (Clock Cycles)                     │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ For each clock cycle (1-50):                                    │    │
│  │                                                                  │    │
│  │ 1. CHECK ARRIVALS                                              │    │
│  │    ├─ Clock 1: Process P2 arrives                              │    │
│  │    ├─ Clock 4: Process P3 arrives                              │    │
│  │    └─ Create new PCBs, allocate memory, add to ready queue     │    │
│  │                                                                  │    │
│  │ 2. SELECT NEXT PROCESS (Scheduler)                             │    │
│  │    ├─ Get next from ready queue using algorithm                │    │
│  │    │  (RR: rotate, HRRN: select by ratio, MLFQ: priority)     │    │
│  │    └─ Ready Queue now: [P1, P2, P3, ...]                       │    │
│  │                                                                  │    │
│  │ 3. EXECUTE INSTRUCTION (Interpreter)                           │    │
│  │    ├─ Get instruction at PC                                    │    │
│  │    ├─ Parse command type                                       │    │
│  │    ├─ Execute:                                                 │    │
│  │    │  • semWait: Try to acquire mutex                          │    │
│  │    │  • semSignal: Release mutex, unblock process              │    │
│  │    │  • assign: Set variable, read input if needed             │    │
│  │    │  • print: Output variable value                           │    │
│  │    │  • printFromTo: Output range of values                    │    │
│  │    │  • readFile: Load from file into variable                 │    │
│  │    │  • writeFile: Save variable to file                       │    │
│  │    ├─ Check if process blocked                                 │    │
│  │    │  └─ If blocked: add to blocked queue, don't increment PC  │    │
│  │    │  └─ If not: increment PC, add to ready queue              │    │
│  │    └─ Store ExecutionTrace                                     │    │
│  │                                                                  │    │
│  │ 4. OUTPUT DISPLAY                                              │    │
│  │    ├─ Clock cycle number                                       │    │
│  │    ├─ Running process and instruction                          │    │
│  │    ├─ Execution result                                         │    │
│  │    ├─ Queue status: Ready: [P1, P3], Blocked: [P2]             │    │
│  │    └─ Memory map showing allocation                            │    │
│  │                                                                  │    │
│  │ 5. CHECK TERMINATION                                           │    │
│  │    └─ If all processes TERMINATED, exit loop                   │    │
│  │                                                                  │    │
│  └────────────────────────┬────────────────────────────────────────┘    │
└─────────────────────────────┼────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                         OUTPUT LAYER                                     │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ Console Display: Execution traces for all 50 clock cycles      │    │
│  │ ├─ Each cycle shows: clock, process, instruction, result      │    │
│  │ ├─ Queue status updates                                        │    │
│  │ ├─ Memory map changes                                          │    │
│  │ ├─ Process-specific output:                                    │    │
│  │ │  ├─ P1 output: "Output: 1 2 3 4 5 6 7 8 9 10"              │    │
│  │ │  ├─ P2 output: "Wrote to file a (content: Hello World)"     │    │
│  │ │  └─ P3 output: Reads and processes file contents            │    │
│  │ ├─ Final summary: All processes terminated                    │    │
│  │ └─ Memory freed (all 40 words marked as free)                 │    │
│  └────────────────────────────────────────────────────────────────┘    │
└──────────────────────────────────────────────────────────────────────────┘
```

### Mutex State Transitions

```
Initial State: lockHolder = null, waitQueue = []

P1 executes semWait:
  lockHolder = null → tryLock(P1) = true
  lockHolder = P1 ✓ (P1 holds lock)

P2 executes semWait (while P1 holds lock):
  lockHolder = P1 → tryLock(P2) = false
  Scheduler.blockProcess(P2)
  Mutex.block(P2)
  P2 state = BLOCKED
  waitQueue = [P2]

P1 executes semSignal:
  lockHolder = P1 → signalAndUnblock(P1)
  Remove P2 from waitQueue: P2
  lockHolder = P2 (transfer ownership)
  Return P2 to Scheduler.unblockProcess(P2)
  P2 state = READY ✓

P3 executes semWait (while P2 holds lock):
  (Same as P2 above)
  P3 state = BLOCKED
  lockHolder = P2, waitQueue = [P3]

P2 executes semSignal:
  lockHolder = P2 → signalAndUnblock(P2)
  Remove P3 from waitQueue: P3
  lockHolder = P3 (transfer ownership)
  Return P3 to Scheduler.unblockProcess(P3)
  P3 state = READY ✓
```

---

## Program Specifications

### Program 1 (Program_1.txt)
```
semWait userInput
assign x input
assign y input
assign z input
semSignal userInput
semWait userOutput
printFromTo 1 10
semSignal userOutput
```
**Purpose**: Read inputs and print range 1-10
**Output**: "Output: 1 2 3 4 5 6 7 8 9 10"

### Program 2 (Program_2.txt)
```
semWait userOutput
print a
print b
print c
semSignal userOutput
semWait file
assign x input
writeFile a x
semSignal file
```
**Purpose**: Write to file
**Output**: File "a" contains "Hello World"

### Program 3 (Program_3.txt)
```
semWait file
readFile a x
semSignal file
semWait userOutput
print x
print a
print b
semSignal userOutput
```
**Purpose**: Read from file and display
**Output**: Processes file contents

---

## Testing & Verification

### Expected Results

| Metric | Value |
|--------|-------|
| Total Clock Cycles | 43-50 |
| P1 Termination Time | ~Clock 10 |
| P2 Termination Time | ~Clock 25 |
| P3 Termination Time | ~Clock 43 |
| Memory Used | 30 words (3 processes × 10) |
| Memory Freed | 40 words (all freed on termination) |

### Verification Checklist
- [ ] All three processes created at correct arrival times (0, 1, 4)
- [ ] Mutex blocking/unblocking works correctly
- [ ] Round Robin rotates processes every 2 instructions
- [ ] HRRN selects process with highest response ratio
- [ ] MLFQ manages 4-level priority queues
- [ ] Memory allocation/deallocation correct
- [ ] All three processes reach TERMINATED state
- [ ] Expected outputs produced for each program
- [ ] Simulation ends after all processes complete

---

## Key Design Decisions

1. **Mutex Ownership**: Uses `ProcessControlBlock lockHolder` instead of boolean `locked` to properly track which process holds the lock. This enables correct ownership transfer on semSignal.

2. **Blocking Strategy**: All blocking (mutex, I/O) goes through `Scheduler.blockProcess()`. Unblocking occurs when the blocking condition is satisfied.

3. **Process Arrival**: Checked BEFORE clock increment to catch time-0 arrivals correctly.

4. **Instruction Format**: Simple text-based parsing (e.g., "semWait userInput"). No complex parsing needed.

5. **Memory Allocation**: Fixed 10 words per process, contiguous allocation. Deallocation happens on process termination.

6. **Scheduling**: Pluggable algorithm selection. All algorithms implement the same interface.

---

## Conclusion

This Operating Systems Simulator successfully models core OS concepts with a focus on process scheduling, mutual exclusion, and memory management. The three test programs demonstrate concurrent execution with proper synchronization and resource sharing. The system can be easily extended with additional scheduling algorithms or I/O operations.
