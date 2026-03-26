
package ETTree;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class map_edge {
    final int u, v;

    public map_edge(int a, int b){
        if (a <= b) {
            this.u = a;
            this.v = b;
        } else {
            this.u = b;
            this.v = a;
        }
    }

    @Override
    public int hashCode() {
        return 31 * u + v;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof map_edge)) return false;
        map_edge other = (map_edge) obj;
        return this.u == other.u && this.v == other.v; //|| this.u == other.v && this.v == other.u;
    }

    @Override
    public String toString() {
        return "(" + u + "-" + v + ")";
    }
}

 
public class EulerTourTree {
    public ArrayList<TreapNode> forest;
    Map<map_edge, TreapNode[]> edges = new HashMap<>();  //edge to its treap nodes. So that when we cut, we know what to cut
    public Map<Integer, java.util.ArrayList<TreapNode>> nodes = new HashMap<>();  //node u to its treap nodes, where u->x, so that when adding we know where to connect

    // idea: We have some edges in our graph. They are not necessarily connected. 
    // We maintain a forest of Euler Tour Trees, one for each connected component.
    // Each Euler Tour Tree is represented as a Treap.
    // Each ETT is going to have a level

    public TreapNode getNode(int u, int v) {
        TreapNode[] edge_nodes = edges.get(new map_edge(u, v));
        if (edge_nodes == null) return null;
        if (edge_nodes[0].key.from == u && edge_nodes[0].key.to == v) {
            return edge_nodes[0];
        }
        return edge_nodes[1];
    }

    public EulerTourTree() {
        forest = new ArrayList<>();
        edges = new HashMap<>();
        nodes = new HashMap<>();
        // forest.add(new Treap()); 
    }

    public boolean connected(int u, int v) {
        // Check if two nodes are in the same tree
        if (nodes.get(u) == null || nodes.get(v) == null || nodes.get(u).isEmpty() || nodes.get(v).isEmpty()) 
            return false;
        // Somewhere, nodes is not being updated during cut. This is not a level issue, since it has been cut at multiple levels
        TreapNode ut = nodes.get(u).get(0);
        TreapNode vt = nodes.get(v).get(0);
        TreapNode rootU = Treap.root(ut);
        TreapNode rootV = Treap.root(vt);
        return rootU == rootV;
        //return Treap.root(ut) == Treap.root(vt);
    }

    public void link(int u, int v) {
        if (connected(u, v)) return;
        if (u == v) {
            if (nodes.containsKey(u) && nodes.get(u).isEmpty()) {
                // System.out.println("Node " + u + " already exists. Self loops are not allowed.");
                nodes.remove((Integer)(u));
            }
            return;
        } //no self loops
        nodes.putIfAbsent(u, new java.util.ArrayList<>());
        nodes.putIfAbsent(v, new java.util.ArrayList<>());
        if (edges.containsKey(new map_edge(u, v))) return; //edge already exists

        TreapNode rootU = (nodes.get(u).isEmpty())? null : (nodes.get(u).get(0)); //from u
        TreapNode rootV = (nodes.get(v).isEmpty())? null : (nodes.get(v).get(0)); //from v
        if (rootU != null && rootV != null) {
            //both u and v are already in the forest
            forest.remove(Treap.root(rootU));
            forest.remove(Treap.root(rootV));
            rootU = Treap.reroot(rootU); //reroot at some node of u before adding it to nodes
            rootV = Treap.reroot(rootV); //reroot at some node of v before adding it to nodes
            //if both exist, we put u->v at the end of the treap of rootU, and v->u at the end of the treap of rootV
        }
        else if (rootU != null) {
            forest.remove(Treap.root(rootU));
            rootU = Treap.reroot(rootU); //reroot at some node of u before adding it to nodes
        }
        else if (rootV != null){
            // u is not in the forest yet, but v is
            //swap u and v to reroot at v
            int temp = u;
            u = v;
            v = temp;
            forest.remove(Treap.root(rootV));
            rootV = Treap.reroot(rootV);
            
            TreapNode tempNode = rootU;
            rootU = rootV;
            rootV = tempNode;  
        }

        TreapNode e = new TreapNode(u, v);
        TreapNode f = new TreapNode(v, u);
        nodes.get(u).add(e);     
        edges.putIfAbsent(new map_edge(u, v), new TreapNode[]{e, f});
        nodes.get(v).add(f);
        rootU = Treap.merge(rootU, e); //add u->v to u's treap
        rootV = Treap.merge(rootV, f); //add v->u to v
        TreapNode root = Treap.merge(rootU, rootV); //merge the two treaps
        System.out.println(" adding edge " + u + "-" + v + "----------------------------");
        Treap.inorder(root);
        System.out.println("----------------------------");
        forest.add(Treap.root(root));
        // System.out.println("is_valid(): " + is_valid()); 
        boolean valid = is_valid();
        // System.out.println("is_valid(): " + valid); 
        if (!valid){
            System.out.println("Forest is not valid after cut. Printing forest:");
            printForest();
            System.out.println("Edges in the graph:");
            printEdges();
            System.out.println("-----------");
        }
        
    }

    void printEdges() {
        for (map_edge e : edges.keySet()) {
            System.out.print(e);

        }
        System.out.println();
    }

    public TreapNode[] peekCut(int u, int v) {  //need to return the result of a cut without actually cutting
        if (!connected(u, v)) 
            return null;
        map_edge e = new map_edge(u, v);
        TreapNode[] edgeNodes = edges.get(e);
        if (edgeNodes == null){
            e = new map_edge(v, u);
            edgeNodes = edges.get(e);
            if (edgeNodes == null) {
                return null;
            } 
        }
        int i = Treap.getIndex(edgeNodes[0]);
        int j =Treap.getIndex(edgeNodes[1]);
        if (i > j){
            int temp = i;
            i = j;
            j = temp;
        }
        //1 8 7 2 3 0 (removing 4-8) -> 0 7 2 3 8 1 5, 4 6
        TreapNode root = Treap.root(edgeNodes[0]);
        TreapNode[] split1 = Treap.peekSplit(root, i);
        TreapNode[] t2 = Treap.peekSplit(split1[1],1);
        TreapNode[] split2 = Treap.peekSplit(t2[1], j - i-1 );
        TreapNode[] split3 = Treap.peekSplit(split2[1], 1); 
        root = Treap.peekMerge(split1[0], split3[1]);
        TreapNode cutroot = split2[0];
        // System.out.println("--------------------");
        // Treap.inorder(root);
        // System.out.println("--------------------");
        // Treap.inorder(cutroot);
        // System.out.println("--------------------");
        return new TreapNode[]{root, cutroot};
    }

    
    public TreapNode[] cut(int u, int v) {
        if (!connected(u, v)) 
            return null;
        // if (!edges.containsKey(new map_edge(v, u))) {
        //     // System.out.println("Edge " + u + "-" + v + " does not exist.");
        // }
        //now to cut edge u-v
        TreapNode[] edgeNodes = edges.get(new map_edge(v, u));
        map_edge e = new map_edge(u, v);
         if (edgeNodes == null){
            e = new map_edge(v, u);
            edgeNodes = edges.get(e);
        }
        
        System.out.println("cutting edge " + u + "-" + v + "----------------------------");
        System.out.println("Edgenodes "+ edgeNodes);
        System.out.println("edges in the graph: ");
        printEdges();

        if (edgeNodes == null) {
            return null;
        } 
        
        TreapNode orig_root = Treap.root(edgeNodes[0]);
        forest.remove(orig_root);
        int i = Treap.getIndex(edgeNodes[0]);
        // TreapNode minnode = (Treap.getIndex(edgeNodes[0]) < Treap.getIndex(edgeNodes[1])) ? edgeNodes[0] : edgeNodes[1];
        // // System.out.println("minnode to reroot at: " + minnode.key);
        int j =Treap.getIndex(edgeNodes[1]);
        if (i > j){
            int temp = i;
            i = j;
            j = temp;
        }
        
        TreapNode root = Treap.root(edgeNodes[0]);
        TreapNode[] split1 = Treap.split(root, i);
        // System.out.println("splitting at index " + i + ":");
        // Treap.inorder(split1[0]);
        // System.out.println("-------");
        // Treap.inorder(split1[1]);
        // System.out.println("-------");
        TreapNode[] t2 = Treap.split(split1[1],1);
        TreapNode[] split2 = Treap.split(t2[1], j - i-1 );
        TreapNode[] split3 = Treap.split(split2[1], 1); 
        // System.out.println("splitting at index " + (j - i -1) + ":");
        // Treap.inorder(split2[0]);
        // System.out.println("-------");
        // Treap.inorder(split2[1]);
        // System.out.println("-------");
        root = Treap.merge(split1[0], split3[1]);
        TreapNode cutroot = split2[0];
        // // System.out.println("After cutting edge " + u + "-" + v + ":");
        // Treap.inorder(root);
        // // System.out.println("------- root after cut");
        // TreapNode cutroot = split2[0];
        // cutroot = Treap.reroot(cutroot);
        // // /cutroot = Treap.reroot(cutroot);
        // // System.out.println("im trying something");
        // Treap.inorder(cutroot);
        // // System.out.println("-------");
        // TreapNode edge = Treap.root(cutroot);
        // cutroot = Treap.delete_root_and_leaf(cutroot);
        // Treap.inorder(cutroot);
        // // System.out.println("-------");
        //cleaning up
        nodes.get(u).remove((TreapNode) new TreapNode(u, v));
        nodes.get(v).remove((TreapNode) new TreapNode(v, u));
        edges.remove(new map_edge(u, v));
        // System.out.println("edges remaining after cut:"+edges.size());
        // if cutroot is empty, then we have made some node an individual node. We can make that node into a treap of size 1 with a self loop
        //TreapNode cutroot = split2[0];
        if (cutroot != null && cutroot.size == 0 && cutroot.key.from == cutroot.key.to 
            && root != null && root.size == 0 && root.key.from == root.key.to){
                return null;
        }
        if (cutroot != null)
            forest.add(cutroot);
        if (cutroot == null){
            if (nodes.get(u).isEmpty()) {
                cutroot = new TreapNode(u, u); 
            }
            else if (nodes.get(v).isEmpty()) {
                cutroot = new TreapNode(v, v); 
            }
        }
        if (nodes.get(u).isEmpty()) {
            nodes.remove(u);
        }
        if (nodes.get(v).isEmpty()) {
            nodes.remove(v);
        }
       
        forest.add(root);
        
        System.out.println("Remaining forest after cut:");
        printForest();
        printEdges();
        boolean valid = is_valid();
        // System.out.println("is_valid(): " + valid); 
        if (!valid){
            System.out.println("Forest is not valid after cut. Printing forest:");
            printForest();
            System.out.println("Edges in the graph:");
            printEdges();
            System.out.println("-----------");
            return null;
        }
        // /printForest();
        // if((i == 0 && j == Treap.size(orig_root) - 1) && (cutroot == null || root == null)){ 
        //     root = (root == null) ? cutroot : root;
        //     //second will be whichever node is getting cut off
        //     int c = (nodes.get(u).isEmpty()) ? u : v; //since if u is empty, then u is getting cut off
        //     return new TreapNode[]{root, new TreapNode(c, c)};
        // }
        //else
        return new TreapNode[]{root, cutroot};
    }

    public void printInorder(TreapNode t) {
        if (t == null) return;
        printInorder(t.left);
        System.out.print(t.key + " ");
        printInorder(t.right);
    }

    public void printTree(int v) {
        TreapNode r = Treap.root(nodes.get(v).get(0));
        printInorder(r);
        System.out.println();
    }

    public void printForest() {
        System.out.println("------------");
        for (TreapNode t : forest) { 
            printInorder(t);
            System.out.println("------------");
        }
    }

    public boolean is_valid(){
        //it is  a valid ETT if for every edge u-v, both u->v and v->u are present in the same treap
        //Also, u-v must be followed by v-w in the inorder traversal for some w (or end)
        for (map_edge e : edges.keySet()) {
            TreapNode[] edgeNodes = edges.get(e);
            
            TreapNode root1 = Treap.root(edgeNodes[0]);
            TreapNode root2 = Treap.root(edgeNodes[1]);
            if (root1 != root2) {
                // System.out.println("Edge " + e + " nodes are in different treaps.");
                // System.out.println("Root of " + edgeNodes[0].key + ": " + root1.key);
                // printInorder(root1);
                // System.out.println("Root of " + edgeNodes[1].key + ": " + root2.key);
                // printInorder(root2);
                return false;
            }
            if (e.u == e.v) {
                // System.out.println("Edge " + e + " is not represented correctly in the treap.");
                // System.out.println("Expected: " + e.u + "->" + e.v);
                // System.out.println("Found: " + edgeNodes[0].key);
                return false;
            }
            //check inorder property
            ArrayList<TreapNode> inorderList = new ArrayList<>();
            Treap.inorderToList(root1, inorderList);
            for (int i = 0; i < inorderList.size() - 1; i++) {
                TreapNode curr = inorderList.get(i);
                TreapNode next = inorderList.get(i + 1);
                if (curr.key.from == e.u && curr.key.to == e.v) {
                    if (!(next.key.from == e.v)) {
                        System.out.println("Inorder property violated at edge " + e);
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
}
