package com.ahspa;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.ui.view.Viewer;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class AhspaApp {

    private static final int INF = 999999;
    private static final String CACHE_FILE = "learning_cache.json";
    private static Map<String, CacheEntry> learningCache = new HashMap<>();
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // GUI Components
    private static JTextField verticesEntry;
    private static JTextField edgesEntry;
    private static JTextField sourceEntry;
    private static JTextField destinationEntry;
    private static JTextArea textInput;
    private static JTextArea outputText;
    private static JComboBox<String> algorithmDropdown;
    private static JCheckBox graphMode;

    public static void main(String[] args) {
        System.setProperty("org.graphstream.ui", "swing");
        loadCache();

        JFrame frame = new JFrame("AHSPA-LC using Dijkstra and Bellman-Ford");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 950);
        frame.getContentPane().setBackground(Color.decode("#EAF2F8"));
        frame.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.decode("#EAF2F8"));

        // Title
        JLabel title = new JLabel("<html><center>Adaptive Hybrid Shortest Path<br>Algorithm with Learning Cache (AHSPA-LC)</center></html>");
        title.setFont(new Font("Arial", Font.BOLD, 18));
        title.setForeground(Color.decode("#00008B"));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(title);

        // Input Frame
        JPanel inputFrame = new JPanel(new GridLayout(4, 2, 10, 5));
        inputFrame.setBackground(Color.decode("#EAF2F8"));
        inputFrame.setMaximumSize(new Dimension(300, 150));

        String[] labels = {"Vertices", "Edges", "Source", "Destination"};
        JTextField[] entries = new JTextField[4];

        for (int i = 0; i < 4; i++) {
            JLabel label = new JLabel(labels[i]);
            label.setFont(new Font("Arial", Font.BOLD, 12));
            inputFrame.add(label);
            entries[i] = new JTextField();
            inputFrame.add(entries[i]);
        }
        verticesEntry = entries[0];
        edgesEntry = entries[1];
        sourceEntry = entries[2];
        destinationEntry = entries[3];

        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(inputFrame);

        // Text Input
        JLabel textLabel = new JLabel("Enter Graph Edges: u v w");
        textLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        textLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(textLabel);

        textInput = new JTextArea(12, 80);
        JScrollPane scrollPane = new JScrollPane(textInput);
        scrollPane.setMaximumSize(new Dimension(800, 200));
        mainPanel.add(scrollPane);

        // Buttons
        JPanel buttonFrame = new JPanel();
        buttonFrame.setBackground(Color.decode("#EAF2F8"));

        JButton btn1 = new JButton("<html><center>Option 1<br>Run Algorithm</center></html>");
        btn1.setBackground(Color.decode("#00008B"));
        btn1.setForeground(Color.WHITE);
        btn1.setFont(new Font("Arial", Font.BOLD, 10));
        btn1.setPreferredSize(new Dimension(150, 50));
        btn1.addActionListener(e -> runAlgorithms(true, false));

        JButton btn2 = new JButton("<html><center>Option 2<br>Compare Algorithms</center></html>");
        btn2.setBackground(Color.decode("#008000"));
        btn2.setForeground(Color.WHITE);
        btn2.setFont(new Font("Arial", Font.BOLD, 10));
        btn2.setPreferredSize(new Dimension(150, 50));
        btn2.addActionListener(e -> runAlgorithms(false, false));

        JButton btn3 = new JButton("<html><center>Option 3<br>Learning Cache</center></html>");
        btn3.setBackground(Color.decode("#800080"));
        btn3.setForeground(Color.WHITE);
        btn3.setFont(new Font("Arial", Font.BOLD, 10));
        btn3.setPreferredSize(new Dimension(150, 50));
        btn3.addActionListener(e -> runAlgorithms(false, true));

        buttonFrame.add(btn1);
        buttonFrame.add(btn2);
        buttonFrame.add(btn3);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(buttonFrame);

        JButton generateBtn = new JButton("Generate Random Graph");
        generateBtn.setBackground(Color.ORANGE);
        generateBtn.setForeground(Color.BLACK);
        generateBtn.setFont(new Font("Arial", Font.BOLD, 12));
        generateBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        generateBtn.addActionListener(e -> generateGraph());
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(generateBtn);

        // Algorithm Selector
        JPanel algoFrame = new JPanel();
        algoFrame.setBackground(Color.decode("#EAF2F8"));
        JLabel algoLabel = new JLabel("Select Algorithm for Option 1:");
        algoLabel.setFont(new Font("Arial", Font.BOLD, 12));
        algoFrame.add(algoLabel);

        String[] algorithms = {"Adaptive Best (Default)", "Dijkstra", "Bellman-Ford", "Floyd-Warshall", "Johnson's"};
        algorithmDropdown = new JComboBox<>(algorithms);
        algorithmDropdown.setFont(new Font("Arial", Font.PLAIN, 12));
        algoFrame.add(algorithmDropdown);
        mainPanel.add(algoFrame);

        // 3D Option & Show Graph
        JPanel graphFrame = new JPanel();
        graphFrame.setBackground(Color.decode("#EAF2F8"));
        graphMode = new JCheckBox("View Graph in 3D");
        graphMode.setFont(new Font("Arial", Font.BOLD, 12));
        graphMode.setBackground(Color.decode("#EAF2F8"));
        graphFrame.add(graphMode);

        JButton showGraphBtn = new JButton("Show Graphical Representation");
        showGraphBtn.setBackground(Color.CYAN);
        showGraphBtn.setForeground(Color.BLACK);
        showGraphBtn.setFont(new Font("Arial", Font.BOLD, 10));
        showGraphBtn.addActionListener(e -> showGraphManually());
        graphFrame.add(showGraphBtn);
        mainPanel.add(graphFrame);

        // Output
        outputText = new JTextArea(20, 120);
        outputText.setFont(new Font("Consolas", Font.PLAIN, 12));
        outputText.setEditable(false);
        outputText.setBackground(Color.WHITE);
        JScrollPane outputScroll = new JScrollPane(outputText);
        outputScroll.setMaximumSize(new Dimension(1000, 300));
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(outputScroll);

        frame.add(mainPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    // ================= DATA STRUCTURES =================
    static class Edge {
        int u, v, w;
        Edge(int u, int v, int w) {
            this.u = u;
            this.v = v;
            this.w = w;
        }
    }

    static class CacheEntry {
        List<Integer> path;
        double time_ms;
        String algorithm;
        int cost;
    }

    static class AlgoResult {
        int[] dist;
        int[] parent;
        int[][] nxt;
        long elapsedNs;
        boolean hasNegativeCycle;
    }

    // ================= CACHE FUNCTIONS =================
    private static void loadCache() {
        try {
            if (Files.exists(Paths.get(CACHE_FILE))) {
                String json = Files.readString(Paths.get(CACHE_FILE));
                Type type = new TypeToken<Map<String, CacheEntry>>(){}.getType();
                learningCache = gson.fromJson(json, type);
                if (learningCache == null) learningCache = new HashMap<>();
            }
        } catch (Exception e) {
            learningCache = new HashMap<>();
        }
    }

    private static void saveCache() {
        try {
            Files.writeString(Paths.get(CACHE_FILE), gson.toJson(learningCache));
        } catch (Exception ignored) {}
    }

    // ================= ALGORITHMS =================
    private static AlgoResult dijkstra(int V, List<List<Edge>> graph, int src, int dest) {
        long start = System.nanoTime();
        int[] dist = new int[V];
        int[] parent = new int[V];
        Arrays.fill(dist, INF);
        Arrays.fill(parent, -1);
        dist[src] = 0;

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        pq.add(new int[]{0, src});

        while (!pq.isEmpty()) {
            int[] top = pq.poll();
            int d = top[0], u = top[1];
            if (d > dist[u]) continue;

            for (Edge edge : graph.get(u)) {
                if (dist[u] + edge.w < dist[edge.v]) {
                    dist[edge.v] = dist[u] + edge.w;
                    parent[edge.v] = u;
                    pq.add(new int[]{dist[edge.v], edge.v});
                }
            }
        }

        long end = System.nanoTime();
        AlgoResult res = new AlgoResult();
        res.dist = dist;
        res.parent = parent;
        res.elapsedNs = end - start;
        return res;
    }

    private static AlgoResult bellmanFord(int V, List<Edge> edges, int src, int dest) {
        long start = System.nanoTime();
        int[] dist = new int[V];
        int[] parent = new int[V];
        Arrays.fill(dist, INF);
        Arrays.fill(parent, -1);
        dist[src] = 0;

        for (int i = 0; i < V - 1; i++) {
            boolean updated = false;
            for (Edge edge : edges) {
                if (dist[edge.u] != INF && dist[edge.u] + edge.w < dist[edge.v]) {
                    dist[edge.v] = dist[edge.u] + edge.w;
                    parent[edge.v] = edge.u;
                    updated = true;
                }
            }
            if (!updated) break;
        }

        boolean hasNegCycle = false;
        for (Edge edge : edges) {
            if (dist[edge.u] != INF && dist[edge.u] + edge.w < dist[edge.v]) {
                hasNegCycle = true;
                break;
            }
        }

        long end = System.nanoTime();
        AlgoResult res = new AlgoResult();
        res.dist = dist;
        res.parent = parent;
        res.elapsedNs = end - start;
        res.hasNegativeCycle = hasNegCycle;
        return res;
    }

    private static AlgoResult floydWarshall(int V, List<Edge> edges, int src, int dest) {
        long start = System.nanoTime();
        int[][] dist = new int[V][V];
        int[][] nxt = new int[V][V];

        for (int i = 0; i < V; i++) {
            Arrays.fill(dist[i], INF);
            Arrays.fill(nxt[i], -1);
            dist[i][i] = 0;
            nxt[i][i] = i;
        }

        for (Edge edge : edges) {
            if (edge.w < dist[edge.u][edge.v]) {
                dist[edge.u][edge.v] = edge.w;
                nxt[edge.u][edge.v] = edge.v;
            }
        }

        for (int k = 0; k < V; k++) {
            for (int i = 0; i < V; i++) {
                for (int j = 0; j < V; j++) {
                    if (dist[i][k] != INF && dist[k][j] != INF && dist[i][k] + dist[k][j] < dist[i][j]) {
                        dist[i][j] = dist[i][k] + dist[k][j];
                        nxt[i][j] = nxt[i][k];
                    }
                }
            }
        }

        boolean hasNegCycle = false;
        for (int i = 0; i < V; i++) {
            if (dist[i][i] < 0) hasNegCycle = true;
        }

        long end = System.nanoTime();
        AlgoResult res = new AlgoResult();
        res.dist = dist[src];
        res.nxt = nxt;
        res.elapsedNs = end - start;
        res.hasNegativeCycle = hasNegCycle;
        return res;
    }

    private static AlgoResult johnsons(int V, List<Edge> edges, int src, int dest) {
        long start = System.nanoTime();
        List<Edge> dummyEdges = new ArrayList<>(edges);
        for (int i = 0; i < V; i++) dummyEdges.add(new Edge(V, i, 0));

        int[] h = new int[V + 1];
        Arrays.fill(h, INF);
        h[V] = 0;

        for (int i = 0; i < V; i++) {
            for (Edge edge : dummyEdges) {
                if (h[edge.u] != INF && h[edge.u] + edge.w < h[edge.v]) {
                    h[edge.v] = h[edge.u] + edge.w;
                }
            }
        }

        boolean hasNegCycle = false;
        for (Edge edge : dummyEdges) {
            if (h[edge.u] != INF && h[edge.u] + edge.w < h[edge.v]) {
                hasNegCycle = true;
                break;
            }
        }

        if (hasNegCycle) {
            AlgoResult res = new AlgoResult();
            res.hasNegativeCycle = true;
            return res;
        }

        List<List<Edge>> reweighted = new ArrayList<>();
        for (int i = 0; i < V; i++) reweighted.add(new ArrayList<>());

        for (Edge edge : edges) {
            int wPrime = edge.w + h[edge.u] - h[edge.v];
            reweighted.get(edge.u).add(new Edge(edge.u, edge.v, wPrime));
        }

        int[] distPrime = new int[V];
        int[] parent = new int[V];
        Arrays.fill(distPrime, INF);
        Arrays.fill(parent, -1);
        distPrime[src] = 0;

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[0]));
        pq.add(new int[]{0, src});

        while (!pq.isEmpty()) {
            int[] top = pq.poll();
            int d = top[0], u = top[1];
            if (d > distPrime[u]) continue;

            for (Edge edge : reweighted.get(u)) {
                if (distPrime[u] + edge.w < distPrime[edge.v]) {
                    distPrime[edge.v] = distPrime[u] + edge.w;
                    parent[edge.v] = u;
                    pq.add(new int[]{distPrime[edge.v], edge.v});
                }
            }
        }

        int[] dist = new int[V];
        Arrays.fill(dist, INF);
        for (int i = 0; i < V; i++) {
            if (distPrime[i] != INF) dist[i] = distPrime[i] - h[src] + h[i];
        }

        long end = System.nanoTime();
        AlgoResult res = new AlgoResult();
        res.dist = dist;
        res.parent = parent;
        res.elapsedNs = end - start;
        return res;
    }

    private static List<Integer> getPath(int[] parent, int dest, int distToDest) {
        if (distToDest == INF) return new ArrayList<>();
        List<Integer> path = new ArrayList<>();
        int curr = dest;
        while (curr != -1) {
            path.add(curr);
            curr = parent[curr];
        }
        Collections.reverse(path);
        return path;
    }

    private static List<Integer> getFwPath(int[][] nxt, int src, int dest, int distToDest) {
        if (distToDest == INF || nxt[src][dest] == -1) return new ArrayList<>();
        List<Integer> path = new ArrayList<>();
        path.add(src);
        int u = src;
        while (u != dest) {
            u = nxt[u][dest];
            if (u == -1) return new ArrayList<>();
            path.add(u);
        }
        return path;
    }

    private static void writeOutput(String text) {
        outputText.setEditable(true);
        outputText.append(text);
        outputText.setEditable(false);
    }

    // ================= GUI LOGIC =================
    private static void generateGraph() {
        try {
            int V, E;
            if (!verticesEntry.getText().isEmpty() && !edgesEntry.getText().isEmpty()) {
                V = Integer.parseInt(verticesEntry.getText());
                E = Integer.parseInt(edgesEntry.getText());
                int maxE = V * (V - 1);
                if (E > maxE) {
                    E = maxE;
                    edgesEntry.setText(String.valueOf(E));
                    JOptionPane.showMessageDialog(null, "Edges capped at " + maxE + " for " + V + " vertices.");
                }
            } else {
                Random rand = new Random();
                V = rand.nextInt(8) + 8; // 8 to 15
                E = rand.nextInt(V) + V + 5; // V+5 to V*2
                verticesEntry.setText(String.valueOf(V));
                edgesEntry.setText(String.valueOf(E));
            }

            int src, dest;
            if (sourceEntry.getText().isEmpty()) {
                src = new Random().nextInt(V);
                sourceEntry.setText(String.valueOf(src));
            } else {
                src = Integer.parseInt(sourceEntry.getText());
            }

            if (destinationEntry.getText().isEmpty()) {
                dest = new Random().nextInt(V);
                while (src == dest && V > 1) dest = new Random().nextInt(V);
                destinationEntry.setText(String.valueOf(dest));
            } else {
                dest = Integer.parseInt(destinationEntry.getText());
            }

            textInput.setText("");
            Set<String> generated = new HashSet<>();
            int attempts = 0;
            Random rand = new Random();
            while (generated.size() < E && attempts < E * 10) {
                attempts++;
                int u = rand.nextInt(V);
                int v = rand.nextInt(V);
                if (u != v && !generated.contains(u + "," + v)) {
                    int w = rand.nextInt(24) - 3; // -3 to 20
                    generated.add(u + "," + v);
                    textInput.append(u + " " + v + " " + w + "\n");
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Graph Generation Failed: " + e.getMessage());
        }
    }

    private static void runAlgorithms(boolean bestOnly, boolean cacheMode) {
        try {
            if (verticesEntry.getText().isEmpty() || edgesEntry.getText().isEmpty() || sourceEntry.getText().isEmpty() || destinationEntry.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please input graph details.");
                return;
            }
            int V = Integer.parseInt(verticesEntry.getText());
            int E = Integer.parseInt(edgesEntry.getText());
            int src = Integer.parseInt(sourceEntry.getText());
            int dest = Integer.parseInt(destinationEntry.getText());

            if (src >= V || dest >= V || src < 0 || dest < 0) {
                JOptionPane.showMessageDialog(null, "Source/Dest out of bounds.");
                return;
            }

            String text = textInput.getText().trim();
            if (text.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please input edges.");
                return;
            }

            List<Edge> edges = new ArrayList<>();
            List<List<Edge>> graph = new ArrayList<>();
            for (int i = 0; i < V; i++) graph.add(new ArrayList<>());
            boolean hasNegativeEdge = false;

            String[] lines = text.split("\n");
            for (String line : lines) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length != 3) continue;
                int u = Integer.parseInt(parts[0]), v = Integer.parseInt(parts[1]), w = Integer.parseInt(parts[2]);
                if (w < 0) hasNegativeEdge = true;
                Edge e = new Edge(u, v, w);
                edges.add(e);
                graph.get(u).add(e);
            }

            String selectedAlg = (bestOnly || cacheMode) ? (String) algorithmDropdown.getSelectedItem() : "Compare";
            
            // Generate Key
            List<String> sortedEdges = new ArrayList<>();
            for (Edge edge : edges) sortedEdges.add(edge.u + "," + edge.v + "," + edge.w);
            Collections.sort(sortedEdges);
            String graphKey = V + "|" + sortedEdges.toString() + "|" + src + "|" + dest + "|" + selectedAlg;

            outputText.setText("");

            if ((bestOnly || cacheMode) && learningCache.containsKey(graphKey)) {
                long cStart = System.nanoTime();
                CacheEntry cached = learningCache.get(graphKey);
                long cEnd = System.nanoTime();
                
                double currTimeMs = (cEnd - cStart) / 1_000_000.0;
                
                if (cacheMode) {
                    double red = cached.time_ms > 0 ? ((cached.time_ms - currTimeMs) / cached.time_ms) * 100 : 0;
                    writeOutput("===== CACHE ANALYSIS =====\n\nGraph Present In Cache : YES\n");
                    writeOutput("Stored Algorithm : " + cached.algorithm + "\n");
                    writeOutput(String.format("Previous Execution Time : %.5f ms\n", cached.time_ms));
                    writeOutput(String.format("Current Execution Time : %.5f ms\n", currTimeMs));
                    writeOutput(String.format("Time Reduced Percentage : %.2f%%\n", red));
                    writeOutput("Shortest Path Cost : " + (cached.cost != INF ? cached.cost : "Unreachable") + "\n");
                    writeOutput("Shortest Path : " + (cached.path.isEmpty() ? "None" : cached.path) + "\n");
                    return;
                } else {
                    writeOutput("===== MANUAL TEST ANALYSIS (CACHED) =====\n\n");
                    writeOutput("Selected Algorithm : " + cached.algorithm + " (Cached)\n");
                    writeOutput("Time Complexity    : O(1) Cache Lookup\n");
                    writeOutput("Space Complexity   : O(V) Cached Path\n");
                    writeOutput("Shortest Path Cost : " + (cached.cost != INF ? cached.cost : "Unreachable") + "\n");
                    writeOutput("Execution Time     : " + (cEnd - cStart) + " ns\n");
                    writeOutput(String.format("Execution Time     : %.5f ms\n", currTimeMs));
                    writeOutput("Shortest Path      : " + (cached.path.isEmpty() ? "None" : cached.path) + "\n");
                    return;
                }
            }

            if (cacheMode) {
                writeOutput("===== CACHE ANALYSIS =====\n\nGraph Present In Cache : NO\n\nRunning Algorithm to cache the result...\n\n");
            }

            if (bestOnly || cacheMode) {
                String algName = "";
                String timeComp = "";
                String spaceComp = "";
                List<Integer> path = new ArrayList<>();
                int cost = 0;
                long elapsed = 0;
                
                if ("Adaptive Best (Default)".equals(selectedAlg)) {
                    if (hasNegativeEdge) {
                        algName = "Bellman-Ford (Adaptive)";
                        AlgoResult r = bellmanFord(V, edges, src, dest);
                        if (r.hasNegativeCycle) { JOptionPane.showMessageDialog(null, "Negative Cycle!"); return; }
                        cost = r.dist[dest]; path = getPath(r.parent, dest, cost); elapsed = r.elapsedNs;
                        timeComp = "O(V × E)"; spaceComp = "O(V)";
                    } else {
                        algName = "Dijkstra (Adaptive)";
                        AlgoResult r = dijkstra(V, graph, src, dest);
                        cost = r.dist[dest]; path = getPath(r.parent, dest, cost); elapsed = r.elapsedNs;
                        timeComp = "O(E log V)"; spaceComp = "O(V)";
                    }
                } else if ("Dijkstra".equals(selectedAlg)) {
                    if (hasNegativeEdge) JOptionPane.showMessageDialog(null, "Warning: Dijkstra may fail with negative edges.");
                    algName = "Dijkstra";
                    AlgoResult r = dijkstra(V, graph, src, dest);
                    cost = r.dist[dest]; path = getPath(r.parent, dest, cost); elapsed = r.elapsedNs;
                    timeComp = "O(E log V)"; spaceComp = "O(V)";
                } else if ("Bellman-Ford".equals(selectedAlg)) {
                    algName = "Bellman-Ford";
                    AlgoResult r = bellmanFord(V, edges, src, dest);
                    if (r.hasNegativeCycle) { JOptionPane.showMessageDialog(null, "Negative Cycle!"); return; }
                    cost = r.dist[dest]; path = getPath(r.parent, dest, cost); elapsed = r.elapsedNs;
                    timeComp = "O(V × E)"; spaceComp = "O(V)";
                } else if ("Floyd-Warshall".equals(selectedAlg)) {
                    algName = "Floyd-Warshall";
                    AlgoResult r = floydWarshall(V, edges, src, dest);
                    if (r.hasNegativeCycle) { JOptionPane.showMessageDialog(null, "Negative Cycle!"); return; }
                    cost = r.dist[dest]; path = getFwPath(r.nxt, src, dest, cost); elapsed = r.elapsedNs;
                    timeComp = "O(V³)"; spaceComp = "O(V²)";
                } else if ("Johnson's".equals(selectedAlg)) {
                    algName = "Johnson's";
                    AlgoResult r = johnsons(V, edges, src, dest);
                    if (r.hasNegativeCycle) { JOptionPane.showMessageDialog(null, "Negative Cycle!"); return; }
                    cost = r.dist[dest]; path = getPath(r.parent, dest, cost); elapsed = r.elapsedNs;
                    timeComp = "O(V² log V + V E)"; spaceComp = "O(V²)";
                }

                double msTime = elapsed / 1_000_000.0;
                writeOutput("===== MANUAL TEST ANALYSIS =====\n\n");
                writeOutput("Selected Algorithm : " + algName + "\n");
                writeOutput("Time Complexity    : " + timeComp + "\n");
                writeOutput("Space Complexity   : " + spaceComp + "\n");
                writeOutput("Shortest Path Cost : " + (cost != INF ? cost : "Unreachable") + "\n");
                writeOutput("Execution Time     : " + elapsed + " ns\n");
                writeOutput(String.format("Execution Time     : %.5f ms\n", msTime));
                writeOutput("Shortest Path      : " + (path.isEmpty() ? "None" : path) + "\n");

                CacheEntry entry = new CacheEntry();
                entry.path = path;
                entry.time_ms = msTime;
                entry.algorithm = algName.split(" ")[0];
                entry.cost = cost;
                learningCache.put(graphKey, entry);
                saveCache();
                
            } else {
                AlgoResult dRes = dijkstra(V, graph, src, dest);
                AlgoResult bRes = bellmanFord(V, edges, src, dest);
                AlgoResult fwRes = floydWarshall(V, edges, src, dest);
                AlgoResult jRes = johnsons(V, edges, src, dest);

                if (bRes.hasNegativeCycle || fwRes.hasNegativeCycle || jRes.hasNegativeCycle) {
                    JOptionPane.showMessageDialog(null, "Negative Cycle Detected!");
                    return;
                }

                double dMs = dRes.elapsedNs / 1_000_000.0;
                double bMs = bRes.elapsedNs / 1_000_000.0;
                double fwMs = fwRes.elapsedNs / 1_000_000.0;
                double jMs = jRes.elapsedNs / 1_000_000.0;

                writeOutput("===== COMPARISON TABLE =====\n\n");
                writeOutput("Algorithm\tTime(ms)\n");
                writeOutput(String.format("Dijkstra\t%.5f\n", dMs));
                writeOutput(String.format("BellmanFord\t%.5f\n", bMs));
                writeOutput(String.format("FloydWarshall\t%.5f\n", fwMs));
                writeOutput(String.format("Johnsons\t%.5f\n", jMs));

                showComparisonChart(new double[]{dMs, bMs, fwMs, jMs});

                String fastest = "";
                double bestTime = 0;
                List<Integer> bestPath = null;
                int bestCost = 0;
                String cacheAlg = "";

                if (hasNegativeEdge) {
                    if (bMs <= jMs) {
                        fastest = "Bellman-Ford";
                        bestPath = getPath(bRes.parent, dest, bRes.dist[dest]);
                        bestCost = bRes.dist[dest];
                        bestTime = bMs;
                        cacheAlg = "Bellman-Ford";
                    } else {
                        fastest = "Johnson's";
                        bestPath = getPath(jRes.parent, dest, jRes.dist[dest]);
                        bestCost = jRes.dist[dest];
                        bestTime = jMs;
                        cacheAlg = "Johnson's";
                    }
                } else {
                    double[] times = {dMs, bMs, fwMs, jMs};
                    String[] names = {"Dijkstra", "Bellman-Ford", "Floyd-Warshall", "Johnson's"};
                    int minIdx = 0;
                    for (int i=1; i<4; i++) if(times[i] < times[minIdx]) minIdx = i;
                    
                    fastest = names[minIdx];
                    bestTime = times[minIdx];
                    cacheAlg = fastest;
                    
                    if (minIdx == 0) { bestPath = getPath(dRes.parent, dest, dRes.dist[dest]); bestCost = dRes.dist[dest]; }
                    else if (minIdx == 1) { bestPath = getPath(bRes.parent, dest, bRes.dist[dest]); bestCost = bRes.dist[dest]; }
                    else if (minIdx == 2) { bestPath = getFwPath(fwRes.nxt, src, dest, fwRes.dist[dest]); bestCost = fwRes.dist[dest]; }
                    else if (minIdx == 3) { bestPath = getPath(jRes.parent, dest, jRes.dist[dest]); bestCost = jRes.dist[dest]; }
                }

                writeOutput("\nRecommended Algorithm : " + fastest + "\n");
                writeOutput("Shortest Path Cost : " + (bestCost != INF ? bestCost : "Unreachable") + "\n");

                CacheEntry entry = new CacheEntry();
                entry.path = bestPath;
                entry.time_ms = bestTime;
                entry.algorithm = cacheAlg;
                entry.cost = bestCost;
                learningCache.put(graphKey, entry);
                saveCache();
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    private static void showGraphManually() {
        try {
            if (verticesEntry.getText().isEmpty() || edgesEntry.getText().isEmpty() || sourceEntry.getText().isEmpty() || destinationEntry.getText().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please input graph details.");
                return;
            }
            int V = Integer.parseInt(verticesEntry.getText());
            int src = Integer.parseInt(sourceEntry.getText());
            int dest = Integer.parseInt(destinationEntry.getText());

            String text = textInput.getText().trim();
            if (text.isEmpty()) return;

            List<Edge> edges = new ArrayList<>();
            String[] lines = text.split("\n");
            for (String line : lines) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length != 3) continue;
                edges.add(new Edge(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
            }

            String selectedAlg = (String) algorithmDropdown.getSelectedItem();
            List<String> sortedEdges = new ArrayList<>();
            for (Edge edge : edges) sortedEdges.add(edge.u + "," + edge.v + "," + edge.w);
            Collections.sort(sortedEdges);
            
            String gKeyOpt1 = V + "|" + sortedEdges.toString() + "|" + src + "|" + dest + "|" + selectedAlg;
            String gKeyOpt2 = V + "|" + sortedEdges.toString() + "|" + src + "|" + dest + "|Compare";

            List<Integer> path = null;
            if (learningCache.containsKey(gKeyOpt1)) path = learningCache.get(gKeyOpt1).path;
            else if (learningCache.containsKey(gKeyOpt2)) path = learningCache.get(gKeyOpt2).path;

            drawGraphs(V, edges, path, graphMode.isSelected());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    private static void drawGraphs(int V, List<Edge> edges, List<Integer> shortestPath, boolean is3d) {
        if (is3d) {
            draw3DGraph(V, edges, shortestPath);
        } else {
            draw2DGraph(V, edges, shortestPath);
        }
    }

    private static void draw2DGraph(int V, List<Edge> edges, List<Integer> shortestPath) {
        Graph graph = new SingleGraph("2D Graph");
        graph.setAttribute("ui.stylesheet", 
            "node { size: 30px; fill-color: #87CEEB; text-size: 16px; text-color: black; text-alignment: center; } " +
            "edge { text-size: 14px; text-color: blue; text-background-mode: plain; }");

        for (int i = 0; i < V; i++) {
            Node n = graph.addNode(String.valueOf(i));
            n.setAttribute("ui.label", String.valueOf(i));
            if (shortestPath != null && shortestPath.contains(i)) {
                n.setAttribute("ui.style", "fill-color: lightgreen;");
            }
        }

        Set<String> pathEdges = new HashSet<>();
        if (shortestPath != null && shortestPath.size() > 1) {
            for (int i = 0; i < shortestPath.size() - 1; i++) {
                pathEdges.add(shortestPath.get(i) + "-" + shortestPath.get(i+1));
            }
        }

        int eId = 0;
        for (Edge e : edges) {
            org.graphstream.graph.Edge ge = graph.addEdge(String.valueOf(eId++), String.valueOf(e.u), String.valueOf(e.v), true);
            ge.setAttribute("ui.label", String.valueOf(e.w));
            if (pathEdges.contains(e.u + "-" + e.v)) {
                ge.setAttribute("ui.style", "fill-color: red; size: 4px;");
            }
        }

        Viewer viewer = graph.display();
        viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);

        if (shortestPath != null && shortestPath.isEmpty()) {
            JOptionPane.showMessageDialog(null, "NO PATH EXISTS BETWEEN SOURCE AND DESTINATION", "Path Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static void draw3DGraph(int V, List<Edge> edges, List<Integer> shortestPath) {
        try {
            StringBuilder html = new StringBuilder();
            html.append("<html><head><script src='https://cdn.plot.ly/plotly-2.27.0.min.js'></script></head><body>");
            html.append("<div id='graph' style='width:100%;height:100%;'></div>");
            html.append("<script>");
            
            // Generate random 3D positions
            Random r = new Random(42);
            double[][] pos = new double[V][3];
            for (int i=0; i<V; i++) {
                pos[i][0] = r.nextDouble() * 10;
                pos[i][1] = r.nextDouble() * 10;
                pos[i][2] = r.nextDouble() * 10;
            }

            Set<String> pathEdges = new HashSet<>();
            if (shortestPath != null && shortestPath.size() > 1) {
                for (int i = 0; i < shortestPath.size() - 1; i++) {
                    pathEdges.add(shortestPath.get(i) + "-" + shortestPath.get(i+1));
                }
            }

            StringBuilder edgeX = new StringBuilder("[");
            StringBuilder edgeY = new StringBuilder("[");
            StringBuilder edgeZ = new StringBuilder("[");
            
            StringBuilder pathX = new StringBuilder("[");
            StringBuilder pathY = new StringBuilder("[");
            StringBuilder pathZ = new StringBuilder("[");

            StringBuilder midX = new StringBuilder("[");
            StringBuilder midY = new StringBuilder("[");
            StringBuilder midZ = new StringBuilder("[");
            StringBuilder midText = new StringBuilder("[");

            for (Edge e : edges) {
                double x0 = pos[e.u][0], y0 = pos[e.u][1], z0 = pos[e.u][2];
                double x1 = pos[e.v][0], y1 = pos[e.v][1], z1 = pos[e.v][2];

                if (pathEdges.contains(e.u + "-" + e.v)) {
                    pathX.append(x0).append(",").append(x1).append(",null,");
                    pathY.append(y0).append(",").append(y1).append(",null,");
                    pathZ.append(z0).append(",").append(z1).append(",null,");
                } else {
                    edgeX.append(x0).append(",").append(x1).append(",null,");
                    edgeY.append(y0).append(",").append(y1).append(",null,");
                    edgeZ.append(z0).append(",").append(z1).append(",null,");
                }

                midX.append((x0+x1)/2).append(",");
                midY.append((y0+y1)/2).append(",");
                midZ.append((z0+z1)/2).append(",");
                midText.append("'").append(e.w).append("',");
            }
            
            edgeX.append("]"); edgeY.append("]"); edgeZ.append("]");
            pathX.append("]"); pathY.append("]"); pathZ.append("]");
            midX.append("]"); midY.append("]"); midZ.append("]"); midText.append("]");

            StringBuilder nodeX = new StringBuilder("[");
            StringBuilder nodeY = new StringBuilder("[");
            StringBuilder nodeZ = new StringBuilder("[");
            StringBuilder nodeColor = new StringBuilder("[");
            StringBuilder nodeText = new StringBuilder("[");

            for (int i=0; i<V; i++) {
                nodeX.append(pos[i][0]).append(",");
                nodeY.append(pos[i][1]).append(",");
                nodeZ.append(pos[i][2]).append(",");
                nodeText.append("'").append(i).append("',");
                if (shortestPath != null && shortestPath.contains(i)) nodeColor.append("'lightgreen',");
                else nodeColor.append("'skyblue',");
            }
            nodeX.append("]"); nodeY.append("]"); nodeZ.append("]"); nodeColor.append("]"); nodeText.append("]");

            html.append("var edgeTrace = { x: ").append(edgeX).append(", y: ").append(edgeY).append(", z: ").append(edgeZ)
                .append(", mode: 'lines', line: {width: 3, color: 'gray'}, type: 'scatter3d', hoverinfo: 'none' };\n");
            
            html.append("var pathTrace = { x: ").append(pathX).append(", y: ").append(pathY).append(", z: ").append(pathZ)
                .append(", mode: 'lines', line: {width: 8, color: 'red'}, type: 'scatter3d', hoverinfo: 'none' };\n");
            
            html.append("var weightTrace = { x: ").append(midX).append(", y: ").append(midY).append(", z: ").append(midZ)
                .append(", mode: 'text', text: ").append(midText).append(", textposition: 'middle center', textfont: {color: 'darkred', size: 14, family: 'Arial'}, type: 'scatter3d', hoverinfo: 'none' };\n");

            html.append("var nodeTrace = { x: ").append(nodeX).append(", y: ").append(nodeY).append(", z: ").append(nodeZ)
                .append(", mode: 'markers+text', text: ").append(nodeText)
                .append(", textposition: 'middle center', textfont: {color: 'black', size: 14, family: 'Arial'}")
                .append(", marker: {size: 14, color: ").append(nodeColor).append(", line: {color: 'black', width: 2}}")
                .append(", type: 'scatter3d', hoverinfo: 'none' };\n");

            html.append("var data = [edgeTrace, pathTrace, nodeTrace, weightTrace];\n");
            
            String title = "Interactive 3D Graph Visualization";
            if (shortestPath != null && shortestPath.isEmpty()) {
                title += "<br><span style='color:red; font-weight:bold;'>NO PATH EXISTS BETWEEN SOURCE AND DESTINATION</span>";
            }

            html.append("var layout = { title: \"").append(title).append("\", showlegend: false, scene: { xaxis: {visible: false}, yaxis: {visible: false}, zaxis: {visible: false} } };\n");
            html.append("Plotly.newPlot('graph', data, layout, {displayModeBar: false, displaylogo: false});\n");

            html.append("</script></body></html>");

            File f = File.createTempFile("graph_3d", ".html");
            Files.writeString(f.toPath(), html.toString());
            Desktop.getDesktop().browse(f.toURI());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void showComparisonChart(double[] times) {
        JFrame chartFrame = new JFrame("Execution Time Comparison");
        chartFrame.setSize(800, 500);
        chartFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int width = getWidth();
                int height = getHeight();
                
                int padding = 60;
                int labelPadding = 40;
                
                double maxTime = 0;
                for (double t : times) if (t > maxTime) maxTime = t;
                if (maxTime == 0) maxTime = 1;
                
                g2d.setColor(Color.BLACK);
                // Draw Axes
                g2d.drawLine(padding, height - padding - labelPadding, padding, padding); // Y axis
                g2d.drawLine(padding, height - padding - labelPadding, width - padding, height - padding - labelPadding); // X axis
                
                // Y axis title
                g2d.setFont(new Font("Arial", Font.BOLD, 12));
                java.awt.geom.AffineTransform orig = g2d.getTransform();
                g2d.rotate(-Math.PI / 2);
                g2d.drawString("Execution Time (ms)", -height / 2 - 40, padding - 40);
                g2d.setTransform(orig);
                
                // X axis title
                g2d.drawString("Algorithms", width / 2 - 30, height - padding + 20);
                
                // Chart Title
                g2d.setFont(new Font("Arial", Font.PLAIN, 16));
                g2d.drawString("Execution Time Comparison", width / 2 - 100, padding - 20);
                
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                
                String[] names = {"Dijkstra", "Bellman-Ford", "Floyd-Warshall", "Johnson's"};
                Color[] colors = {Color.BLUE, new Color(0, 128, 0), Color.ORANGE, new Color(128, 0, 128)};
                
                int numBars = times.length;
                int barWidth = (width - 2 * padding) / numBars - 20;
                
                for (int i = 0; i < numBars; i++) {
                    int x = padding + i * (barWidth + 20) + 10;
                    // Add 40 to maxTime so the tallest bar doesn't touch the top
                    int barHeight = (int) ((times[i] / (maxTime * 1.1)) * (height - 2 * padding - labelPadding));
                    int y = height - padding - labelPadding - barHeight;
                    
                    g2d.setColor(colors[i]);
                    g2d.fillRect(x, y, barWidth, barHeight);
                    
                    g2d.setColor(Color.BLACK);
                    // Label below
                    int labelWidth = g2d.getFontMetrics().stringWidth(names[i]);
                    g2d.drawString(names[i], x + (barWidth - labelWidth) / 2, height - padding - labelPadding + 20);
                    
                    // Value above
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    String valStr = String.format("%.5f", times[i]);
                    int valWidth = g2d.getFontMetrics().stringWidth(valStr);
                    g2d.drawString(valStr, x + (barWidth - valWidth) / 2, y - 10);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                }
            }
        };
        
        panel.setBackground(Color.WHITE);
        chartFrame.add(panel);
        chartFrame.setLocationRelativeTo(null);
        chartFrame.setVisible(true);
    }
}
