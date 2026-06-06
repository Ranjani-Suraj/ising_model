package Dynamic_Graph;

import java.util.ArrayList;

public class Graph_ops {
//    int n;
//    ArrayList<ArrayList<Node>> ET_tour;
//    //the ET tour needs to store 3 things:
//    //need to store the name of the node, need to store the outgoing non-tree edges from that node
//    //need a bit to store if there is a node after this that has non-tree outgoing edges
//    //so every node needs to be [node, list_of_non_tree_neighbours, children?_bit]
//    ArrayList<Edge> edges;
//    Graph_ops(int n){
//        this.n = n;
//        ET_tour = new ArrayList<ArrayList<Node>>();
//        edges = new ArrayList<Edge>();
//
//    }
//
//    //what are we doing
//    //ok so there is a function to insert, delete, and replace. What do we need for that
//    //well, given a graph, we need to take the ET tour and store it
//    public void insert(String edge){
//
//        //wrong. now that ET_tour is an arraylist, need to split it into components.
//        //Ig it should be an arraylist of components?
//        //String[] connected_components = ET_tour.split(" ");
//        ArrayList<Node> T_u = new ArrayList<Node>(); //euler tree tour representation of the tree containing u
//        ArrayList<Node> T_v = new ArrayList<>();
//        boolean u_in = false, v_in = false;
//        int u_at = -1, v_at = -1;
//        //booleans for if v or u are in the component
//        //if one of them is and the other isnt,
//        // we merge. If neither is, we go to the nex  t component
//        for(int i = 0; i<ET_tour.size(); i++) {
//            ArrayList<Node> connected_component = ET_tour.get(i);
//            for (Node node : connected_component) {
//                if(node.getName() == edge.charAt(0)){
//                    u_in = true;
//                    T_u = connected_component;
//                    u_at = i;
//                }
//                if(node.getName() == edge.charAt(1)){
//                    v_in = true;
//                    T_v = connected_component;
//                    v_at = i;
//                }
//                if(u_in && v_in && (v_at == u_at)){
//                    //both are in same component so it can bea  non-tree edge
//                    Edge new_edge = new Edge(edge.charAt(0), edge.charAt(1), false);
//                    //new_edge.setTree_edge(false);
//                    edges.add(new_edge);
//                    return;
//                }
//                if(u_in && v_in && (v_at != u_at)){
//                    //now we need to find where the other one is fuck
//                    //ok both components are found, now we merge
//                    edges.add(new Edge(edge.charAt(0), edge.charAt(1), true));
//                    ET_tour.remove(T_u);
//                    ET_tour.remove(T_v);
//                    ET_tour.add(merge(T_u, T_v, edge));
//                    return;
//                }
//            }
//
//        }
//        //we merge the component with u in it and the component w v in it
//    }
//
//    ArrayList<Node> merge(ArrayList<Node> T_u, ArrayList<Node> T_v, String edge) {
//        char u = edge.charAt(0);
//        char v = edge.charAt(1);
//
//        // 1. Reroot T_v at v (in Euler tour terms, this means rotating the tour so v is first)
//        int v_index = -1;
//        for (int i = 0; i < T_v.size(); i++) {
//            if (T_v.get(i).getName() == v) {
//                v_index = i;
//                break;
//            }
//        }
//        if (v_index == -1) {
//            throw new RuntimeException("Node v not found in T_v!");
//        }
//
//        ArrayList<Node> rerooted_T_v = new ArrayList<>();
//        for (int i = v_index; i < T_v.size(); i++) rerooted_T_v.add(T_v.get(i));
//        for (int i = 0; i < v_index; i++) rerooted_T_v.add(T_v.get(i));
//
//        // 2. Merge as: T_u + (u,v) + rerooted_T_v + (v,u)
//        ArrayList<Node> merged = new ArrayList<>(T_u);
//        merged.add(new Node(u)); // simulate visiting u again before moving to v
//        merged.add(new Node(v)); // simulate entering v
//        merged.addAll(rerooted_T_v);
//        merged.add(new Node(v)); // simulate backtracking from v
//        merged.add(new Node(u)); // simulate backtracking to u
//
//        return merged;
//    }
//
//}
}
