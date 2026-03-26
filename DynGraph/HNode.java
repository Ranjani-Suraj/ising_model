package DynGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


enum HNodeType {
    branching, singlechild, leaf, root
}

enum endpointType {WITNESS, PRIMARY, SECONDARY}

public class HNode {
    public int depth; // depth of the node in the HForest
    //when depth is 0, it is a root
    public Map<Integer, HashSet<Integer>> edges_being_covered; //this should not technically exist but i can keep it as a sanity check
    //public HashSet<Integer> nodes_covered;
    public ArrayList<HNode> children;
    public HNode parent;
    public HNodeType nodeType;
    // public HashMap<Integer, HashSet<Integer>> primary_edges;
    // public HashMap<Integer, HashSet<Integer>> secondary_edges;

    // Each node in 𝑣 ∈ H stores two bitmaps of size 3𝑑max = 𝑂(log 𝑛) each.
    // The frst indicates for each (𝑖, 𝑡) pair whether 𝑣 is an (𝑖, 𝑡)-node, and if so, the second indicates
    // whether 𝑣 is an (𝑖, 𝑡)-branching node or not
    // 3dmax because there are 3 types of endpoints (witness, primary, secondary) and dmax is the maximum degree of a node in the HForest
    // so for each node, we need to store information about whether it is an (i, t)-node for each possible i and t, which results in 3dmax combinations.
    public int n = 0;
    //get induced (i, t) forest by taking the union of the paths from each leaf to the corresponding root
    //We do not need to store the entire forest explicitly, but we can store the parent-child relationships and the node types to reconstruct the forest when needed.
    
    final int D_MAX; // maximum degree of a node in the HForest, which is O(log n)
    //approximate counters
    // for each (i, primary)-leaf pair, we maintain the approximate number of (i, primary)-endpoints touching the leaf.
    float beta = 2.0f; // error parameter for the approximate counters
    static float[] approximateCounter = new float[2]; //first is the mantissa, 2nd is exponent. No. counters = concatenation of m and e => (beta + 1)log log n + 1 bits.
    //mantissa = {0, 1}^(betaloglogn) bit string and exponent = {0, 1}^(loglogn + 1) bit string
    //integer representation is m2^e.
    //when adding 2 approximate counters, round DOWN to nearest approximate counter value. THis is done by
    // |+| => (1 − log^(−𝛽)𝑛) (𝑎 + 𝑏) ≤ 𝑎 |+| 𝑏 ≤ 𝑎 + 𝑏. 
    int weight; // number of endpoints in the subtree rooted at this node
    public int[] approximateCounters; // array of approximate counters for each (i, primary)-leaf pair
    private static int nextId = 0;
    public final int ID = nextId++;
    public HLeaf leafData; // only for leaf nodes, stores the isEndpoint bitmap and witness edges
    public boolean isRoot;
    public boolean[][] isEndpoint;

    public HNode(int n, int depth) {
        this.n = n;
        this.D_MAX = Math.max(1, (int) Math.floor(Math.log(n) / Math.log(2)))+1; // maximum degree of a node in the HForest, which is O(log n)
        this.depth = depth;
        this.edges_being_covered = new HashMap<>();
        this.children = new ArrayList<>();
        this.parent = null;
        this.nodeType = HNodeType.leaf;
        this.leafData = null;
        this.isEndpoint = new boolean[3][D_MAX + 1]; //3 types of endpoints, and we need to store whether this node is an endpoint for each possible depth up to D_MAX
        //initialize all endpoints to false
        for (int i = 0; i < 3; i++) {
            for (int j = 1; j <= D_MAX; j++) {
                isEndpoint[i][j] = false;
            }
        }
        this.approximateCounters = new int[D_MAX + 1]; // we need to store an approximate counter for each possible depth up to D_MAX
        this.weight = 1; // initially, the weight of a leaf node is 1, since it represents one vertex. For non-leaf nodes, the weight will be the sum of the weights of its children. This is used to maintain the invariant that the weight of a node at depth d is at most n/2^d.
        this.isRoot = false;

    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        HNode oth = (HNode)(obj);
        return this.ID == oth.ID;
    }


    public static HNode root(HNode node){
        HNode current = node;
        while (current != null && current.depth > 0){
            current = current.parent;
        }
        return current;
    }
    

    //Should store these as bitwise strings
    public int approximate_counter_add(int a, int b) {
        // add two approximate counters a and b, and return the result as an integer representation of the approximate counter
        // we need to round down to the nearest approximate counter value, which is done by multiplying the sum by (1 - log^(-beta)n)
        int sum = a + b;
        int rounded_sum = (int) Math.floor((1 - Math.pow(Math.log(n), -beta)) * sum);
        return rounded_sum;
    }

    public int approximate_counter_value(int m, int e) {
        // return the integer value of the approximate counter represented by mantissa m and exponent e
        return (int) (m * Math.pow(2, e));
    }

    public int approximate_counter(int j){
        double max_possible_height = (this.D_MAX - j) * (Math.log(Math.log(n))) + Math.floor(Math.log(weight)); // H_v = (dmax - depth) * loglogn/logbeta
        int C = (int) Math.pow(Math.pow(1-(Math.log(n)), (-beta)), max_possible_height); 
        return C;
    }

    //add a child node to the parent node. The edge will represent which edge is allowing the preexisting subtree to connect to whatever child subtree this is 
    public int add_child_to_node(HNode child, int[] edge){
        if (child == null)
            return -1;
        if (edge != null){ //it is a root{
            this.edges_being_covered.get(edge[0]).add(edge[1]);
            this.edges_being_covered.get(edge[1]).add(edge[0]);
            // this.nodes_covered.add(edge[0]);
            // this.nodes_covered.add(edge[1]);
        }
        if (this.weight+child.weight  >= Math.floor(this.n/Math.pow(2, this.depth))){
            //we need to make a new level as this will hit the invariant. This should never happen though
            return -1;
        }
        this.weight += child.weight;
        this.children.add(child);
        child.parent = this;
        // child.depth = this.depth + 1; not necessary since we have shortcuts?  
        
        return 1;
    }

    

    public void add_witness_edge(int u, int v){
        if (!edges_being_covered.containsKey(u)){
            edges_being_covered.put(u, new HashSet<>());
        }
        edges_being_covered.get(u).add(v);
    }

    public void recomputeBitmap(){
        //recompute the isEndpoint bitmaps for this node based on the edges being covered and the node types of its children
        //this should only be called when we are adding a child to a node, and we need to update the bitmaps of the parent node based on the new child node
        //we can do this by iterating through the edges being covered by this node, and checking if any of them are primary or secondary edges. If they are, we can update the corresponding bitmaps accordingly.
        //isEndpoint is the union of the isEndpoint bitmaps of the children
        
        if (this.leafData != null){
            //this is a leaf node, so we can just copy the isEndpoint bitmap from the leaf data
            this.leafData.recomputeBitmap(); //recompute the bitmap for the leaf data first, since it may have changed since the last time we computed the bitmap for this node
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < D_MAX; j++) {
                    this.isEndpoint[i][j] = this.leafData.isEndpoint[i][j];
                }
            }
            return;
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < D_MAX; j++) {
                this.isEndpoint[i][j] = false;
            }
        }
        for (HNode child : children) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < D_MAX; j++) {
                    this.isEndpoint[i][j] = this.isEndpoint[i][j] || child.isEndpoint[i][j];
                }
            }
        }
    }

    public void recomputeBitmapsUp(){
        //recompute the isEndpoint bitmaps for this node and all its ancestors based on the edges being covered and the node types of its children
        HNode current = this;
        while (current != null){
            current.recomputeBitmap();
            current = current.parent;
        }
    }
   

}

