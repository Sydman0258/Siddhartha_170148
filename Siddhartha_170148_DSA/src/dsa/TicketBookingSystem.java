package dsa;
import javax.swing.*;                        // GUI components
import java.awt.*;                          // For layout and graphics
import java.awt.event.*;                    // Event handling
import java.util.*;                         // Utilities (e.g., Random)
import java.util.concurrent.*;              // Concurrency utilities
import java.util.concurrent.locks.ReentrantLock; // Locking mechanism

public class TicketBookingSystem extends JFrame {
    private static final int ROWS = 10;     // Seat grid rows
    private static final int COLS = 10;     // Seat grid columns
    private static final int CELL_SIZE = 40;// Size of each seat box

    private volatile boolean[][] seats;     // Seat booking status (true = booked)
    private volatile int[][] versions;      // Version tracking for optimistic locking
    private final ReentrantLock[] seatLocks;// Lock array for pessimistic locking
    private final ConcurrentLinkedQueue<BookingRequest> bookingQueue; // Booking requests queue
    private final ConcurrentHashMap<String, String> bookingLog;       // Log of booking events
    private final ExecutorService threadPool;                         // Thread pool for processing

    private JPanel seatPanel;               // Panel to draw seats
    private JTextArea queueArea, logArea;   // Displays for queue and log
    private volatile boolean useOptimisticLocking; // Locking strategy toggle
    private volatile int successCount, conflictCount; // Booking stats

    // BookingRequest stores one booking attempt
    private static class BookingRequest {
        String user;
        int row, col;

        BookingRequest(String user, int row, int col) {
            this.user = user;
            this.row = row;
            this.col = col;
        }
    }

    public TicketBookingSystem() {
        seats = new boolean[ROWS][COLS];    // Initialize seat matrix
        versions = new int[ROWS][COLS];     // Initialize version numbers
        seatLocks = new ReentrantLock[ROWS * COLS]; // Lock for each seat
        bookingQueue = new ConcurrentLinkedQueue<>(); // Thread-safe queue
        bookingLog = new ConcurrentHashMap<>();       // Thread-safe log
        threadPool = Executors.newFixedThreadPool(5); // Fixed thread pool for booking
        useOptimisticLocking = true;        // Default locking strategy

        // Initialize locks
        for (int i = 0; i < ROWS * COLS; i++) {
            seatLocks[i] = new ReentrantLock();
        }

        setTitle("Online Ticket Booking System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Control buttons
        JPanel controlPanel = new JPanel();
        JButton bookButton = new JButton("Simulate Booking");
        JButton optimisticButton = new JButton("Use Optimistic Locking");
        JButton pessimisticButton = new JButton("Use Pessimistic Locking");
        JButton processButton = new JButton("Process Bookings");
        JButton cancelButton = new JButton("Cancel Booking");
        controlPanel.add(bookButton);
        controlPanel.add(optimisticButton);
        controlPanel.add(pessimisticButton);
        controlPanel.add(processButton);
        controlPanel.add(cancelButton);

        // Custom seat drawing panel
        seatPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawSeats(g); // Custom method to draw seat boxes
            }
        };
        seatPanel.setPreferredSize(new Dimension(COLS * CELL_SIZE, ROWS * CELL_SIZE));

        // Display panels for queue and logs
        JPanel sidePanel = new JPanel(new GridLayout(2, 1));
        queueArea = new JTextArea(10, 20);
        logArea = new JTextArea(10, 20);
        queueArea.setEditable(false);
        logArea.setEditable(false);
        sidePanel.add(new JScrollPane(queueArea));
        sidePanel.add(new JScrollPane(logArea));

        add(controlPanel, BorderLayout.NORTH);
        add(seatPanel, BorderLayout.CENTER);
        add(sidePanel, BorderLayout.EAST);

        // Button actions
        bookButton.addActionListener(e -> simulateBooking());
        optimisticButton.addActionListener(e -> useOptimisticLocking = true);
        pessimisticButton.addActionListener(e -> useOptimisticLocking = false);
        processButton.addActionListener(e -> processBookings());
        cancelButton.addActionListener(e -> cancelBooking());

        // Mouse click on seat grid to add booking request
        seatPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = e.getY() / CELL_SIZE;
                int col = e.getX() / CELL_SIZE;
                if (row < ROWS && col < COLS) {
                    bookingQueue.offer(new BookingRequest("User" + (int)(Math.random() * 100), row, col));
                    updateQueueDisplay();
                }
            }
        });

        pack();
        setLocationRelativeTo(null); // Center the window
    }

    // Draw seat boxes with status and label
    private void drawSeats(Graphics g) {
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                g.setColor(seats[i][j] ? Color.RED : Color.GREEN); // RED = booked
                g.fillRect(j * CELL_SIZE, i * CELL_SIZE, CELL_SIZE - 2, CELL_SIZE - 2);
                g.setColor(Color.BLACK);
                g.drawRect(j * CELL_SIZE, i * CELL_SIZE, CELL_SIZE - 2, CELL_SIZE - 2);
                g.drawString((i * COLS + j + 1) + "", j * CELL_SIZE + 10, i * CELL_SIZE + 25);
            }
        }
        // Display success/conflict stats
        g.setColor(Color.BLACK);
        g.drawString("Success: " + successCount + " Conflicts: " + conflictCount, 10, ROWS * CELL_SIZE + 20);
    }

    // Generate 5 random booking requests
    private void simulateBooking() {
        Random rand = new Random();
        for (int i = 0; i < 5; i++) {
            int row = rand.nextInt(ROWS);
            int col = rand.nextInt(COLS);
            bookingQueue.offer(new BookingRequest("User" + (i + 1), row, col));
        }
        updateQueueDisplay();
    }

    // Process bookings using threads
    private void processBookings() {
        threadPool.submit(() -> {
            while (!bookingQueue.isEmpty()) {
                BookingRequest request = bookingQueue.poll();
                if (request == null) continue;

                boolean success;
                // Choose locking strategy
                if (useOptimisticLocking) {
                    success = bookSeatOptimistic(request);
                } else {
                    success = bookSeatPessimistic(request);
                }

                // Log results
                if (success) {
                    successCount++;
                    bookingLog.put(request.user + "-" + System.currentTimeMillis(),
                            "Booked seat " + (request.row * COLS + request.col + 1));
                } else {
                    conflictCount++;
                    bookingLog.put(request.user + "-" + System.currentTimeMillis(),
                            "Failed to book seat " + (request.row * COLS + request.col + 1));
                }

                // Update UI on the Event Dispatch Thread
                SwingUtilities.invokeLater(() -> {
                    updateQueueDisplay();
                    updateLogDisplay();
                    seatPanel.repaint();
                });

                try {
                    Thread.sleep(500); // Delay to simulate time-consuming operation
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }

    // Try booking using optimistic locking (version check)
    private boolean bookSeatOptimistic(BookingRequest request) {
        int attempts = 3; // Retry limit
        while (attempts-- > 0) {
            int version = versions[request.row][request.col]; // Get current version
            if (seats[request.row][request.col]) return false; // Already booked

            try {
                Thread.sleep(100); // Simulate delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Check if version is unchanged before booking
            synchronized (this) {
                if (versions[request.row][request.col] == version) {
                    seats[request.row][request.col] = true;
                    versions[request.row][request.col]++;
                    return true;
                }
            }
        }
        return false;
    }

    // Try booking using pessimistic locking (acquire lock first)
    private boolean bookSeatPessimistic(BookingRequest request) {
        int lockIndex = request.row * COLS + request.col;
        if (seatLocks[lockIndex].tryLock()) {
            try {
                if (seats[request.row][request.col]) return false; // Already booked

                try {
                    Thread.sleep(100); // Simulate delay
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                seats[request.row][request.col] = true;
                versions[request.row][request.col]++;
                return true;
            } finally {
                seatLocks[lockIndex].unlock(); // Always release the lock
            }
        }
        return false; // Couldn't acquire lock
    }

    // Cancel a booked seat (requires seat number)
    private void cancelBooking() {
        String seat = JOptionPane.showInputDialog(this, "Enter seat number to cancel (1-" + (ROWS * COLS) + "):");
        try {
            int seatNum = Integer.parseInt(seat) - 1;
            int row = seatNum / COLS;
            int col = seatNum % COLS;

            if (row >= 0 && row < ROWS && col >= 0 && col < COLS) {
                synchronized (this) {
                    if (seats[row][col]) {
                        seats[row][col] = false; // Unbook the seat
                        versions[row][col]++;     // Bump version
                        bookingLog.put("Cancel-" + System.currentTimeMillis(), "Cancelled seat " + (seatNum + 1));
                        updateLogDisplay();
                        seatPanel.repaint();
                    } else {
                        JOptionPane.showMessageDialog(this, "Seat is not booked!");
                    }
                }
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid seat number!");
        }
    }

    // Update queue area in UI
    private void updateQueueDisplay() {
        StringBuilder sb = new StringBuilder("Pending Bookings:\n");
        for (BookingRequest req : bookingQueue) {
            sb.append(req.user).append(": Seat ").append(req.row * COLS + req.col + 1).append("\n");
        }
        queueArea.setText(sb.toString());
    }

    // Update log area in UI
    private void updateLogDisplay() {
        StringBuilder sb = new StringBuilder("Booking Log:\n");
        for (Map.Entry<String, String> entry : bookingLog.entrySet()) {
            sb.append(entry.getValue()).append("\n");
        }
        logArea.setText(sb.toString());
    }

    // Main entry point
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TicketBookingSystem().setVisible(true));
    }
}
