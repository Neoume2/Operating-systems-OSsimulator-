import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Memory {
    private static final int MEMORY_SIZE = 40;
    
    // Memory word: stores variable name and value
    private static class MemoryWord {
        String varName;
        Object value;
        int pid;  // which process owns this
        
        MemoryWord(String varName, Object value, int pid) {
            this.varName = varName;
            this.value = value;
            this.pid = pid;
        }
        
        @Override
        public String toString() {
            return varName + "=" + value;
        }
    }
    
    private MemoryWord[] memory = new MemoryWord[MEMORY_SIZE];
    private boolean[] occupied = new boolean[MEMORY_SIZE];
    private Map<Integer, int[]> processMemoryMap = new HashMap<>(); // PID -> [start, size]
    
    // Disk storage for swapped processes
    private List<SwappedProcess> diskStorage = new ArrayList<>();
    
    public static class SwappedProcess {
        public int pid;
        public int[] memoryContent;
        
        public SwappedProcess(int pid, int[] content) {
            this.pid = pid;
            this.memoryContent = content;
        }
    }

    // Read from memory at address
    public Object read(int address) {
        if (address < 0 || address >= MEMORY_SIZE) {
            throw new IllegalArgumentException("Invalid memory address: " + address);
        }
        if (memory[address] != null) {
            return memory[address].value;
        }
        return null;
    }

    // Write to memory at address
    public void write(int address, Object data) {
        if (address < 0 || address >= MEMORY_SIZE) {
            throw new IllegalArgumentException("Invalid memory address: " + address);
        }
        if (memory[address] != null) {
            memory[address].value = data;
        }
    }

    // Allocate memory for a process (code + 3 variables + PCB = ~10 words)
    public int allocate(int pid, int size) {
        int start = -1;
        int count = 0;
        
        for (int i = 0; i < MEMORY_SIZE; i++) {
            if (!occupied[i]) {
                if (start == -1) {
                    start = i;
                }
                count++;
                if (count >= size) {
                    // Mark space as occupied
                    for (int j = start; j < start + size; j++) {
                        occupied[j] = true;
                        memory[j] = new MemoryWord("", null, pid);
                    }
                    processMemoryMap.put(pid, new int[]{start, size});
                    return start;
                }
            } else {
                start = -1;
                count = 0;
            }
        }
        return -1;  // No space available
    }

    // Free memory block
    public void free(int pid) {
        if (processMemoryMap.containsKey(pid)) {
            int[] bounds = processMemoryMap.get(pid);
            int start = bounds[0];
            int size = bounds[1];
            
            for (int i = start; i < start + size && i < MEMORY_SIZE; i++) {
                memory[i] = null;
                occupied[i] = false;
            }
            processMemoryMap.remove(pid);
        }
    }

    // Swap a process to disk
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

    // Swap a process from disk
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

    // Check if process is swapped
    public boolean isSwapped(int pid) {
        for (SwappedProcess p : diskStorage) {
            if (p.pid == pid) {
                return true;
            }
        }
        return false;
    }

    // Check if there's enough space
    public boolean hasSpace(int size) {
        int count = 0;
        for (boolean b : occupied) {
            if (!b) count++;
            if (count >= size) return true;
        }
        return false;
    }

    // Get available space count
    public int getAvailableSpace() {
        int count = 0;
        for (boolean b : occupied) {
            if (!b) count++;
        }
        return count;
    }

    // Get memory map for display
    public String getMemoryMap() {
        StringBuilder sb = new StringBuilder();
        sb.append("Memory Map (40 words):\n");
        for (int i = 0; i < MEMORY_SIZE; i++) {
            if (i % 10 == 0) sb.append("[");
            if (memory[i] != null) {
                sb.append(String.format("P%d", memory[i].pid));
            } else {
                sb.append(" . ");
            }
            if (i % 10 == 9) sb.append("]\n");
            else sb.append(" ");
        }
        return sb.toString();
    }
}
