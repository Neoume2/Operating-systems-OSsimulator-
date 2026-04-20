import javax.swing.JOptionPane;
import javax.swing.JFrame;

/**
 * Provides user input through GUI dialogs for processes during simulation.
 * Used by OSSimulatorGUI for interactive input handling.
 */
public class GUIInputProvider implements InputProvider {
    private JFrame parentFrame;
    private boolean cancelled = false;
    
    public GUIInputProvider(JFrame parentFrame) {
        this.parentFrame = parentFrame;
    }
    
    @Override
    public String getInput(int processId, String prompt) {
        String title = "Process P" + processId + " Input";
        
        // Create a more informative prompt
        String fullPrompt = prompt != null && !prompt.isEmpty() 
            ? "[Process P" + processId + "] " + prompt 
            : "[Process P" + processId + "] Enter input";
        
        try {
            String input = JOptionPane.showInputDialog(
                parentFrame,
                fullPrompt,
                title,
                JOptionPane.PLAIN_MESSAGE
            );
            
            if (input == null) {
                // User clicked cancel - use empty string or default
                cancelled = true;
                return "";
            }
            
            return input.trim();
        } catch (Exception e) {
            // Fallback to empty string if dialog fails
            return "";
        }
    }
    
    @Override
    public boolean hasInput() {
        // GUI dialogs are always available
        return true;
    }
    
    /**
     * Check if the user cancelled the last input dialog
     */
    public boolean wasCancelled() {
        return cancelled;
    }
    
    /**
     * Reset the cancelled flag
     */
    public void resetCancelledFlag() {
        cancelled = false;
    }
}
