package ETTree;

public class ETTNode {
    int vertex_id, size, height;
    int tree_id;
    boolean is_first;
    ETTNode left, right, parent;

    public ETTNode(int vertex_id, boolean is_first) {
        this.vertex_id = vertex_id;
        this.is_first = is_first;
        this.size = 1;
        this.height = 0;
        this.left = null;
        this.right = null;
        this.parent = null;
        this.tree_id = -1;
    }
    

    
}
