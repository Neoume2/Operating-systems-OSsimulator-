import java.util.Scanner;

/**
 * Provides user input from console (stdin) for processes during simulation.
 */
public class ConsoleInputProvider implements InputProvider {
    private Scanner scanner;
    
    public ConsoleInputProvider() {
        this.scanner = new Scanner(System.in);
    }
    
    public ConsoleInputProvider(Scanner scanner) {
        this.scanner = scanner;
    }
    
    @Override
    public String getInput(int processId, String prompt) {
        if (prompt != null && !prompt.isEmpty()) {
            System.out.print("[P" + processId + "] " + prompt + ": ");
        } else {
            System.out.print("[P" + processId + "] Input required: ");
        }
        
        if (scanner.hasNextLine()) {
            return scanner.nextLine().trim();
        }
        return "";
    }
    
    @Override
    public boolean hasInput() {
        return scanner.hasNextLine();
    }
    
    public void close() {
        if (scanner != null) {
            scanner.close();
        }
    }
}
