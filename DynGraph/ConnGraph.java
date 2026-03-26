
package DynGraph;

import ETTree.EulerTourTree;
import ETTree.Treap;
import ETTree.TreapNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class ConnGraph {
    Map<Integer, Map<Integer, Set<Integer>>> tree_edges;  //edge to its treap nodes. So that when we cut, we know what to cut
    public ArrayList<EulerTourTree> forest; //forest of Euler Tour Trees, one for each level. 
    // There are log_2(n) levels, each with size increasing by a factor of 2
    // When we add an edge, we add it to level 0.
    Map<Integer, Map<Integer, Set<Integer>>> ntree_edges;
    //level to non-tree edges. So that when we need to find a replacement edge, we can look for it in the non-tree edges of the same level or lower.
    
    public ConnGraph() {
        tree_edges = new HashMap<>();
        ntree_edges = new HashMap<>();
        forest = new ArrayList<>();
    }

    // public boolean connected(int u, int v) {
    //     // Check if two nodes are in the same tree
    //     // for (EulerTourTree ett : forest) {
    //     //     if (ett.connected(u, v)) return true;
    //     // }
    //     if (forest.isEmpty()) return false;
    //      return forest.get(0).connected(u, v); 
    // }

    public boolean connected(int u, int v) {
        if (forest.isEmpty()) return false;
        return forest.get(0).connected(u, v);
    }

    public void addEdge(int u, int v){
        System.out.println("Adding edge "+u+ "-"+v);
        print_edges();  
        if (edge_level(u, v) >= 0) return;

        boolean non_tree = connected(u, v);
        if (non_tree &&( ntree_edges.containsKey(0) && ntree_edges.get(0).containsKey(u) && ntree_edges.get(0).get(u).isEmpty()) || (ntree_edges.containsKey(0) && ntree_edges.get(0).containsKey(v) && ntree_edges.get(0).get(v).isEmpty())) {
            System.out.println("Edge "+u+"-"+v+" already exists as a non-tree edge at level 0. Not adding it again.");
            //return;
        }
        if (non_tree){
            //need to add it to the non-tree edges
            ntree_edges.computeIfAbsent(0, k -> new HashMap<>()).computeIfAbsent(u, k -> new HashSet<>()).add(v);
            ntree_edges.computeIfAbsent(0, k -> new HashMap<>()).computeIfAbsent(v, k -> new HashSet<>()).add(u);
            print_edges();
            return;
        }
        //need to add it to the tree edges
        tree_edges.computeIfAbsent(0, k -> new HashMap<>()).computeIfAbsent(u, k -> new HashSet<>()).add(v);
        tree_edges.computeIfAbsent(0, k -> new HashMap<>()).computeIfAbsent(v, k -> new HashSet<>()).add(u);
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
        print_edges();
        valid(u, v);
    }

    public void print_edges(){
        System.out.println("----------------");
        System.out.println("Tree edges:");
        for (int level : tree_edges.keySet()){
            System.out.println("Level "+level+":");
            for (int u : tree_edges.get(level).keySet()){
                for (int v : tree_edges.get(level).get(u)){
                    System.out.print(u+"-"+v+" ");
                }
            }
            System.out.println();
        }
        System.out.println("Non-tree edges:");
        for (int level : ntree_edges.keySet()){
            System.out.println("Level "+level+":");
            for (int u : ntree_edges.get(level).keySet()){
                for (int v : ntree_edges.get(level).get(u)){
                    System.out.print(u+"-"+v+" ");
                }
            }
        }
        System.out.println();
    }

    public boolean hasEdge(int u, int v){
        // System.out.println("Checking if edge "+u+"-"+v+" exists:");
        int maxlevel = Math.max(forest.size() - 1, ntree_edges.size() - 1);
        for (int level = maxlevel; level >= 0; level--){
            if (hasEdge(u, v, level)) {
                // print_edges();
                return true;
            }
        }
        return false;
    }

    public boolean hasEdge(int u, int v, int level ){
        return ((tree_edges.containsKey(level) && tree_edges.get(level).containsKey(u) && 
        tree_edges.get(level).get(u).contains(v)) && (tree_edges.containsKey(level) && tree_edges.get(level).containsKey(v) && tree_edges.get(level).get(v).contains(u))) || 
        ((ntree_edges.containsKey(level) && ntree_edges.get(level).containsKey(u) && 
        ntree_edges.get(level).get(u).contains(v)) && (ntree_edges.get(level).containsKey(v) && ntree_edges.get(level).get(v).contains(u)));
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

    public boolean find_replacement_at_higher_levels(TreapNode s, TreapNode l, int v, int level){
        int higher_level = level + 1;
        //First check if some edge at a higher level would work
        if (s == null) return false;
        
        // Check current node
        //we cut 7, 1. 7 here is neighbour, so u must be 2
        int u = s.key.from;
        System.out.println("Checking replacement at level "+higher_level+". Current node is "+u+", destination is "+v);
        if (u == v) {
            System.out.println("Current node "+u+" is the same as destination "+v+". Skipping it as a replacement edge.");
            //if u is the same as v, then we cannot use it as a replacement edge because it will not connect the two components. So we just ignore it and move on to the next node.
            return false;
        }
        //if (u == v) return false; //if u is the same as v, then we cannot use it as a replacement edge because it will not connect the two components. So we just ignore it and move on to the next node.
        while (ntree_edges.containsKey(higher_level) && ntree_edges.get(higher_level).containsKey(u)) {
            for (int neighbor : new HashSet<>(ntree_edges.get(higher_level).get(u))) {
                System.out.println("Checking non-tree edge "+u+"-"+neighbor+" at higher level "+higher_level);
                
                if (forest.get(level).connected(u, neighbor)) continue;
                else {
                    System.out.println("Found replacement edge "+u+"-"+neighbor+" at higher level "+higher_level);
                    ntree_edges.get(higher_level).get(u).remove((Integer) neighbor);
                    ntree_edges.get(higher_level).get(neighbor).remove((Integer) u);
                    // if (ntree_edges.get(higher_level).get(neighbor).isEmpty()) ntree_edges.get(higher_level).remove(neighbor);
                    // if (ntree_edges.get(higher_level).get(u).isEmpty()) ntree_edges.get(higher_level).remove(u);
                    tree_edges.computeIfAbsent(level, k -> new HashMap<>())
                            .computeIfAbsent(u, k -> new HashSet<>()).add(neighbor);
                    tree_edges.computeIfAbsent(level, k -> new HashMap<>())
                            .computeIfAbsent(neighbor, k -> new HashSet<>()).add(u);
                    addEdgeToAllLevels(u, neighbor, level); // use level 
                    //remove it from all levels between level and higher level
                    for (int x = level + 1; x <= higher_level; x++) {
                        ntree_edges.get(x).get(u).remove((Integer) neighbor);
                        //if (ntree_edges.get(x).get(u).isEmpty()) ntree_edges.get(x).remove(u);
                        ntree_edges.get(x).get(neighbor).remove((Integer) u);
                        //if (ntree_edges.get(x).get(neighbor).isEmpty()) ntree_edges.get(x).remove(neighbor);
                    }
                    return true;
                }
                // else{// if (forest.get(level).connected(v, neighbor)) {
                //     System.out.println("Found replacement edge "+u+"-"+neighbor+" at higher level "+higher_level);
                //     if (neighbor == v) continue;
                //     tree_edges.computeIfAbsent(level, k -> new HashMap<>()).computeIfAbsent(u, k -> new HashSet<>()).add(neighbor);
                //     tree_edges.computeIfAbsent(level, k -> new HashMap<>()).computeIfAbsent(neighbor, k -> new HashSet<>()).add(u);
                //     addEdgeToAllLevels(u, neighbor, level);
                //     ntree_edges.get(higher_level).get(u).remove((Integer) neighbor);
                //     ntree_edges.get(higher_level).get(neighbor).remove((Integer) u);
                //     if (ntree_edges.get(higher_level).get(neighbor).isEmpty()) ntree_edges.get(higher_level).remove(neighbor);
                //     if (ntree_edges.get(higher_level).get(u).isEmpty()) ntree_edges.get(higher_level).remove(u);
                //     return true;
                // }
            }
            higher_level++;
        }
        //if (s.key.from == s.key.to) return false;
        if (find_replacement_at_higher_levels(s.left, l, v, level)) return true;
        else if (find_replacement_at_higher_levels(s.right, l, v, level)) return true;
        return false;
    }

    boolean find_replacement_at_level(TreapNode s, TreapNode l, int v, int level) {
        if (s == null) return false;
        
        // Check current node
        int u = s.key.from;
        
        if (ntree_edges.containsKey(level) && ntree_edges.get(level).containsKey(u)) {
            for (int neighbor : new HashSet<>(ntree_edges.get(level).get(u))) {
                System.out.println("Checking non-tree edge "+u+"-"+neighbor+" at level "+level);
                if (forest.get(level).connected(u, neighbor)) { //make sure that neighbor is not already in s
                    //promote the edge
                     ntree_edges.get(level).get(u).remove((Integer) neighbor);
                    ntree_edges.get(level).get(neighbor).remove((Integer) u);
                    ntree_edges.computeIfAbsent(level+1, k -> new HashMap<>()).computeIfAbsent(u, k -> new HashSet<>()).add(neighbor);
                    ntree_edges.computeIfAbsent(level+1, k -> new HashMap<>()).computeIfAbsent(neighbor, k -> new HashSet<>()).add(u);
                } 
                else{// if (forest.get(level).connected(v, neighbor)) {
                    System.out.println("Found replacement edge "+u+"-"+neighbor+" at level "+level); 
                    tree_edges.computeIfAbsent(level, k -> new HashMap<>()).computeIfAbsent(u, k -> new HashSet<>()).add(neighbor);
                    tree_edges.computeIfAbsent(level, k -> new HashMap<>()).computeIfAbsent(neighbor, k -> new HashSet<>()).add(u);
                    addEdgeToAllLevels(u, neighbor, level);
                    ntree_edges.get(level).get(u).remove((Integer) neighbor);
                    ntree_edges.get(level).get(neighbor).remove((Integer) u);
                    if (ntree_edges.get(level).get(neighbor).isEmpty()) ntree_edges.get(level).remove(neighbor);
                    if (ntree_edges.get(level).get(u).isEmpty()) ntree_edges.get(level).remove(u);
                    if (!connected(v, u)) {
                        System.out.println("Replacement edge "+u+"-"+neighbor+" at level "+level+" connects the components");
                    }
                    return true;
                }
                // else{
                //     //really should not be here, but if this is reached then it means that neighbor is in a different tree and cannot be a replacement edge. So we just ignore it and move on to the next neighbor.
                //     continue;
                // }
            }
        }
       
        
        // Recurse into children
        System.out.println("Checking children of "+u+" at level "+level+" left: "+(s.left != null ? s.left.key.from : "null")+", right: "+(s.right != null ? s.right.key.from : "null"));
        if (find_replacement_at_level(s.left, l, v, level)) return true;
        else if (find_replacement_at_level(s.right, l, v, level)) return true;
        return false;
    }

    // Set<Integer> collectVertices(TreapNode node, Set<Integer> vertices) {
    //     if (node == null) return vertices;
    //     vertices.add(node.key.from);
    //     collectVertices(node.left, vertices);
    //     collectVertices(node.right, vertices);
    //     return vertices;
    // }

    boolean valid(int u, int v){
        //check if u and v are in both tree and non tree edges
        boolean treeedge = is_tree_edge(u, v);
        boolean nottreeedge = false;
        for(int level = 0; level < Math.max(forest.size(), ntree_edges.size()); level++){
            if (ntree_edges.containsKey(level) && ntree_edges.get(level).containsKey(u) && ntree_edges.get(level).get(u).contains(v)){
                nottreeedge = true;
            }
            if(tree_edges.containsKey(level) && tree_edges.get(level).containsKey(u) && tree_edges.get(level).get(u).contains(v)){
                treeedge = true;
            }
        }
        for (int level = 0; level < Math.max(forest.size(), ntree_edges.size()); level++){
            if (nottreeedge && treeedge) {
                System.out.println("Error: edge "+u+"-"+v+" is both a tree edge and a non-tree edge at level "+level);
                return false;
            }
        }
        return true;
    }

    void addEdgeToAllLevels(int u, int v, int level){
        if (u == v) return;
        for (int i = 0; i <= level; i++){
            forest.get(i).link(u, v);
        }
    }

    void removeEdgeFromAllLevels(int u, int v, int level, boolean is_tree){
        for (int i = 0; i <= Math.max(forest.size()-1, ntree_edges.size()-1); i++){
            // forest.get(i).cut(u, v);
            if (is_tree) {
                if (tree_edges.containsKey(i) && tree_edges.get(i).containsKey(u) && tree_edges.get(i).get(u).contains(v)){
                    tree_edges.get(i).get(u).remove((Integer) v);
                    if (tree_edges.get(i).get(u).isEmpty()) tree_edges.get(i).remove(u);
                    tree_edges.get(i).get(v).remove((Integer) u);
                    if (tree_edges.get(i).get(v).isEmpty()) tree_edges.get(i).remove(v);
                }
                
            }
            else {
                ntree_edges.get(i).get(u).remove((Integer) v);
                if (ntree_edges.get(i).get(u).isEmpty()) ntree_edges.get(i).remove(u);
                ntree_edges.get(i).get(v).remove((Integer) u);
                if (ntree_edges.get(i).get(v).isEmpty()) ntree_edges.get(i).remove(v);
            }
            System.out.println("Cutting edge "+u+"-"+v+" at level "+i);
            //forest.get(i).cut(u, v);
        }
        
        print_edges();
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
  
    
    TreapNode _increase_ett_level(TreapNode root, int level) {
        if (root == null) return null;
        int u = root.key.from;
        int v = root.key.to;

        if (tree_edges.containsKey(level) && tree_edges.get(level).containsKey(u)
                && tree_edges.get(level).get(u).contains(v)) {
            tree_edges.get(level).get(u).remove((Integer) v);
            if (tree_edges.get(level).get(u).isEmpty()) tree_edges.get(level).remove(u);

            tree_edges.computeIfAbsent(level + 1, k -> new HashMap<>()).computeIfAbsent(u, k -> new HashSet<>()).add(v);
        }

        forest.get(level + 1).link(u, v);

        _increase_ett_level(root.left, level);
        _increase_ett_level(root.right, level);
        return root;
    }
    //add edges and rebuild f(lv+1)

    // TreapNode increase_ett_level(TreapNode root, int level){
    //     if (forest.size() <= level + 1) {
    //         forest.add(new EulerTourTree());
    //     }
    //     TreapNode newRoot = _increase_ett_level(root, level);
    //     List<int[]> edgesToPromote = new ArrayList<>();
    //     Set<Integer> smallerVertices = collectVertices(root, new HashSet<>());
    //     //TreapNode newRoot = null;
        
    //     for (int u : smallerVertices) {
    //         if (!tree_edges.containsKey(level)) continue;
    //         if (!tree_edges.get(level).containsKey(u)) continue;

    //         for (int v : tree_edges.get(level).get(u)) {
    //             if (smallerVertices.contains(v)) {
    //                 if (u < v) {  // avoid duplicates
    //                     edgesToPromote.add(new int[]{u, v});
    //                 }
    //             }
    //         }
    //     }
    //     return newRoot;
    // }

    TreapNode increase_ett_level(TreapNode root, int level) {
        if (forest.size() <= level + 1) {
            forest.add(new EulerTourTree());
        }
        return _increase_ett_level(root, level);
    }


    int edge_level(int u, int v) {
        for (int level = forest.size() - 1; level >= 0; level--) {
            if (hasEdge(u, v, level)) return level;
        }
        return -1;
    }

    void remove_from_all_ett(int u, int v, int level){
        for (int i = 0; i <= level; i++){
            forest.get(i).cut(u, v);
        }
    }

    void relinkComponentAtLowerLevels(TreapNode root, int level) {
        if (root == null) return;
        int u = root.key.from;
        int v = root.key.to;
        // This edge is now at level+1, re-link it into levels 0..level-1
        for (int i = 0; i < level; i++) {
            forest.get(i).link(u, v);
        }
        relinkComponentAtLowerLevels(root.left, level);
        relinkComponentAtLowerLevels(root.right, level);
    }

    void relinkIntoLevel0(TreapNode root) {
        if (root == null) return;
        int u = root.key.from;
        int v = root.key.to;
        if (u == v) return;
        forest.get(0).link(u, v);
        relinkIntoLevel0(root.left);
        relinkIntoLevel0(root.right);
    }

    public boolean deleteEdge(int u, int v){
        // System.out.println("Deleting edge "+u+"-"+v);
        // if (!connected(u, v) || edge_level(u, v) == -1) { //it is not an edge
        //     return false;
        // }
        int level = edge_level(u, v);
        if (is_tree_edge(u, v)){

            //need to find a replacement edge if possible
            removeEdgeFromAllLevels(u, v, level, true);
            //print_edges();
            //it only cuts it at the level it is at. cut it wherever it is a tree edge.
            System.out.println("Trying to find a replacement edge for "+u+"-"+v+" at level "+level);
            TreapNode lastS = null;
            for (int j = forest.size() - 1; j > level; j--) {
                if (forest.get(j).connected(u, v)) {
                    System.out.println("Error: "+u+" and "+v+" are still connected at level "+j+" after cut. This should not happen.");
                    printForest();
                    print_edges();
                    forest.get(j).printForest();
                    forest.get(j).cut(u, v);
                    //hasEdge(u, v);
                    //return false;
                }
            }
            for(int lev = level; lev>=0; lev--){
                TreapNode split[] = forest.get(lev).cut(u, v);
                // whichever side of the split is smaller will go into s and the other into l
                if (forest.get(lev).connected(u, v)){
                    print_edges();
                    forest.get(lev).printForest();
                    System.out.println("Error: "+u+" and "+v+" are still connected at level "+lev+" after cut. This should not happen.");
                    
                    return false;
                }
                
                // forest.get(lev).nodes.get(u).remove((TreapNode) new TreapNode(u, v));
                // forest.get(lev).nodes.get(v).remove((TreapNode) new TreapNode(v, u));
                TreapNode s, l;
                if(split == null) { //only the edge existed. 
                    //This is not changing forest of level 1. 
                    print_edges();
                    forest.get(lev).printForest();
                    continue;
                }
                //when cutting a leaf on the tree, s will be null
                //if it is null, you still need to see if the individual nodes in s have any possible replacement edges in l.
                s = (TreapNode.size(split[0]) < TreapNode.size(split[1])) ? split[0] : split[1];
                l = (TreapNode.size(split[0]) < TreapNode.size(split[1])) ? split[1] : split[0];
                int dest, not_dest;
                // System.out.println("Trying to find replacement edge for "+u+"-"+v+" at level "+lev);
                int flag = 0;
                if (Treap.contains(s, u)) {
                    dest = v;
                    not_dest = u;
                    flag = 1;
                }
                else {
                    dest = u;
                    not_dest = v;
                }
                int f = 0;
                // forest.get(lev).cut(u, v);
                if (s == null && l!= null) {
                    // System.out.println("Smaller component is empty, skipping to next level");
                    // forest.get(lev).printForest();
                    f = 1;
                    s = new TreapNode(not_dest, not_dest); //just a dummy node to represent the smaller component. This is needed because find_replacement_at_higher_levels and find_replacement_at_level expect a treapnode as input, and they will check the neighbors of this node for replacement edges.
                }
                boolean replacement_found = find_replacement_at_higher_levels(s, l, dest, level);
                if (!replacement_found && f == 1) {
                    replacement_found = find_replacement_at_higher_levels(new TreapNode(dest, dest), l, not_dest, level);
                    int temp = dest;
                    dest = not_dest;
                    not_dest = temp;
                    s = new TreapNode(dest, dest);
                }
                if (replacement_found) {
                    remove_from_all_ett(u, v, lev-1);  //remove edge from all levels will remove the edge from edgeset so it cannot be reconsidered. this will remove it from teh forest as well
                    // addEdgeToAllLevels(u, v, lev-1);
                    print_edges();
                    valid(u, v);
                    return true;
                }
                
                replacement_found = find_replacement_at_level(s, l, dest, lev);
                
                //need to remove it from all levels of ETT
                lastS = s;
                if (!replacement_found){
                    //need to increase the level of all the edges in the subtree of root by 1, since they are now disconnected from the main tree
                    // TreapNode root = forest.get(level).getNode(u, v);
                    // if (root == null) 
                    //     root = forest.get(level).getNode(v, u); //this is not possible
                    System.out.println("No replacement edge found for "+u+"-"+v+" at level "+lev+", increasing the level of the smaller component");
                    // if (forest.size() <= lev + 1) {
                    //     forest.add(new EulerTourTree());
                    // }
                    //print_edges();
                    //boolean r = find_replacement_at_level(s, l, dest, lev);
                    

                    System.out.println("-----------------------------");
                    Treap.inorder(l);
                    System.out.println("-----------------------------");
                    Treap.inorder(s);
                    System.out.println("-----------------------------");

                    if (Treap.size(s) > 1)
                        increase_ett_level(s, lev);
                    // if (lev == 0) 
                    //     relinkIntoLevel0(s);
                    relinkComponentAtLowerLevels(s, level);
                }
                else{
                    remove_from_all_ett(u, v, lev-1);  //remove edge from all levels will remove the edge from edgeset so it cannot be reconsidered. this will remove it from teh forest as well
                    // addEdgeToAllLevels(u, v, lev-1);
                    valid(u, v);
                    return true;
                }
                //need to increase the level of all the edges in the subtree of root by 1, since they are now disconnected from the main tree

            }
            // if (lastS != null) {
            //     relinkIntoLevel0(lastS);
            // }
            relinkComponentAtLowerLevels(lastS, level);
            valid(u, v);
            return true;
        } else {
            System.out.println("Deleting non-tree edge "+u+"-"+v+" at level "+level);
            //need to remove it from the non-tree edges
            for (int i = ntree_edges.size() -1; i >= 0; i--){
                if (ntree_edges.containsKey(i) && ntree_edges.get(i).containsKey(u) && ntree_edges.get(i).containsKey(v)){
                    ntree_edges.get(i).get(u).remove((Integer) v);
                    //if (ntree_edges.get(i).get(u).isEmpty()) ntree_edges.get(i).remove(u);
                    ntree_edges.get(i).get(v).remove((Integer) u);
                    //if (ntree_edges.get(i).get(v).isEmpty()) ntree_edges.get(i).remove(v);
                    System.out.println("Removed non-tree edge "+u+"-"+v+" from level "+i);
                    print_edges();
                }
            }
            if (hasEdge(u, v, 1)){
                System.out.println("Error: edge "+u+"-"+v+" still exists after deletion. This should not happen.");
                print_edges();
                return false;
            }

            // if (ntree_edges.get(level).containsKey(u)) {
            //     // remove specific element instead of the index
            //     ntree_edges.get(level).get(u).remove((Integer) v);
            //     // ntree_edges.get(level).get(u).remove(v);
            //     if (ntree_edges.get(level).get(u).isEmpty()) ntree_edges.get(level).remove(u);
            // }
            // if (ntree_edges.get(level).containsKey(v)) {
            //     ntree_edges.get(level).get(v).remove((Integer) u);
            //     if (ntree_edges.get(level).get(v).isEmpty()) ntree_edges.get(level).remove(v);
            // }
            print_edges();
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

    public int max_comp_size(){
        int max_size = 0;
        for (int i = 0; i < forest.size(); i++){
            for (TreapNode root : forest.get(i).forest) {
                int size = Treap.size(root);
                if (size > max_size) max_size = size;
            }
        }
        return max_size;
    }
}
