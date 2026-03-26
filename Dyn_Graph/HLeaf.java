package Dyn_Graph;
import java.util.ArrayList;
import java.util.HashSet;

enum EndpointType {
    WITNESS, PRIMARY, SECONDARY
}

public class HLeaf {
    public boolean[][] isEndpoint;
    public HNode node;
    //These will return neighbours. Use the HForest edgeMap to get the corresponding HNode for each neighbour, and then get the neighbour vertex from the vertexToLeaf map
    // order of types is W P S
    public HashSet<Integer> witness_edges[]; //witness_edges.get(i) = set of neighbours this vertex has at depth i
    public HashSet<Integer> primary_edges[]; // primary_edges.get(i) = set of neighbours this vertex has at depth i that are primary edges
    public HashSet<Integer> secondary_edges[]; // secondary_edges.get(i) = set of neighbours this vertex has at depth i that are secondary edges
    public int depth; // depth of the leaf in the H-tree, which is the same as the depth of its corresponding H-node
    public int vertex; // the vertex that this leaf corresponds to
    public HLeaf(int vertex, int dMax, HNode node) {
        this.node = node;
        this.vertex = vertex;
        isEndpoint = new boolean[3][dMax];
        witness_edges = new HashSet[dMax];
        for (int i = 0; i < dMax; i++) {
            witness_edges[i] = new HashSet<Integer>();
        }
        primary_edges = new HashSet[dMax];
        for (int i = 0; i < dMax; i++) {
            primary_edges[i] = new HashSet<Integer>();
        }
        secondary_edges = new HashSet[dMax];
        for (int i = 0; i < dMax; i++) {
            secondary_edges[i] = new HashSet<Integer>();
        }
        depth = dMax;
    }

    public void add_edge_info(int v, int depth, EndpointType type){
        switch (type) {
            case WITNESS -> {
                // WITNESS
                witness_edges[depth].add(v);
                isEndpoint[0][depth] = true;
            }
            case PRIMARY -> {
                // PRIMARY
                primary_edges[depth].add(v);
                isEndpoint[1][depth] = true;
            }
            case SECONDARY -> {
                // SECONDARY
                secondary_edges[depth].add(v);
                isEndpoint[2][depth] = true;
            }
        }
    }
    public void add_secondary_edge(int v, int depth){
        secondary_edges[depth].add(v); 
        isEndpoint[2][depth] = true;
    }
    public void add_witness_edge(int v, int depth){
        witness_edges[depth].add(v); 
        isEndpoint[0][depth] = true;
    }
    public void remove_primary_edge(int v, int depth){
        primary_edges[depth].remove(v); 
        if (primary_edges[depth].isEmpty()){
            isEndpoint[1][depth] = false;
        }
    }
    public void remove_secondary_edge(int v, int depth){
        secondary_edges[depth].remove(v); 
        if (secondary_edges[depth].isEmpty()){
            isEndpoint[2][depth] = false;
        }
    }
    public void remove_witness_edge(int v, int depth){
        witness_edges[depth].remove(v); 
        if (witness_edges[depth].isEmpty()){
            isEndpoint[0][depth] = false;
        }
    }
    public void remove_edge(int v, int depth, EndpointType type){
        switch (type) {
            case WITNESS -> remove_witness_edge(v, depth);
            case PRIMARY -> remove_primary_edge(v, depth);
            case SECONDARY -> remove_secondary_edge(v, depth);
        }
    }
    public void promote_primary_edge(int v, int depth){
        remove_primary_edge(v, depth);
        add_edge_info(v, depth + 1, EndpointType.PRIMARY);
    }


    public boolean[][] recomputeBitmap(){
        //recompute the isEndpoint bitmap for this leaf based on the current edges it has
        //this should only be called when we are adding or removing an edge from this leaf, and we need to update the isEndpoint bitmap accordingly.
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < depth; j++) {
                this.isEndpoint[i][j] = false;
            }
        }
        for (int i = 0; i < depth; i++) {
            if (!witness_edges[i].isEmpty()){
                isEndpoint[0][i] = true;
            }
            if (!primary_edges[i].isEmpty()){
                isEndpoint[1][i] = true;
            }
            if (!secondary_edges[i].isEmpty()){
                isEndpoint[2][i] = true;
            }
        }
        return this.isEndpoint;
    }

    public ArrayList<EdgeRecord> get(int i, EndpointType t){
            //get the edges of type t at depth i that this leaf is an endpoint of
            ArrayList<EdgeRecord> edges = new ArrayList<>();
            HashSet<Integer> neighbours;
            switch (t) {
                case EndpointType.WITNESS -> neighbours = witness_edges[i];
                case EndpointType.PRIMARY -> neighbours = primary_edges[i];
                case EndpointType.SECONDARY -> neighbours = secondary_edges[i];
                default -> throw new IllegalStateException("Unexpected value: " + t);
            }
            for (int v : neighbours){
                EdgeRecord edge = new EdgeRecord(v, i, t);
                edges.add(edge);
            }
            return edges;
    }
}