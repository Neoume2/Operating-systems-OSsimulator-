import java.util.ArrayList;
import java.util.List;

public class ExecutionTrace {
    private int clock;
    private ProcessControlBlock currentProcess;
    private String instruction;
    private String result;
    private List<ProcessControlBlock> readyQueue;
    private List<ProcessControlBlock> blockedQueue;
    private String memoryMap;
    private ProcessControlBlock.ProcessState stateChange;
    private int stateChangePid;
    
    public ExecutionTrace(int clock) {
        this.clock = clock;
        this.readyQueue = new ArrayList<>();
        this.blockedQueue = new ArrayList<>();
    }
    
    public void setCurrentProcess(ProcessControlBlock pcb) {
        this.currentProcess = pcb;
    }
    
    public void setInstruction(String instr) {
        this.instruction = instr;
    }
    
    public void setResult(String res) {
        this.result = res;
    }
    
    public void setReadyQueue(List<ProcessControlBlock> queue) {
        this.readyQueue = new ArrayList<>(queue);
    }
    
    public void setBlockedQueue(List<ProcessControlBlock> queue) {
        this.blockedQueue = new ArrayList<>(queue);
    }
    
    public void setMemoryMap(String map) {
        this.memoryMap = map;
    }
    
    public void setStateChange(ProcessControlBlock.ProcessState state, int pid) {
        this.stateChange = state;
        this.stateChangePid = pid;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("========================================\n");
        sb.append(String.format("CLOCK CYCLE %d\n", clock));
        sb.append("========================================\n");
        
        if (stateChange != null) {
            sb.append(String.format("STATE CHANGE: P%d -> %s\n", stateChangePid, stateChange));
        }
        
        if (currentProcess != null) {
            sb.append(String.format("RUNNING: P%d (PC=%d)\n", currentProcess.getPid(), currentProcess.getProgramCounter()));
            if (instruction != null) {
                sb.append(String.format("INSTRUCTION: %s\n", instruction));
            }
            if (result != null) {
                sb.append(String.format("RESULT: %s\n", result));
            }
        } else {
            sb.append("RUNNING: (idle)\n");
        }
        
        sb.append("\nQUEUE STATUS:\n");
        sb.append("  Ready Queue: ");
        if (readyQueue.isEmpty()) {
            sb.append("(empty)");
        } else {
            for (ProcessControlBlock pcb : readyQueue) {
                sb.append("P").append(pcb.getPid()).append(" ");
            }
        }
        sb.append("\n");
        
        sb.append("  Blocked Queue: ");
        if (blockedQueue.isEmpty()) {
            sb.append("(empty)");
        } else {
            for (ProcessControlBlock pcb : blockedQueue) {
                sb.append("P").append(pcb.getPid()).append(" ");
            }
        }
        sb.append("\n");
        
        return sb.toString();
    }
    
    public String getDetailedTrace() {
        return toString();
    }
    
    public int getClock() {
        return clock;
    }
}
