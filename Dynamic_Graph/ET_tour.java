package Dynamic_Graph;

//import java.lang.reflect.Array;
import java.util.*;

//failing because sets are the devil what even is this code tbh


//i really need to fix this cuz idk if the dictionary stuff is gonna hold up
public class ET_tour{
    //need a map for IDtoNode, NodeSet,
    //edgemap, and adj_map
    Map<Integer, Node> IDtoNode;
    Map<Integer, Set<Node>> NodeSet; //set of nodes (edges that node is in) for each node
    Map<Integer, Map<Integer, Node>> edgemap;//maps (u, v) to node u
    //so edgemap.get(u) returns a map of all adj v
    //it->second maps (u, v) it to node u
    Map<Boolean, Map<Integer, Integer>> adj_map; //number of non tree edges and tree edges adjacentto u
    //adjacency list?

    Bst btree; //wait but we could have multiple trees because theyre not necessarily connected
    //ig that doesnt matter? idk. wait no it definitely does matter wtf is happening


    public ET_tour(){
        NodeSet = new HashMap<>();
        IDtoNode = new HashMap<>();
        edgemap = new HashMap<>();
        btree = new Bst();
        adj_map = new HashMap<>();
        adj_map.put(true, new HashMap<>()); //tree edges
        adj_map.put(false, new HashMap<>()); //non tree edges
        
    }
    public Node get_node(int u){ //returns IDtoNode[u], so node version of u
        if(!IDtoNode.containsKey(u)){
            return null;
        }
        return IDtoNode.get(u);
    }
    //returns node u 
    public Node get_edge(int u, int v){
        //ArrayList<Node> neighbours = NodeSet.get(u);
        //int[] edge = {u, v}; returns u
        //System.out.println("Getting edge " + u + "-" + v);
        if (edgemap.get(u) !=null){
            ////System.out.println("Getting edge " + u + "-" + v);
            
            return edgemap.get(u).get(v); //will j return node u bro
        }
        
        return null;
    }

    public void add_node(int u, Node nu){
        //System.out.println("Adding node " + u);

        Bst.change_root(nu);
        NodeSet.putIfAbsent(u, new HashSet<>());
        NodeSet.get(u).add(nu);
        if(!IDtoNode.containsKey(u)){ //it is not already in the tree
            IDtoNode.put(u, nu);
            //nu.createIfAbsent();
            // adj_map.get(false).computeIfAbsent(u, k -> new HashMap<Integer, Integer>());
            // adj_map.get(true).computeIfAbsent(u, k -> new HashMap<Integer, Integer>());
            nu.adjacent_nodes[1] = adj_map.get(true).getOrDefault(u, 0); //tree edges
            //so that is. no. adhacent nodes of u which we are..not in the tree
            nu.adjacent_nodes[0] = adj_map.get(false).getOrDefault(u, 0); //non tree edges
            
            //System.out.println("adjacent nodes: " + nu.adjacent_nodes[0] + " " + nu.adjacent_nodes[1]);
        }
        nu.update();

    }

    public void add_edge(int u, int v, Node n){

        edgemap.putIfAbsent(u, new HashMap<>());
        //edgemap.computeIfAbsent(v, k -> new HashMap<>());
        edgemap.get(u).put(v, n);
        //edgemap.get(v).put(u, n);
        //^ is supposed to store node v not u but ok?

        //since its pointers this should be okay? i think?
        //how does this initialize tho like when i initiali
    }

    void remove_node(int u, Node n){
        // if(n == null){
        //     System.out.println("Node " + u + " does not exist.");
        //     return; //node does not exist
        // }
        int ntree = n.adjacent_nodes[1], nntree = n.adjacent_nodes[0];
        // if(!IDtoNode.containsKey(u)){
        //     return; //node does not exist
        // }

        NodeSet.get(u).remove(n);

        if(NodeSet.get(u).isEmpty()){
            IDtoNode.remove(u);
        }
        else{
            Node next = NodeSet.get(u).iterator().next(); //get the next node in the set
            IDtoNode.put(u, next);
            Bst.change_root(next);
            next.adjacent_nodes[1] = ntree;
            next.adjacent_nodes[0] = nntree;
            next.update();
        }
    }

    void remove_edge(int u, int v){
        edgemap.get(u).remove(v);
        //do i edit adj map as well
        // if(adj_map.get(true).containsKey(u)){
        //     adj_map.get(true).merge(u, -1, Integer::sum); //if is_treeedge then we search in index 1
        // }
        // if(adj_map.get(false).containsKey(u)){
        //     adj_map.get(false).merge(u, -1, Integer::sum); //if non
        // }
        //edgemap.get(v).remove(u);
    }

    void reroot(Node u){
        Bst.change_root(u);
        if(u.left == null){
            return;
        }
        Node lchild = u.left;
        //remove lchild, so we remove the entire left side of the tree
        Bst.remove_child_node(lchild); //removes this nodes connection to its parent, but keeps its suBst
        Node front = Bst.leftmost(lchild); //make the leftmost of the above suBst the root
        front.left = u; //connects the leftmost of the subtree to u
        //why... are we doing this again
        u.parent = front;
        front.update();
        // lchild.update();
        //at this point the root is the previous leftmost node, and u is the left child
        //ok so 
        //
        Bst.change_root(u);
        //this changes the root to u, so it only has a right subtree rooted at the previous leftmost
    }

    public boolean connected(int u, int v){
        if(u == v){
            return true;
        }
        //print_tour(u);
        Node x = get_node(u);
        Node y = get_node(v);
        if(x == null || y == null){
            //System.out.println("missing node 154");
            return false;
        }
        Bst.change_root(x);
        Bst.change_root(y);
        //keep rotating until y becomes x's parent, or x becomes the root
        while(x.parent != null && x.parent !=y){
            Bst.rotate(x);
        }
        return x.parent == y;
    }

    Node subtree_has_adj(Node u, boolean is_treeedge){
        if(u == null)
            return new Node(0);
        if(u.adjacent_nodes[is_treeedge?1:0] > 0){
            return u;
        }
        //Bst.change_root(u);
        if(u.left!=null){
            if (u.left.adjacent_nodes[is_treeedge? 1:0] > 0)
                return u.left;
            subtree_has_adj(u.left, is_treeedge);
        }
        else if(u.right!=null){
            if (u.right.adjacent_nodes[is_treeedge? 1:0] > 0)
                return u.right;
            subtree_has_adj(u.right, is_treeedge);
        }
        return new Node(0);
    }


    int explore(int start, Integer max_cc){
        ArrayList<Integer> stack = new ArrayList<>();
        stack.add(start);
        Set<Integer> visited = new HashSet<>();
        visited.add(start);
        //int size = 0;
        while (stack.size() > 0){
            int v = stack.remove(stack.size()-1);
            //System.out.println("adj of v:"+edges.get(v));
            for(int u : edgemap.get(v).keySet()){
                // System.out.println("u="+u);
                // System.out.println("stack:"+stack);
                // System.out.println("visited:"+visited);
                if(!visited.contains(u)){
                    visited.add(u);
                    stack.add(u);
                }

                if(visited.size() == edgemap.size()){
                    stack.clear();
                    break;
                }
            }
        }
        // System.out.println("size: "+visited.size()+" for v: "+start);
        // System.out.println("set: "+visited);
        if(visited.size() > max_cc){
            max_cc = (visited.size());
        }
        Map<Integer, Set<Integer>> result = new HashMap<>();
        result.put(max_cc, visited);
        return max_cc;
    }


    public int size(int u){
        Node x = get_node(u);
        if(x == null){
            return 1;
        }
        Bst.change_root(x);
        return x.size_subtree/2 + 1;
        
        //return explore(u, 0);
    }

    //return some adj node in the subtree of u
    public int get_adjacent(int u, boolean is_treeedge){
        // System.out.println("Getting adjacent node for " + u + " with is_treeedge = " + is_treeedge);
        // System.out.println("nodeset: " + NodeSet);
        // System.out.println("IDtoNode: " + IDtoNode);
        // System.out.println("adj_map: " + adj_map);
        // System.out.println("edgemap: " + edgemap);

        Node x = get_node(u);
        if(x == null){ //the node is not used yet
            //System.out.println("Node " + u + " does not exist??");
            return adj_map.get(is_treeedge).getOrDefault(u, -1)>0? u: -1; 
            //return -1;
        }
        Bst.change_root(x);
        int rep = subtree_has_adj(x, is_treeedge).name;
        //if the number of adj nodes in the tree is 0
        if(x.sum_adjacent_nodes[(is_treeedge)? 1:0] < 0 && rep!=0){ //true  = 1, false = 0. and tree nodes are in sum[1]
            
            System.out.println("subtree " + u + " has no adjacent nodes of type treeedge = "+is_treeedge+" : "+x.sum_adjacent_nodes[is_treeedge? 1:0]);
            System.out.println("Size of subtree: "+x.size_subtree);
            System.out.println("Getting adjacent node for " + u + " with is_treeedge = " + is_treeedge);
            System.out.println("nodeset: " + NodeSet);
            System.out.println("IDtoNode: " + IDtoNode);
            System.out.println("adj_map: " + adj_map);
            System.out.println("edgemap: " + edgemap);
            System.out.println(rep);
            Bst.print_bst(x);
            return -1;
        }
        else if(x.sum_adjacent_nodes[(is_treeedge)? 1:0] == 0){
            // System.out.println("subtree " + u + " has no adjacent nodes: "+x.sum_adjacent_nodes[is_treeedge? 1:0]);
            // System.out.println("Getting adjacent node for " + u + " with is_treeedge = " + is_treeedge);
            // System.out.println("nodeset: " + NodeSet);
            // System.out.println("IDtoNode: " + IDtoNode);
            // System.out.println("adj_map: " + adj_map);
            // System.out.println("edgemap: " + edgemap);
            return -1;
        }
    
        //if something in the subtree has adjacent nodes
        else{
            System.out.println("subtree " + u + " has adjacent nodes: "+ adj_map.get(is_treeedge).get(u) + " " + x.sum_adjacent_nodes[is_treeedge? 1:0]);
        }
        //is this ... right?
        //we search the subtree of x for a node that has an adjacent node of type is_treeedge
        int new_x = x.name;
        while(x!=null && adj_map.get(is_treeedge).getOrDefault(x.name, 0) <= 0){
            //System.out.println("Node " + x.name + " has no adjacent nodes so we look in subtree");
            new_x = x.name;;
            Node lchild = x.left;
            Node rchild = x.right;
            if(lchild != null && lchild.sum_adjacent_nodes[is_treeedge? 1:0] > 0){
                x = lchild;
            }
            else if(rchild != null && rchild.sum_adjacent_nodes[is_treeedge? 1:0] > 0){
                x = rchild;
            }
            if(new_x == x.name){
                //System.out.println("No adjacent nodes found in subtree of " + u);
                return -1; //no adjacent nodes found
            }
            //System.out.println("Moving to next node " + x + " with adj nodes = " + adj_map.get(is_treeedge).get(x.name));
        }
        //System.out.println("Found adjacent node " + x.name + " with adj nodes = " + edgemap.get(x.name));
        Bst.change_root(x);
        return x.name;
    }

    //if it is a tree edfe then we add the num adj vertices to the vertex
    //splay root to u. change number of adjacent nodes - if we remove/add a treeedge then we update if not then same 
    public void update_adjacent(int u, int add_adj, boolean is_treeedge){

        // if(adj_map.get(is_treeedge).getOrDefault(u, 0) <= 0 && add_adj < 0){
        //     //System.out.println("No adjacent nodes to remove for " + u + " with is_treeedge = " + is_treeedge);
        //     return; //no adjacent nodes to remove
           
        // }
        // if(adj_map.get(is_treeedge).containsKey(u) && adj_map.get(is_treeedge).get(u) >= NodeSet.size()-1 && add_adj > 0){
        //     //System.out.println("already saturated");
        //     return;
        // }
        int adj_sum = adj_map.get(is_treeedge).getOrDefault(u, 0);
        adj_map.get(is_treeedge).put(u, adj_sum + add_adj);  //if is_treeedge then we search in index 1 ieuvsh 8wfesc

        Node x = get_node(u);
        
        if(x == null){
            return;
        }

        Bst.change_root(x);
        x.adjacent_nodes[is_treeedge? 1:0] += add_adj; //if tree edge then we put in adjajcent[1]
        x.update();

        System.out.println("Updated sum adjacent nodes for " + u + ": sum nontree " + x.sum_adjacent_nodes[0] + " sum tree " + x.sum_adjacent_nodes[1]);
        System.out.println("adj_map: " + adj_map);
    }

    //now to do cut and link

    public boolean cut(int u, int v){
        System.out.println("Cutting " + u + " and " + v);
        if(!connected(u, v)){
            
            //System.out.println("Nodes " + u + " and " + v + " are not connected.");
            //System.out.println(edgemap);

            return false; //they are not connected so it is not a tree edge
        }
        //get edge is failing when we know that the edge DOES exist which makes no sense
        Node x = get_edge(u, v);
        if(x == null){
            //System.out.println("Edge " + u + "-" + v + " does not exist in the tree 264. "+ get_edge(u, v));
            //System.out.println(edgemap);
            return false; //edge does not exist
        }
        Node y = get_edge(v, u);
        if(y == null){
            //System.out.println("y: Edge " + v + "-" + u + " does not exist in the tree 269.");
            return false; //edge does not exist
        }
        reroot(x);
        Bst.change_root(y);
        while(x.parent != y){
            Bst.rotate(x);
        }
        Bst.remove_child_node(x); //rseparates child and parent

        Node next = Bst.next(y);
        if(next != null){ //if there is a next node, we need to update the edge
            int temp = next.name;
            Node t = Bst.rightmost(next);
            remove_edge(v, temp);
            
            remove_edge(temp, v);

            add_edge(v, temp, t);

            add_edge(temp, v, next);

        }
        
        remove_node(u, x);
        remove_node(v, y);

        remove_edge(u, v);
        remove_edge(v, u);

        Bst.delete_node(x);
        Bst.delete_node(y);
        //System.out.println("Cut complete.");
        System.out.println("new edgemap: "+edgemap);
        //
        return true;

    }

    public boolean link(int u, int v){
        System.out.println("Linking " + u + " and " + v);
        // print_tour(u);
        // print_tour(v);
        if(connected(u, v)){
            //System.out.println("Nodes " + u + " and " + v + " are already connected.");
            return false; //they are already connected so no need to link
        }
        Node x = get_node(u);
        Node y = get_node(v);
        if(x != null)
            reroot(x);
        if(y != null)
            reroot(y);
        //make x and y the roots of their respective trees
        Node utemp = Bst.insert_node(x);
        Node vtemp = Bst.insert_node(y);
        //add a new child to x and y and name it utemp and vtemp
        utemp.name = u;
        vtemp.name = v;
        //print_tour(u);
        //print_tour(v); //this is j ->u u
        //inserts a node t the end of u's tree and v's tree and names it u and v
        add_node(u, utemp);
        add_node(v, vtemp);

        //add it to teh ett
        if(y == null){//if there is no node v, then set it to vtemp, which is the new node iserted into y's tree
            y = vtemp;
        }
        Bst.change_root(y);

        utemp.right = y;
        y.parent = utemp;
        utemp.update();
        
        add_edge(u, v, utemp);
        add_edge(v, u, vtemp);

        
        System.out.println("Link complete: edges = "+ edgemap);
        //print_tour(u);
        return true;
    }

    // boolean subtree_has_adj_helper(Node u, boolean is_treeedge){
    //      if(u == null)
    //         return false;
    //     //Bst.change_root(u);
    //     if(u!=null){
    //         return u.adjacent_nodes[is_treeedge? 1:0] > 0? true: false;
    //     }
    // }

    

    void inorder(Node u){
        if (u == null){
            return;
        }
        inorder(u.left);
        System.out.print(u.name + " ");

        inorder(u.right);
    }

    // void preorder(Node u){
    //     if (u == null){
    //         return;
    //     }
    //     //System.out.print(u.name + " ");
    //     preorder(u.left);
    //     preorder(u.right);
    // }

    public void print_tour(int u){
        Node root = get_node(u);
       
        Bst.change_root(root);
        inorder(root);

        System.out.println("^^ This is the tour of the tree rooted at " + u);
    }

    //now the actual ET Tree functions -> link, split, is connected, size, get adjacent, update adjacent
}