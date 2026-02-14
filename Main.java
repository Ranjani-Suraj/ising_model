import DynGraph.ConnGraph;
import ETTree.Treap;
import ETTree.TreapNode;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

class map_edge {
    int u, v;
    public map_edge(int u, int v){
        this.u = u;
        this.v = v;
    }
    @Override
    public int hashCode() {
        return Integer.hashCode(u) * 31 + Integer.hashCode(v);
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        map_edge other = (map_edge) obj;
        return this.u == other.u && this.v == other.v;
    }
}


public class Main {
    // Driver Program to test above functions
    public static void main(String[] args)
    {
        TreapNode.random = new Random(42);
        from_ett();
        System.out.println("****************************");
    }

    static void from_ett(){
        ConnGraph g = new ConnGraph();
        g.addEdge(1, 2);
        g.addEdge(2, 3);
        g.addEdge(3, 4);
        g.addEdge(4, 5);
        // g.addEdge(1, 4);
        System.out.println("After linking edges (1,2), (2,3), (3,4), (4,5):");
        g.printForest();
        System.out.println("----------------------------");

        g.deleteEdge(2, 3);
        System.out.println("After cutting edge (2,3):");
        g.printForest();
        System.out.println("----------------------------");

        g.addEdge(1, 3);
        System.out.println("After linking edge (1,3):");
        g.printForest();
        System.out.println("----------------------------");
        g.addEdge(2, 3);
        System.out.println("After linking edge (2,3):");
        g.printForest();
        g.deleteEdge(1, 2);
        System.out.println("After cutting edge (2,1):");
        g.printForest();
    }

    void from_treap(){
        Random rand = new Random(42);
        Map<map_edge, TreapNode[]> edges = new HashMap<>();  //edge to its treap nodes
        Map<Integer, java.util.ArrayList<TreapNode>> nodes = new HashMap<>();  //node u to its treap nodes, where u->x
        Treap t = new Treap();
        TreapNode root12 = null;
        TreapNode root23 = null;
        TreapNode root = null;
        TreapNode a = new TreapNode(1, 2);
        nodes.putIfAbsent(1, new java.util.ArrayList<>());
        nodes.get(1).add(a);
        nodes.putIfAbsent(2, new java.util.ArrayList<>());
        TreapNode b = new TreapNode(2, 1);
        nodes.get(2).add(b);
        edges.put(new map_edge(1, 2), new TreapNode[]{a, b});
        root12 = Treap.merge(a, b);
        Treap.reroot(b);

        //now to add edge 2-3
        TreapNode c = new TreapNode(2, 3);
        TreapNode d = new TreapNode(3, 2);
        nodes.get(2).add(c);    
        nodes.putIfAbsent(3, new java.util.ArrayList<>());
        edges.put(new map_edge(3, 2), new TreapNode[]{c, d});
        nodes.get(3).add(d);
        root23 = Treap.merge(c, d);
        root = Treap.merge(root12, root23);
        Treap.inorder(root);
        System.out.println("----------------------------");

        //now to add edge 3-4
        TreapNode e = new TreapNode(3, 4);
        TreapNode f = new TreapNode(4, 3);
        nodes.get(3).add(e);    
        nodes.putIfAbsent(4, new java.util.ArrayList<>());
        edges.put(new map_edge(4, 3), new TreapNode[]{e, f});
        nodes.get(4).add(f);
        //search through nodes, take some node, reroot. Reroot the same in the other treap, merge. 
        Treap.reroot(nodes.get(3).get(0)); //reroot at some node of 3
        Treap.reroot(e); //reroot at some node of 3
        TreapNode root34 = Treap.merge(e, f);
        root = Treap.merge(root, root34);
        Treap.inorder(root);
        System.out.println("----------------------------");
        
        //now to cut edge 1-2
        TreapNode[] edgeNodes = edges.get(new map_edge(1, 2));
        int i = Math.min(Treap.getIndex(edgeNodes[0]), Treap.getIndex(edgeNodes[1]));
        int j = Math.max(Treap.getIndex(edgeNodes[0]), Treap.getIndex(edgeNodes[1]));
        TreapNode[] split1 = Treap.split(root, i);
        Treap.inorder(split1[1]);
        // System.out.println("-------");
        /// Treap.inorder(split1[0]);
        // System.out.println("-------");
        TreapNode[] split2 = Treap.split(split1[1], j - i + 1);
        // Treap.inorder(split2[1]);
        // System.out.println("-------");
        // Treap.inorder(split2[0]);
        // System.out.println("-------");
        root = Treap.merge(split1[0], split2[1]);
        System.out.println("After cutting edge 1-2:");
        Treap.inorder(root);
        TreapNode cutroot = split2[0];
        System.out.println("-------");
        cutroot = Treap.delete_root_and_leaf(cutroot);
        Treap.inorder(cutroot);
        System.out.println("-------");
    }

}
