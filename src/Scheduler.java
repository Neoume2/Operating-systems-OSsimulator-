import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Scheduler {
    public enum SchedulingAlgorithm {
        ROUND_ROBIN,
        HRRN,
        MLFQ
    }

    private SchedulingAlgorithm algorithm;
    private Queue<ProcessControlBlock> readyQueue;
    private Queue<ProcessControlBlock> blockedQueue;
    private ProcessControlBlock currentProcess;
    private int instructionCountForCurrent = 0;  // Instructions executed by current process
    private ProcessControlBlock lastPreemptedProcess = null;  // Track which process was just preempted
    private List<ProcessControlBlock> lastUnblockedProcesses = new ArrayList<>();  // Track unblocked processes
    
    // For Round Robin
    private int timeQuantum;  // Configurable quantum
    
    // For HRRN
    private int simulationClock = 0;
    
    // For MLFQ - 4 priority levels
    private List<Queue<ProcessControlBlock>> priorityQueues;
    private int[] quantumPerLevel;  // Configurable per level
    private int instructionCountForMLFQ = 0;  // Track instructions at current priority level
    
    public Scheduler(SchedulingAlgorithm algo) {
        this(algo, 2, new int[]{2, 4, 8, 16});  // Default values
    }
    
    public Scheduler(SchedulingAlgorithm algo, int roundRobinQuantum, int[] mlfqQuantums) {
        this.algorithm = algo;
        this.readyQueue = new LinkedList<>();
        this.blockedQueue = new LinkedList<>();
        this.currentProcess = null;
        this.timeQuantum = roundRobinQuantum;
        this.quantumPerLevel = mlfqQuantums;
        
        // Initialize MLFQ queues
        if (algo == SchedulingAlgorithm.MLFQ) {
            this.priorityQueues = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                priorityQueues.add(new LinkedList<>());
            }
        }
    }

    // Add process to ready queue
    public void addProcess(ProcessControlBlock pcb) {
        pcb.setState(ProcessControlBlock.ProcessState.READY);
        
        if (algorithm == SchedulingAlgorithm.MLFQ) {
            priorityQueues.get(0).add(pcb);  // Start at highest priority
        } else {
            readyQueue.add(pcb);
        }
    }

    // Get next process to run (depends on algorithm)
    public ProcessControlBlock getNextProcess() {
        switch (algorithm) {
            case ROUND_ROBIN:
                return getNextRoundRobin();
            case HRRN:
                return getNextHRRN();
            case MLFQ:
                return getNextMLFQ();
            default:
                return readyQueue.poll();
        }
    }

    // Round Robin: preemptive, rotates every N instructions
    private ProcessControlBlock getNextRoundRobin() {
        // If we have a current process and haven't exceeded quantum, return it
        if (currentProcess != null) {
            instructionCountForCurrent++;
            
            // If quantum exceeded, preempt and move to back of queue
            if (instructionCountForCurrent >= timeQuantum) {
                lastPreemptedProcess = currentProcess;  // Track preemption
                // Always ensure state is READY before adding back to queue
                currentProcess.setState(ProcessControlBlock.ProcessState.READY);
                readyQueue.add(currentProcess);
                currentProcess = null;
                instructionCountForCurrent = 0;
            } else {
                // Quantum not exceeded, continue with current process
                lastPreemptedProcess = null;
                return currentProcess;
            }
        }
        
        // Select next process from ready queue
        if (!readyQueue.isEmpty()) {
            currentProcess = readyQueue.poll();
            currentProcess.setState(ProcessControlBlock.ProcessState.RUNNING);
            instructionCountForCurrent = 0;  // Will be incremented on next call, allowing quantum instructions
            return currentProcess;
        }
        
        return null;
    }

    // HRRN: Highest Response Ratio Next (Non-preemptive)
    // Once selected, process runs to completion
    private ProcessControlBlock getNextHRRN() {
        // If we have a current process, keep returning it (non-preemptive)
        if (currentProcess != null) {
            return currentProcess;
        }
        
        if (readyQueue.isEmpty()) {
            return null;
        }
        
        // Find process with highest response ratio
        ProcessControlBlock bestProcess = null;
        double highestRatio = -1;
        
        for (ProcessControlBlock pcb : readyQueue) {
            int waitingTime = simulationClock - pcb.getArrivalTime();
            int burstTime = 1;  // Estimate: 1 instruction
            double responseRatio = (double) (waitingTime + burstTime) / burstTime;
            
            if (responseRatio > highestRatio) {
                highestRatio = responseRatio;
                bestProcess = pcb;
            }
        }
        
        if (bestProcess != null) {
            readyQueue.remove(bestProcess);
            bestProcess.setState(ProcessControlBlock.ProcessState.RUNNING);
            currentProcess = bestProcess;
            instructionCountForCurrent = 0;  // For consistency with RR
        }
        
        return currentProcess;
    }

    // MLFQ: Multi-Level Feedback Queue
    // 4 levels, each with own quantum. Demote if quantum exceeded.
    private ProcessControlBlock getNextMLFQ() {
        // If current process exceeded quantum for its level, demote it
        if (currentProcess != null) {
            instructionCountForMLFQ++;
            int currentLevel = currentProcess.getPriority();
            
            if (instructionCountForMLFQ >= quantumPerLevel[currentLevel]) {
                lastPreemptedProcess = currentProcess;  // Track preemption
                // Move to lower priority (higher level number)
                currentProcess.setState(ProcessControlBlock.ProcessState.READY);
                
                int newLevel = Math.min(currentLevel + 1, 3);
                currentProcess.setPriority(newLevel);
                priorityQueues.get(newLevel).add(currentProcess);
                
                currentProcess = null;
                instructionCountForMLFQ = 0;
            } else {
                // Quantum not exceeded, continue with current process
                return currentProcess;
            }
        }
        
        // Get from highest priority queue with processes
        for (int i = 0; i < 4; i++) {
            if (!priorityQueues.get(i).isEmpty()) {
                currentProcess = priorityQueues.get(i).poll();
                currentProcess.setState(ProcessControlBlock.ProcessState.RUNNING);
                instructionCountForMLFQ = 0;  // Will be incremented on next call
                break;
            }
        }
        
        return currentProcess;
    }

    // Block a process
    public void blockProcess(ProcessControlBlock pcb) {
        pcb.setState(ProcessControlBlock.ProcessState.BLOCKED);
        blockedQueue.add(pcb);
        if (currentProcess == pcb) {
            currentProcess = null;
            instructionCountForCurrent = 0;
            instructionCountForMLFQ = 0;
        }
    }

    // Unblock a process
    public void unblockProcess(ProcessControlBlock pcb) {
        if (blockedQueue.remove(pcb)) {
            pcb.setState(ProcessControlBlock.ProcessState.READY);
            lastUnblockedProcesses.add(pcb);  // Track unblocking
            
            if (algorithm == SchedulingAlgorithm.MLFQ) {
                priorityQueues.get(pcb.getPriority()).add(pcb);
            } else {
                readyQueue.add(pcb);
            }
        }
    }

    // Get ready queue size
    public int getReadyQueueSize() {
        if (algorithm == SchedulingAlgorithm.MLFQ) {
            int total = 0;
            for (Queue<ProcessControlBlock> q : priorityQueues) {
                total += q.size();
            }
            return total;
        }
        return readyQueue.size();
    }

    // Get blocked queue size
    public int getBlockedQueueSize() {
        return blockedQueue.size();
    }

    // Get current process
    public ProcessControlBlock getCurrentProcess() {
        return currentProcess;
    }

    // Get queue status string
    public String getQueueStatus() {
        StringBuilder sb = new StringBuilder();
        
        if (algorithm == SchedulingAlgorithm.MLFQ) {
            for (int i = 0; i < 4; i++) {
                sb.append("Level ").append(i).append(": ");
                for (ProcessControlBlock pcb : priorityQueues.get(i)) {
                    sb.append("P").append(pcb.getPid()).append(" ");
                }
                sb.append("\n");
            }
        } else {
            sb.append("Ready: ");
            for (ProcessControlBlock pcb : readyQueue) {
                sb.append("P").append(pcb.getPid()).append(" ");
            }
            sb.append("\n");
        }
        
        sb.append("Blocked: ");
        for (ProcessControlBlock pcb : blockedQueue) {
            sb.append("P").append(pcb.getPid()).append(" ");
        }
        
        return sb.toString();
    }

    // Getters for quantum configuration
    public int getTimeQuantum() {
        return timeQuantum;
    }

    public int[] getQuantumPerLevel() {
        return quantumPerLevel;
    }

    public String getQuantumInfo() {
        if (algorithm == SchedulingAlgorithm.ROUND_ROBIN) {
            return String.format("Round Robin Quantum: %d instructions", timeQuantum);
        } else if (algorithm == SchedulingAlgorithm.MLFQ) {
            StringBuilder sb = new StringBuilder("MLFQ Quantums: ");
            for (int i = 0; i < quantumPerLevel.length; i++) {
                sb.append(String.format("Level%d=%d ", i, quantumPerLevel[i]));
            }
            return sb.toString();
        }
        return "HRRN (Non-preemptive)";
    }
    
    // Update clock for HRRN (call this from OSSimulator at each clock cycle)
    public void updateClock(int clock) {
        this.simulationClock = clock;
    }
    
    // Reset current process (called when process terminates)
    public void processTerminated(ProcessControlBlock pcb) {
        if (currentProcess == pcb) {
            currentProcess = null;
            instructionCountForCurrent = 0;
            instructionCountForMLFQ = 0;
        }
    }

    // Get queue contents as lists
    public List<ProcessControlBlock> getReadyQueueAsList() {
        List<ProcessControlBlock> list = new ArrayList<>();
        if (algorithm == SchedulingAlgorithm.MLFQ) {
            for (int i = 0; i < 4; i++) {
                list.addAll(priorityQueues.get(i));
            }
        } else {
            list.addAll(readyQueue);
        }
        return list;
    }

    public List<ProcessControlBlock> getBlockedQueueAsList() {
        return new ArrayList<>(blockedQueue);
    }

    // Get the process that was just preempted (for logging)
    public ProcessControlBlock getLastPreemptedProcess() {
        return lastPreemptedProcess;
    }

    // Clear the preemption flag after checking
    public void clearLastPreempted() {
        lastPreemptedProcess = null;
    }

    // Get list of processes that were just unblocked
    public List<ProcessControlBlock> getLastUnblockedProcesses() {
        return new ArrayList<>(lastUnblockedProcesses);
    }

    // Clear the unblocked list after checking
    public void clearLastUnblocked() {
        lastUnblockedProcesses.clear();
    }
}