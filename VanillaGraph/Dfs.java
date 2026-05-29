package VanillaGraph;

import java.util.*;

/**
 * Java conversion of plot_glauber.py
 * Implements Glauber dynamics coupling for random graph processes.
 */ 

/**
 * PlotGlauber — a DFS-based dynamic graph that is API-compatible with VanillaGraph.Dfs,
 * so CouplingPast can use it as a drop-in replacement in the vanilla route.
 *
 * Instance methods match Dfs exactly:
 *   add_vertex(i), addEdge(u,v), removeEdge(u,v), connected(u,v), explore(start), max_cc()
 *
 * The static glauberQ() method is retained for standalone use (Main.java).
 */
public class Dfs {

    // -------------------------------------------------------------------------
    // Instance state — mirrors Dfs.graph so CouplingPast can access it directly:
    //   vg1.graph.get(edge[0]).contains(edge[1])
    // -------------------------------------------------------------------------
    public Map<Integer, Set<Integer>> graph;
    private int n;

    /** Constructs an empty graph with capacity for n vertices. Mirrors: new Dfs(n) */
    public Dfs(int n) {
        this.n = n;
        this.graph = new HashMap<>();
    }

    // -------------------------------------------------------------------------
    // Instance API — matches VanillaGraph.Dfs method-for-method
    // -------------------------------------------------------------------------

    /** Adds a vertex with no edges. Mirrors: vg1.add_vertex(i) */
    public void add_vertex(int i) {
        graph.putIfAbsent(i, new HashSet<>());
    }

    /** Adds an undirected edge. Mirrors: vg1.addEdge(u, v) */
    public void addEdge(int u, int v) {
        graph.computeIfAbsent(u, k -> new HashSet<>()).add(v);
        graph.computeIfAbsent(v, k -> new HashSet<>()).add(u);
    }

    /** Removes an undirected edge. Mirrors: vg1.removeEdge(u, v) */
    public void removeEdge(int u, int v) {
        if (graph.containsKey(u)) graph.get(u).remove(v);
        if (graph.containsKey(v)) graph.get(v).remove(u);
    }

    /**
     * Returns true if u and v are in the same connected component.
     * Uses iterative DFS so it is safe for large graphs.
     * Mirrors: vg1.connected(u, v)
     */
    public boolean connected(int u, int v) {
        if (!graph.containsKey(u) || !graph.containsKey(v)) return false;
        if (u == v) return true;
        return exploreStack(graph, u).contains(v);
    }

    /**
     * Returns all nodes reachable from start (the connected component of start).
     * Mirrors: vg1.explore(start)
     */
    public Set<Integer> explore(int start) {
        return exploreStack(graph, start);
    }

    /**
     * Returns the size of the largest connected component.
     * Mirrors: vg1.max_cc()
     */
    public int max_cc() {
        Set<Integer> allVisited = new HashSet<>();
        int maxSize = 0;
        for (int node : graph.keySet()) {
            if (!allVisited.contains(node)) {
                Set<Integer> component = exploreStack(graph, node);
                allVisited.addAll(component);
                if (component.size() > maxSize) {
                    maxSize = component.size();
                }
            }
        }
        return maxSize;
    }

    // -------------------------------------------------------------------------
    // Static helpers — used by glauberQ and available for general use
    // -------------------------------------------------------------------------

    /**
     * Iterative DFS from start over the given graph map.
     * Safe for large graphs (no recursion stack overflow risk).
     */
    public static Set<Integer> exploreStack(Map<Integer, Set<Integer>> graph, int start) {
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(start);
        Set<Integer> visited = new HashSet<>();
        visited.add(start);

        while (!stack.isEmpty()) {
            int v = stack.pop();
            for (int u : graph.get(v)) {
                if (!visited.contains(u)) {
                    visited.add(u);
                    stack.push(u);
                }
                // Early exit once the entire graph is visited
                if (visited.size() == graph.size()) return visited;
            }
        }
        return visited;
    }

    /**
     * Recursive DFS — kept for compatibility with the original Python code.
     * WARNING: may throw StackOverflowError on graphs with 1000+ nodes.
     * Use exploreStack() for large graphs.
     */
    public static Set<Integer> explore(Map<Integer, Set<Integer>> graph, int start, Set<Integer> visited) {
        if (visited == null) visited = new HashSet<>();
        visited.add(start);
        Set<Integer> neighbors = new HashSet<>(graph.get(start));
        neighbors.removeAll(visited);
        for (int next : neighbors) {
            explore(graph, next, visited);
        }
        return visited;
    }

    /**
     * Returns all connected components of graph as a list of sets.
     */
    public static List<Set<Integer>> dfs(Map<Integer, Set<Integer>> graph) {
        List<Set<Integer>> components = new ArrayList<>();
        Set<Integer> allVisited = new HashSet<>();
        for (int start : graph.keySet()) {
            if (!allVisited.contains(start)) {
                Set<Integer> component = exploreStack(graph, start);
                components.add(component);
                allVisited.addAll(component);
            }
        }
        return components;
    }

    // -------------------------------------------------------------------------
    // Static glauberQ — standalone coupling algorithm (used by Main.java)
    // -------------------------------------------------------------------------

    /**
     * Runs Glauber dynamics coupling between graph1 (complete graph) and graph2 (empty graph)
     * until both have the same number of edges.
     *
     * @param graph1 Complete graph as adjacency map
     * @param graph2 Empty graph as adjacency map
     * @param p      Edge probability parameter
     * @param q      Bias parameter for the stationary distribution
     * @return double[] { largestComponentSize, stepsTaken }
     */
    public static double[] glauberQ(
            Map<Integer, Set<Integer>> graph1,
            Map<Integer, Set<Integer>> graph2,
            double p,
            double q) {
        System.out.println("Starting glauberQ with p = " + p + ", q = " + q);
        int rows = graph1.size();
        double edges1 = ((double) rows * rows - rows) / 2.0;
        double edges2 = 0;
        double pi = p / (p + q * (1.0 - p));

        long t = 0;
        Random rand = new Random();

        // Track connected components incrementally
        // ans1 starts as one big component (complete graph); ans2 starts as n singletons
        List<List<Integer>> ans1 = new ArrayList<>();
        List<Integer> allNodes = new ArrayList<>();
        for (int i = 0; i < rows; i++) allNodes.add(i);
        ans1.add(allNodes);

        List<List<Integer>> ans2 = new ArrayList<>();
        for (int i = 0; i < rows; i++) ans2.add(new ArrayList<>(List.of(i)));

        while (edges1 != edges2) {
            t++;
            double r = rand.nextDouble();
            int u = rand.nextInt(rows);
            int v = rand.nextInt(rows);

            if (u == v) { t--; continue; }

            // Temporarily remove (u,v) from both graphs to test cut-edge status
            boolean g1rem = graph1.get(u).remove(v);
            if (g1rem) graph1.get(v).remove(u);
            boolean g2rem = graph2.get(u).remove(v);
            if (g2rem) graph2.get(v).remove(u);

            List<Integer> uCc1 = new ArrayList<>(exploreStack(graph1, u));
            List<Integer> vCc1 = new ArrayList<>(exploreStack(graph1, v));
            boolean cutEdge1 = !uCc1.contains(v);

            List<Integer> uCc2 = new ArrayList<>(exploreStack(graph2, u));
            List<Integer> vCc2 = new ArrayList<>(exploreStack(graph2, v));
            boolean cutEdge2 = !uCc2.contains(v);

            // Restore edges
            if (g1rem) { graph1.get(u).add(v); graph1.get(v).add(u); }
            if (g2rem) { graph2.get(u).add(v); graph2.get(v).add(u); }

            // Pull out (and drop) the CC containing both u and v, if any
            boolean sameCc1 = removeSharedCc(ans1, u, v);
            boolean sameCc2 = removeSharedCc(ans2, u, v);

            // Apply Markov update to graph1 / ans1
            edges1 = applyUpdate(graph1, ans1, u, v, uCc1, vCc1, cutEdge1, sameCc1, r, p, pi, edges1);

            // Apply Markov update to graph2 / ans2
            edges2 = applyUpdate(graph2, ans2, u, v, uCc2, vCc2, cutEdge2, sameCc2, r, p, pi, edges2);
        }

        // Find the largest CC in the final graph1
        int maxSize = 0;
        for (List<Integer> scc : ans1) {
            if (scc.size() > maxSize) {
                maxSize = scc.size();
                System.out.println("Largest CC so far: " + scc);
            }
        }
        System.out.println("maxSize: " + maxSize);
        return new double[]{maxSize, t};
    }

    /**
     * Removes (from ans) the unique CC that contains both u and v.
     * Returns true if such a CC existed (meaning u and v were already connected).
     */
    private static boolean removeSharedCc(List<List<Integer>> ans, int u, int v) {
        Iterator<List<Integer>> it = ans.iterator();
        while (it.hasNext()) {
            List<Integer> cc = it.next();
            if (cc.contains(u) && cc.contains(v)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    /**
     * Applies one Glauber step to a single graph and its CC list.
     * Returns the updated edge count.
     */
    private static double applyUpdate(
            Map<Integer, Set<Integer>> graph,
            List<List<Integer>> ans,
            int u, int v,
            List<Integer> uCc, List<Integer> vCc,
            boolean cutEdge, boolean sameCc,
            double r, double p, double pi,
            double edgeCount) {

        if (cutEdge) {
            if (r <= pi) {
                // Add edge — merges the two components
                if (!graph.get(u).contains(v)) {
                    edgeCount++;
                    graph.get(u).add(v);
                    graph.get(v).add(u);
                }
                if (sameCc) {
                    // Already in same CC; re-add combined view
                    ans.add(merged(uCc, vCc));
                } else {
                    // Remove both individual CCs and merge
                    ans.removeIf(cc -> cc.contains(u) || cc.contains(v));
                    ans.add(merged(uCc, vCc));
                }
            } else {
                // Remove edge — splits the component
                if (graph.get(u).contains(v)) {
                    graph.get(u).remove(v);
                    graph.get(v).remove(u);
                    edgeCount--;
                }
                if (sameCc) {
                    ans.add(new ArrayList<>(uCc));
                    ans.add(new ArrayList<>(vCc));
                }
                // If not in same CC, nothing to split
            }
        } else if (r <= p) {
            // Not a cut edge, add it
            if (!graph.get(u).contains(v)) {
                edgeCount++;
                graph.get(u).add(v);
                graph.get(v).add(u);
            }
            if (sameCc) {
                ans.add(new ArrayList<>(uCc));
            } else {
                ans.removeIf(cc -> cc.contains(u) || cc.contains(v));
                ans.add(merged(uCc, vCc));
            }
        } else {
            // Not a cut edge, remove it
            if (graph.get(u).contains(v)) {
                graph.get(u).remove(v);
                graph.get(v).remove(u);
                edgeCount--;
            }
            if (sameCc) {
                ans.add(new ArrayList<>(uCc));
            }
            // Not in same CC + not a cut edge: no CC change
        }
        return edgeCount;
    }

    /** Returns a new list containing all elements of a and b. */
    private static List<Integer> merged(List<Integer> a, List<Integer> b) {
        List<Integer> result = new ArrayList<>(a);
        result.addAll(b);
        return result;
    }
}


// public class Dfs {
//     public Map<Integer, Set<Integer>> graph = new HashMap<>();
//     int n;
//     int cycles;
    
//     //ArrayList<ConnVertex> dyn_vertices;
//     public Dfs(int size){
//         n = size;
//     }

//     public boolean hasEdge(int u, int v){
//         return graph.get(u).contains(v) || graph.get(v).contains(u);
//     }

//     public void add_vertex(int v){
//         graph.put(v, new HashSet<>());
//     }

//     public void addEdge(int u, int v){
//         graph.get(u).add(v);
//         graph.get(v).add(u);
//     }
//     public void removeEdge(int u, int v){
//         graph.get(u).remove(v);
//         graph.get(v).remove(u);
//     }

//     public boolean connected(int u, int v){
//         Set<Integer> cc_u = explore(u);
//         return cc_u.contains(v);
//     }

//     public Set<Integer> explore(int start){
//         Set<Integer> visited = new HashSet<>();
//         ArrayList<Integer> stack = new ArrayList<>();
//         stack.add(start);
//         int v;
//         while(stack.isEmpty() == false){
//             v = stack.remove(stack.size()-1);
//             for(int u : graph.get(v)){
//                 if(!visited.contains(u)){
//                     visited.add(u);
//                     stack.add(u);
//                 }
//                 if(visited.size() == n){
//                     stack.clear();
//                     break;
//                 }
//             }
//         }

//         return visited;
//     }

//     public void dfs(int start){
//         ArrayList<Integer> stack = new ArrayList<>();
//         stack.add(start);
//         Set<Integer> visited = new HashSet<>();
//         visited.add(start);
//         ArrayList<Integer> remaining_unvisited = new ArrayList<>();
//         for(int i = 0; i < n; i++){
//             if (i!=start && !graph.get(start).contains(i)){
//                 remaining_unvisited.add(i);
//             }
//             else {
//                 visited.add(i);
//             }
//         }
//         while(remaining_unvisited.isEmpty() == false){
//             visited.addAll(explore(remaining_unvisited.get(remaining_unvisited.size())));
//             remaining_unvisited.clear();
//             for(int i = 0; i < n; i++){
//                 if (!visited.contains(i)){
//                     remaining_unvisited.add(i);
//                 }
//             }
//         }
//     }

//     public int max_cc(){
//         int max_size = 0;
//         for(int i = 0; i < n; i++){
//             Set<Integer> cc = explore(i);
//             if(cc.size() > max_size){
//                 max_size = cc.size();
//             }
//         }
//         return max_size;
//     }
// }
