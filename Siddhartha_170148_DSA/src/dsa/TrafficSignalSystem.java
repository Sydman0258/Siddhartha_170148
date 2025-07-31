package dsa;

import javax.swing.*;                     // For GUI components 
import javax.swing.Timer;// For timer
import java.awt.*;                        // For drawing graphics
import java.awt.event.*;                  // For ActionListener
import java.util.*;                       // Utility classes (List, Iterator)
import java.util.concurrent.*;            // For multithreading
import java.util.concurrent.atomic.AtomicBoolean; // Thread-safe boolean flags

@SuppressWarnings("serial")
public class TrafficSignalSystem extends JFrame {
    private static final int ROAD_WIDTH = 600;      // Road panel width
    private static final int ROAD_HEIGHT = 400;     // Road panel height
    private static final int VEHICLE_SIZE = 40;     // Size of vehicle rectangle

    // Enum for signal state
    private enum SignalState { GREEN, RED }

    private volatile SignalState signalState = SignalState.RED; // Initial signal state

    // Queues for vehicle management
    private final LinkedList<Vehicle> vehicleQueue = new LinkedList<>(); // Regular vehicles (FIFO)
    private final PriorityQueue<Vehicle> emergencyQueue = new PriorityQueue<>( // Emergency vehicles (priority queue)
        Comparator.comparingInt(v -> -v.priority) // Highest priority first
    );

    private final Object queueLock = new Object();   // Lock object for queue synchronization

    private final AtomicBoolean emergencyMode = new AtomicBoolean(false); // Emergency mode toggle
    private final AtomicBoolean addingVehicles = new AtomicBoolean(false); // Auto-adding vehicles toggle

    // GUI components
    private JPanel roadPanel;
    private JTextArea queueArea;
    private JButton addVehicleButton, emergencyModeButton, signalChangeButton;

    private ExecutorService executor = Executors.newCachedThreadPool(); // Thread pool for concurrent tasks

    // Vehicles currently being animated on screen
    private final java.util.List<Vehicle> displayVehicles = Collections.synchronizedList(new ArrayList<>());

    // Vehicle model class
    private static class Vehicle {
        String id;                // Vehicle ID string
        int x, y;                 // Coordinates (x for animation, y fixed)
        boolean isEmergency;      // Emergency flag
        int priority;             // Used by priority queue

        Vehicle(String id, boolean isEmergency) {
            this.id = id;
            this.isEmergency = isEmergency;
            this.priority = isEmergency ? 1 : 0;     // 1 = emergency, 0 = regular
            this.x = -VEHICLE_SIZE;                   // Start off-screen (left)
            this.y = ROAD_HEIGHT / 2;                 // Fixed vertical position (center lane)
        }
    }

    public TrafficSignalSystem() {
        setTitle("Traffic Signal Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Control panel with buttons
        JPanel controlPanel = new JPanel();
        signalChangeButton = new JButton("Change Signal");
        addVehicleButton = new JButton("Start Adding Vehicles");
        emergencyModeButton = new JButton("Enable Emergency Mode");

        controlPanel.add(signalChangeButton);
        controlPanel.add(addVehicleButton);
        controlPanel.add(emergencyModeButton);

        // Panel to draw road, vehicles, and signal
        roadPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawIntersection(g);
            }
        };
        roadPanel.setPreferredSize(new Dimension(ROAD_WIDTH, ROAD_HEIGHT));

        // Text area to display vehicle queues on the side
        queueArea = new JTextArea(15, 20);
        queueArea.setEditable(false);
        JScrollPane queueScroll = new JScrollPane(queueArea);

        // Add components to frame
        add(controlPanel, BorderLayout.NORTH);
        add(roadPanel, BorderLayout.CENTER);
        add(queueScroll, BorderLayout.EAST);

        // Bind buttons to actions
        signalChangeButton.addActionListener(e -> changeSignal());
        emergencyModeButton.addActionListener(e -> toggleEmergencyMode());
        addVehicleButton.addActionListener(e -> toggleAddingVehicles());

        // Start background threads for traffic signal and vehicle processing
        startTrafficLightThread();
        startVehicleProcessingThread();

        // Start animation timer for smooth vehicle movement
        startAnimationTimer();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // Draw road, signal light, and all vehicles (queued + animating)
    private void drawIntersection(Graphics g) {
        // Draw road as gray rectangle
        g.setColor(Color.GRAY);
        g.fillRect(0, ROAD_HEIGHT / 2 - 50, ROAD_WIDTH, 100);

        // Draw traffic light circle (green or red)
        g.setColor(signalState == SignalState.GREEN ? Color.GREEN : Color.RED);
        g.fillOval(ROAD_WIDTH / 2 - 25, ROAD_HEIGHT / 2 - 25, 50, 50);

        // Draw all animated vehicles on the road (moving left to right)
        synchronized(displayVehicles) {
            for (Vehicle v : displayVehicles) {
                g.setColor(v.isEmergency ? Color.RED : Color.BLUE);
                g.fillRect(v.x, v.y - VEHICLE_SIZE / 2, VEHICLE_SIZE, VEHICLE_SIZE);
                g.setColor(Color.WHITE);
                g.drawString(v.id, v.x + 5, v.y);
            }
        }
    }

    // Toggle traffic signal between RED and GREEN
    private void changeSignal() {
        signalState = (signalState == SignalState.GREEN) ? SignalState.RED : SignalState.GREEN;
        SwingUtilities.invokeLater(() -> roadPanel.repaint()); // Redraw signal immediately
    }

    // Toggle emergency mode on/off
    private void toggleEmergencyMode() {
        emergencyMode.set(!emergencyMode.get());
        emergencyModeButton.setText(emergencyMode.get() ? "Disable Emergency Mode" : "Enable Emergency Mode");
    }

    // Start or stop automatic vehicle addition
    private void toggleAddingVehicles() {
        if (addingVehicles.get()) {
            addingVehicles.set(false);
            addVehicleButton.setText("Start Adding Vehicles");
        } else {
            addingVehicles.set(true);
            addVehicleButton.setText("Stop Adding Vehicles");
            startVehicleAdderThread(); // Begin adding vehicles in background
        }
    }

    // Background thread to switch traffic signal every 5 seconds
    private void startTrafficLightThread() {
        executor.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Thread.sleep(5000);
                    changeSignal();
                }
            } catch (InterruptedException ignored) {}
        });
    }

    // Background thread to add vehicles periodically (20% chance emergency)
    private void startVehicleAdderThread() {
        executor.submit(() -> {
            Random rand = new Random();
            try {
                while (addingVehicles.get()) {
                    boolean isEmergency = rand.nextDouble() < 0.2; // 20% emergency
                    String id = (isEmergency ? "EM" : "V") + rand.nextInt(1000);

                    Vehicle vehicle = new Vehicle(id, isEmergency);
                    synchronized (queueLock) {
                        if (isEmergency) emergencyQueue.offer(vehicle);
                        else vehicleQueue.offer(vehicle);
                    }

                    updateQueueDisplay();  // Update queue list on UI
                    roadPanel.repaint();   // Refresh drawing panel
                    Thread.sleep(1500);    // Delay between additions
                }
            } catch (InterruptedException ignored) {}
        });
    }

    // Background thread that processes vehicles: selects next vehicle, starts its animation
    private void startVehicleProcessingThread() {
        executor.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Vehicle vehicle = null;

                    synchronized (queueLock) {
                        if (emergencyMode.get() && !emergencyQueue.isEmpty()) {
                            vehicle = emergencyQueue.poll(); // Emergency vehicles prioritized
                        } else if (!vehicleQueue.isEmpty() && signalState == SignalState.GREEN) {
                            vehicle = vehicleQueue.poll();   // Regular vehicles only if green signal
                        }
                    }

                    if (vehicle != null) {
                        // Add vehicle to the animation list (it will move on screen)
                        displayVehicles.add(vehicle);
                    }

                    updateQueueDisplay(); // Refresh queue list on UI
                    Thread.sleep(100);    // Small pause before next check
                }
            } catch (InterruptedException ignored) {}
        });
    }

    // Swing Timer to animate all vehicles currently on screen
    private void startAnimationTimer() {
        Timer timer = new Timer(40, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized(displayVehicles) {
                    Iterator<Vehicle> iter = displayVehicles.iterator();
                    while (iter.hasNext()) {
                        Vehicle v = iter.next();
                        // Move vehicle forward by 5 pixels per tick
                        if (signalState == SignalState.GREEN || v.isEmergency) {
                            v.x += 5;
                        }
                        // Remove vehicle if it has passed the road
                        if (v.x > ROAD_WIDTH) {
                            iter.remove();
                        }
                    }
                }
                roadPanel.repaint(); // Refresh panel to show updated positions
            }
        });
        timer.start(); // Start the animation timer
    }

    // Update text area showing queued vehicles
    private void updateQueueDisplay() {
        StringBuilder sb = new StringBuilder("Vehicle Queue:\n");
        synchronized(queueLock) {
            for (Vehicle v : vehicleQueue) {
                sb.append(v.id).append(" (Regular)\n");
            }
            for (Vehicle v : emergencyQueue) {
                sb.append(v.id).append(" (Emergency)\n");
            }
        }
        SwingUtilities.invokeLater(() -> queueArea.setText(sb.toString()));
    }

    // Main method: start GUI in Event Dispatch Thread
    public static void main(String[] args) {
        SwingUtilities.invokeLater(TrafficSignalSystem::new);
    }
}
