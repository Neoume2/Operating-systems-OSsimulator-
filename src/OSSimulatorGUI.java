import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class OSSimulatorGUI extends JFrame {
    private JTextArea outputArea;
    private JTextArea memoryArea;
    private JTextArea queueArea;
    private JButton startButton;
    private JButton stepButton;
    private JButton pauseButton;
    private JComboBox<String> algorithmSelector;
    private JLabel clockLabel;
    private JLabel currentProcessLabel;
    
    private OSSimulator simulator;
    private Timer simulationTimer;
    private boolean isRunning = false;
    
    public OSSimulatorGUI() {
        setTitle("Operating System Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 800);
        setLocationRelativeTo(null);
        
        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Top panel: Controls
        JPanel controlPanel = createControlPanel();
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        
        // Center panel: Display areas
        JPanel centerPanel = createCenterPanel();
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        // Bottom panel: Info
        JPanel infoPanel = createInfoPanel();
        mainPanel.add(infoPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Algorithm selector
        JLabel algoLabel = new JLabel("Scheduling Algorithm:");
        String[] algorithms = {"Round Robin", "HRRN", "MLFQ"};
        algorithmSelector = new JComboBox<>(algorithms);
        
        // Buttons
        startButton = new JButton("Start");
        startButton.addActionListener(e -> startSimulation());
        
        stepButton = new JButton("Step");
        stepButton.addActionListener(e -> simulateOneStep());
        
        pauseButton = new JButton("Pause");
        pauseButton.addActionListener(e -> pauseSimulation());
        pauseButton.setEnabled(false);
        
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> resetSimulation());
        
        // Add components
        panel.add(algoLabel);
        panel.add(algorithmSelector);
        panel.add(Box.createHorizontalStrut(20));
        panel.add(startButton);
        panel.add(stepButton);
        panel.add(pauseButton);
        panel.add(resetButton);
        
        clockLabel = new JLabel("Clock: 0");
        clockLabel.setFont(new Font("Arial", Font.BOLD, 14));
        panel.add(Box.createHorizontalStrut(50));
        panel.add(clockLabel);
        
        return panel;
    }
    
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Output area
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder("Execution Output"));
        outputArea = new JTextArea(25, 40);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputPanel.add(outputScroll, BorderLayout.CENTER);
        
        // Memory area
        JPanel memoryPanel = new JPanel(new BorderLayout());
        memoryPanel.setBorder(BorderFactory.createTitledBorder("Memory Map"));
        memoryArea = new JTextArea(25, 20);
        memoryArea.setEditable(false);
        memoryArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        JScrollPane memoryScroll = new JScrollPane(memoryArea);
        memoryPanel.add(memoryScroll, BorderLayout.CENTER);
        
        // Queue area
        JPanel queuePanel = new JPanel(new BorderLayout());
        queuePanel.setBorder(BorderFactory.createTitledBorder("Queue Status"));
        queueArea = new JTextArea(25, 20);
        queueArea.setEditable(false);
        queueArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        JScrollPane queueScroll = new JScrollPane(queueArea);
        queuePanel.add(queueScroll, BorderLayout.CENTER);
        
        panel.add(outputPanel);
        panel.add(memoryPanel);
        panel.add(queuePanel);
        
        return panel;
    }
    
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(240, 240, 240));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        currentProcessLabel = new JLabel("Current Process: None");
        currentProcessLabel.setFont(new Font("Arial", Font.BOLD, 12));
        
        panel.add(currentProcessLabel);
        
        return panel;
    }
    
    private void startSimulation() {
        if (isRunning) return;
        
        isRunning = true;
        startButton.setEnabled(false);
        stepButton.setEnabled(false);
        pauseButton.setEnabled(true);
        algorithmSelector.setEnabled(false);
        
        String selectedAlgo = (String) algorithmSelector.getSelectedItem();
        Scheduler.SchedulingAlgorithm algo = switch(selectedAlgo) {
            case "Round Robin" -> Scheduler.SchedulingAlgorithm.ROUND_ROBIN;
            case "HRRN" -> Scheduler.SchedulingAlgorithm.HRRN;
            case "MLFQ" -> Scheduler.SchedulingAlgorithm.MLFQ;
            default -> Scheduler.SchedulingAlgorithm.ROUND_ROBIN;
        };
        
        simulator = new OSSimulator(algo, this);
        
        simulationTimer = new Timer(500, e -> {
            if (isRunning) {
                simulator.stepSimulation();
                updateDisplay();
                
                // Check if all processes are done
                if (simulator.allProcessesDone()) {
                    isRunning = false;
                    simulationTimer.stop();
                    pauseButton.setEnabled(false);
                    startButton.setEnabled(true);
                    stepButton.setEnabled(true);
                    algorithmSelector.setEnabled(true);
                    outputArea.append("\n=== ALL PROCESSES TERMINATED - SIMULATION COMPLETE ===\n");
                    outputArea.setCaretPosition(outputArea.getDocument().getLength());
                }
            }
        });
        simulationTimer.start();
    }
    
    private void simulateOneStep() {
        String selectedAlgo = (String) algorithmSelector.getSelectedItem();
        Scheduler.SchedulingAlgorithm algo = switch(selectedAlgo) {
            case "Round Robin" -> Scheduler.SchedulingAlgorithm.ROUND_ROBIN;
            case "HRRN" -> Scheduler.SchedulingAlgorithm.HRRN;
            case "MLFQ" -> Scheduler.SchedulingAlgorithm.MLFQ;
            default -> Scheduler.SchedulingAlgorithm.ROUND_ROBIN;
        };
        
        if (simulator == null) {
            simulator = new OSSimulator(algo, this);
        }
        
        simulator.stepSimulation();
        updateDisplay();
    }
    
    private void pauseSimulation() {
        if (isRunning) {
            isRunning = false;
            simulationTimer.stop();
            pauseButton.setEnabled(false);
            startButton.setEnabled(true);
            stepButton.setEnabled(true);
        }
    }
    
    private void resetSimulation() {
        pauseSimulation();
        if (simulationTimer != null) {
            simulationTimer.stop();
        }
        
        simulator = null;
        isRunning = false;
        startButton.setEnabled(true);
        stepButton.setEnabled(true);
        pauseButton.setEnabled(false);
        algorithmSelector.setEnabled(true);
        
        outputArea.setText("");
        memoryArea.setText("");
        queueArea.setText("");
        clockLabel.setText("Clock: 0");
        currentProcessLabel.setText("Current Process: None");
    }
    
    private void updateDisplay() {
        if (simulator != null) {
            outputArea.append(simulator.getLastOutput() + "\n");
            outputArea.setCaretPosition(outputArea.getDocument().getLength());
            
            memoryArea.setText(simulator.getMemoryMap());
            queueArea.setText(simulator.getQueueStatus());
            clockLabel.setText("Clock: " + simulator.getClock());
            
            ProcessControlBlock current = simulator.getCurrentProcess();
            if (current != null) {
                currentProcessLabel.setText("Current Process: PID " + current.getPid() + 
                    " (PC: " + current.getProgramCounter() + ")");
            } else {
                currentProcessLabel.setText("Current Process: None");
            }
        }
    }
    
    public void appendOutput(String text) {
        outputArea.append(text + "\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength());
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            OSSimulatorGUI gui = new OSSimulatorGUI();
            gui.setVisible(true);
        });
    }
}