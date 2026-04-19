import java.util.HashMap;
import java.util.Map;

public class ProcessControlBlock {
    private int pid;
    private ProcessState state;
    private int programCounter;
    private int memoryStart;
    private int memorySize;
    private int arrivalTime;
    private int priority;  // For MLFQ
    private int burstTime;  // For HRRN
    private Map<String, Object> variables;  // Process-local variables
    private String[] program;  // Instructions for this process
    
    // Process states
    public enum ProcessState {
        NEW,
        READY,
        RUNNING,
        BLOCKED,
        TERMINATED
    }

    public ProcessControlBlock(int pid) {
        this.pid = pid;
        this.state = ProcessState.NEW;
        this.programCounter = 0;
        this.memoryStart = -1;
        this.memorySize = 0;
        this.arrivalTime = 0;
        this.priority = 0;  // Highest priority
        this.burstTime = 0;
        this.variables = new HashMap<>();
    }

    // Getters and Setters
    public int getPid() {
        return pid;
    }

    public ProcessState getState() {
        return state;
    }

    public void setState(ProcessState state) {
        this.state = state;
    }

    public int getProgramCounter() {
        return programCounter;
    }

    public void setProgramCounter(int programCounter) {
        this.programCounter = programCounter;
    }

    public void incrementProgramCounter() {
        this.programCounter++;
    }

    public int getMemoryStart() {
        return memoryStart;
    }

    public void setMemoryStart(int memoryStart) {
        this.memoryStart = memoryStart;
    }

    public int getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(int memorySize) {
        this.memorySize = memorySize;
    }

    public int getMemoryEnd() {
        return memoryStart + memorySize - 1;
    }

    public int getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(int arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getBurstTime() {
        return burstTime;
    }

    public void setBurstTime(int burstTime) {
        this.burstTime = burstTime;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariable(String name, Object value) {
        variables.put(name, value);
    }

    public Object getVariable(String name) {
        return variables.get(name);
    }

    public String[] getProgram() {
        return program;
    }

    public void setProgram(String[] program) {
        this.program = program;
        this.burstTime = program.length;
    }

    @Override
    public String toString() {
        return "PCB[PID=" + pid + ", State=" + state + ", PC=" + programCounter + 
               ", Arr=" + arrivalTime + ", Mem=" + memoryStart + "-" + getMemoryEnd() + "]";
    }
}
