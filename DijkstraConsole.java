
import java.util.*;

public class DijkstraConsole {

    static class Edge {
        int to;
        int weight;

        Edge(int to, int weight) {
            this.to = to;
            this.weight = weight;
        }
    }

    static class Node implements Comparable<Node> {
        int vertex;
        long distance;

        Node(int vertex, long distance) {
            this.vertex = vertex;
            this.distance = distance;
        }

        public int compareTo(Node other) {
            return Long.compare(distance, other.distance);
        }
    }

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        System.out.println("DIJKSTRA'S ALGORITHM");
        System.out.println("SHORTEST PATH");

        try {
            // INPUT NUMBER OF VERTICES
            System.out.print("Enter number of vertices: ");
            int count = sc.nextInt();

            if (count < 1 || count > 30) {
                throw new IllegalArgumentException(
                    "Vertices must be between 1 and 30."
                );
            }

            // INPUT STARTING VERTEX
            System.out.print("Enter starting vertex: ");
            int source = sc.nextInt() - 1;

            if (source < 0 || source >= count) {
                throw new IllegalArgumentException(
                    "Invalid starting vertex."
                );
            }

            // DIRECTED OR UNDIRECTED GRAPH
            System.out.print("Is the graph directed? (yes/no): ");
            String answer = sc.next();

            if (!answer.equalsIgnoreCase("yes") &&
                !answer.equalsIgnoreCase("no")) {
                throw new IllegalArgumentException(
                    "Please enter yes or no."
                );
            }

            boolean directed = answer.equalsIgnoreCase("yes");

            // CREATE GRAPH
            ArrayList<ArrayList<Edge>> graph = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                graph.add(new ArrayList<>());
            }

            // INPUT NUMBER OF EDGES
            System.out.print("Enter number of edges: ");
            int edgeCount = sc.nextInt();

            if (edgeCount < 0 || edgeCount > 1000) {
                throw new IllegalArgumentException(
                    "Invalid number of edges."
                );
            }

            // INPUT EDGES
            System.out.println("\nEnter edges (FROM TO WEIGHT)");

            for (int i = 0; i < edgeCount; i++) {

                System.out.print("Edge " + (i + 1) + ": ");

                int from = sc.nextInt() - 1;
                int to = sc.nextInt() - 1;
                int weight = sc.nextInt();

                if (from < 0 || from >= count ||
                    to < 0 || to >= count) {
                    throw new IllegalArgumentException(
                        "Invalid vertex number."
                    );
                }

                if (weight < 0) {
                    throw new IllegalArgumentException(
                        "Weight cannot be negative."
                    );
                }

                graph.get(from).add(new Edge(to, weight));

                if (!directed) {
                    graph.get(to).add(new Edge(from, weight));
                }
            }

            // DISPLAY GRAPH INFORMATION
            System.out.println("GRAPH INFORMATION");

            System.out.println("Vertices: " + count);
            System.out.println("Starting Vertex: V" + (source + 1));
            System.out.println("Graph Type: " +
                (directed ? "Directed" : "Undirected"));

            // DIJKSTRA'S ALGORITHM
            long[] distance = new long[count];
            int[] previous = new int[count];

            Arrays.fill(distance, Long.MAX_VALUE);
            Arrays.fill(previous, -1);

            PriorityQueue<Node> queue = new PriorityQueue<>();

            distance[source] = 0;
            queue.add(new Node(source, 0));

            System.out.println("DIJKSTRA'S ALGORITHM STEPS");

            int step = 1;

            while (!queue.isEmpty()) {

                Node current = queue.poll();

                if (current.distance != distance[current.vertex]) {
                    continue;
                }

                System.out.println("\nStep " + step++);
                System.out.println("Visiting Vertex: V" +
                    (current.vertex + 1));

                System.out.println("Current Distance: " +
                    current.distance);

                // CHECK CONNECTED VERTICES
                for (Edge edge : graph.get(current.vertex)) {

                    long newDistance =
                        current.distance + edge.weight;

                    System.out.println("\nChecking V" +
                        (edge.to + 1) + " (Weight: " +
                        edge.weight + ")");

                    if (newDistance < distance[edge.to]) {

                        String oldDistance =
                            distance[edge.to] == Long.MAX_VALUE
                            ? "Infinity"
                            : String.valueOf(distance[edge.to]);

                        distance[edge.to] = newDistance;
                        previous[edge.to] = current.vertex;

                        queue.add(new Node(
                            edge.to, newDistance
                        ));

                        System.out.println("Distance Updated: " +
                            oldDistance + " -> " + newDistance);

                    } else {
                        System.out.println(
                            "No update needed."
                        );
                    }
                }
            }

            // DISPLAY FINAL RESULTS
            System.out.println("FINAL SHORTEST PATHS");

            for (int i = 0; i < count; i++) {

                System.out.print("To V" + (i + 1) + ": ");

                if (distance[i] == Long.MAX_VALUE) {
                    System.out.println("Unreachable");
                } else {
                    System.out.println(
                        "Distance = " + distance[i] +
                        " | Path = " + getPath(previous, i)
                    );
                }
            }

            System.out.println("PROGRAM COMPLETED");

        } catch (InputMismatchException e) {

            System.out.println(
                "\nError: Please enter valid whole numbers."
            );

        } catch (IllegalArgumentException e) {

            System.out.println("\nError: " + e.getMessage());

        } finally {
            sc.close();
        }
    }

    // GET SHORTEST PATH
    static String getPath(int[] previous, int destination) {

        LinkedList<Integer> path = new LinkedList<>();

        for (int v = destination; v != -1; v = previous[v]) {
            path.addFirst(v + 1);
        }

        return path.toString()
            .replace("[", "")
            .replace("]", "")
            .replace(", ", " -> ");
    }
}
