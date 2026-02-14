package DynGraph;
import ETTree.EulerTourTree;
import ETTree.Treap;
import ETTree.TreapNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ConnGraph {
    Map<Integer, Map<Integer, ArrayList<Integer>>> tree_edges;  //edge to its treap nodes. So that when we cut, we know what to cut
    ArrayList<EulerTourTree> forest; //forest of Euler Tour Trees, one for each level. 
    // There are log_2(n) levels, each with size increasing by a factor of 2
    // When we add an edge, we add it to level 0.
    Map<Integer, Map<Integer, ArrayList<Integer>>> ntree_edges;
    //level to non-tree edges. So that when we need to find a replacement edge, we can look for it in the non-tree edges of the same level or lower.
    public ConnGraph() {
        tree_edges = new HashMap<>();
        ntree_edges = new HashMap<>();
        forest = new ArrayList<>();
    }

    public boolean connected(int u, int v) {
        // Check if two nodes are in the same tree
        // for (EulerTourTree ett : forest) {
        //     if (ett.connected(u, v)) return true;
        // }
        if (forest.isEmpty()) return false;
         return forest.get(0).connected(u, v); 
    }

    public void addEdge(int u, int v){
        boolean non_tree = connected(u, v);
        if (non_tree){
            //need to add it to the non-tree edges
            ntree_edges.computeIfAbsent(0, k -> new HashMap<>()).computeIfAbsent(u, k -> new ArrayList<>()).add(v);
            ntree_edges.computeIfAbsent(0, k -> new HashMap<>()).computeIfAbsent(v, k -> new ArrayList<>()).add(u);
            return;
        }
        //need to add it to the tree edges
        tree_edges.computeIfAbsent(0, k -> new HashMap<>()).computeIfAbsent(u, k -> new ArrayList<>()).add(v);
        tree_edges.computeIfAbsent(0, k -> new HashMap<>()).computeIfAbsent(v, k -> new ArrayList<>()).add(u);
        //need to link it in the Euler Tour Tree
        
        if (forest.isEmpty()) {
            EulerTourTree ett = new EulerTourTree();
            ett.link(u, v);
            forest.add(ett);
        } else if (forest.size() > 0 && !non_tree) {
            //turns out that when adding this here, it somehow also adds u-v to forest.get(1). 
            // This is bad. How does that even happen
            forest.get(0).link(u, v);
        }
    }

    public boolean hasEdge(int u, int v){
        for (int level = 0; level < forest.size(); level++){
            if (hasEdge(u, v, level)) return true;
        }
        return false;
    }

    public boolean hasEdge(int u, int v, int level ){
        return (tree_edges.containsKey(level) && tree_edges.get(level).containsKey(u) && tree_edges.get(level).get(u).contains(v)) || 
        (ntree_edges.containsKey(level) && ntree_edges.get(level).containsKey(u) && ntree_edges.get(level).get(u).contains(v));
    }

    public boolean is_tree_edge(int u, int v, int level){
        return tree_edges.containsKey(level) && tree_edges.get(level).containsKey(u) && tree_edges.get(level).get(u).contains(v);
    }

    public boolean is_tree_edge(int u, int v){
        for (int level = 0; level < forest.size(); level++){
            if (is_tree_edge(u, v, level)) return true;
        }
        return false;
    }

    boolean find_replacement_at_level(TreapNode s, TreapNode l, int v, int level){
        //need to store the level of the edge, shit 
        
        Set<Integer> smallerVertices = new HashSet<>();
        smallerVertices = collectVertices(s, smallerVertices);
        int lev = level;
        //for (int lev = 1; lev >= 0; lev--){ //only checking the current level
            if (!ntree_edges.containsKey(lev)) return false;
            for (int u : smallerVertices) {
                if (!ntree_edges.get(lev).containsKey(u)) //u does not have any non tree edges incident
                    continue;
                for (int neighbor : ntree_edges.get(lev).get(u)) {
                    if (forest.get(lev).connected(u, neighbor)) continue; //don't consider edges that are in the same tree as s
                    //At the current level, see if the current outgoing edge from u to neighbor is such that neighbor is in the same tree as l
                    // TreapNode neighbor_node = forest.get(lev).getNode(u, neighbor); // this doesnt make sense, since the node wont exist
                    if (forest.get(lev).connected(v, neighbor)){ 
                        //need to add it to the tree edges
                        tree_edges.computeIfAbsent(lev, k -> new HashMap<>()).computeIfAbsent(u, k -> new ArrayList<>()).add(neighbor);
                        tree_edges.computeIfAbsent(lev, k -> new HashMap<>()).computeIfAbsent(neighbor, k -> new ArrayList<>()).add(u);
                        //need to link it in the Euler Tour Tree
                        // forest.get(lev).link(u, neighbor);
                        addEdgeToAllLevels(u, neighbor, lev);

                        //need to remove it from the non-tree edges
                        ntree_edges.get(lev).get(u).remove((Integer) neighbor);
                        ntree_edges.get(lev).get(neighbor).remove((Integer) u);
                        if (ntree_edges.get(lev).get(neighbor).isEmpty())                            
                            ntree_edges.get(lev).remove(neighbor);
                        if (ntree_edges.get(lev).get(u).isEmpty()) 
                            ntree_edges.get(lev).remove(u);
                        return true;
                    }
                }
            }
        
        //No replacements found
        return false;
    }

    Set<Integer> collectVertices(TreapNode node, Set<Integer> vertices) {
        if (node == null) return vertices;
        vertices.add(node.key.from);
        collectVertices(node.left, vertices);
        collectVertices(node.right, vertices);
        return vertices;
    }

    void addEdgeToAllLevels(int u, int v, int level){
        for (int i = 0; i <= level; i++){
            forest.get(i).link(u, v);
        }
    }

    // void increase_edge_level(int u, int v, int level, boolean is_tree){
    //     //need to remove it from the current level
    //     if (is_tree) {
    //         tree_edges.get(level).get(u).remove((Integer) v);
    //         if (tree_edges.get(level).get(u).isEmpty()) tree_edges.get(level).remove(u);
    //         tree_edges.get(level).get(v).remove((Integer) u);
    //         if (tree_edges.get(level).get(v).isEmpty()) tree_edges.get(level).remove(v);
    //         tree_edges.computeIfAbsent(level + 1, k -> new HashMap<>()).computeIfAbsent(u, k -> new ArrayList<>()).add(v);
    //         tree_edges.computeIfAbsent(level + 1, k -> new HashMap<>()).computeIfAbsent(v, k -> new ArrayList<>()).add(u);
    //     }
    //     else {
    //         ntree_edges.get(level).get(u).remove((Integer) v);
    //         if (ntree_edges.get(level).get(u).isEmpty()) ntree_edges.get(level).remove(u);
    //         ntree_edges.get(level).get(v).remove((Integer) u);
    //         if (ntree_edges.get(level).get(v).isEmpty()) ntree_edges.get(level).remove(v);
    //         ntree_edges.computeIfAbsent(level + 1, k -> new HashMap<>()).computeIfAbsent(u, k -> new ArrayList<>()).add(v);
    //         ntree_edges.computeIfAbsent(level + 1, k -> new HashMap<>()).computeIfAbsent(v, k -> new ArrayList<>()).add(u);
    //     }
    //     if (forest.size() <= level + 1) {
    //         forest.add(new EulerTourTree());
    //         if (is_tree)
    //             forest.get(level+1).link(u, v);
    //     }
    //     //need to add it to the next level
        
    // }

    //increase the level of all the edges in the subtree of root by 1
    //everything here will already be  atree node. 
    //we are assuming non tree nodes are already promoted while sampling them
    TreapNode _increase_ett_level(TreapNode root, int level){
            if (root == null) return Treap.root(root);
            int u = root.key.from;
            int v = root.key.to;
            //increase_edge_level(u, v, level, true);
            tree_edges.get(level).get(u).remove((Integer) v);
            if (tree_edges.get(level).get(u).isEmpty()) tree_edges.get(level).remove(u);
            tree_edges.computeIfAbsent(level + 1, k -> new HashMap<>()).computeIfAbsent(u, k -> new ArrayList<>()).add(v);
            forest.get(level+1).link(u, v);
            _increase_ett_level(root.left, level);
            _increase_ett_level(root.right, level);
            printForest();
            return root;
    }
    //add edges and rebuild f(lv+1)

    TreapNode increase_ett_level(TreapNode root, int level){
        if (forest.size() <= level + 1) {
            forest.add(new EulerTourTree());
        }
        TreapNode newRoot = _increase_ett_level(root, level);
        List<int[]> edgesToPromote = new ArrayList<>();
        Set<Integer> smallerVertices = collectVertices(root, new HashSet<>());
        //TreapNode newRoot = null;
        
        for (int u : smallerVertices) {
            if (!tree_edges.containsKey(level)) continue;
            if (!tree_edges.get(level).containsKey(u)) continue;

            for (int v : tree_edges.get(level).get(u)) {
                if (smallerVertices.contains(v)) {
                    if (u < v) {  // avoid duplicates
                        edgesToPromote.add(new int[]{u, v});
                    }
                }
            }
        }
        return newRoot;
    }


    int edge_level(int u, int v){
        for(int level = 0; level < forest.size(); level++){
            if (hasEdge(u, v, level)) return level;
        }
        return -1;
    }

    void remove_from_all_ett(int u, int v, int level){
        for (int i = 0; i <= level; i++){
            forest.get(i).cut(u, v);
        }
    }

    public boolean deleteEdge(int u, int v){
        if (!connected(u, v) || !hasEdge(u, v)) { //it is not an edge
            return false;
        }
        int level = edge_level(u, v);
        if (is_tree_edge(u, v)){
            //need to find a replacement edge if possible
            
            //it only cuts it at the level it is at. cut it wherever it is a tree edge.
            for(int lev = level; lev>=0; lev--){
                TreapNode split[] = forest.get(lev).cut(u, v);
                // whichever side of the split is smaller will go into s and the other into l
                TreapNode s, l;
                if(split == null) { //only the edge existed. 
                    //This is not changing forest of level 1. 
                    continue;
                }
                //when cutting a leaf on the tree, s will be null
                //if it is null, you still need to see if the individual nodes in s have any possible replacement edges in l.
                s = (TreapNode.size(split[0]) < TreapNode.size(split[1])) ? split[0] : split[1];
                l = (TreapNode.size(split[0]) < TreapNode.size(split[1])) ? split[1] : split[0];
                int dest =(Treap.contains(s, u)) ? v : u;
                boolean replacement_found = find_replacement_at_level(s, l, dest, lev);
                //need to remove it from all levels of ETT
                
                if (!replacement_found){
                    //need to increase the level of all the edges in the subtree of root by 1, since they are now disconnected from the main tree
                    // TreapNode root = forest.get(level).getNode(u, v);
                    // if (root == null) 
                    //     root = forest.get(level).getNode(v, u); //this is not possible
                    if (forest.size() <= lev + 1) {
                        forest.add(new EulerTourTree());
                    }
                    increase_ett_level(s, lev);
                }
                else{
                    remove_from_all_ett(u, v, lev-1);
                    addEdgeToAllLevels(u, v, lev-1);
                    return true;
                }
            }
            return true;
        } else {
            
            //need to remove it from the non-tree edges
            if (ntree_edges.get(level).containsKey(u)) {
                ntree_edges.get(level).get(u).remove((Integer) v);
                if (ntree_edges.get(level).get(u).isEmpty()) ntree_edges.get(level).remove(u);
            }
            if (ntree_edges.get(level).containsKey(v)) {
                ntree_edges.get(level).get(v).remove((Integer) u);
                if (ntree_edges.get(level).get(v).isEmpty()) ntree_edges.get(level).remove(v);
            }
            return true;
        }
    }

    public void printForest(){
        for (int i = 0; i < forest.size(); i++){
            System.out.println("Level " + i + ":");
            for (TreapNode root : forest.get(i).forest) {
                ArrayList<TreapNode> nodes = Treap.inorderToList(root, new ArrayList<>());
                for (TreapNode node : nodes) {
                    System.out.print("(" + node.key.from + "->" + node.key.to + ") ");
                }
                System.out.println();
            }
        }
    }
}
