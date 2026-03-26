package DynGraph;

public class Dynamic_Graph {
    int n; 
    HTree forest;
    //keep a map from integer nodes to all the HNodes they hit
    // Map<Integer, HashSet<HNode>> nodeMap;
    // Map<Integer, HashMap<Integer, HNode>> edgeMap;

    public Dynamic_Graph(int n) {
        this.n = n;
        forest = new HTree(n);
        //TODO: !!!!!!!!!!!!!!!!!!!!
        //should actually make this a forest of just all the roots. 


        // nodeMap = new HashMap<>();
        // edgeMap = new HashMap<>();

    }

    public boolean connected(int u, int v){
        return forest.connected(u, v);
    }

    // public HNode addEdge(int u, int v){
    //     if (connected(u, v)){
    //         //add it as a primary 1-non witness edge
    //         //this means we need to find the HNode at level 1 that contains both u and v, and add it there
    //         //this is just the root, right?
    //         HNode root = HNode.root(forest.getNext(u));
    //         root.add_primary_edge(u, v);
    //         return root;
    //     }
    //     //it will either join 2 existing sets, join an existing tree to a new vertex, or join 2 new vertices
    //     HNode rootu = HNode.root(forest.getNext(u));
    //     HNode rootv = HNode.root(forest.getNext(v));
    //     if (rootu == null && rootv == null){
    //         //both nodes are new, so they can make their own tree
    //         HNode newRoot = new HNode(n, 1);
    //         newRoot.add_witness_edge(u, v);
    //         HNode u_leaf = HNode.create_leaf(n, 2, u);
    //         HNode v_leaf = HNode.create_leaf(n, 2, v);
    //         int edge[] = {u, v};
    //         newRoot.add_child_to_node(u_leaf, edge);
    //         newRoot.add_child_to_node(v_leaf, edge);
    //         return newRoot;
    //     }
    //     else if (rootu == null || rootv == null){
    //         //make root v the empty one
    //         if (rootu == null){
    //             rootu = rootv;
    //             rootv = null;
    //         }
           
    //         rootu.add_witness_edge(u, v);
    //         HNode v_leaf = HNode.create_leaf(n, 2, v);
    //         int edge[] = {u, v};
    //         rootu.add_child_to_node(v_leaf, edge);
    //         return rootu;
    //     }
    //     else{
    //         //both trees exist
    //         HNode newRoot = HTree.merge(rootu, rootv);
    //         return newRoot;
    //     }
        
    // }

        
}
