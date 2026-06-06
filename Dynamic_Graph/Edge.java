package Dynamic_Graph;

public class Edge {
    Node u, v;
    boolean tree_edge;

    int level;
    Edge(Node u1, Node v1, boolean tree_edge){
        u = u1;
        v = v1;
        this.tree_edge = tree_edge;
        this.level = 0;
    }
    public void setTree_edge(boolean tree_edge){
        this.tree_edge = tree_edge;
    }
    public void setLevel(int new_level){
        this.level = new_level;
    }
}
