import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;

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
    
    // Modern color scheme
    private static final Color PRIMARY_COLOR = new Color(59, 89, 152);
    private static final Color ACCENT_COLOR = new Color(66, 165, 245);
    private static final Color BACKGROUND_COLOR = new Color(33, 33, 33);
    private static final Color DARK_BG = new Color(25, 25, 25);
    private static final Color TEXT_COLOR = new Color(230, 230, 230);
    private static final Color PANEL_BG = new Color(45, 45, 45);
    private static final Color GREEN_ACCENT = new Color(76, 175, 80);
    private static final Color RED_ACCENT = new Color(244, 67, 54);
    
    public OSSimulatorGUI() {
        setTitle("Operating System Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1600, 900);
        setLocationRelativeTo(null);
        setBackground(BACKGROUND_COLOR);
        
        // Create main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BACKGROUND_COLOR);
        
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
        panel.setBackground(PRIMARY_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 0));
        
        // Title
        JLabel titleLabel = new JLabel("[CONTROL] Scheduler");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_COLOR);
        panel.add(titleLabel);
        
        panel.add(Box.createHorizontalStrut(20));
        
        // Algorithm selector
        JLabel algoLabel = new JLabel("Algorithm:");
        algoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        algoLabel.setForeground(TEXT_COLOR);
        String[] algorithms = {"Round Robin", "HRRN", "MLFQ"};
        algorithmSelector = new JComboBox<>(algorithms);
        algorithmSelector.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        algorithmSelector.setBackground(PANEL_BG);
        algorithmSelector.setForeground(TEXT_COLOR);
        algorithmSelector.setFocusable(false);
        
        panel.add(algoLabel);
        panel.add(algorithmSelector);
        
        panel.add(Box.createHorizontalStrut(20));
        
        // Buttons
        startButton = createModernButton("START", GREEN_ACCENT);
        startButton.addActionListener(e -> startSimulation());
        
        stepButton = createModernButton("STEP", ACCENT_COLOR);
        stepButton.addActionListener(e -> simulateOneStep());
        
        pauseButton = createModernButton("PAUSE", RED_ACCENT);
        pauseButton.addActionListener(e -> pauseSimulation());
        pauseButton.setEnabled(false);
        
        JButton resetButton = createModernButton("RESET", new Color(255, 152, 0));
        resetButton.addActionListener(e -> resetSimulation());
        
        panel.add(startButton);
        panel.add(stepButton);
        panel.add(pauseButton);
        panel.add(resetButton);
        
        panel.add(Box.createHorizontalGlue());
        
        clockLabel = new JLabel("Clock: 0");
        clockLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        clockLabel.setForeground(TEXT_COLOR);
        panel.add(clockLabel);
        
        return panel;
    }
    
    private JButton createModernButton(String text, Color bgColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2.setColor(new Color(
                        Math.max(0, bgColor.getRed() - 40),
                        Math.max(0, bgColor.getGreen() - 40),
                        Math.max(0, bgColor.getBlue() - 40)
                    ));
                } else if (getModel().isArmed()) {
                    g2.setColor(new Color(
                        Math.min(255, bgColor.getRed() + 20),
                        Math.min(255, bgColor.getGreen() + 20),
                        Math.min(255, bgColor.getBlue() + 20)
                    ));
                } else {
                    g2.setColor(bgColor);
                }
                
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
            }
        };
        
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(false);
        
        return button;
    }
    
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 12, 0));
        panel.setBackground(BACKGROUND_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Output area
        JPanel outputPanel = createStyledPanel("[OUTPUT] Execution");
        outputArea = createStyledTextArea();
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBackground(PANEL_BG);
        outputScroll.getVerticalScrollBar().setBackground(PANEL_BG);
        outputScroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        outputPanel.add(outputScroll, BorderLayout.CENTER);
        
        // Memory area
        JPanel memoryPanel = createStyledPanel("[MEMORY] Memory Map");
        memoryArea = createStyledTextArea();
        JScrollPane memoryScroll = new JScrollPane(memoryArea);
        memoryScroll.setBackground(PANEL_BG);
        memoryScroll.getVerticalScrollBar().setBackground(PANEL_BG);
        memoryScroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        memoryPanel.add(memoryScroll, BorderLayout.CENTER);
        
        // Queue area
        JPanel queuePanel = createStyledPanel("[QUEUE] Status");
        queueArea = createStyledTextArea();
        JScrollPane queueScroll = new JScrollPane(queueArea);
        queueScroll.setBackground(PANEL_BG);
        queueScroll.getVerticalScrollBar().setBackground(PANEL_BG);
        queueScroll.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        queuePanel.add(queueScroll, BorderLayout.CENTER);
        
        panel.add(outputPanel);
        panel.add(memoryPanel);
        panel.add(queuePanel);
        
        return panel;
    }
    
    private JPanel createStyledPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_BG);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(8, ACCENT_COLOR, 2),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLabel.setForeground(ACCENT_COLOR);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        
        panel.add(titleLabel, BorderLayout.NORTH);
        
        return panel;
    }
    
    private JTextArea createStyledTextArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Consolas", Font.PLAIN, 11));
        area.setBackground(new Color(20, 20, 20));
        area.setForeground(new Color(100, 220, 100));
        area.setCaretColor(TEXT_COLOR);
        area.setMargin(new Insets(8, 8, 8, 8));
        area.setLineWrap(false);
        area.setTabSize(4);
        
        return area;
    }
    
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(PRIMARY_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        
        currentProcessLabel = new JLabel("Current Process: None");
        currentProcessLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        currentProcessLabel.setForeground(TEXT_COLOR);
        
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
        // Set GUI input provider for dialog-based input
        simulator.setInputProvider(new GUIInputProvider(this));
        
        simulationTimer = new Timer(2000, e -> {
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
            // Set GUI input provider for dialog-based input
            simulator.setInputProvider(new GUIInputProvider(this));
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
                    " | PC: " + current.getProgramCounter());
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

// Helper class for rounded borders
class RoundedBorder extends AbstractBorder {
    private int radius;
    private Color color;
    private int thickness;
    
    public RoundedBorder(int radius, Color color, int thickness) {
        this.radius = radius;
        this.color = color;
        this.thickness = thickness;
    }
    
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(thickness));
        g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
    }
    
    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(thickness + 2, thickness + 2, thickness + 2, thickness + 2);
    }
}

// Helper class for modern scrollbar appearance
class ModernScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
    private static final Color THUMB_COLOR = new Color(100, 100, 100);
    private static final Color TRACK_COLOR = new Color(45, 45, 45);
    
    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createZeroButton();
    }
    
    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createZeroButton();
    }
    
    private JButton createZeroButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
    }
    
    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(THUMB_COLOR);
        g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y, thumbBounds.width - 4, thumbBounds.height, 4, 4);
    }
    
    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(TRACK_COLOR);
        g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
    }
}