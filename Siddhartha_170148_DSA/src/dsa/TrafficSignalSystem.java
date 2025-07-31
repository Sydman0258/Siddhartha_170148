package dsa;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("serial")
public class TrafficSignalSystem extends JFrame {
    private static final int ROAD_WIDTH = 600;
    private static final int ROAD_HEIGHT = 400;
    private static final int VEHICLE_SIZE = 30;

    private enum SignalState { GREEN, RED }

    private volatile SignalState signalState = SignalState.RED;
    private final LinkedList<Vehicle> vehicleQueue = new LinkedList<>();
    private final PriorityQueue<Vehicle> emergencyQueue = new PriorityQueue<>(
        Comparator.comparingInt(v -> -v.priority)
    );
    
    private final Object queueLock = new Object();

    private final AtomicBoolean emergencyMode = new AtomicBoolean(false);
    private final AtomicBoolean addingVehicles = new AtomicBoolean(false);

    private JPanel roadPanel;
    private JTextArea queueArea;
    private JButton addVehicleButton, emergencyModeButton, signalChangeButton;

    private ExecutorService executor = Executors.newCachedThreadPool();

    private static class Vehicle {
        String id;
        int x, y;
        boolean isEmergency;
        int priority;

        Vehicle(String id, boolean isEmergency) {
            this.id = id;
            this.isEmergency = isEmergency;
            this.priority = isEmergency ? 1 : 0;
            this.x = -VEHICLE_SIZE;
            this.y = ROAD_HEIGHT / 2;
        }
    }

    public TrafficSignalSystem() {
        setTitle("Traffic Signal Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Controls
        JPanel controlPanel = new JPanel();
        signalChangeButton = new JButton("Change Signal");
        addVehicleButton = new JButton("Start Adding Vehicles");
        emergencyModeButton = new JButton("Enable Emergency Mode");

        controlPanel.add(signalChangeButton);
        controlPanel.add(addVehicleButton);
        controlPanel.add(emergencyModeButton);

        // Road Panel for animation
        roadPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawIntersection(g);
            }
        };
        roadPanel.setPreferredSize(new Dimension(ROAD_WIDTH, ROAD_HEIGHT));

        // Queue display area
        queueArea = new JTextArea(15, 20);
        queueArea.setEditable(false);
        JScrollPane queueScroll = new JScrollPane(queueArea);

        add(controlPanel, BorderLayout.NORTH);
        add(roadPanel, BorderLayout.CENTER);
        add(queueScroll, BorderLayout.EAST);

        // Button listeners
        signalChangeButton.addActionListener(e -> changeSignal());
        emergencyModeButton.addActionListener(e -> toggleEmergencyMode());
        addVehicleButton.addActionListener(e -> toggleAddingVehicles());

        // Start traffic light thread
        startTrafficLightThread();

        // Start vehicle processing thread
        startVehicleProcessingThread();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void drawIntersection(Graphics g) {
        // Road background
        g.setColor(Color.GRAY);
        g.fillRect(0, ROAD_HEIGHT / 2 - 50, ROAD_WIDTH, 100);

        // Traffic light circle
        g.setColor(signalState == SignalState.GREEN ? Color.GREEN : Color.RED);
        g.fillOval(ROAD_WIDTH / 2 - 25, ROAD_HEIGHT / 2 - 25, 50, 50);

        // Draw vehicles
        synchronized(queueLock) {
            for (Vehicle v : vehicleQueue) {
                g.setColor(v.isEmergency ? Color.RED : Color.BLUE);
                g.fillRect(v.x, v.y - VEHICLE_SIZE / 2, VEHICLE_SIZE, VEHICLE_SIZE);
                g.setColor(Color.WHITE);
                g.drawString(v.id, v.x + 5, v.y);
            }

            for (Vehicle v : emergencyQueue) {
                g.setColor(Color.RED);
                g.fillRect(v.x, v.y - VEHICLE_SIZE / 2, VEHICLE_SIZE, VEHICLE_SIZE);
                g.setColor(Color.WHITE);
                g.drawString(v.id, v.x + 5, v.y);
            }
        }
    }

    private void changeSignal() {
        signalState = (signalState == SignalState.GREEN) ? SignalState.RED : SignalState.GREEN;
        SwingUtilities.invokeLater(() -> roadPanel.repaint());
    }

    private void toggleEmergencyMode() {
        emergencyMode.set(!emergencyMode.get());
        emergencyModeButton.setText(emergencyMode.get() ? "Disable Emergency Mode" : "Enable Emergency Mode");
    }

    private void toggleAddingVehicles() {
        if (addingVehicles.get()) {
            addingVehicles.set(false);
            addVehicleButton.setText("Start Adding Vehicles");
        } else {
            addingVehicles.set(true);
            addVehicleButton.setText("Stop Adding Vehicles");
            startVehicleAdderThread();
        }
    }

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

    private void startVehicleAdderThread() {
        executor.submit(() -> {
            Random rand = new Random();
            try {
                while (addingVehicles.get()) {
                    boolean isEmergency = rand.nextDouble() < 0.2; // 20% emergency chance
                    String id = (isEmergency ? "EM" : "V") + rand.nextInt(1000);

                    Vehicle vehicle = new Vehicle(id, isEmergency);
                    synchronized(queueLock) {
                        if (isEmergency) emergencyQueue.offer(vehicle);
                        else vehicleQueue.offer(vehicle);
                    }
                    updateQueueDisplay();
                    roadPanel.repaint();

                    Thread.sleep(1500);
                }
            } catch (InterruptedException ignored) {}
        });
    }

    private void startVehicleProcessingThread() {
        executor.submit(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Vehicle vehicle = null;

                    synchronized(queueLock) {
                        if (emergencyMode.get() && !emergencyQueue.isEmpty()) {
                            vehicle = emergencyQueue.poll();
                        } else if (!vehicleQueue.isEmpty() && signalState == SignalState.GREEN) {
                            vehicle = vehicleQueue.poll();
                        }
                    }

                    if (vehicle != null) {
                        processVehicleMovement(vehicle);
                    }

                    updateQueueDisplay();
                    Thread.sleep(100);
                }
            } catch (InterruptedException ignored) {}
        });
    }

    private void processVehicleMovement(Vehicle vehicle) {
        executor.submit(() -> {
            try {
                while (vehicle.x < ROAD_WIDTH && (signalState == SignalState.GREEN || vehicle.isEmergency)) {
                    vehicle.x += 5;
                    SwingUtilities.invokeLater(() -> roadPanel.repaint());
                    Thread.sleep(50);
                }

                synchronized(queueLock) {
                    // Vehicle passed or stopped
                    if (vehicle.x >= ROAD_WIDTH) {
                        if (vehicle.isEmergency) emergencyQueue.remove(vehicle);
                        else vehicleQueue.remove(vehicle);
                    } else if (signalState == SignalState.RED && !vehicle.isEmergency) {
                        vehicleQueue.offer(vehicle);
                    }
                }
                updateQueueDisplay();
            } catch (InterruptedException ignored) {}
        });
    }

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TrafficSignalSystem::new);
    }
}
