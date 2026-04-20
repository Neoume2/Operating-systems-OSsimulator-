/**
 * Interface for providing user input to processes during simulation.
 * This allows for different input sources (console, GUI, predefined, etc.)
 */
public interface InputProvider {
    /**
     * Request input for a specific process
     * @param processId The process ID requesting input
     * @param prompt An optional prompt to display to the user
     * @return The user's input as a String
     */
    String getInput(int processId, String prompt);
    
    /**
     * Check if input is available without blocking
     * @return true if input can be read, false otherwise
     */
    boolean hasInput();
}
