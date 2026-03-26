import java.util.*;
// import random;

import DynGraph.*;

public class test_dyn {

    ConnGraph g1 = new ConnGraph();
    Map<Integer, Set<Integer>> graph = new HashMap<>();
    int n;
    int cycles;
    
    // ArrayList<ConnVertex> dyn_vertices;
    public test_dyn(int size, int runs){
        n = size; cycles = runs;
        // dyn_vertices = new ArrayList<>(n);
    }

    
    Set<Integer> explore(int start){
        Set<Integer> visited = new HashSet<>();
        ArrayList<Integer> stack = new ArrayList<>();
        stack.add(start);
        int v;
        while(stack.isEmpty() == false){
            v = stack.remove(stack.size()-1);
            for(int u : graph.get(v)){
                if(!visited.contains(u)){
                    visited.add(u);
                    stack.add(u);
                }
                if(visited.size() == n){
                    stack.clear();
                    break;
                }
            }
        }

        return visited;
    }


    boolean isCutEdge(int u, int v){
        //check if edge u-v is a cut edge by checking if there is another path from u to v
        //we can delete edge u-v, and then check connectivity. If u and v are still connected, then u-v is not a cut edge. If they are not connected, then u-v is a cut edge.
        if (!connected(u, v)) { //if u and v are not connected or if u-v is not an edge, then u-v cannot be a cut edge
            return true;
        }
        //if this is reached tehn u and v are connected
        if (!hasEdge(u, v)) {
            return false;
        }
        //if this is reached then u and v are connected and uv is an edge
        //delete edge u-v
        graph.get(u).remove(v);
        graph.get(v).remove(u);
        boolean connected = connected(u, v);
        //restore edge u-v
        graph.get(u).add(v);
        graph.get(v).add(u);
        System.out.println("Edge "+u+"-"+v+" is a cut edge: "+!connected);
        System.out.println(graph);
        return !connected;
    }

    public boolean hasEdge(int u, int v){
        return graph.get(u).contains(v);
    }

    public void dfs(int start){
        ArrayList<Integer> stack = new ArrayList<>();
        stack.add(start);
        Set<Integer> visited = new HashSet<>();
        visited.add(start);
        ArrayList<Integer> remaining_unvisited = new ArrayList<>();
        for(int i = 0; i < n; i++){
            if (i!=start && !graph.get(start).contains(i)){
                remaining_unvisited.add(i);
            }
            else {
                visited.add(i);
            }
        }
        while(remaining_unvisited.isEmpty() == false){
            visited.addAll(explore(remaining_unvisited.get(remaining_unvisited.size())));
            remaining_unvisited.clear();
            for(int i = 0; i < n; i++){
                if (!visited.contains(i)){
                    remaining_unvisited.add(i);
                }
            }
        }
    }

    // void generate_dyn_graph(){
    //     for(int i = 0; i<n; i++){
    //         dyn_vertices.add(new ConnVertex());
    //     }
    // }
    void generate_reg_graph(){
        for(int i = 0; i<n; i++){
            graph.put(i, new HashSet<>());
        }
        // for(int i = 0; i<n; i++){
        //     for(int j = 0; j<n; j++){
        //         if(i!=j){
        //             graph.get(i).add(j);
        //             graph.get(j).add(i);
        //         }
        //     }
        // }
    }

    boolean connected(int u, int v){
        Set<Integer> cc_u = explore(u);
        return cc_u.contains(v);
    }

    public boolean test(){
        Random random = new Random(42);
        // generate_dyn_graph();
        generate_reg_graph();
        double add_time = 0.0, del_time = 0.0, conn_time = 0.0;
        double avg_add = 0.0, avg_del = 0.0, avg_conn = 0.0;
        int num_add = 0, num_del = 0, num_conn = 0;
        double start, end;
        for(int i = 0; i<cycles; i++){
            add_time = 0.0; del_time = 0.0; conn_time = 0.0;
            int[] edge = {random.nextInt(n-1), random.nextInt(n-1)};
            if(edge[0] == edge[1]){
                //i-=1;
                continue;
            }
            if (edge[0] < edge[1]){
                int temp = edge[0];
                edge[0] = edge[1];
                edge[1] = temp;
            }
            
            boolean add = random.nextBoolean();
            if(add == true){
                System.out.println("Testing adding edge "+ edge[0]+"-"+edge[1]);
                if(!graph.get(edge[0]).contains(edge[1])){
                    graph.get(edge[0]).add(edge[1]);
                    graph.get(edge[1]).add(edge[0]);
                }
                start = System.nanoTime();
                g1.addEdge( edge[0], edge[1]);
                add_time = (System.nanoTime() - start);
                num_add += 1;
            }
            else{
                System.out.println("Testing deleting edge "+ edge[0]+"-"+edge[1]);
                if(graph.get(edge[0]).contains(edge[1])){
                    graph.get(edge[0]).remove(edge[1]);
                    graph.get(edge[1]).remove(edge[0]);
                }
                start = System.nanoTime();
                g1.deleteEdge(edge[0], edge[1]);
                del_time = (System.nanoTime() - start);
                num_del += 1;
            }
            int[] edge2 = {random.nextInt(n-1), random.nextInt(n-1)};
            if(edge2[0] == edge2[1]){
                //i-=1;
                continue;
            }
            if (edge2[0] < edge2[1]){
                int temp = edge2[0];
                edge2[0] = edge2[1];
                edge2[1] = temp;
            }
            System.out.println("Querying connectivity of edge "+ edge2[0]+"-"+edge2[1]);
            boolean vanilla_conn = connected(edge2[0], edge2[1]);
            start = System.nanoTime();
            boolean dyn_conn =  g1.connected(edge2[0], edge2[1]);
            conn_time = (System.nanoTime() - start);
            num_conn += 1;
            boolean res = (vanilla_conn && dyn_conn) || (!vanilla_conn && !dyn_conn);
            // System.out.println("huh"+res);
            if(res != true){
                System.out.println(i);
                System.out.println(vanilla_conn+" "+ dyn_conn);
                System.out.println("dyn graph: ");
                //g1.print_edges();
                g1.forest.get(0).printForest();
                g1.print_edges();
                System.out.println("graph: "+graph);
                g1.printForest();
                g1.connected(edge2[0], edge2[1]);
                
                return false;
            }

            int[] edge3 = {random.nextInt(n-1), random.nextInt(n-1)};
            if(edge3[0] == edge3[1]){
                //i-=1;
                continue;
            }
            if (edge3[0] < edge3[1]){
                int temp = edge3[0];
                edge3[0] = edge3[1];
                edge3[1] = temp;
            }
            boolean vanilla_has = graph.get(edge3[0]).contains(edge3[1]), dyn_has =  g1.hasEdge(edge3[0], edge3[1]);
           
            boolean res2 = (vanilla_has && dyn_has) || !(vanilla_has || dyn_has);
            // System.out.println("huh"+res);
            if(res2 != true){
                System.out.println(i);
                System.out.println("has "+vanilla_has+" "+ dyn_has);
                System.out.println("edge "+ edge3[0]+"-"+edge3[1]);
                System.out.println("dyn graph: ");
                g1.print_edges();
                g1.forest.get(0).printForest();
                System.out.println("graph: "+graph);
                return false;
            }
            int[] edge4 = {random.nextInt(n-1), random.nextInt(n-1)};
            if(edge4[0] == edge4[1]){
                i-=1;
                continue;
            }
            System.out.println("Testing if an edge is a cut edge or not "+ edge4[0]+"-"+edge4[1]);
            // boolean vanilla_cut = isCutEdge(edge4[0], edge4[1]);
            // boolean dyn_cut = g1.isCutEdge(edge4[0], edge4[1]);
            // boolean res3 = (vanilla_cut && dyn_cut) || (!vanilla_cut && !dyn_cut);
            // if(res3 != true){
            //     System.out.println(i);
            //     System.out.println("cut edge vanilla"+vanilla_cut+" dyn"+ dyn_cut);
            //     System.out.println("edge "+ edge4[0]+"-"+edge4[1]);
            //     System.out.println("graph: "+graph);
            //     System.out.println("dyn graph: ");
            //     g1.print_edges();
            //     g1.isCutEdge(edge4[0], edge4[1]);
            //     return false;
            // }

            // System.out.println("dyn graph: ");
            //     g1.print_edges();
            //     System.out.println("graph: "+graph);
            //System.out.println("add time: "+add_time+" del time: "+del_time+" conn time: "+conn_time);
            avg_add = (add_time !=0)? avg_add + add_time : avg_add; 
            avg_del = (del_time !=0)? avg_del + del_time : avg_del;
            avg_conn = (conn_time !=0)? avg_conn + conn_time : avg_conn;
            //averages=> add time: 4881.438093314482 del time: 1128.5742652899125 conn time: 1461.3527054108217 ???
        }
        System.out.println("add time: "+avg_add/num_add+" del time: "+avg_del/num_del+" conn time: "+avg_conn/num_conn);

        return true;
    }
}
