package DynGraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class HTree {
    Map<Integer, HashSet<Integer>> graph = new HashMap<>();
    int n; // number of vertices in the graph
    HNode root; 
    Map<Integer, HashSet<HNode>> nodeMap;
    Map<Integer, HashMap<Integer, HNode>> edgeMap;
    
    // ArrayList<ConnVertex> dyn_vertices = new ArrayList<>();

    public HTree(int n) {
        this.n = n;
        this.root = new HNode(n, 0); //depth for the overall root will always be 0
        this.nodeMap = new HashMap<>();
        this.edgeMap = new HashMap<>();
    }
    //assuming we are 100% adding this node, not checking for connectivity
    public void add_node_to_root(HNode node, int[] edge){
        if (node.nodeType == HNodeType.leaf){
            //the node only represents one vertex, it probably connects to some parent
            this.root.add_child_to_node(node, edge);
            //does what edge the node covers depend on... it will have edges if the parent node is a root only
        }
        else{
            //this should never happen since you should only be able to add leaf nodes. Anything else will always need ot happen externally
            return;
        }
    }

    //underlying architecture for this hierarchy forest could be stored as a treap? which would make no sense at all...
    //okay no treaps, just vanilla this

    public boolean connected(int u, int v){
        // to check if theyre connected we need to see if their roots are the same
        // that means we need to find all the nodes where this vertex is an endpoint
        HNode nodeu = nodeMap.getOrDefault(u, null).iterator().next();
        HNode nodev = nodeMap.getOrDefault(v, null).iterator().next();
        if (nodeu == null || nodev == null)
            return false;
        return HNode.root(nodev) == HNode.root(nodeu);
    }

    public boolean connected(HNode nodeu, HNode nodev){
        if (nodeu == null || nodev == null)
            return false;
        return HNode.root(nodev) == HNode.root(nodeu);
    }

    public HNode getNext(int u){
        HNode nodeu = nodeMap.getOrDefault(u, null).iterator().next();
        return nodeu;
    }

    public static HNode merge(HNode u, HNode v){
        //we merge them at level 0
        
        HNode rootu = HNode.root(u);
        HNode rootv = HNode.root(v);
        if (rootu == rootv){
            return null;
        }
        rootu.children.addAll(rootv.children);
        //rootu.nodes_covered.addAll(rootv.nodes_covered);
        rootu.edges_being_covered.putAll(rootv.edges_being_covered);
        //need to update all the children of u and v that the new parent is u
        HNode child;
        for(int i = 0; i < rootv.children.size(); i++){
            child = rootv.children.get(i);
            child.parent = rootu;
        }
        
        return u;
    }
    

}