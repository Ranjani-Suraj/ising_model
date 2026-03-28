package DynGraph;

import java.util.*;


// Manages the full hierarchy forest H.
// One H-root per connected component.
// One H-leaf per original vertex.
public class HForest {

    public final int n;
    public final int dMax;
    public final int betaLogLogN;
    public Map<Integer, HNode> vertexToLeaf; // maps original vertex to its H-leaf node
    //public Map<EdgeRecord, HNode> edgeMap;
    // leaves[v] = the H-leaf node for original vertex v
    public final HNode[] leaves;

    // current H-roots (one per connected component)
    public final Set<HNode> roots;

    public HForest(int n) {
        this.n    = n;
        this.dMax = Math.max(1, (int) Math.floor(Math.log(n) / Math.log(2)));

        double logN    = Math.log(n) / Math.log(2);
        double logLogN = Math.log(Math.max(2, logN)) / Math.log(2);
        this.betaLogLogN = Math.max(1, (int) Math.ceil(2 * logLogN)); // beta=2

        this.leaves = new HNode[n];
        this.roots  = new HashSet<>();
        this.vertexToLeaf = new HashMap<>();
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
            this.vertexToLeaf.put(v, leaf);
            System.out.println("Initialized leaf for vertex " + v + " with depth " + leaf.depth + "ID = " + leaf.ID);
        }
    }

    // In HForest.java — add a private constructor for testing
    private HForest(int n, boolean empty) {
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
        return HNode.root(leaves[u]) == HNode.root(leaves[v]);
    }

    public HNode getRoot(HNode node) {
        return HNode.root(node);
    }

    // ---------------------------------------------------------------
    // Leaves start off at level dmax. when we add an edge w that vertex, we set the leaf depth to 1, and the leaf depth
    // will increase as we merge components and promote edges, but the leaf depth will always be the depth of the node that the leaf is in.
    // alternatively, we could just have it such that the leaf depth is always dmax, and we are just not storing the path from root - leaf. This would be more in line with hte paper
    // ---------------------------------------------------------------
    public void add_edge(int u, int v) {
        if (getEdgeType(u, v) != null) {
            System.out.println("Error: edge already exists");
            return; // edge already exists
        }
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
            if (rootU == leaves[u]) {
                leaves[u].parent = newroot;
                newroot.children.add(leaves[u]);
                leaves[u].depth = dMax+1;
            }
            if (rootV == leaves[v]) {
                leaves[v].parent = newroot;
                newroot.children.add(leaves[v]);
                leaves[v].depth = dMax+1;
            }
        }

        // Register at both leaves 
        leaves[u].leafData.add_edge_info(v, 1, rec.type);  // u's leaf records neighbor v
        leaves[v].leafData.add_edge_info(u, 1, rec.type);  // v's leaf records neighbor u
        
        leaves[u].recomputeBitmapsUp(); 
        leaves[v].recomputeBitmapsUp();

        //TODO: ill figure out counters later
        // if (recU.type == EndpointType.PRIMARY) {
        //     leaves[u].recomputeCounter(1, betaLogLogN);
        //     propagateCounterUp(leaves[u], 1);
        // }
        System.out.println("Added edge {" + u + ", " + v + "} of type " + rec.type);
    }



    public EndpointType getEdgeType(int u, int v){
        HNode leaf = leaves[u];

        for (int i = 1; i <= dMax; i++){
            if (leaf.leafData.isEndpoint[0][i]){ //if this is a witness edge, we need to add the neighbor to the queue
                for (int neighbor : leaf.leafData.witness_edges[i]){
                    if (neighbor == v){
                        //System.out.println("WITNESS");
                        return EndpointType.WITNESS;
                    }
                }
            }
            if (leaf.leafData.isEndpoint[1][i]){ //if this is a primary edge, we need to add the neighbor to the queue
                for (int neighbor : leaf.leafData.primary_edges[i]){
                    if (neighbor == v){
                        //System.out.println("PRIMARY");
                        return EndpointType.PRIMARY;
                    }
                }
            }
            if (leaf.leafData.isEndpoint[2][i]){ //if this is a secondary edge, we need to add the neighbor to the queue
                for (int neighbor : leaf.leafData.secondary_edges[i]){
                    if (neighbor == v){
                        //System.out.println("SECONDARY");
                        return EndpointType.SECONDARY;
                    }
                }
            }
        }
        return null;
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
        
        int depth = recU.depth;
        delete_edge_at_depth(u, v, depth);
    }

    //lower levels contain information about higher levels. The whole invariant is that  G_i \subset G_(i-1)
    //if there are gaps, we can jsut take something at a level that is lower than the desired level
    public HNode getNodeAtDepth(HLeaf leafu, int depth){
        HNode current = leafu.node;
        while (current.depth > depth){
            current = current.parent;
        }
        if (current.depth <= depth){
            return current;
        }
        return null;
    }

    public boolean delete_edge_at_depth(int u, int v, int depth) {
        if (depth < 1 || depth > dMax) 
            return false; // invalid depth
        HNode leafu = leaves[u];
        HNode leafv = leaves[v];
        EndpointType type = getEdgeType(u, v);
        
        if (type == null) return false; // edge doesn't exist
        leafu.leafData.remove_edge(v, depth, type);
        leafv.leafData.remove_edge(u, depth, type);
        leafu.recomputeBitmapsUp();
        leafv.recomputeBitmapsUp();

        if(type != EndpointType.WITNESS){
            //edgeMap.remove(new EdgeRecord(u, v, depth, type));
            System.out.println("Deleted non witness edge {" + u + ", " + v + "} at depth " + depth);
            return true;
        }

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
            System.out.println("Error: no node at this depth");
            return false;
        }
        if (u_i != getNodeAtDepth(leafv.leafData, depth)){
            System.out.println("Error: u and v are not in the same component at this depth");
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
        total_c_u_leaves.add(leafu.leafData);
        c_v_leaves.add(leafv.leafData);
        total_c_v_leaves.add(leafv.leafData);
        while(!c_u_leaves.isEmpty() && !c_v_leaves.isEmpty()){
            HLeaf leaf = c_u_leaves.iterator().next();
            c_u_leaves.remove(leaf);
            visited.add(leaf.vertex);
            //
            if (leaf.isEndpoint[0][depth]){ // If this leaf has a witness edge at this depth, we can add it to c_u
                for (int neighbor : leaf.witness_edges[depth]){
                    if (!total_c_u_leaves.contains(leaves[neighbor].leafData)){
                        c_u_leaves.add(leaves[neighbor].leafData);
                        total_c_u_leaves.add(leaves[neighbor].leafData);
                    }
                }
            }
            //-------
            leaf = c_v_leaves.iterator().next();
            c_v_leaves.remove(leaf);
            visited.add(leaf.vertex);
            //
            if (leaf.isEndpoint[0][depth]){ //if this is a witness edge, we need to add the neighbor to the queue
                for (int neighbor : leaf.witness_edges[depth]){
                    if (!total_c_v_leaves.contains(leaves[neighbor].leafData)){
                        c_v_leaves.add(leaves[neighbor].leafData);
                        total_c_v_leaves.add(leaves[neighbor].leafData);
                    } 
                }
            }
        }
        //we dont know which ended, we just know one of htem did. Let us assert that u is finished but v is not
        if(c_v_leaves.isEmpty()){
            // HashSet<HLeaf> temp = c_u_leaves;
            // c_u_leaves = c_v_leaves;
            // c_v_leaves = temp;
            HashSet<HLeaf> total_temp = total_c_u_leaves;
            total_c_u_leaves = total_c_v_leaves;
            total_c_v_leaves = total_temp;
        }
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
            HNode parent = leaf.node.parent;
            if(leaf.node.parent == u_i) { //if we are at the lowest level, so the only children the node has are leaves
                c_u.add(leaf.node);
                c_u_weight += 1;
                c_v.remove(leaf.node);
                continue;
            }
            while (parent != null && parent.depth < depth - 1) 
                parent = parent.parent;
            if (parent != null && parent.depth == depth - 1){
                c_u.add(parent);
                c_u_weight += parent.weight;
                c_v.remove(parent);
            }
        }
        //check the weights of c_u and c_v to ensure c_u is the smaller one if nto swap them
        if (c_u_weight > u_i.weight - c_u_weight){
            HashSet<HNode> temp = c_u;
            c_u = c_v;
            c_v = temp;
        }
        
        //promote all i-witness edges on c_u to (i+1)
        visited.clear();
        for (HLeaf nleaf : c_u_leaves){
            visited.add(nleaf.vertex);
            if (nleaf.isEndpoint[0][depth]){ //if it has witness edges at this depth, we need to promote them to the next depth
                for(int neighbor : nleaf.witness_edges[depth]){
                    //if (!visited.contains(neighbor)){
                        //check if this neighbor is in c_u
                        HLeaf neighborLeaf = leaves[neighbor].leafData;
                        if (c_u_leaves.contains(neighborLeaf) && !visited.contains(neighbor)){
                            //we need to promote the level of this witness edge to i+1, 
                            // and then we will find it when we search for replacement edges at depth i+1
                            neighborLeaf.promote_witness_edge(nleaf.vertex, depth);
                            nleaf.promote_witness_edge(neighbor, depth);
                            nleaf.node.recomputeBitmapsUp();
                            neighborLeaf.node.recomputeBitmapsUp();
                        }
                        else if (!c_u_leaves.contains(neighborLeaf)){
                            System.out.println("problem: witness edge not in c_u");
                        }
                    //}
                }
                // nleaf.promote_witness_edge(u, depth);
                // leafu.leafData.promote_witness_edge(nleaf.vertex, depth);
                // leafu.recomputeBitmapsUp();
                // nleaf.node.recomputeBitmapsUp();
            }
        }

        //Merge all of c_u into one HNode at depth i and make it the child of u_i
        if(c_u.size() > 1){
            HNode child = new HNode(n, depth+1);
            for (HNode nodeu : c_u){
                child = mergeRoots(nodeu, child);
            }
            this.roots.remove(child);
            child.parent = u_i;
            u_i.children.add(child);
            u_i.children.removeAll(c_u);
            u_i.recomputeBitmap();
            
        }

        //Now we can try to search for replacement edges. 

        //we can enumerate all the c_u leaves at the component at this depth
        //we only need to enumerate the leaves that have a primary edge at depth though
        for(int i = depth+1; i <= dMax; i++){
            for(HLeaf nleaf : c_u_leaves){
                if(nleaf.isEndpoint[0][i]){ //if this leaf has neighbors at this depth or higher, enumerate them
                    for (int neighbor : nleaf.witness_edges[i]){
                        if(leaves[neighbor].leafData.isEndpoint[1][i]){ //if this neighbor has a primary edge at this depth, then we need to add it to the list of leaves we will check for replacement edges
                            c_u_leaves.add(leaves[neighbor].leafData);
                        }
                    }
                }
            }
        }
        visited.clear();
        //search through all the leaves to see if any of them have primary edges that go outside the component, 
        // if they do, then we can promote that edge to a witness edge and merge the components.
        for (HLeaf nleaf : c_u_leaves){
            visited.add(nleaf.vertex);
            if (nleaf.isEndpoint[1][depth]){ //if it has a primary edge at this depth
                for (int neighbor : nleaf.primary_edges[depth]){
                    if (!visited.contains(neighbor)){
                        //check if this neighbor is in c_u
                        HLeaf neighborLeaf = leaves[neighbor].leafData;
                        if (c_u_leaves.contains(neighborLeaf)){
                            //we need to promote the level of this primary edge to i+1, 
                            // and then we will find it when we search for replacement edges at depth i+1
                            neighborLeaf.promote_primary_edge(nleaf.vertex, depth);
                            nleaf.promote_primary_edge(neighbor, depth);
                            nleaf.node.recomputeBitmapsUp();
                            neighborLeaf.node.recomputeBitmapsUp();
                        }
                        else{//edge found. 
                            //need to promote it to a witness edge in the edgeMap
                            //need to convert hte primary edge to a witness edge now 
                            neighborLeaf.remove_edge(nleaf.vertex, depth, EndpointType.PRIMARY);
                            nleaf.remove_edge(neighbor, depth, EndpointType.PRIMARY);
                            neighborLeaf.add_edge_info(nleaf.vertex, depth, EndpointType.WITNESS);
                            nleaf.add_edge_info(neighbor, depth, EndpointType.WITNESS);
                            nleaf.node.recomputeBitmapsUp();
                            neighborLeaf.node.recomputeBitmapsUp();
                            return true;
                            //promoteToWitness(new EdgeRecord(neighbor, depth, EndpointType.PRIMARY), depth);
                            //mergeComponents(c_u.iterator().next(), c_v.iterator().next());
                        }
                    }
                }

            }
        }
        if (depth <= 1) //no replacement at the root, so u_i and v_i become new roots
        //the roots just have the same thing twice, what the fuck
        {
            //c_u is the one that raises level to i+1. Cv stays the same. 
            // Therefore, if c_v is just the leaf, then v_i must specify leaf_info and update v's leaf
            HNode v_i = new HNode(n, depth);
            this.roots.remove(u_i);
            u_i.isRoot = true;
            //u_i is just apath node with one child, which is the new u_i_1 that has all of c_u as its children.
            HNode u_i_1 = new HNode(n, depth+1);

            u_i.children.clear();
            u_i_1.children.addAll(c_u);
            for (HNode node : c_u){
                node.parent = u_i_1;
                node.isRoot = false;
                //TODO: need to increase the level of this component to join the other components at level i+1
            }
            v_i.weight = u_i.weight - c_u_weight;
            u_i.weight = c_u_weight;
            u_i_1.weight = c_u_weight;
            //circular pointers somewhere
            v_i.children.addAll(c_v);
            for (HNode node : c_v){
                node.parent = v_i;
                node.isRoot = false;
            }
            if (c_v.size() == 1 && c_v.iterator().next().leafData != null){ //if c_v is just the leaf, then v_i must specify leaf_info and update v's leaf
                v_i.leafData = c_v.iterator().next().leafData;
                v_i.leafData.node = v_i;
                leaves[v_i.leafData.vertex] = v_i;
            }
            promoteWitnessEdges(u_i, depth);
            u_i_1.recomputeBitmap();
            v_i.isRoot = true;
            u_i.recomputeBitmap();
            v_i.recomputeBitmap();
            v_i.parent = null;
            u_i.parent = null;
            
            this.roots.add(u_i);
            this.roots.add(v_i);
            return true;
        }
        //no replacement at this level, so we split the components into their own HNodes at depth i-1, and recursively call deletion again with depth = depth - 1.
        HNode parent = u_i.parent;
        parent.children.remove(u_i);
        u_i.depth = depth;
        HNode v_i = new HNode(n, depth);
        u_i.children.clear();
        HNode u_i_1 = new HNode(n, depth+1);
        u_i_1.children.addAll(c_u);
        for (HNode node : c_u){
            node.parent = u_i_1;
        }
        u_i.children.add(u_i_1);
        u_i_1.parent = u_i;
        u_i_1.weight = c_u_weight;
        promoteWitnessEdges(u_i, depth); //mapping all the edges that used to join i+1 components to i+1, so they refer to u_i_1 instead of u_i
        v_i.weight = u_i.weight - c_u_weight;
        u_i.weight = c_u_weight;
        v_i.children.addAll(c_v);
        for (HNode node : c_v){
            node.parent = v_i;
        }
        u_i.recomputeBitmap();
        u_i_1.recomputeBitmap();
        v_i.recomputeBitmap();
        u_i.parent = parent;
        v_i.parent = parent;
        parent.children.add(u_i);
        parent.children.add(v_i);

        delete_edge_at_depth(u, v, depth-1);
        return false;
        //no replacement found at this level, so we give c_u its own parent 
    }



    private EdgeRecord findReplacement(HashSet<HNode> c_u, HashSet<HLeaf> c_u_leaves, HashSet<HNode> c_v, HashSet<HLeaf> c_v_leaves, int depth){
        
        //make u the smaller component
        
        if (c_u_leaves.size() > c_v_leaves.size()){
            
            HashSet<HLeaf> temp_leaves = c_u_leaves;
            c_u_leaves = c_v_leaves;
            c_v_leaves = temp_leaves;
        }
        //Now we search through all the neighbours of c_u to see if they have an endpoint not in c_u
        
        for(int i = depth; i > 0; i--){
            HashSet<Integer> visited = new HashSet<>();
            for (HLeaf leaf : c_u_leaves){
                visited.add(leaf.vertex);
                if(leaf.isEndpoint[1][i]){
                    for (int neighbor : leaf.primary_edges[i]){
                        if (!visited.contains(neighbor)){
                            //check if this neighbor is in c_v
                            HNode neighborNode = leaves[neighbor];
                            HLeaf neighborLeaf = leaves[neighbor].leafData;
                            if (c_u_leaves.contains(neighborLeaf)){
                                //we need to promote the level of this primary edge to i+1, and then we will find it when we search for replacement edges at depth i+1
                                neighborLeaf.promote_primary_edge(leaf.vertex, i);
                                leaf.promote_primary_edge(neighbor, i);
                            }
                            else{ //edge found. 
                                //need to promote it to a witness edge in the edgeMap
                                //need to convert hte primary edge to a witness edge now 
                                neighborLeaf.remove_edge(leaf.vertex, i, EndpointType.PRIMARY);
                                leaf.remove_edge(neighbor, i, EndpointType.PRIMARY);
                                neighborLeaf.add_edge_info(leaf.vertex, i, EndpointType.WITNESS);
                                leaf.add_edge_info(neighbor, i, EndpointType.WITNESS);
                                
                                //promoteToWitness(new EdgeRecord(neighbor, i, EndpointType.PRIMARY), i);
                                //mergeComponents(c_u.iterator().next(), c_v.iterator().next());
                                return new EdgeRecord(leaf.vertex, neighbor, i, EndpointType.PRIMARY);
                            }
                        }
                    }
                }
            }
            //need to combine all of c_u into one component at level i+1
            HNode u_i = new HNode(n, depth);
            //need to merge all of c_u into u_i_1, and have u_i point to it. 
            HNode child = new HNode(n, depth+1); 
            for (HNode nodeu : c_u){
                child = mergeRoots(nodeu, child);
            }
            child.parent = u_i;
            u_i.children.add(child);
            u_i.recomputeBitmap();
            
            HNode v_i = new HNode(n, depth);
            v_i.children.addAll(c_v);
            v_i.recomputeBitmap();
            }
        //if no replacement is found, we create a new HNode at depth i-1 and make it the parent of all nodes in c_u
        //c_v stays the same 
        return null;
    }

    private HashSet[] getComponentNodes(HNode leaf, int depth){
        //get all the leaf nodes in the component of this leaf at this depth
        HashSet<HLeaf> neighbor_queue = new HashSet<>();
        HashSet<HLeaf> total_neighbors = new HashSet<>();
        neighbor_queue.add(leaf.leafData);
        total_neighbors.add(leaf.leafData);
        //we want to return the set of all HNodes that are in the same component as this leaf at this depth, 
        // which we can find by doing a BFS on the witness, primary, and secondary edges at this depth until we have visited all
        //  the nodes in the component. 
        // We can use a queue to keep track of the neighbors we need to visit, and a set to keep track of the nodes 
        // we have already visited to avoid cycles. 

        HashSet<HNode> componentNodes = new HashSet<>();
        HLeaf current;
        HNode current_node = leaf;
        while (!neighbor_queue.isEmpty()){
            current = neighbor_queue.iterator().next();
            neighbor_queue.remove(current);
            current_node = current.node;
            //if the neighbor has no witness edges at this depth, then it is not connected to any other nodes at this depth, 
            // so we can just add it to the component and continue
            
            //need to find the level where it DOES have neighbors, and add those neighbors to the queue, and then add the corresponding HNode to the component.
            for (int i = depth; i < dMax; i++){
                if (current.isEndpoint[0][i]){ //if this is a witness edge, we need to add the neighbor to the queue
                    for (int neighbor : current.witness_edges[i]){
                        neighbor_queue.add(leaves[neighbor].leafData);
                        total_neighbors.add(leaves[neighbor].leafData);
                    }
                }
            }
            //if the current node doesnt have any edges at depth + 1, then we dont need to search for its parent
            if(!current.isEndpoint[0][depth]) continue;
            //we want to add the HNode that corresponds to this leaf to the component, which is the leaf node itself if the depth of the leaf is equal to the depth we are looking at, otherwise it is the parent node of the leaf node at the depth we are looking at
            HNode parent = current_node.parent;
            while (parent != null && parent.depth < depth) parent = parent.parent;
            if (parent != null && parent.depth == depth){
                componentNodes.add(parent);    
            }
        }
        return new HashSet[]{componentNodes, total_neighbors};
    }



    // ---------------------------------------------------------------
    // Core deletion logic: search for replacement edge from depth i
    // down to depth 1. Section 3.2.
    // ---------------------------------------------------------------
    private void findReplacementEdge(int u, int v, int startDepth) {
        HNode nodeU = leaves[u];
        HNode nodeV = leaves[v];

        for (int i = startDepth; i >= 1; i--) {
            // Find the two components created by the deletion
            HNode compU = getComponentAtLevel(nodeU, i);
            HNode compV = getComponentAtLevel(nodeV, i);

            // STUB: naive scan for replacement edge at depth i.
            // Will be replaced by two-stage batch sampling (Section 8.1):
            //   Stage 1: sample O(log log p) primary endpoints
            //   Stage 2: sample O(log p) primary endpoints
            //   Stage 3: enumerate all (enumeration procedure)
            EdgeRecord replacement = findReplacementAtDepth(compU, compV, i);

            if (replacement != null) {
                // Promote this non-witness edge to witness
                promoteToWitness(replacement, i);
                mergeComponents(compU, compV);
                return;
            }

            // No replacement at depth i.
            // Promote all i-witness edges in smaller component to depth i+1.
            // Then split the H-node and try depth i-1.
            HNode smaller = compU.weight <= compV.weight ? compU : compV;
            promoteWitnessEdges(smaller, i);
            splitComponent(smaller, i);
        }

        // No replacement found at any depth — the graph is now disconnected.
        // The split already happened in the loop above; nothing more to do.
    }

    // ---------------------------------------------------------------
    // merged 2 roots into one new root at the same depth, and return the new root
    // ---------------------------------------------------------------
    public HNode mergeRoots(HNode rootU, HNode rootV) {
        if (rootU == rootV) 
            return rootU; // already the same component

        roots.remove(rootU);
        roots.remove(rootV);
        // Create a new root node above both
        HNode newRoot = new HNode(n, rootU.depth);
        newRoot.weight         = rootU.weight + rootV.weight;
        newRoot.isRoot         = true;

        newRoot.children.addAll(rootU.children);
        newRoot.children.addAll(rootV.children);
        for (HNode child : newRoot.children)
            child.parent = newRoot;
        newRoot.recomputeBitmap();
        roots.add(newRoot);
        // for (int i = 1; i <= dMax; i++)
        //     newRoot.recomputeCounter(i, betaLogLogN);
        return newRoot;
    }


    // ---------------------------------------------------------------
    // STUB helpers — these will be replaced by proper implementations
    // ---------------------------------------------------------------

    // Walk up from a leaf to find its enclosing component at H-level i.
    // STUB: O(depth) walk — will be replaced by O(log log n) via shortcuts.
    private HNode getComponentAtLevel(HNode leaf, int level) {
        HNode cur = leaf;
        while (cur != null && cur.depth > level)
            cur = cur.parent;
        return cur;
    }

    // Scan all primary endpoints in compU's subtree at depth i,
    // check if the other endpoint is in compV's component.
    // STUB: O(n) naive scan — will be replaced by two-stage sampling.
    private EdgeRecord findReplacementAtDepth(HNode compU, HNode compV, int depth) {
        return scanForReplacement(compU, compV, depth);
    }

    private EdgeRecord scanForReplacement(HNode node, HNode otherComp, int depth) {
        if (node.leafData != null) {
            for (EdgeRecord rec : node.leafData.get(depth, EndpointType.PRIMARY)) {
                HNode otherLeaf  = leaves[rec.neighbor];
                HNode otherSide  = getComponentAtLevel(otherLeaf, depth);
                if (otherSide == otherComp) return rec;
            }
            // Also check secondary endpoints during enumeration procedure
            for (EdgeRecord rec : node.leafData.get(depth, EndpointType.SECONDARY)) {
                HNode otherLeaf  = leaves[rec.neighbor];
                HNode otherSide  = getComponentAtLevel(otherLeaf, depth);
                if (otherSide == otherComp) return rec;
            }
            return null;
        }
        for (HNode child : node.children) {
            EdgeRecord found = scanForReplacement(child, otherComp, depth);
            if (found != null) return found;
        }
        return null;
    }

    public void add_edge_at_depth(int u, int v, int depth, EndpointType type) {
        
    }


    // Promote a non-witness edge to witness status
    private void promoteToWitness(EdgeRecord rec, int depth) {
        int u = rec.neighbor; // this is relative. in a real impl
                              // you'd carry both endpoints
        // STUB: update type fields and bitmaps
        rec.type = EndpointType.WITNESS;
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
                node.recomputeBitmapsUp();
            }
            return;
        }
        for (HNode child : node.children)
            promoteWitnessEdgesRecursive(child, depth);
    }

    // Split a component node after failing to find a replacement edge.
    // STUB: placeholder — will be replaced by Operation 6 of Lemma 3.1.
    private void splitComponent(HNode component, int depth) {
        // In the full implementation:
        // 1. Create two sibling H-nodes representing c_u and c_v
        // 2. Adjust parent pointers
        // 3. Rebuild bitmaps and counters for both new nodes
        // For now: the structural split was already implied by the
        // deletion — this is where you'd make it explicit in H.
    }

    // Merge two components after finding a replacement edge
    private void mergeComponents(HNode compU, HNode compV) {
        // Find their common parent and mark as still one component
        // STUB: for now reuse mergeRoots logic on the relevant level
        HNode parentU = compU.parent;
        HNode parentV = compV.parent;
        if (parentU == parentV) return; // already siblings under same parent
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
        for (int i = 1; i <= dMax; i++)
            for (EndpointType t : EndpointType.values())
                for (EdgeRecord rec : leaves[u].leafData.get(i, t))
                    if (rec.neighbor == v) return rec;
        return null;
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
                            for (int j = 0; j < indent + 1; j++) System.out.print("  ");
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