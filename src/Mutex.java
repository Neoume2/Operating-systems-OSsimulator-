import java.util.LinkedList;
import java.util.Queue;

public class Mutex {
    private String name;
    private ProcessControlBlock lockHolder;  // Track which process holds the lock
    private Queue<ProcessControlBlock> blockedQueue;
    
    public Mutex(String name) {
        this.name = name;
        this.lockHolder = null;
        this.blockedQueue = new LinkedList<>();
    }

    // Try to acquire the mutex
    // Returns true if acquired, false if already locked by someone else
    public boolean tryLock(ProcessControlBlock requester) {
        if (lockHolder == null) {
            lockHolder = requester;
            return true;
        }
        if (lockHolder == requester) {
            // Process already holds the lock (recursive locking not supported, but for simplicity, allow it)
            return true;
        }
        return false;
    }
    
    // Legacy tryLock for backward compatibility
    public boolean tryLock() {
        if (lockHolder == null) {
            lockHolder = new ProcessControlBlock(0);  // Dummy process
            return true;
        }
        return false;
    }

    // Add a process to the blocked queue
    public void block(ProcessControlBlock pcb) {
        blockedQueue.add(pcb);
    }

    // Release the mutex and unblock waiting process
    // Returns the process that should be unblocked (with the lock transferred to it)
    // or null if no processes are waiting
    public ProcessControlBlock signalAndUnblock(ProcessControlBlock releaser) {
        // Verify the releaser actually holds the lock
        if (lockHolder != releaser) {
            return null;  // Error: only lock holder can signal
        }
        
        if (blockedQueue.isEmpty()) {
            lockHolder = null;  // No one waiting, just unlock
            return null;
        }
        
        // Transfer lock to next waiting process
        ProcessControlBlock unblocked = blockedQueue.poll();
        lockHolder = unblocked;  // Transfer ownership
        return unblocked;
    }
    
    // Legacy signalAndUnblock for backward compatibility
    public ProcessControlBlock signalAndUnblock() {
        if (blockedQueue.isEmpty()) {
            lockHolder = null;
            return null;
        }
        ProcessControlBlock unblocked = blockedQueue.poll();
        lockHolder = unblocked;
        return unblocked;
    }

    // Release the mutex (simple unlock)
    public void unlock(ProcessControlBlock releaser) {
        if (lockHolder == releaser) {
            lockHolder = null;
        }
    }
    
    // Legacy unlock for backward compatibility
    public void unlock() {
        lockHolder = null;
    }

    // Check if mutex is locked
    public boolean isLocked() {
        return lockHolder != null;
    }

    // Check if there are blocked processes
    public boolean hasBlockedProcesses() {
        return !blockedQueue.isEmpty();
    }

    // Get the name of the mutex
    public String getName() {
        return name;
    }

    // Get queue size
    public int getBlockedCount() {
        return blockedQueue.size();
    }
}
