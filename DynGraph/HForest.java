package DynGraph;

import java.util.*;


// Manages the full hierarchy forest H.
// One H-root per connected component.
// One H-leaf per original vertex.
public class HForest {

    public final int n;
    public final int dMax;
    public final int betaLogLogN;
    //public Map<Integer, HNode> vertexToLeaf; // maps original vertex to its H-leaf node
    //public Map<EdgeRecord, HNode> edgeMap;
    // leaves[v] = the H-leaf node for original vertex v
    public final HNode[] leaves;

    // current H-roots (one per connected component)
    public final Set<HNode> roots;
    public double timeForAddEdge = 0.0;
    public double timeForDeleteEdge = 0.0;
    public double timeForRecompute = 0.0;
    public double timeForConnected = 0.0;
    public double timeInteractedWith = 0.0; //time spent overall while interacting with the HForest, including time spent in add_edge, delete_edge, and connected. This is for benchmarking purposes to see how much of the total time is spent in these functions versus other overhead.
    public int numconn = 0;
    public HForest(int n) {
        this.n    = n;
        this.dMax = Math.max(1, (int) Math.floor(Math.log(n) / Math.log(2)));

        double logN    = Math.log(n) / Math.log(2);
        double logLogN = Math.log(Math.max(2, logN)) / Math.log(2);
        this.betaLogLogN = Math.max(1, (int) Math.ceil(2 * logLogN)); // beta=2

        this.leaves = new HNode[n];
        this.roots  = new HashSet<>();
        //this.vertexToLeaf = new HashMap<>();
        // Initially: each vertex is its own component.
        //initialize every lead node
        // Each leaf is also its own root (a single-node hierarchy tree).
        //leaves will start off at depth 1, and then as we add edges and merge components, they will get promoted up the tree and their depth will increase.
        //this does mean nto all leaves will be at the same depth

        for (int v = 0; v < n; v++) {
            HNode leaf = new HNode(n, 1);
            leaf.depth = dMax+1;
            leaf.weight         = 1;
            leaf.leafData       = new HLeaf(v, leaf.depth, leaf);
            leaf.isRoot         = true;
            this.leaves[v]           = leaf;
            //to start with, all leaves are roots
            this.roots.add(leaf);
            //this.vertexToLeaf.put(v, leaf);
            ////System.out.println("Initialized leaf for vertex " + v + " with depth " + leaf.depth + "ID = " + leaf.ID);
        }
    }

    // In HForest.java — add a private constructor for testing
    public HForest(int n, boolean empty) {
        this.n    = n;
        this.dMax = Math.max(1, (int) Math.floor(Math.log(n) / Math.log(2))) + 1;
        double logN    = Math.log(n) / Math.log(2);
        double logLogN = Math.log(Math.max(2, logN)) / Math.log(2);
        this.betaLogLogN = Math.max(1, (int) Math.ceil(2 * logLogN));
        this.leaves = new HNode[n];
        this.roots  = new HashSet<>();
        
        // No initialization — caller builds everything
    }

    public static HForest emptyForTest(int n) {
        return new HForest(n, true);
    }

    // ---------------------------------------------------------------
    // Connectivity query — O(depth) walk up to root.
    // STUB: will be replaced by the Euler tour forest 𝔇_F
    //       giving O(log n / log log log n) worst case query time.
    // ---------------------------------------------------------------
    public boolean connected(int u, int v) {
        numconn++;
        long start_time = System.nanoTime();
        boolean connected = HNode.root(leaves[u]) == HNode.root(leaves[v]);
        timeForConnected += System.nanoTime() - start_time;
        return connected;
    }

    public HNode getRoot(HNode node) {
        return HNode.root(node);
    }

    public boolean isCutEdge(int u, int v) {
        EndpointType rec = getEdgeType(u, v);
        // Edge doesn't exist — cut edge iff u and v are disconnected
        // System.out.println("forest Checking if edge {" + u + ", " + v + "} is a cut edge. Edge type: " + rec + "depth: "+(rec == null? "N/A" : findEdgeRecord(u, v).depth));
        if (rec == null) {
            // System.out.println("edge does not exist, conn = "+connected(u, v));
            return !connected(u, v);
        }

        // Non-witness edges are never cut edges (spanning forest property)
        if (rec != EndpointType.WITNESS) {
            // System.out.println("edge is not tree, conn = "+rec);
            return false;}

        int depth = findEdgeRecord(u, v).depth;
        // System.out.println("is cut base cases checked");
        // Bidirectional BFS on spanning tree, skipping {u,v}
        ArrayDeque<HLeaf> qU = new ArrayDeque<>();
        ArrayDeque<HLeaf> qV = new ArrayDeque<>();
        HashSet<HLeaf> sU = new HashSet<>();
        HashSet<HLeaf> sV = new HashSet<>();

        sU.add(leaves[u].leafData);
        qU.add(leaves[u].leafData);
        sV.add(leaves[v].leafData);
        qV.add(leaves[v].leafData);

        while (!qU.isEmpty() && !qV.isEmpty()) {
            HLeaf leaf = qU.poll();
            for (int i = 1; i <= dMax; i++) {
                if (!leaf.isEndpoint[0][i]) continue;
                for (int neighbor : leaf.witness_edges[i]) {
                    // Skip the candidate edge in both directions
                    if (leaf.vertex == u && neighbor == v && i == depth) continue;
                    if (leaf.vertex == v && neighbor == u && i == depth) continue;
                    if (sU.add(leaves[neighbor].leafData)) {
                        if (sV.contains(leaves[neighbor].leafData)) return false;
                        qU.add(leaves[neighbor].leafData);
                    }
                }
            }

            leaf = qV.poll();
            for (int i = 1; i <= dMax; i++) {
                if (!leaf.isEndpoint[0][i]) continue;
                for (int neighbor : leaf.witness_edges[i]) {
                    if (leaf.vertex == u && neighbor == v && i == depth) continue;
                    if (leaf.vertex == v && neighbor == u && i == depth) continue;
                    if (sV.add(leaves[neighbor].leafData)){
                        if (sU.contains(leaves[neighbor].leafData)) return false;
                        qV.add(leaves[neighbor].leafData);}
                }
            }
            // System.out.println("sU: " + sU.toString());
            // System.out.println("sV: " + sV.toString());
        }
        // System.out.println("sU: " + sU.toString());
        // System.out.println("sV: " + sV.toString());
        //if there is any overlap at all between qU and qV, false
        for (HLeaf leaf : qU) {
            if (sV.contains(leaf)) return false;
        }

        // Whichever queue emptied first is the smaller side
        HashSet<HLeaf> smallerSide = qU.isEmpty() ? sU : sV;
        HashSet<HLeaf> visited = new HashSet<>();
        // Check if any primary edge crosses from smallerSide to outside
        // Must check all depths >= depth, matching deletion's replacement search
        
        for (HLeaf leaf : smallerSide) {
            visited.add(leaf);
            for (int i = 1; i <= dMax; i++) {
                if (true){ //if it has primary edges at this depth, we need to check if any of them are crossing the cut
                    ArrayList<Integer> neighborsToCheck = new ArrayList<>(leaf.primary_edges[i]);
                    // System.out.println("Checking primary edges at depth "+i+" for leaf "+leaf.vertex+" with neighbors "+neighborsToCheck.toString());
                    for (int neighbor : neighborsToCheck) {
                        HLeaf neighborLeaf = leaves[neighbor].leafData;
                        visited.add(neighborLeaf);
                        if (!smallerSide.contains(neighborLeaf)){// && largerSide.contains(neighborLeaf)) {
                            return false; // replacement exists, not a cut edge
                        }
                    }
                }
            }
        }
        //print qU and qV for debugging
        // System.out.println("visited: " + visited.toString());
        // System.out.println("sU: " + sU.toString());
        // System.out.println("sV: " + sV.toString());
        return true; // no replacement found, is a cut edge
    }
    
    
    
    public static HForest buildFromEdgeList(int n, List<int[]> edges) {
        // Step 1: union-find to classify spanning tree edges
        int[] uf = new int[n];
        Arrays.fill(uf, -1); // negative = root, value = -size

        // path-compressed find
        // union by size, returns true if u,v were in different components
        
        List<int[]> witnessEdges = new ArrayList<>();
        List<int[]> primaryEdges = new ArrayList<>();

        for (int[] e : edges) {
            int ru = ufFind(uf, e[0]), rv = ufFind(uf, e[1]);
            if (ru != rv) {
                // merge smaller into larger
                if (uf[ru] > uf[rv]) { int t = ru; ru = rv; rv = t; }
                uf[ru] += uf[rv];
                uf[rv] = ru;
                witnessEdges.add(e);
            } else {
                primaryEdges.add(e);
            }
        }

        // Step 2: allocate forest — reuse existing leaf init
        HForest forest = new HForest(n);
        // HForest constructor already built leaves[v] with depth=dMax+1
        // We just need to wire up edges and component roots

        // Step 3: register edges directly into leaf data (no hierarchy restructuring)
        for (int[] e : witnessEdges) {
            forest.leaves[e[0]].leafData.add_edge_info(e[1], 1, EndpointType.WITNESS);
            forest.leaves[e[1]].leafData.add_edge_info(e[0], 1, EndpointType.WITNESS);
        }
        for (int[] e : primaryEdges) {
            forest.leaves[e[0]].leafData.add_edge_info(e[1], 1, EndpointType.PRIMARY);
            forest.leaves[e[1]].leafData.add_edge_info(e[0], 1, EndpointType.PRIMARY);
        }

        // Step 4: build flat HNode structure — one root per component
        // Re-run find to group vertices by component root
        Map<Integer, List<Integer>> components = new HashMap<>();
        for (int v = 0; v < n; v++) {
            components.computeIfAbsent(ufFind(uf, v), k -> new ArrayList<>()).add(v);
        }

        forest.roots.clear();
        for (List<Integer> comp : components.values()) {
            if (comp.size() == 1) {
                int v = comp.get(0);
                forest.leaves[v].isRoot = true;
                forest.roots.add(forest.leaves[v]);
            } else {
                HNode root = new HNode(n, 1);
                root.isRoot  = true;
                root.weight  = comp.size();
                for (int v : comp) {
                    root.children.add(forest.leaves[v]);
                    forest.leaves[v].parent = root;
                    forest.leaves[v].isRoot = false;
                }
                root.recomputeBitmap();
                forest.roots.add(root);
            }
        }
        return forest;
    }

    private static int ufFind(int[] uf, int x) {
        while (uf[x] >= 0) {
            if (uf[uf[x]] >= 0) uf[x] = uf[uf[x]]; // path compression
            x = uf[x];
        }
        return x;
    }
    
    
    // ---------------------------------------------------------------
    // Leaves start off at level dmax. when we add an edge w that vertex, we set the leaf depth to 1, and the leaf depth
    // will increase as we merge components and promote edges, but the leaf depth will always be the depth of the node that the leaf is in.
    // alternatively, we could just have it such that the leaf depth is always dmax, and we are just not storing the path from root - leaf. This would be more in line with hte paper
    // ---------------------------------------------------------------
    public void add_edge(int u, int v) {
        if (getEdgeType(u, v) != null) {
            ////System.out.println("Error: edge already exists");
            return; // edge already exists
        }
        long start_time = System.nanoTime();
        EdgeRecord rec = new EdgeRecord(u, v, 1, EndpointType.PRIMARY);
        
        if (!connected(u, v)) {
            // Make it a witness edge and merge the two components
            rec.type = EndpointType.WITNESS;
            HNode rootU = getRoot(leaves[u]);
            HNode rootV = getRoot(leaves[v]);
            //if the roots are themselves, then they are still leaves. We always add an edge at level 1, so we would need to promote the leaf to level 2, and then merge the two level 2 nodes.
            
            if (rootU == leaves[u]) { //we are adding this vertex for the first time, 
                rootU.depth = 1;
                rootU.leafData.depth_of_node = 1; 
                rootU.weight = 1;
            }
            if(rootV == leaves[v]) { //we are adding this vertex for the first time, 
                rootV.depth = 1;
                rootV.leafData.depth_of_node = 1; 
                rootV.weight = 1;
            }
            //the difference when adding leaves versus adding existing subtrees when adding subtrees we can just merge the roots
            //when adding leaves, the leaf IS the root so we need the new root to have the leaves as children
            
            HNode newroot = mergeRoots(rootU, rootV);
            this.roots.add(newroot);
            if (rootU == leaves[u]) {
                leaves[u].parent = newroot;
                // newroot.children.add(leaves[u]);
                leaves[u].depth = dMax+1;
            }
            if (rootV == leaves[v]) {
                leaves[v].parent = newroot;
                //newroot.children.add(leaves[v]);
                leaves[v].depth = dMax+1;
            }
        }

        // Register at both leaves 
        leaves[u].leafData.add_edge_info(v, 1, rec.type);  // u's leaf records neighbor v
        leaves[v].leafData.add_edge_info(u, 1, rec.type);  // v's leaf records neighbor u
        // long start_recompute_time = System.nanoTime();
        // leaves[u].recomputeBitmapsUp(); 
        // leaves[v].recomputeBitmapsUp();
        // timeForRecompute += System.nanoTime() - start_recompute_time;
        timeForAddEdge += System.nanoTime() - start_time;
        //TODO: ill figure out counters later
        // if (recU.type == EndpointType.PRIMARY) {
        //     leaves[u].recomputeCounter(1, betaLogLogN);
        //     propagateCounterUp(leaves[u], 1);
        // }
        ////System.out.println("Added edge {" + u + ", " + v + "} of type " + rec.type);
    }

    public boolean isTreeEdge(int u, int v){
        EndpointType type = getEdgeType(u, v);
        return type == EndpointType.WITNESS;
    }


    public EndpointType getEdgeType(int u, int v){
        HNode leaf = leaves[u];
        EdgeRecord rec = leaf.leafData.edgeLookup.get(v);
        return rec == null? null : rec.type;
    }

    // ---------------------------------------------------------------
    // Delete edge {u, v}.
    // If non-witness: remove and done.
    // If witness: search for replacement edge depth d_e down to 1.
    // ---------------------------------------------------------------
    public void delete_edge(int u, int v) {
        // Find the edge record at u's leaf
        
        EdgeRecord recU = findEdgeRecord(u, v);
        if (recU == null) return; // edge doesn't exist
        long start_time = System.nanoTime();
        int depth = recU.depth;
        HNode leafu = leaves[u];
        HNode leafv = leaves[v];
        EndpointType type = getEdgeType(u, v);
        
        if (type == null) return; // edge doesn't exist
        leafu.leafData.remove_edge(v, depth, type);
        leafv.leafData.remove_edge(u, depth, type);
        long start_recompute_time = System.nanoTime();
        // leafu.recomputeBitmapsUp();
        // leafv.recomputeBitmapsUp();
        timeForRecompute += System.nanoTime() - start_recompute_time;
        if(type != EndpointType.WITNESS){
            //edgeMap.remove(new EdgeRecord(u, v, depth, type));
            //System.out.println("Deleted non witness edge {" + u + ", " + v + "} at depth " + depth);
            timeForDeleteEdge += System.nanoTime() - start_time;
            return;
        }
        double average = 0.0;
        for(int i = depth; i <= dMax; i++){
            average += leafu.primary_edge_count[i] + leafv.primary_edge_count[i];
        }
        average /= (dMax - depth + 1);
        if(average != 0){
            //System.out.println("Error: witness edge has primary neighbors at this depth, cannot delete");
            timeForDeleteEdge += System.nanoTime() - start_time;
             //witness edge cannot be deleted if it has primary neighbors at this depth, because those primary neighbors would be disconnected from the rest of the component
        }
        delete_edge_at_depth(u, v, depth);
        //System.out.println("DELETED EDGE "+u+", "+v+" from depth "+depth);
        //start_recompute_time = System.nanoTime();
        // TODO: Idk if this is... correct to comment this out?
        // leaves[u].recomputeBitmapsUp(); 
        // leaves[v].recomputeBitmapsUp();

        timeForRecompute += System.nanoTime() - start_recompute_time;
        timeForDeleteEdge += System.nanoTime() - start_time;
    }

    //lower levels contain information about higher levels. The whole invariant is that  G_i \subset G_(i-1)
    //if there are gaps, we can jsut take something at a level that is lower than the desired level
    public HNode getNodeAtDepth(HLeaf leafu, int depth){
        HNode current = leafu.node;
        while (current != null && current.depth > depth){
            current = current.parent;
        }
        if (current != null && current.depth <= depth){
            return current;
        }
        return null;
    }

    public boolean delete_edge_at_depth(int u, int v, int depth) {
        //System.out.println("Attempting to delete edge {" + u + ", " + v + "} at depth " + depth);
        if(u == v){
            //System.out.println("Error: cannot delete self loop");
            return false; // cannot delete self loop
        }
        if (depth < 1 || depth > dMax) 
            return false; // invalid depth
        EdgeRecord recU = findEdgeRecord(u, v);
        if (recU != null && recU.type != EndpointType.WITNESS) {
            //System.out.println("Error: edge is not a witness edge");
            return false; // edge is not a witness edge
        }
        HNode leafu = leaves[u];
        HNode leafv = leaves[v];
        // EndpointType type = getEdgeType(u, v);
        
        // if (type == null) return false; // edge doesn't exist
        // leafu.leafData.remove_edge(v, depth, type);
        // leafv.leafData.remove_edge(u, depth, type);
        // leafu.recomputeBitmapsUp();
        // leafv.recomputeBitmapsUp();

        // if(type != EndpointType.WITNESS){
        //     //edgeMap.remove(new EdgeRecord(u, v, depth, type));
        //     //System.out.println("Deleted non witness edge {" + u + ", " + v + "} at depth " + depth);
        //     return true;
        // }

        //if it is a witness edge, we need to 
        //1) establish components. 
            //start with a queue, within the given depth and up, add all neighbors to the queue. 
            // If they have witness edges at this depth, find them and add them. 
        //2) get all level i-witness edges adjacent to the smaller component, and promote them to level i+1. Update bitmaps
            // Merge all the level i+1 nodes into one big i+1 node u_i, and make it the parent of all the nodes in the smaller component.
        //3) find replacement edge if it exists, and if it does, merge the components.
            // if it doesnt, then split the components into their own HNodes at depth i-1, and recursively call deletion again with depth = depth - 1.
        HNode u_i = getNodeAtDepth(leafu.leafData, depth);
        if (u_i == null){
            //System.out.println("Error: no node at this depth");
            return false;
        }
        if (u_i != getNodeAtDepth(leafv.leafData, depth)){
            //System.out.println("Error: u and v are not in the same component at this depth");
            return false;
        }
        //edgeMap.remove(new EdgeRecord(u, v, depth, type));
        //TODO: the recomputing here is not nice, storing this informaion across recursive calls would be bettre
        HashSet<HLeaf> total_c_u_leaves = new HashSet<>();
        HashSet<HLeaf> c_u_leaves = new HashSet<>();
        HashSet<HLeaf> total_c_v_leaves = new HashSet<>();
        HashSet<HLeaf> c_v_leaves = new HashSet<>();
        HashSet<Integer> visited = new HashSet<>();
        HashSet<HNode> c_u = new HashSet<>();
        HashSet<HNode> c_v;
        c_u_leaves.add(leafu.leafData);
        //total_c_u_leaves.add(leafu.leafData);
        c_v_leaves.add(leafv.leafData);
        //total_c_v_leaves.add(leafv.leafData);

        // Instead of the HashSet-as-queue pattern, use two ArrayDeques
        ArrayDeque<HLeaf> queueU = new ArrayDeque<>();
        ArrayDeque<HLeaf> queueV = new ArrayDeque<>();
        ArrayList<HLeaf> c_u_list = new ArrayList<>();
        ArrayList<HLeaf> c_v_list = new ArrayList<>();

        queueU.add(leafu.leafData);
        total_c_u_leaves.add(leafu.leafData);
        queueV.add(leafv.leafData);
        total_c_v_leaves.add(leafv.leafData);
        c_u_list.add(leafu.leafData);
        c_v_list.add(leafv.leafData);
        int p_neighbors_u = 0, p_neighbors_v = 0;

                
        while (!queueU.isEmpty() && !queueV.isEmpty()) {
            HLeaf leaf = queueU.poll();
            p_neighbors_u += leaf.primary_edge_count[depth];
            for (int i = depth; i <= dMax; i++) {
                if (!leaf.isEndpoint[0][i]) continue;
                for (int neighbor : leaf.witness_edges[i]) {
                    HLeaf neighborLeaf = leaves[neighbor].leafData;
                    if (total_c_u_leaves.add(neighborLeaf)) {
                        c_u_list.add(neighborLeaf);
                        queueU.add(neighborLeaf);
                    }
                }
            }

            leaf = queueV.poll();
            p_neighbors_v += leaf.primary_edge_count[depth];
            for (int i = depth; i <= dMax; i++) {
                if (!leaf.isEndpoint[0][i]) continue;
                for (int neighbor : leaf.witness_edges[i]) {
                    HLeaf neighborLeaf = leaves[neighbor].leafData;
                    if (total_c_v_leaves.add(neighborLeaf)) {
                        c_v_list.add(neighborLeaf);
                        queueV.add(neighborLeaf);
                    }
                }
            }
        }

        if (queueV.isEmpty() && !queueU.isEmpty()) {
            HashSet<HLeaf> total_temp = total_c_u_leaves;
            total_c_u_leaves = total_c_v_leaves;
            total_c_v_leaves = total_temp;
            ArrayList<HLeaf> list_temp = c_u_list;
            c_u_list = c_v_list;
            c_v_list = list_temp;
            int temp_neighbors = p_neighbors_u;
            p_neighbors_u = p_neighbors_v;
            p_neighbors_v = temp_neighbors;
        }

        c_u_leaves = total_c_u_leaves;
        c_v_leaves = total_c_v_leaves;
        //based on the size of the two sets of leaves, we can add the i-HNode to c_u
        //All children of u_i nodes not added to c_u are added to c_v
        
        c_v = //everything in u_i that is not in c_u is in c_v
            new HashSet<>(u_i.children);
        int c_u_weight = 0;
        c_u_leaves = total_c_u_leaves;
        c_v_leaves = total_c_v_leaves;
        for (HLeaf leaf: c_u_leaves){
            //HLeaf leaf = c_u_leaves.iterator().next();
            //c_u_leaves.remove(leaf);
            //HNode parent = leaf.node.parent;
            if(leaf.node.parent == u_i) { //if we are at the lowest level, so the only children the node has are leaves
                c_u.add(leaf.node);
                c_u_weight += 1;
                c_v.remove(leaf.node);
                continue;
            }
            HNode cur = leaf.node;
            while (cur != null && cur.parent != u_i) {
                cur = cur.parent;
            }
            if (cur != null) {
                if (c_u.add(cur)) { // add() returns false if already present
                    c_u_weight += cur.weight;
                    c_v.remove(cur);
                }
            }
        }

        //check the weights of c_u and c_v to ensure c_u is the smaller one if nto swap them
        if (c_u_weight > u_i.weight - c_u_weight){
            HashSet<HNode> temp = c_u;
            c_u = c_v;
            c_v = temp; 
            c_u_weight = u_i.weight - c_u_weight; 
            c_u_leaves = c_v_leaves;
        }

        
        //promote all i-witness edges on c_u to (i+1)
        visited.clear();
        //Arrays.fill(visited, null);
        ArrayDeque<HLeaf> queue = new ArrayDeque<>();
        for (HLeaf nleaf : c_u_leaves) queue.add(nleaf);

        while (!queue.isEmpty()) {
            HLeaf nleaf = queue.poll();
            visited.add(nleaf.vertex);
            if (!nleaf.isEndpoint[0][depth]) continue;
            int[] neighbors = nleaf.witness_edges[depth]
                .stream().mapToInt(Integer::intValue).toArray();
            for (int neighbor : neighbors) {
                HLeaf neighborLeaf = leaves[neighbor].leafData;
                if (c_u_leaves.contains(neighborLeaf) && visited.contains(neighbor)) {
                    neighborLeaf.promote_witness_edge(nleaf.vertex, depth);
                    nleaf.promote_witness_edge(neighbor, depth);
                    queue.add(neighborLeaf);
                }
            }
        }


        //Merge all of c_u into one HNode at depth i and make it the child of u_i
        if(c_u.size() > 1){
            HNode child = new HNode(n, depth+1);
            child.weight = 0;
            for (HNode nodeu : c_u){
                //System.out.println("depth of nodeu before merge: " + nodeu.depth);
                child = mergeRoots(nodeu, child);
            }
            this.roots.remove(child);
            child.parent = u_i;
            u_i.children.add(child);
            u_i.children.removeAll(c_u);
            child.recomputeBitmap();
            u_i.recomputeBitmap();
        }

        //Now we can try to search for replacement edges. 

        //sampling time

        Random rand = new Random();
        if(p_neighbors_u > 1){
            // System.out.println("No primary neighbors on smaller side, skipping sampling");
            //return false;
        }
        int samplesize = (int) Math.log(Math.log(p_neighbors_u + 1)) + 1;
int working_key = -1, working_value = -1;
int num_bridges = 0;
int actual_cycles = 0;
Map<Integer, Integer> sampled = new HashMap<>();

for (int i = 0; i < samplesize; i++) {
    HLeaf nleaf = c_u_list.get(rand.nextInt(c_u_list.size()));
    actual_cycles++;
    if (!nleaf.isEndpoint[1][depth]) {
        i--;
        if (actual_cycles > c_u_list.size() * 2) break;
        continue;
    }
    ArrayList<Integer> primaries = new ArrayList<>(nleaf.primary_edges[depth]);
    int neighbor = primaries.get(rand.nextInt(primaries.size()));
    HLeaf neighborLeaf = leaves[neighbor].leafData;

    if (sampled.containsKey(nleaf.vertex) && 
        sampled.get(nleaf.vertex) == neighbor) {
        i--;
        if (actual_cycles > c_u_list.size() * 2) break;
        continue;
    }
    sampled.put(nleaf.vertex, neighbor);

    if (!c_u_leaves.contains(neighborLeaf)) {
        num_bridges++;
        working_key = nleaf.vertex;
        working_value = neighbor;
    }
}

if (num_bridges > (samplesize / 2) && num_bridges > 0) {
    HLeaf nleaf = leaves[working_key].leafData;
    HLeaf neighborLeaf = leaves[working_value].leafData;
    nleaf.remove_edge(neighborLeaf.vertex, depth, EndpointType.PRIMARY);
    neighborLeaf.remove_edge(nleaf.vertex, depth, EndpointType.PRIMARY);
    nleaf.add_edge_info(neighborLeaf.vertex, depth, EndpointType.WITNESS);
    neighborLeaf.add_edge_info(nleaf.vertex, depth, EndpointType.WITNESS);
    return true;
}
        //else, enumeration
        //upgrade all i-secondary endpoints touching u_i to primary
        //then enumerate all i-primary endpoints touoching u_i
        //promote all non-witness edges to secondary edges at depth (i+1)


        //we can enumerate all the c_u leaves at the component at this depth
        //we only need to enumerate the leaves that have a primary edge at depth though
        
        // for(int i = depth; i <= dMax; i++){
        //     ArrayList<HLeaf> leavesToCheck = new ArrayList<>(c_u_leaves);
        //     for(HLeaf nleaf : leavesToCheck){
        //         if(nleaf.isEndpoint[0][i]){ //if this leaf has neighbors at this depth or higher, enumerate them
        //             for (int neighbor : nleaf.witness_edges[i]){
        //                 c_u_leaves.add(leaves[neighbor].leafData);
                        
        //             }
        //         }
        //         if(nleaf.isEndpoint[2][i]) //if this leaf has secondary neighbors at this depth, promote them to primary and enumerate them
        //         {
        //             ArrayList<Integer> neighborsToPromote = new ArrayList<>(nleaf.secondary_edges[i]);
        //             for (int neighbor : neighborsToPromote){
        //                 HLeaf neighborLeaf = leaves[neighbor].leafData;
        //                 nleaf.remove_edge(neighbor, i, EndpointType.SECONDARY);
        //                 neighborLeaf.remove_edge(nleaf.vertex, i, EndpointType.SECONDARY);
        //                 nleaf.add_edge_info(neighbor, i, EndpointType.PRIMARY);
        //                 neighborLeaf.add_edge_info(nleaf.vertex, i, EndpointType.PRIMARY);
        //                 long start_recompute_time = System.nanoTime();
        //                 // nleaf.node.recomputeBitmapsUp();
        //                 // neighborLeaf.node.recomputeBitmapsUp();
        //                 timeForRecompute += System.nanoTime() - start_recompute_time;
        //                 c_u_leaves.add(neighborLeaf);
        //             }
        //         }
        //         if(nleaf.isEndpoint[1][i]){ 
        //             //convert to secondary edge at depth i+1
        //             ArrayList<Integer> neighborsToPromote = new ArrayList<>(nleaf.primary_edges[i]);
        //             for (int neighbor : neighborsToPromote){
        //                 HLeaf neighborLeaf = leaves[neighbor].leafData;
        //                 nleaf.remove_edge(neighbor, i, EndpointType.PRIMARY);
        //                 neighborLeaf.remove_edge(nleaf.vertex, i, EndpointType.PRIMARY);
        //                 nleaf.add_edge_info(neighbor, i+1, EndpointType.SECONDARY);
        //                 neighborLeaf.add_edge_info(nleaf.vertex, i+1, EndpointType.SECONDARY);
        //                 long start_recompute_time = System.nanoTime();
        //                 // nleaf.node.recomputeBitmapsUp();
        //                 // neighborLeaf.node.recomputeBitmapsUp();
        //                 timeForRecompute += System.nanoTime() - start_recompute_time;
        //             }
        //         }
        //     }
        // }

        
        // --- ENUMERATION PROCEDURE ---

        // Pass 1: upgrade ALL i-secondary endpoints touching c_u to i-primary FIRST
        // Pass 1: upgrade ALL i-secondary endpoints to i-primary
        // Pass 1: upgrade ALL i-secondary endpoints to i-primary
for (HLeaf nleaf : c_u_leaves) {
    if (!nleaf.isEndpoint[2][depth]) continue;
    ArrayList<Integer> secondaryNeighbors = 
        new ArrayList<>(nleaf.secondary_edges[depth]);
    for (int neighbor : secondaryNeighbors) {
        HLeaf neighborLeaf = leaves[neighbor].leafData;
        nleaf.remove_edge(neighbor, depth, EndpointType.SECONDARY);
        neighborLeaf.remove_edge(nleaf.vertex, depth, EndpointType.SECONDARY);
        nleaf.add_edge_info(neighbor, depth, EndpointType.PRIMARY);
        neighborLeaf.add_edge_info(nleaf.vertex, depth, EndpointType.PRIMARY);
    }
}

        // Pass 2: classify ALL i-primary endpoints — do not promote yet
        // Pass 2: classify ALL i-primary endpoints
List<int[]> replacementEdges = new ArrayList<>();
List<int[]> nonReplacementEdges = new ArrayList<>();

visited.clear();
for (HLeaf nleaf : c_u_leaves) {
    if (!nleaf.isEndpoint[1][depth]) continue;
    visited.add(nleaf.vertex);
    ArrayList<Integer> primaryNeighbors = 
        new ArrayList<>(nleaf.primary_edges[depth]);
    for (int neighbor : primaryNeighbors) {
        if (visited.contains(neighbor)) continue; // skip already-processed side
        HLeaf neighborLeaf = leaves[neighbor].leafData;
        if (!c_u_leaves.contains(neighborLeaf)) {
            replacementEdges.add(new int[]{nleaf.vertex, neighbor});
        } else {
            nonReplacementEdges.add(new int[]{nleaf.vertex, neighbor});
        }
    }
}

        // Pass 3: promote all non-replacement edges to secondary at depth+1 FIRST
        for (int[] edge : nonReplacementEdges) {
            HLeaf a = leaves[edge[0]].leafData;
            HLeaf b = leaves[edge[1]].leafData;
            a.remove_edge(edge[1], depth, EndpointType.PRIMARY);
            b.remove_edge(edge[0], depth, EndpointType.PRIMARY);
            a.add_edge_info(edge[1], depth + 1, EndpointType.SECONDARY);
            b.add_edge_info(edge[0], depth + 1, EndpointType.SECONDARY);
        }

        // Pass 4: use the first replacement edge if one was found
        if (!replacementEdges.isEmpty()) {
            int[] rep = replacementEdges.iterator().next();
            HLeaf nleaf = leaves[rep[0]].leafData;
            HLeaf neighborLeaf = leaves[rep[1]].leafData;
            nleaf.remove_edge(rep[1], depth, EndpointType.PRIMARY);
            neighborLeaf.remove_edge(rep[0], depth, EndpointType.PRIMARY);
            nleaf.add_edge_info(rep[1], depth, EndpointType.WITNESS);
            neighborLeaf.add_edge_info(rep[0], depth, EndpointType.WITNESS);
            return true;
        }


        // if(working_key != -1 && working_value != -1){
        //     // System.out.println("Using enumerated edge {" + working_key + ", " + working_value + "} as replacement");
        //     HLeaf nleaf = leaves[working_key].leafData;
        //     HLeaf neighborLeaf = leaves[working_value].leafData;
        //     nleaf.remove_edge(neighborLeaf.vertex, depth + 1, EndpointType.SECONDARY);
        //     neighborLeaf.remove_edge(nleaf.vertex, depth + 1, EndpointType.SECONDARY);
        //     nleaf.add_edge_info(neighborLeaf.vertex, depth, EndpointType.WITNESS);
        //     neighborLeaf.add_edge_info(nleaf.vertex, depth, EndpointType.WITNESS);
        //     long start_recompute_time = System.nanoTime();
        //     // nleaf.node.recomputeBitmapsUp();
        //     // neighborLeaf.node.recomputeBitmapsUp();
        //     timeForRecompute += System.nanoTime() - start_recompute_time;
        //     return true;
        // }
        
        // visited.clear();
        // //search through all the leaves to see if any of them have primary edges that go outside the component, 
        // // if they do, then we can promote that edge to a witness edge and merge the components.
        // for (HLeaf nleaf : c_u_leaves){
        //     visited.add(nleaf.vertex);
        //     if (nleaf.isEndpoint[1][depth]){ //if it has a primary edge at this depth
        //         ArrayList<Integer> neighborsToCheck = new ArrayList<>(nleaf.primary_edges[depth]);
        //         for (int neighbor : neighborsToCheck){
        //             if (!visited.contains(neighbor)){
        //                 //check if this neighbor is in c_u
        //                 HLeaf neighborLeaf = leaves[neighbor].leafData;
        //                 if (c_u_leaves.contains(neighborLeaf)){
        //                     //we need to promote the level of this primary edge to i+1, 
        //                     // and then we will find it when we search for replacement edges at depth i+1
        //                     neighborLeaf.promote_primary_edge(nleaf.vertex, depth);
        //                     nleaf.promote_primary_edge(neighbor, depth);
        //                     long start_recompute_time = System.nanoTime();
        //                     // nleaf.node.recomputeBitmapsUp();
        //                     // neighborLeaf.node.recomputeBitmapsUp();
        //                     timeForRecompute += System.nanoTime() - start_recompute_time;
        //                 }
        //                 else{//edge found. 
        //                     //need to promote it to a witness edge in the edgeMap
        //                     //need to convert hte primary edge to a witness edge now 
        //                     neighborLeaf.remove_edge(nleaf.vertex, depth, EndpointType.PRIMARY);
        //                     nleaf.remove_edge(neighbor, depth, EndpointType.PRIMARY);
        //                     neighborLeaf.add_edge_info(nleaf.vertex, depth, EndpointType.WITNESS);
        //                     nleaf.add_edge_info(neighbor, depth, EndpointType.WITNESS);
        //                     long start_recompute_time = System.nanoTime();
        //                     // nleaf.node.recomputeBitmapsUp();
        //                     // neighborLeaf.node.recomputeBitmapsUp();
        //                     timeForRecompute += System.nanoTime() - start_recompute_time;
        //                     return true;
        //                     //promoteToWitness(new EdgeRecord(neighbor, depth, EndpointType.PRIMARY), depth);
        //                     //mergeComponents(c_u.iterator().next(), c_v.iterator().next());
        //                 }
        //             }
        //         }

        //     }
        // }
        if (depth <= 1) //no replacement at the root, so u_i and v_i become new roots
        //the roots just have the same thing twice, what
        {
            //c_u is the one that raises level to i+1. Cv stays the same. 
            // Therefore, if c_v is just the leaf, then v_i must specify leaf_info and update v's leaf
            HNode v_i = new HNode(n, depth);
            boolean wasInRoots =this.roots.remove(u_i);
            u_i.isRoot = true;
            //u_i is just apath node with one child, which is the new u_i_1 that has all of c_u as its children.
            HNode u_i_1 = new HNode(n, depth+1);
            //if cu is only one leaf
            u_i.children.clear();
            //u_i_1.children.addAll(c_u);
            u_i_1.parent = u_i;
            boolean leafChild = false;
            for (HNode node : c_u){
                u_i_1.children.addAll(node.children);
                for (HNode child : node.children){
                    child.parent = u_i_1;
                    child.isRoot = false;
                    if (child.leafData != null){ //if c_v is just the leaf, then v_i must specify leaf_info and update v's leaf
                        //u_i_1.leafData = c_u.iterator().next().leafData;
                        //u_i_1.leafData.node = u_i_1; 
                        leaves[child.leafData.vertex].parent = u_i_1;
                    }
                }
                if (node.leafData != null && !u_i_1.children.contains(node)){ //if c_v is just the leaf, then v_i must specify leaf_info and update v's leaf
                    //u_i_1.leafData = c_u.iterator().next().leafData;
                    //u_i_1.leafData.node = u_i_1; a
                    leaves[node.leafData.vertex].parent = u_i_1;
                    u_i_1.children.add(node);
                    //if c_u has only one child, and it is a leaf, then  we dont really need to increase the level of anything
                    
                }
                if (node.weight == 1 && c_u.size() == 1){
                    leafChild = true;
                }
            }
            u_i_1.leafData = null;
            v_i.weight = u_i.weight - c_u_weight;
            u_i.weight = c_u_weight;
            u_i_1.weight = c_u_weight;
            //circular pointers somewhere
            v_i.children.addAll(c_v);
            for (HNode node : c_v){
                node.parent = v_i;
                node.isRoot = false;
                
                if (node.leafData != null){ //if c_v is just the leaf, then v_i must specify leaf_info and update v's leaf
                    //v_i.leafData = c_v.iterator().next().leafData;
                    //v_i.leafData.node = v_i;
                    //leaves[c_v.iterator().next().leafData.vertex] = v_i;
                    leaves[node.leafData.vertex].parent = v_i;
                }
            }
            promoteWitnessEdges(u_i, depth);
            u_i.children.add(u_i_1);
            if(leafChild){
                //we can just remove u_i_1 as a concept, and have u_i set the leaf as the child and move on. 
                u_i.children.remove(u_i_1);
                //u_i.children.addAll(u_i_1.children);
                // HNode curr = u_i_1.children.iterator().next(); //there is only one child so this is fine
                // while (curr.leafData == null){
                //     curr = curr.children.iterator().next(); //we know the weight is 1 so there is only one nonde as the child
                // }
                // u_i.children.add(curr);
                // curr.parent = u_i;
                HNode leafNode = c_u_leaves.iterator().next().node;
                u_i.children.add(leafNode);
                leafNode.parent = u_i; 
            }
            else{
                long start_recompute_time = System.nanoTime();
                u_i_1.recomputeBitmap();
                timeForRecompute += System.nanoTime() - start_recompute_time;
            }
            
            
            v_i.isRoot = true;
            long start_recompute_time = System.nanoTime();
            u_i.recomputeBitmap();
            v_i.recomputeBitmap();
            timeForRecompute += System.nanoTime() - start_recompute_time;
            v_i.parent = null;
            u_i.parent = null;
            
            this.roots.add(u_i);
            this.roots.add(v_i);
            return true;
        }
        //no replacement at this level, so we split the components into their own HNodes at depth i-1, and recursively call deletion again with depth = depth - 1.
        HNode parent = u_i.parent;
        if (parent == null) {
            // u_i is already a root despite depth > 1
            // treat the same as the depth <= 1 case
            u_i.isRoot = true;
            this.roots.remove(u_i);
            
            HNode v_i = new HNode(n, depth);
            v_i.isRoot = true;
            v_i.weight = u_i.weight - c_u_weight;
            u_i.weight = c_u_weight;
            
            v_i.children.addAll(c_v);
            for (HNode node : c_v) {
                node.parent = v_i;
                node.isRoot = false;
                if (node.leafData != null)
                    leaves[node.leafData.vertex].parent = v_i;
            }
            
            promoteWitnessEdges(u_i, depth);
            v_i.recomputeBitmap();
            u_i.recomputeBitmap();
            v_i.parent = null;
            u_i.parent = null;
            this.roots.add(u_i);
            this.roots.add(v_i);
            return delete_edge_at_depth(u, v, depth - 1);
        }

        HNode parent_parent = parent.parent; // now safe
        parent.children.remove(u_i);
        u_i.depth = depth;
        HNode v_i = new HNode(n, depth);
        u_i.children.clear();
        HNode u_i_1 = new HNode(n, depth+1);
        boolean leafChild = false;
        for (HNode node : c_u){
            u_i_1.children.addAll(node.children);
            for (HNode child : node.children){
                child.parent = u_i_1;
                child.isRoot = false;
                if (child.leafData != null){ //if c_v is just the leaf, then v_i must specify leaf_info and update v's leaf
                        //u_i_1.leafData = c_u.iterator().next().leafData;
                        //u_i_1.leafData.node = u_i_1; 
                    leaves[child.leafData.vertex].parent = u_i_1;
                }
            }
            if (node.leafData != null){ //if c_v is just the leaf, then v_i must specify leaf_info and update v's leaf
                    //u_i_1.leafData = c_u.iterator().next().leafData;
                    //u_i_1.leafData.node = u_i_1; 
                leaves[node.leafData.vertex].parent = u_i_1;
                u_i_1.children.add(node);
                // //if c_u has only one child, and it is a leaf, then  we dont really need to increase the level of anything
                // if (c_u.size() == 1 && node.leafData != null){
                //     leafChild = true;
                // }
            }
            if(node.weight == 1 && c_u.size() == 1){
                leafChild = true;
            }
            
        }
        u_i.children.add(u_i_1);
        u_i_1.parent = u_i;
        //u_i_1.weight = c_u_weight;
        promoteWitnessEdges(u_i, depth); //mapping all the edges that used to join i+1 components to i+1, so they refer to u_i_1 instead of u_i
        v_i.weight = u_i.weight - c_u_weight;
        u_i.weight = c_u_weight;
        u_i_1.weight = c_u_weight;
        
        //since we are not reducing levels for v_i, this is not a problem. 
        v_i.children.addAll(c_v);
        for (HNode node : c_v){
            node.parent = v_i;
            node.isRoot = false;
            if (node.leafData != null){ //if c_v is just the leaf, then v_i must specify leaf_info and update v's leaf
                // v_i.leafData = c_v.iterator().next().leafData;
                // v_i.leafData.node = v_i;
                leaves[node.leafData.vertex].parent = v_i;
            }
        }
        long start_recompute_time = System.nanoTime();
        v_i.recomputeBitmap();
        timeForRecompute += System.nanoTime() - start_recompute_time;
        v_i.parent = parent;
        u_i.parent = parent;
        if(leafChild){
            //we can just remove u_i_1 as a concept, and have u_i set the leaf as the child and move on. 
            u_i.children.remove(u_i_1);
            // u_i.children.addAll(u_i_1.children);
            // for (HNode child : u_i_1.children){
            //     child.parent = u_i;
            // }
            HNode curr = c_u_leaves.iterator().next().node; //there is only one child so this is fine
            u_i.children.add(curr);
            curr.parent = u_i;
        }
        else{
            start_recompute_time = System.nanoTime();
            u_i_1.recomputeBitmap();
            timeForRecompute += System.nanoTime() - start_recompute_time;
        }
        start_recompute_time = System.nanoTime();
        u_i_1.recomputeBitmap();
        u_i.recomputeBitmap();
        parent.recomputeBitmap();
        timeForRecompute += System.nanoTime() - start_recompute_time;
        parent.children.add(u_i);
        parent.children.add(v_i);
        parent.parent = parent_parent;
        return delete_edge_at_depth(u, v, depth-1);
        //no replacement found at this level, so we give c_u its own parent 
    }

    public boolean hasEdge(int u, int v){
        if (getEdgeType(u, v) != (findEdgeRecord(u, v) == null ? null : findEdgeRecord(u, v).type)){
            System.out.println("Error: edge type mismatch for edge {" + u + ", " + v + "}");
        }
        return getEdgeType(u, v) != null;
    }

    public int max_comp_size() {
        int maxSize = 0;
        for (HNode root : roots) {
            if (root.weight > maxSize) {
                maxSize = root.weight;
            }
        }
        return maxSize;
    }

    // ---------------------------------------------------------------
    // merged 2 roots into one new root at the same depth, and return the new root
    // ---------------------------------------------------------------
    public HNode mergeRoots(HNode rootU, HNode rootV) {
        if (rootU == rootV) 
            return rootU; // already the same component
        if (rootU == null || rootV == null) {
            ////System.out.println("Error: one of the roots is null");
            return rootU != null ? rootU : rootV; // return the non-null root
        }
        // if(rootU.parent != null){
        //     while(rootU.parent != null){
        //         rootU = rootU.parent;
        //     }
        // }
        // if(rootV.parent != null){
        //     while(rootV.parent != null){
        //         rootV = rootV.parent;
        //     }
        // }
        roots.remove(rootU);
        roots.remove(rootV);
        if (rootU.depth != rootV.depth) {
            ////System.out.println("Error: trying to merge roots at different depths");
            // return null;
        }
        // Create a new root node above both
        HNode newRoot = new HNode(n, Math.min(rootU.depth, rootV.depth));
        newRoot.weight         = rootU.weight + rootV.weight;
        newRoot.isRoot         = true;
    //     if (rootU.leafData != null){
    //         newRoot.children.add(rootU);
    //         leaves[rootU.leafData.vertex].parent = newRoot;
    //         leaves[rootU.leafData.vertex].isRoot = false;
    //     }
    //     else
    //         newRoot.children.addAll(rootU.children);
    //     if (rootV.leafData != null){
    //         newRoot.children.add(rootV);
    //         leaves[rootV.leafData.vertex].parent = newRoot;
    //         leaves[rootV.leafData.vertex].isRoot = false;
    //     }
    //     else
    //         newRoot.children.addAll(rootV.children); 
    //     for (HNode child : newRoot.children)
    //         child.parent = newRoot;
    //     long start_recompute_time = System.nanoTime();
    //     newRoot.recomputeBitmap();
    //     timeForRecompute += System.nanoTime() - start_recompute_time;
        
    //     // for (int i = 1; i <= dMax; i++)
    //     //     newRoot.recomputeCounter(i, betaLogLogN);
    //     return newRoot;
    // }
        addChildrenFrom(newRoot, rootU);
        addChildrenFrom(newRoot, rootV);
        
        for (HNode child : newRoot.children)
            child.parent = newRoot;
        // No recomputeBitmap call needed
        roots.add(newRoot);
        return newRoot;
    }

    private void addChildrenFrom(HNode parent, HNode source) {
        Iterable<HNode> toAdd = (source.leafData != null) 
            ? Collections.singletonList(source) 
            : source.children;
        for (HNode child : toAdd) {
            parent.children.add(child);
            for (int t = 0; t < 3; t++)
                for (int d = 1; d <= dMax; d++)
                    if (child.isEndpoint[t][d]) {
                        if (parent.childBitCount[t][d]++ == 0)
                            parent.isEndpoint[t][d] = true;
                    }
        }
    }

    // Promote all i-witness edges touching a component to depth i+1.
    // Section 3.2.1 — the smaller component's witness edges get promoted.
    // STUB: naive traversal — will be replaced by Operation 2 of Lemma 3.1.
    private void promoteWitnessEdges(HNode component, int depth) {
        promoteWitnessEdgesRecursive(component, depth);
    }

    private void promoteWitnessEdgesRecursive(HNode node, int depth) {
        if (node.leafData != null) {
            List<EdgeRecord> witnesses = node.leafData.get(depth, EndpointType.WITNESS);
            for (EdgeRecord rec : witnesses){
                rec.depth = depth + 1;
                //TODO: replace with whatever shortcut bs
                node.leafData.promote_witness_edge(rec.neighbor, depth);
                long start_recompute_time = System.nanoTime();
                // node.recomputeBitmapsUp();
                timeForRecompute += System.nanoTime() - start_recompute_time;
            }
            return;
        }
        for (HNode child : node.children)
            promoteWitnessEdgesRecursive(child, depth);
    }

    // Propagate counter changes upward after a leaf update
    // STUB: O(depth) — will be replaced by local tree counter propagation
    // private void propagateCounterUp(HNode leaf, int depth) {
    //     HNode cur = leaf;
    //     while (cur != null) {
    //         cur.recomputeCounter(depth, betaLogLogN);
    //         cur = cur.parent;
    //     }
    // }

    // Find the EdgeRecord for edge {u,v} at u's leaf
    private EdgeRecord findEdgeRecord(int u, int v) {
        // for (int i = 1; i <= dMax; i++)
        //     for (EndpointType t : EndpointType.values())
        //         for (EdgeRecord rec : leaves[u].leafData.get(i, t))
        //             if (rec.neighbor == v) return rec;
        // return null;
        return leaves[u].leafData.edgeLookup.get(v);
    }

    public void drawHForest() {
        for (HNode root : roots) {
            drawHNode(root, 0);
        }
    }
    public void drawHNode(HNode node, int indent) {
        for (int i = 0; i < indent; i++) System.out.print("  ");
        System.out.println("HNode(depth=" + node.depth + ", weight=" + node.weight + ", isRoot=" + node.isRoot + ")");
        if (node.leafData != null) {
            for (int i = 1; i <= dMax; i++) {
                for (EndpointType t : EndpointType.values()) {
                    List<EdgeRecord> edges = node.leafData.get(i, t);
                    if (!edges.isEmpty()) {
                        for (EdgeRecord rec : edges) {
                            for (int j = 0; j < indent + 1; j++) ////System.out.print("  ");
                                System.out.println("Edge to " + rec.neighbor + " at depth " + rec.depth + " type " + rec.type);
                        
                        }
                    }
                }
            }
        }
        for (HNode child : node.children) {
            drawHNode(child, indent + 1);
        }
    }
}