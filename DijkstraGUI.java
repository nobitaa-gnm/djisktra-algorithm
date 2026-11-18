import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class DijkstraGUI extends JFrame {
    static final long INF = Long.MAX_VALUE;

    private final JTextField vertexField = new JTextField("5", 5);
    private final JTextField sourceField = new JTextField("1", 5);
    private final JCheckBox directedBox = new JCheckBox("Directed graph");
    private final JTextArea edgesArea = new JTextArea("1 2 10\n1 3 3\n2 3 1\n2 4 2\n3 4 2\n3 5 5", 8, 18);
    private final JTextArea outputArea = new JTextArea();
    private final GraphPanel graphPanel = new GraphPanel();
    private int sampleIndex = 0;


    private static final String[][] SAMPLES = {
            {"Textbook sample (undirected)", "5", "1", "false", "1 2 10\n1 3 3\n2 3 1\n2 4 2\n3 4 2\n3 5 5"},
            {"Directed graph (one-way edges)", "5", "1", "true", "1 2 4\n1 3 1\n3 2 2\n2 4 1\n3 4 5\n4 5 3"},
            {"Disconnected graph (unreachable vertices)", "6", "1", "false", "1 2 4\n2 3 6\n4 5 2"},
            {"Parallel edges and a self-loop", "3", "1", "false", "1 2 9\n1 2 2\n2 2 4\n2 3 1"},
            {"Boundary: single vertex", "1", "1", "false", ""},
            {"Auto vertex count, comments and commas", "", "1", "false", "# vertices detected from edges\n1, 2, 7\n2, 3, 2\n1, 3, 12"},
            {"Invalid input: negative weight", "3", "1", "false", "1 2 5\n2 3 -4"},
            {"Invalid input: vertex out of range", "3", "1", "false", "1 2 5\n2 9 4"},
    };

    static class Edge {
        final int from, to;
        final long weight;

        Edge(int from, int to, long weight) { this.from = from; this.to = to; this.weight = weight; }
    }

    static class Graph {
        int n, source, ignoredLoops;
        boolean directed;
        final List<Edge> edges = new ArrayList<>();           
        final List<List<Edge>> adj = new ArrayList<>();        
    }

    static class Result {
        final long[] dist;
        final int[] prev;
        final String log;

        Result(long[] dist, int[] prev, String log) { this.dist = dist; this.prev = prev; this.log = log; }
    }

    private static JPanel titled(String title, LayoutManager layout) {
        JPanel p = new JPanel(layout);
        p.setBorder(BorderFactory.createTitledBorder(title));
        return p;
    }

    public DijkstraGUI() {
        super("Dijkstra's Shortest Path");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 680);
        setLocationRelativeTo(null);

        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 13));

        JPanel input = titled("Graph Input", new BorderLayout(6, 6));
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

        JPanel visual = titled("Graph Visualization", new BorderLayout());
        visual.add(graphPanel, BorderLayout.CENTER);
        JPanel results = titled("Algorithm Steps and Results", new BorderLayout());
        results.add(new JScrollPane(outputArea), BorderLayout.CENTER);

        JSplitPane right = new JSplitPane(JSplitPane.VERTICAL_SPLIT, visual, results);
        right.setResizeWeight(0.55);
        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, input, right);
        main.setDividerLocation(300);
        add(main);

        run.addActionListener(e -> calculate());
        sample.addActionListener(e -> loadSample());
        clear.addActionListener(e -> clearResults());
    }
    private void loadSample() {
        String[] s = SAMPLES[sampleIndex];
        sampleIndex = (sampleIndex + 1) % SAMPLES.length;
        vertexField.setText(s[1]);
        sourceField.setText(s[2]);
        directedBox.setSelected(Boolean.parseBoolean(s[3]));
        edgesArea.setText(s[4]);
        clearResults();
        outputArea.setText("Test case loaded: " + s[0] + "\nClick Find Shortest Paths to run it, "
                + "or Load Sample again for the next test case.");
    }

    private void clearResults() {
        outputArea.setText("");
        graphPanel.setGraph(null, null, null);
    }

    private void calculate() {
        try {
            Graph g = parse(vertexField.getText(), sourceField.getText(), edgesArea.getText(), directedBox.isSelected());
            Result r = solve(g);
            outputArea.setText(r.log);
            outputArea.setCaretPosition(0);
            graphPanel.setGraph(g, r.prev, r.dist);
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        } catch (OutOfMemoryError ex) {
            showError("This graph is too large for the available memory.");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Input Error", JOptionPane.ERROR_MESSAGE);
    }

    private static long toNumber(String text, String errorMessage) {
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private static int toInt(String text, String errorMessage) {
        long v = toNumber(text, errorMessage);
        if (v < Integer.MIN_VALUE || v > Integer.MAX_VALUE) throw new IllegalArgumentException(errorMessage);
        return (int) v;
    }

    static Graph parse(String vertexText, String sourceText, String edgeText, boolean directed) {
        List<int[]> raw = new ArrayList<>();        
        List<Long> weights = new ArrayList<>();
        int maxVertex = 0;

        String[] lines = edgeText.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].split("#", 2)[0].replace(',', ' ').trim();    
            if (line.isEmpty()) continue;
            String where = "Line " + (i + 1) + ": ";
            String[] p = line.split("\\s+");
            if (p.length != 3) throw new IllegalArgumentException(where + "enter FROM TO WEIGHT.");

            String notWhole = where + "vertices and weight must be whole numbers.";
            int from = toInt(p[0], notWhole), to = toInt(p[1], notWhole);
            long w = toNumber(p[2], notWhole);
            if (from < 1 || to < 1) throw new IllegalArgumentException(where + "vertex numbers start at 1.");
            if (w < 0) throw new IllegalArgumentException(where + "weight cannot be negative. Dijkstra's algorithm "
                    + "needs non-negative weights (use Bellman-Ford for negative weights).");
            raw.add(new int[]{from, to, i + 1});
            weights.add(w);
            maxVertex = Math.max(maxVertex, Math.max(from, to));
        }

        String st = sourceText.trim();
        int source = st.isEmpty() ? 1 : toInt(st, "The starting vertex must be a whole number.");
        if (source < 1) throw new IllegalArgumentException("The starting vertex must be 1 or higher.");

        String vt = vertexText.trim();
        int n = vt.isEmpty() ? Math.max(1, Math.max(maxVertex, source))            
                : toInt(vt, "The number of vertices must be a whole number (or leave it blank).");
        if (n < 1) throw new IllegalArgumentException("The graph needs at least 1 vertex.");
        if (source > n) throw new IllegalArgumentException("The starting vertex " + source + " is outside the graph (1 to " + n + ").");

        Graph g = new Graph();
        g.n = n;
        g.source = source - 1;
        g.directed = directed;
        for (int i = 0; i < n; i++) g.adj.add(new ArrayList<>());
        for (int i = 0; i < raw.size(); i++) {
            int[] e = raw.get(i);
            if (e[0] > n || e[1] > n)
                throw new IllegalArgumentException("Line " + e[2] + ": vertex " + Math.max(e[0], e[1]) + " is outside the graph (1 to " + n + ").");
            if (e[0] == e[1]) {                                  // a self-loop never shortens a path
                g.ignoredLoops++;
                continue;
            }
            g.edges.add(new Edge(e[0] - 1, e[1] - 1, weights.get(i)));
        }
        for (Edge e : g.edges) {
            g.adj.get(e.from).add(e);
            if (!directed) g.adj.get(e.to).add(new Edge(e.to, e.from, e.weight));
        }
        return g;
    }

    static String fmt(long d) {
        return d == INF ? "infinity" : String.valueOf(d);
    }

    static Result solve(Graph g) {
        int n = g.n;
        long[] dist = new long[n];
        int[] prev = new int[n];
        boolean[] done = new boolean[n];
        Arrays.fill(dist, INF);
        Arrays.fill(prev, -1);

        StringBuilder log = new StringBuilder("DIJKSTRA'S ALGORITHM\n\nInput: ")
                .append(n).append(n == 1 ? " vertex, " : " vertices, ")
                .append(g.edges.size()).append(g.edges.size() == 1 ? " edge, " : " edges, ")
                .append(g.directed ? "directed" : "undirected").append('\n');
        if (g.ignoredLoops > 0) log.append("Ignored ").append(g.ignoredLoops).append(" self-loop(s).\n");
        log.append("Starting vertex: ").append(g.source + 1).append("\n")
                .append("Initial distance: 0 for source; infinity for others.\n\n");

        PriorityQueue<long[]> queue = new PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));   
        dist[g.source] = 0;
        queue.add(new long[]{0, g.source});

        int step = 1;
        while (!queue.isEmpty()) {
            long[] cur = queue.poll();
            int u = (int) cur[1];
            if (done[u] || cur[0] != dist[u]) continue;                
            done[u] = true;
            log.append("Step ").append(step++).append(": Visit vertex ").append(u + 1)
                    .append(" (distance ").append(dist[u]).append(")\n");

            for (Edge e : g.adj.get(u)) {
                if (done[e.to]) continue;
                long candidate;
                try {
                    candidate = Math.addExact(dist[u], e.weight);
                } catch (ArithmeticException ex) {
                    throw new IllegalArgumentException("A path length is too large for a 64-bit whole number. Use smaller weights.");
                }
                boolean better = candidate < dist[e.to];
                log.append(better ? "  Update V" : "  Keep   V").append(e.to + 1).append(": ").append(fmt(dist[e.to]))
                        .append(better ? " -> " + candidate + "  (" + dist[u] + " + " + e.weight + ")\n"
                                : "  (" + candidate + " is not shorter)\n");
                if (better) {
                    dist[e.to] = candidate;
                    prev[e.to] = u;
                    queue.add(new long[]{candidate, e.to});
                }
            }
            log.append('\n');
        }

        log.append("FINAL SHORTEST PATHS\n").append(String.format("%-8s %-10s %s%n", "Vertex", "Distance", "Path"));
        for (int i = 0; i < n; i++) {
            log.append(String.format("%-8s %-10s %s%n", "V" + (i + 1),
                    dist[i] == INF ? "-" : String.valueOf(dist[i]),
                    dist[i] == INF ? "Unreachable" : pathString(prev, i)));
        }
        return new Result(dist, prev, log.toString());
    }

    private static String pathString(int[] prev, int destination) {
        LinkedList<String> path = new LinkedList<>();
        for (int v = destination; v != -1; v = prev[v]) path.addFirst("V" + (v + 1));
        return String.join(" -> ", path);
    }


    private static class GraphPanel extends JPanel {
        private static final int BOTTOM = 34;                        
        private static final Color TREE = new Color(220, 55, 55), EDGE = new Color(150, 155, 165);
        private static final Color START = new Color(55, 160, 95), VERTEX = new Color(65, 125, 210);

        private Graph graph;
        private int[] previous;
        private long[] distance;
        private final Set<Long> edgeKeys = new HashSet<>();
        private double[] nx, ny;                                      
        private int dragged = -1;
        private double dragDx, dragDy;

        GraphPanel() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(500, 350));
            MouseAdapter mouse = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (graph == null || nx == null || !SwingUtilities.isLeftMouseButton(e)) return;
                    int hit = vertexAt(e.getX(), e.getY());
                    if (hit >= 0) {
                        dragged = hit;
                        dragDx = nx[hit] * getWidth() - e.getX();     
                        dragDy = ny[hit] * getHeight() - e.getY();
                    } else if (e.getClickCount() == 2) {
                        nx = null;                                    
                        ny = null;
                    }
                    hover(e);
                    repaint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragged < 0) return;
                    double half = nodeSize() / 2.0;
                    double x = Math.max(half, Math.min(getWidth() - half, e.getX() + dragDx));
                    double y = Math.max(half, Math.min(getHeight() - BOTTOM - half, e.getY() + dragDy));
                    nx[dragged] = x / getWidth();
                    ny[dragged] = y / getHeight();
                    repaint();
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    dragged = -1;
                    hover(e);
                    repaint();
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    hover(e);
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        void setGraph(Graph graph, int[] previous, long[] distance) {
            this.graph = graph;
            this.previous = previous == null ? null : previous.clone();
            this.distance = distance == null ? null : distance.clone();
            if (graph == null || (nx != null && nx.length != graph.n)) nx = ny = null;  
            edgeKeys.clear();
            if (graph != null) for (Edge e : graph.edges) edgeKeys.add((long) e.from * graph.n + e.to);
            setCursor(Cursor.getDefaultCursor());
            repaint();
        }

        private void hover(MouseEvent e) {
            boolean over = graph != null && nx != null && vertexAt(e.getX(), e.getY()) >= 0;
            setCursor(Cursor.getPredefinedCursor(dragged >= 0 ? Cursor.MOVE_CURSOR : over ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        }

        private double radius() {
            return Math.max(40, Math.min(getWidth(), getHeight() - BOTTOM) / 2.0 - 55);
        }

       
        private int nodeSize() {
            return graph.n == 1 ? 42 : (int) Math.max(8, Math.min(42, 2 * Math.PI * radius() / graph.n * 0.7));
        }

        private void ensureLayout() {
            if (graph == null || getWidth() <= 0 || getHeight() <= 0 || (nx != null && nx.length == graph.n)) return;
            int n = graph.n;
            nx = new double[n];
            ny = new double[n];
            double cx = getWidth() / 2.0, cy = (getHeight() - BOTTOM) / 2.0, r = radius();
            for (int i = 0; i < n; i++) {
                double angle = -Math.PI / 2 + 2 * Math.PI * i / n;
                nx[i] = (n == 1 ? cx : cx + r * Math.cos(angle)) / getWidth();
                ny[i] = (n == 1 ? cy : cy + r * Math.sin(angle)) / getHeight();
            }
        }

        private int vertexAt(int mx, int my) {
            double reach = nodeSize() / 2.0 + 3;
            for (int i = graph.n - 1; i >= 0; i--)
                if (Math.hypot(nx[i] * getWidth() - mx, ny[i] * getHeight() - my) <= reach) return i;
            return -1;
        }

        private boolean inTree(int u, int v, long w) {
            return previous != null && previous[v] == u && distance[v] != INF && distance[u] != INF && distance[v] - distance[u] == w;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (graph == null) {
                g.setColor(Color.GRAY);
                g.drawString("Load or enter a graph, then click Find Shortest Paths.", 25, 35);
                return;
            }
            ensureLayout();
            if (nx == null) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int n = graph.n, size = nodeSize();
            double[] x = new double[n], y = new double[n];
            for (int i = 0; i < n; i++) {
                x[i] = nx[i] * getWidth();
                y[i] = ny[i] * getHeight();
            }

            boolean showWeights = graph.edges.size() <= 250 && size >= 14;
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            for (Edge e : graph.edges) {
                boolean shortest = inTree(e.from, e.to, e.weight) || (!graph.directed && inTree(e.to, e.from, e.weight));
                g2.setColor(shortest ? TREE : EDGE);
                g2.setStroke(new BasicStroke(shortest ? 3.5f : 1.5f));
                double offset = graph.directed && edgeKeys.contains((long) e.to * n + e.from) ? 7 : 0;   // two-way edges side by side
                double[] mid = drawEdge(g2, x[e.from], y[e.from], x[e.to], y[e.to], size, graph.directed, offset);
                if (showWeights && mid != null) {
                    g2.setColor(Color.DARK_GRAY);
                    g2.drawString(String.valueOf(e.weight), (float) mid[0], (float) mid[1]);
                }
            }

            for (int i = 0; i < n; i++) {
                int left = (int) (x[i] - size / 2.0), top = (int) (y[i] - size / 2.0);
                g2.setColor(i == graph.source ? START : VERTEX);
                g2.fillOval(left, top, size, size);
                if (i == dragged) {                                  
                    g2.setColor(new Color(15, 23, 42));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawOval(left, top, size, size);
                }
                if (size >= 24) {
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, Math.min(14, size / 3))));
                    String label = "V" + (i + 1);
                    g2.drawString(label, (float) (x[i] - g2.getFontMetrics().stringWidth(label) / 2.0), (float) (y[i] + 5));
                }
                if (distance != null && distance[i] != INF && size >= 16) {
                    g2.setColor(Color.BLACK);
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                    String d = "d=" + distance[i];
                    g2.drawString(d, (float) (x[i] - g2.getFontMetrics().stringWidth(d) / 2.0), (float) (y[i] + size / 2.0 + 12));
                }
            }

            g2.setColor(Color.GRAY);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2.drawString("Drag a vertex to move it. Double-click empty space to reset the layout.", 12, getHeight() - 26);
            g2.setColor(Color.DARK_GRAY);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g2.drawString("Green = start   Blue = vertex   Red edge = shortest-path tree", 12, getHeight() - 10);
            g2.dispose();
        }

        /** Draws one edge between two vertex circles; returns where its weight label should go. */
        private double[] drawEdge(Graphics2D g, double x1, double y1, double x2, double y2, int size, boolean arrow, double offset) {
            double dx = x2 - x1, dy = y2 - y1, len = Math.hypot(dx, dy);
            if (len < 1) return null;
            double ux = dx / len, uy = dy / len, r = size / 2.0;
            double ox = -uy * offset, oy = ux * offset;
            double sx = x1 + ux * r + ox, sy = y1 + uy * r + oy, ex = x2 - ux * r + ox, ey = y2 - uy * r + oy;
            g.draw(new Line2D.Double(sx, sy, ex, ey));
            if (arrow) {
                double angle = Math.atan2(dy, dx);
                for (int side = -1; side <= 1; side += 2)
                    g.draw(new Line2D.Double(ex, ey, ex - 10 * Math.cos(angle + side * Math.PI / 6), ey - 10 * Math.sin(angle + side * Math.PI / 6)));
            }
            return new double[]{(sx + ex) / 2 - uy * 9, (sy + ey) / 2 + ux * 9};
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DijkstraGUI().setVisible(true));
    }
}