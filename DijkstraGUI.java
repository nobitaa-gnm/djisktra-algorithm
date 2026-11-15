import java.awt.*;
import java.util.*;
import javax.swing.*;

public class DijkstraGUI extends JFrame {
    private final JTextField vertexField = new JTextField("5", 5);
    private final JTextField sourceField = new JTextField("1", 5);
    private final JCheckBox directedBox = new JCheckBox("Directed graph");
    private final JTextArea edgesArea = new JTextArea(
            "1 2 10\n1 3 3\n2 3 1\n2 4 2\n3 4 2\n3 5 5", 8, 18);
    private final JTextArea outputArea = new JTextArea();
    private final GraphPanel graphPanel = new GraphPanel();

    private static class Edge {
        int to, weight;
        Edge(int to, int weight) { this.to = to; this.weight = weight; }
    }

    private static class Node implements Comparable<Node> {
        int vertex, distance;
        Node(int vertex, int distance) {
            this.vertex = vertex;
            this.distance = distance;
        }
        public int compareTo(Node other) {
            return Integer.compare(distance, other.distance);
        }
    }

    public DijkstraGUI() {
        super("Dijkstra's Shortest Path");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 680);
        setLocationRelativeTo(null);

        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JPanel input = new JPanel(new BorderLayout(6, 6));
        input.setBorder(BorderFactory.createTitledBorder("Graph Input"));

        JPanel fields = new JPanel(new GridLayout(0, 2, 5, 5));
        fields.add(new JLabel("Number of vertices:"));
        fields.add(vertexField);
        fields.add(new JLabel("Starting vertex:"));
        fields.add(sourceField);
        fields.add(directedBox);
        fields.add(new JLabel("Edges: FROM TO WEIGHT"));
        input.add(fields, BorderLayout.NORTH);
        input.add(new JScrollPane(edgesArea), BorderLayout.CENTER);

        JButton run = new JButton("Find Shortest Paths");
        JButton sample = new JButton("Load Sample");
        JButton clear = new JButton("Clear Results");
        JPanel buttons = new JPanel();
        buttons.add(run);
        buttons.add(sample);
        buttons.add(clear);
        input.add(buttons, BorderLayout.SOUTH);

        JPanel visual = new JPanel(new BorderLayout());
        visual.setBorder(BorderFactory.createTitledBorder("Graph Visualization"));
        visual.add(graphPanel, BorderLayout.CENTER);

        JPanel results = new JPanel(new BorderLayout());
        results.setBorder(BorderFactory.createTitledBorder("Algorithm Steps and Results"));
        results.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        JSplitPane right = new JSplitPane(JSplitPane.VERTICAL_SPLIT, visual, results);
        right.setResizeWeight(0.55);

        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, input, right);
        main.setDividerLocation(300);
        add(main);

        run.addActionListener(e -> calculate());
        sample.addActionListener(e -> loadSample());
       clear.addActionListener(e -> outputArea.setText(""));
    }

    private void loadSample() {
        vertexField.setText("5");
        sourceField.setText("1");
        directedBox.setSelected(false);
        edgesArea.setText("1 2 10\n1 3 3\n2 3 1\n2 4 2\n3 4 2\n3 5 5");
        outputArea.setText("");
        graphPanel.setGraph(new ArrayList<>(), false, -1, null, null);
    }

    private void calculate() {
        try {
            int count = Integer.parseInt(vertexField.getText().trim());
            int source = Integer.parseInt(sourceField.getText().trim()) - 1;
            if (count < 1 || count > 30) throw new IllegalArgumentException("Vertices must be from 1 to 30.");
            if (source < 0 || source >= count) throw new IllegalArgumentException("Starting vertex is outside the graph.");

            ArrayList<ArrayList<Edge>> graph = new ArrayList<>();
            for (int i = 0; i < count; i++) graph.add(new ArrayList<>());

            String[] lines = edgesArea.getText().split("\\R");
            for (int i = 0; i < lines.length; i++) {
                if (lines[i].trim().isEmpty()) continue;
                String[] p = lines[i].trim().split("\\s+");
                if (p.length != 3) throw new IllegalArgumentException("Line " + (i + 1) + ": enter FROM TO WEIGHT.");
                int from = Integer.parseInt(p[0]) - 1;
                int to = Integer.parseInt(p[1]) - 1;
                int weight = Integer.parseInt(p[2]);
                if (from < 0 || from >= count || to < 0 || to >= count)
                    throw new IllegalArgumentException("Line " + (i + 1) + ": vertex number is invalid.");
                if (weight < 0) throw new IllegalArgumentException("Line " + (i + 1) + ": weight cannot be negative.");
                graph.get(from).add(new Edge(to, weight));
                if (!directedBox.isSelected()) graph.get(to).add(new Edge(from, weight));
            }

            StringBuilder log = new StringBuilder("DIJKSTRA'S ALGORITHM\n\n");
            log.append("Starting vertex: ").append(source + 1).append("\n");
            log.append("Initial distance: 0 for source; infinity for others.\n\n");

            int[] distance = new int[count];
            int[] previous = new int[count];
            Arrays.fill(distance, Integer.MAX_VALUE);
            Arrays.fill(previous, -1);
            PriorityQueue<Node> queue = new PriorityQueue<>();
            distance[source] = 0;
            queue.add(new Node(source, 0));

            int step = 1;
            while (!queue.isEmpty()) {
                Node current = queue.poll();
                if (current.distance != distance[current.vertex]) continue;

                log.append("Step ").append(step++).append(": Visit vertex ")
                   .append(current.vertex + 1).append(" (distance ")
                   .append(current.distance).append(")\n");

                for (Edge edge : graph.get(current.vertex)) {
                    int newDistance = current.distance + edge.weight;
                    if (newDistance < distance[edge.to]) {
                        String old = distance[edge.to] == Integer.MAX_VALUE
                                ? "infinity" : String.valueOf(distance[edge.to]);
                        distance[edge.to] = newDistance;
                        previous[edge.to] = current.vertex;
                        queue.add(new Node(edge.to, newDistance));
                        log.append("  Update V").append(edge.to + 1).append(": ")
                           .append(old).append(" -> ").append(newDistance).append("\n");
                    }
                }
                log.append("\n");
            }

            log.append("FINAL SHORTEST PATHS\n");
            for (int i = 0; i < count; i++) {
                log.append("To V").append(i + 1).append(": ");
                if (distance[i] == Integer.MAX_VALUE) {
                    log.append("Unreachable\n");
                } else {
                    log.append(distance[i]).append(" | ").append(getPath(previous, i)).append("\n");
                }
            }

            outputArea.setText(log.toString());
            outputArea.setCaretPosition(0);
            graphPanel.setGraph(graph, directedBox.isSelected(), source, previous, distance);

        } catch (NumberFormatException ex) {
            showError("Please enter valid whole numbers.");
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private String getPath(int[] previous, int destination) {
        LinkedList<Integer> path = new LinkedList<>();
        for (int v = destination; v != -1; v = previous[v]) path.addFirst(v + 1);
        return path.toString().replace("[", "").replace("]", "").replace(", ", " -> ");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input Error", JOptionPane.ERROR_MESSAGE);
    }

    // Draws the graph and highlights the shortest-path tree in red.
    private static class GraphPanel extends JPanel {
        private ArrayList<ArrayList<Edge>> graph = new ArrayList<>();
        private boolean directed;
        private int source = -1;
        private int[] previous;
        private int[] distance;

        GraphPanel() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(500, 350));
        }

        void setGraph(ArrayList<ArrayList<Edge>> graph, boolean directed,
                      int source, int[] previous, int[] distance) {
            this.graph = graph;
            this.directed = directed;
            this.source = source;
            this.previous = previous == null ? null : previous.clone();
            this.distance = distance == null ? null : distance.clone();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (graph.isEmpty()) {
                g.setColor(Color.GRAY);
                g.drawString("Load or enter a graph, then click Find Shortest Paths.", 25, 35);
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int n = graph.size(), cx = getWidth() / 2, cy = getHeight() / 2;
            int radius = Math.max(60, Math.min(getWidth(), getHeight()) / 2 - 55);
            int nodeSize = 42;
            int[] x = new int[n], y = new int[n];

            for (int i = 0; i < n; i++) {
                double angle = -Math.PI / 2 + 2 * Math.PI * i / n;
                x[i] = cx + (int)(radius * Math.cos(angle));
                y[i] = cy + (int)(radius * Math.sin(angle));
            }

            // Draw edges and their weights.
            for (int from = 0; from < n; from++) {
                for (Edge edge : graph.get(from)) {
                    int to = edge.to;
                    if (!directed && from > to) continue;
                    boolean shortest = previous != null && previous[to] == from;
                    g2.setColor(shortest ? new Color(220, 55, 55) : new Color(150, 155, 165));
                    g2.setStroke(new BasicStroke(shortest ? 3.5f : 1.5f));
                    drawEdge(g2, x[from], y[from], x[to], y[to], nodeSize, directed);
                    g2.setColor(Color.DARK_GRAY);
                    int mx = (x[from] + x[to]) / 2;
                    int my = (y[from] + y[to]) / 2;
                    g2.drawString(String.valueOf(edge.weight), mx + 5, my - 5);
                }
            }

            // Draw vertices. The source is green; other vertices are blue.
            for (int i = 0; i < n; i++) {
                g2.setColor(i == source ? new Color(55, 160, 95) : new Color(65, 125, 210));
                g2.fillOval(x[i] - nodeSize / 2, y[i] - nodeSize / 2, nodeSize, nodeSize);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                String label = "V" + (i + 1);
                g2.drawString(label, x[i] - g2.getFontMetrics().stringWidth(label) / 2, y[i] + 5);
                if (distance != null && distance[i] != Integer.MAX_VALUE) {
                    g2.setColor(Color.BLACK);
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                    String d = "d=" + distance[i];
                    g2.drawString(d, x[i] - g2.getFontMetrics().stringWidth(d) / 2, y[i] + 31);
                }
            }

            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawString("Green = start   Blue = vertex   Red edge = shortest-path tree", 12, getHeight() - 12);
            g2.dispose();
        }

        private void drawEdge(Graphics2D g, int x1, int y1, int x2, int y2,
                              int nodeSize, boolean arrow) {
            double angle = Math.atan2(y2 - y1, x2 - x1);
            int sx = x1 + (int)(Math.cos(angle) * nodeSize / 2);
            int sy = y1 + (int)(Math.sin(angle) * nodeSize / 2);
            int ex = x2 - (int)(Math.cos(angle) * nodeSize / 2);
            int ey = y2 - (int)(Math.sin(angle) * nodeSize / 2);
            g.drawLine(sx, sy, ex, ey);
            if (arrow) {
                int a = 10;
                int ax = ex - (int)(a * Math.cos(angle - Math.PI / 6));
                int ay = ey - (int)(a * Math.sin(angle - Math.PI / 6));
                int bx = ex - (int)(a * Math.cos(angle + Math.PI / 6));
                int by = ey - (int)(a * Math.sin(angle + Math.PI / 6));
                g.drawLine(ex, ey, ax, ay);
                g.drawLine(ex, ey, bx, by);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DijkstraGUI().setVisible(true));
    }
}
