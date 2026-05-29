package Cftp; 

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import DynGraph.ConnGraph;
import DynGraph.HForest;
import VanillaGraph.Dfs;
//import dyn_connectivity::*;

//stress test for all values of n, p, 1<=q<=2
//check why the time behaviour is weird
//test without time checks

//


public class CouplingPast {
    //ConnGraph g1, g2;
    HForest g1, g2;
    Dfs vg1, vg2;
    int n;
    int epochs;
    int original_epochs;
    int num_cycles = 1;
    double p, q, pi;
    int edges1 = 0, edges2 =0; //number of edges in g1 and g2 respectively
    int iterations;
    ArrayList<Integer> seeds;
    double add_time = 0.0;
    double del_time = 0.0;
    double conn_time = 0.0;
    //double addtime = 0.0, deltime, conntime;
    int num_add = 0, num_del = 0, num_conn = 0;
    boolean vanilla; 
    Random rand = new Random(42);
    // ArrayList<ConnVertex> vertices = new ArrayList<ConnVertex>();
    public CouplingPast(int epochs, int n, double p, double q, boolean warm_start, boolean vanilla){ 
        this.epochs = epochs;
        this.original_epochs = epochs;
        this.n = n;
        this.g1 = new HForest(n);
        this.g2 = new HForest(n);
        this.vg1 = new Dfs(n);
        this.vg2 = new Dfs(n);
        this.p = p;
        this.q = q;
        this.pi = p/(p+q*(1-p));
        this.vanilla = vanilla;
        Random r = new Random(rand.nextInt()); 
        seeds = new ArrayList<Integer>();
        //make g1 a complete graph
        //make g2 an empty graph
        if(vanilla){
            System.out.println("Running vanilla coupling from the past...");
            for (int i = 0; i<n; i++){
                // vertices.add(new ConnVertex());
                vg1.add_vertex(i);
                vg2.add_vertex(i);
                //System.out.println("Added vertex " + i + "-> " + vertices.get(i));
                
            }
        }
        ////System.out.println("NODES ADDED");
        for(int i = 0; i<n; i++){
            for(int j = i+1; j<n; j++){
                ////System.out.println("ADD EDGE: "+j);
                double ch = r.nextDouble();
                if (warm_start){
                    if(ch < p){
                            //g1.addEdge(i, j);
                            if(!vanilla)
                                g1.add_edge(i, j);
                            else
                                vg1.addEdge(i, j);
                            // g1.addEdge(vertices.get(j), vertices.get(i));
                            edges1+=1;

                            
                    }
                    if(ch < pi){
                            //g2.addEdge(i, j);
                            if(!vanilla)
                                g2.add_edge(i, j);
                            else
                                vg2.addEdge(i, j);
                            //g2.addEdge(j, i);
                            edges2+=1;
                            
                    }
                }
                else{
                    //g1.addEdge(i, j);
                    if(!vanilla){
                        g1.add_edge(i, j);
                    }
                    else{
                        vg1.addEdge(i, j);
                    }
                    // g1.addEdge(vertices.get(j), vertices.get(i));
                    edges1+=1;
                }
                    //add_time += (System.nanoTime() - start);
            }
        }

        // List<int[]> initEdges = new ArrayList<>();
        // for (int i = 0; i < n; i++)
        //     for (int j = i+1; j < n; j++) {
        //         double ch = r.nextDouble();
        //         if (warm_start ? ch < this.p : true){
        //             edges1+=1;
        //             initEdges.add(new int[]{i, j});}
        //     }
        // this.g1 = HForest.buildFromEdgeList(n, initEdges);

        // List<int[]> initEdges2 = new ArrayList<>();
        // // rebuild with pi threshold for g2 if warm_start, else same complete graph
        //  for (int i = 0; i < n; i++)
        //     for (int j = i+1; j < n; j++) {
        //         double ch = r.nextDouble();
        //         if (warm_start ? ch < this.pi : false){
        //             initEdges2.add(new int[]{i, j});
        //             edges2+=1;
        //         }
        //     }
        // this.g2 = HForest.buildFromEdgeList(n, initEdges2);


        //edges1*=0.8; //each edge was added twice

        System.out.println("Initial edges in g1: "+edges1);
        System.out.println("Initial edges in g2: "+edges2);
        //edges1 = n * (n - 1) / 2; //complete graph has n(n-1)/2 edges
        //edges2 = 0; //empty graph has 0 edges
        iterations = 0;
    }

    ArrayList<ArrayList<Integer>> generate_edges(){
        Random random = new Random();
        ArrayList<ArrayList<Integer>> edges = new ArrayList<>();
        for(int i = 0; i<epochs; i++){
            ArrayList<Integer> edge = new ArrayList<Integer>();
            edge.add((int)(random.nextInt(n))); edge.add((int)(random.nextInt(n)));
            if(edge.get(0) == edge.get(1)){
                i--;
                continue;
            }
            edges.add(edge);
            
        }
        return edges;
    }

    ArrayList<Double> generate_r(){
        Random random = new Random();
        ArrayList<Double> probabilities = new ArrayList<>();
        for(int i = 0; i<epochs; i++){
            probabilities.add(random.nextDouble());
        }
        return probabilities;
    }

    public boolean run_epochs(ArrayList<ArrayList<Integer>> edges, ArrayList<Double> r_s){
        //System.out.println("running epochs tada---------------------------------------------");
        double start = 0;
        int seed = seeds.get(0);
        int t = 0, i=0;
        //int[] edges_used = new int[epochs];
        //double[] r_used = new double[epochs];
        
        int sub_epoch_size = num_cycles > 2?original_epochs*(int)Math.pow(2, num_cycles-2): original_epochs;
        int base = sub_epoch_size;
        int seed_index = 0;
        //if r<pi and p then ig we def add for both, if r>p and pi then we def remove for both? i guess?
        Random random = new Random(seed);
        double time_for_g1 = 0;
        double time_for_g2 = 0;
        double time_to_get_cut_edges_g1 = 0;
        double time_to_get_cut_edges_g2 = 0;
        //System.out.println("edges: "+edges+" rs"+r_s);
        for (i = 0; i<epochs; i++){
            //need to store the seeds and access them as needed
            //seed 0 will be used for original epoch size. seed 1 will be for orig epoch * 2, etc.
            //double r = r_s.get(i);
            if(i >= sub_epoch_size){
                seed_index++;
                if(seed_index >= seeds.size()){
                    seeds.add((int)(random.nextInt()));
                }
                seed = seeds.get(seed_index);
                sub_epoch_size += original_epochs*(int)(base/2);
                base/=2;
                random = new Random(seed);
            }
            
            double r = random.nextDouble();
            if(edges1 == edges2){
                //coupling over
                //System.out.println("Coupling successful after " + t + " more epochs.");
                this.iterations += t;
                return true;
            }
            
            //ArrayList<Integer> _edge = edges.get(i);
            int[] edge = {random.nextInt(n), random.nextInt(n)};
            //edge[0] = _edge.get(0); edge[1] = _edge.get(1);
            
            if(edge[0] == edge[1]){
                i--;
                continue; 
            }
            //r_used[i] = r;
            t++;
            int u = (edge[0]);
            int v = (edge[1]);
            //need to check if edge is a cut edge. This means we remove edge, if it is still connected, then it is not.
            
            //if it is not a tree edge and is in the graph, then it is def not a cut edge
            boolean replace1 = g1.hasEdge(u, v), replace2 = g2.hasEdge(u, v);
            //do i need to actually delete and replace it. Maybe i can just see if i find a replacement?
            //code to find a replacement exists, maybe I dublicate it to see if it returns soemthing?
            boolean cut_edge1 = false, cut_edge2 = false;
            // boolean cutedge1 = g1.isCutEdge(u, v);
            // System.out.println("-----------"+g1.getEdgeType(u, v)+" "+g2.getEdgeType(u, v));
            
            // cut_edge1 = g1.isCutEdge(u, v);
            // cut_edge2 = g2.isCutEdge(u, v);
            double start_time = System.currentTimeMillis();
            if(g1.isTreeEdge(u, v)){
                //System.out.println("edge is a tree edge in g1");
                if(replace1){
                    //System.out.println("edge is in g1");
                    //start = System.nanoTime();
                    g1.delete_edge(u, v);
                    //del_time += (System.nanoTime() - start);
                    num_del += 1;
                }
                
                
                //if they are not connected, then removing the edge
                //splits the cc so it IS a cut edge
                //start = System.nanoTime();
                boolean connected = g1.connected(u, v);
                //conn_time += (System.nanoTime() - start);
                num_conn += 1;
                // if(!connected){
                //     //edge is not a cut edge, so we can add it to g2
                //     ////System.out.println("edge is a cut edge for g1");
                //     cut_edge1 = true;
                // }
                // else{
                //     ////System.out.println("edge is not a cut edge for g1");
                //     cut_edge1 = false;
                // }
                cut_edge1 = !connected;
                if(replace1){
                    //System.out.println("adding edge back to g1 "+edge[0]  + " " + edge[1]);
                    //start = System.nanoTime();
                    g1.add_edge(u, v); //add it back
                    //add_time += (System.nanoTime() - start);
                    num_add += 1;
                }
                // System.out.println("1) nodes u: "+edge[0]+" and v: "+edge[1]+" are "+connected+" "+g1.getEdgeType(u, v));
            }
            else if (replace1){ //if they are connected but its not a tree edge, then it cannot be a cut edge
                cut_edge1 = false;
                // System.out.println("2) edge is not a tree edge in g1 but it is IN g1 so it cannot be a cut edge"+g1.getEdgeType(u, v));
            }
            else{ //if they are not connected, it is def a cut edge
                //it is not in the graph, but we need to check if they are alr connected
                //start = System.nanoTime();
                boolean connected = g1.connected(u, v);
                //conn_time += (System.nanoTime() - start);
                num_conn += 1;
                // System.out.println("3) nodes u: "+edge[0]+" and v: "+edge[1]+" are "+connected+" "+g1.getEdgeType(u, v));
                cut_edge1 = !connected;
            }
            time_to_get_cut_edges_g1 += (System.currentTimeMillis() - start_time);
            // if(cut_edge1 != cutedge1){
            //     System.out.println("ERROR: cut edge detection mismatch for g1 on edge "+u+" "+v);
            // }

            //boolean cutedge2 = g2.isCutEdge(u, v);
            start_time = System.currentTimeMillis();
            if(g2.isTreeEdge(u, v)){
                //System.out.println("edge is a tree edge in g1");
                if(replace2){
                    //System.out.println("edge is in g1");
                    //start = System.nanoTime();
                    g2.delete_edge(u, v);
                    //del_time += (System.nanoTime() - start);
                    num_del += 1;                
                }
                
                
                //if they are not connected, then removing the edge
                //splits the cc so it IS a cut edge
                //start = System.nanoTime();
                boolean connected = g2.connected(u, v);
                //conn_time += (System.nanoTime() - start);
                num_conn += 1;
                if(!connected){
                    //edge is not a cut edge, so we can add it to g2
                    ////System.out.println("edge is a cut edge for g1");
                    cut_edge2 = true;
                }
                else{
                    ////System.out.println("edge is not a cut edge for g1");
                    cut_edge2 = false;
                }

                if(replace2){
                    //System.out.println("adding edge back to g1 "+edge[0]  + " " + edge[1]);
                    //start = System.nanoTime();
                    g2.add_edge(u, v); //add it back
                    //add_time += (System.nanoTime() - start);
                    num_add += 1;
                }
            }
            else if (replace2){
                cut_edge2 = false;
                //System.out.println("edge is not a tree edge in g1 but it is IN g1 so it cannot be a cut edge");
            }
            else{
                //it is not in the graph, but we need to check if they are alr connected
                //start = System.nanoTime();
                boolean connected = g2.connected(u, v);
                //conn_time += (System.nanoTime() - start);
                num_conn += 1;
                //System.out.println("nodes u: "+edge[0]+" and v: "+edge[1]+" are "+connected);
                cut_edge2 = !connected;
            }
            time_to_get_cut_edges_g2 += (System.currentTimeMillis() - start_time);
            // if(cut_edge2 != cutedge2){
            //     System.out.println("ERROR: cut edge detection mismatch for g2 on edge "+u+" "+v); 
            // }
            ////System.out.println("starting to add/delete!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!11");
            //need to do the markov stuff lol i forgor
            //need to keep track of number of edges whoops
            start_time = System.currentTimeMillis();
            if(cut_edge1){
                //System.out.println("cut edge for g1 "+edge[0]  + " " + edge[1]+ ", r = " + r + " pi = " + pi);
                if(r <= pi){ //we add it if it is a cut edge w probability pi
                    if(!replace1){
                        edges1++;
                        //start = System.nanoTime();
                        g1.add_edge(u, v); 
                        //add_time += (System.nanoTime() - start);
                        num_add += 1;
                        //System.out.println("added edge to g1 "+edge[0]  + " " + edge[1]+ "------------------------152");
                    }
                    //dont need to mess with teh cc stuff since thats all handled
                }
                else{
                    //remove it 
                    if(replace1){
                        //start = System.nanoTime();
                        g1.delete_edge(u, v);
                        //del_time += (System.nanoTime() - start);
                        num_del += 1;
                        edges1--;
                        //System.out.println("deleted edge to g1 "+edge[0]  + " " + edge[1]+ "------------------------161");
                    }
                }
            }
            else if(r <= p){ //we still add it if its not a cut edge w prob p
                ////System.out.println("not cut edge for g1 "+edge[0]  + " " + edge[1]+ ", r = " + r + " p = " + p);
                if(!replace1){
                    edges1++;
                    //start = System.nanoTime();
                    g1.add_edge(u, v);
                    //add_time += (System.nanoTime() - start);
                    num_add += 1;
                    //System.out.println("added edge to g1 "+edge[0]  + " " + edge[1]+ "------------------------169");
                }
            }
            else{ //we remove it
                ////System.out.println("not cut edge for g1 "+edge[0]  + " " + edge[1]+ ", r = " + r + " p = " + p);
                if(replace1){
                    //start = System.nanoTime();
                    g1.delete_edge(u, v);
                    //del_time += (System.nanoTime() - start);
                    num_del += 1;
                    edges1--;
                    //System.out.println("deleted edge to g1 "+edge[0]  + " " + edge[1]+ "------------------------176");
                }
            }
            time_for_g1 += (System.currentTimeMillis() - start_time);
            start_time = System.currentTimeMillis();
            if(cut_edge2){
                //System.out.println("cut edge for g2 "+edge[0]  + " " + edge[1]+ ", r = " + r + " pi = " + pi);

                if(r <= pi){ //we add it if it is a cut edge w probability pi
                    if(!replace2){
                        edges2++;
                        //start = System.nanoTime();
                        g2.add_edge(u, v); 
                        num_add += 1;
                        //add_time += (System.nanoTime() - start);
                        //System.out.println("added edge to g2 "+edge[0]  + " " + edge[1]+ "------------------------185");
                    }
                    //else //System.out.println("already in g2 "+edge[0]  + " " + edge[1]+ "------------------------187");
                    //dont need to mess with teh cc stuff since thats all handled
                }
                else{
                    //remove it 
                    if(replace2){
                        //start = System.nanoTime();
                        g2.delete_edge(u, v);
                        //del_time += (System.nanoTime() - start);
                        num_del += 1;
                        edges2--;
                        //System.out.println("deleted edge to g2 "+edge[0]  + " " + edge[1]+ "------------------------194");

                    }
                }
            }
            else if(r <= p){ //we still add it if its not a cut edge w prob p
                if(!replace2){
                    edges2++;
                    //start = System.nanoTime();
                    g2.add_edge(u, v);
                    //add_time += (System.nanoTime() - start);
                    num_add += 1;
                    //System.out.println("added edge to g2 "+edge[0]  + " " + edge[1] + "------------------------203");

                }
            }
            else{ //we remove it
                if(replace2){
                    //start = System.nanoTime();
                    g2.delete_edge(u, v);
                    //del_time += (System.nanoTime() - start);
                    num_del += 1;
                    edges2--;
                    //System.out.println("deleted edge to g2 "+edge[0]  + " " + edge[1]+ "------------------------211");

                }
            }
            time_for_g2 += (System.currentTimeMillis() - start_time);
            ////System.out.println("additions over");
            ////System.out.println("Graph rn:" +g2);
        }
        //System.out.println("rs used:"+r_used+"+++++++++++++++++++++++++++++++++++++++++++++++++")
        System.out.println("Time to add/del from g1: "+time_for_g1/t);
        System.out.println("Time to add/del from g2: "+time_for_g2/t);
        System.out.println("Time to get cut edges for g1: "+time_to_get_cut_edges_g1/t);
        System.out.println("Time to get cut edges for g2: "+time_to_get_cut_edges_g2/t);
        this.iterations+=t;
        return false;
    }

    public boolean run_epochs_vanilla(ArrayList<ArrayList<Integer>> edges, ArrayList<Double> r_s) {
        int seed = seeds.get(0);
        int t = 0, i = 0;
        double[] r_used = new double[epochs];

        int sub_epoch_size = num_cycles > 2
                ? original_epochs * (int) Math.pow(2, num_cycles - 2)
                : original_epochs;
        int base = sub_epoch_size;
        int seed_index = 0;

        Random random = new Random(seed);

        for (i = 0; i < epochs; i++) {
            // Advance to the next seed window for coupling-from-the-past
            if (i >= sub_epoch_size) {
                seed_index++;
                seed = seeds.get(seed_index);
                sub_epoch_size += original_epochs * (int) (base / 2);
                base /= 2;
                random = new Random(seed);
            }

            double r = random.nextDouble();

            if (edges1 == edges2) {
                this.iterations += t;
                return true;
            }

            int[] edge = {random.nextInt(n), random.nextInt(n)};
            if (edge[0] == edge[1]) { i--; continue; }

            r_used[i] = r;
            t++;

            int u = edge[0], v = edge[1];

            // --- Cut-edge detection via Dfs.connected() ---
            boolean replace1 = vg1.graph.get(u).contains(v);
            boolean replace2 = vg2.graph.get(u).contains(v);
            boolean cut_edge1, cut_edge2;

            if (replace1) {
                vg1.removeEdge(u, v);
                cut_edge1 = !vg1.connected(u, v);
                vg1.addEdge(u, v);
            } else {
                cut_edge1 = !vg1.connected(u, v);
            }

            if (replace2) {
                vg2.removeEdge(u, v);
                cut_edge2 = !vg2.connected(u, v);
                vg2.addEdge(u, v);
            } else {
                cut_edge2 = !vg2.connected(u, v);
            }

            // --- Update vg1 ---
            if (cut_edge1) {
                if (r <= pi) {
                    if (!vg1.graph.get(u).contains(v)) {
                        edges1++;
                        long start = System.nanoTime();
                        vg1.addEdge(u, v);
                        add_time += (System.nanoTime() - start);
                        num_add++;
                    }
                } else {
                    if (vg1.graph.get(u).contains(v)) {
                        long start = System.nanoTime();
                        vg1.removeEdge(u, v);
                        del_time += (System.nanoTime() - start);
                        num_del++;
                        edges1--;
                    }
                }
            } else if (r <= p) {
                if (!vg1.graph.get(u).contains(v)) {
                    edges1++;
                    long start = System.nanoTime();
                    vg1.addEdge(u, v);
                    add_time += (System.nanoTime() - start);
                    num_add++;
                }
            } else {
                if (vg1.graph.get(u).contains(v)) {
                    long start = System.nanoTime();
                    vg1.removeEdge(u, v);
                    del_time += (System.nanoTime() - start);
                    num_del++;
                    edges1--;
                }
            }

            // --- Update vg2 ---
            if (cut_edge2) {
                if (r <= pi) {
                    if (!vg2.graph.get(u).contains(v)) {
                        edges2++;
                        long start = System.nanoTime();
                        vg2.addEdge(u, v);
                        add_time += (System.nanoTime() - start);
                        num_add++;
                    }
                } else {
                    if (vg2.graph.get(u).contains(v)) {
                        long start = System.nanoTime();
                        vg2.removeEdge(u, v);
                        del_time += (System.nanoTime() - start);
                        num_del++;
                        edges2--;
                    }
                }
            } else if (r <= p) {
                if (!vg2.graph.get(u).contains(v)) {
                    edges2++;
                    long start = System.nanoTime();
                    vg2.addEdge(u, v);
                    add_time += (System.nanoTime() - start);
                    num_add++;
                }
            } else {
                if (vg2.graph.get(u).contains(v)) {
                    long start = System.nanoTime();
                    vg2.removeEdge(u, v);
                    del_time += (System.nanoTime() - start);
                    num_del++;
                    edges2--;
                }
            }
        }

        this.iterations += t;
        return false;
    }

    
    public boolean run_epochs_vanilla2(ArrayList<ArrayList<Integer>> edges, ArrayList<Double> r_s){
        //System.out.println("running epochs tada---------------------------------------------");
        double start = 0;
        int seed = seeds.get(0);
        int t = 0, i=0;
        //int[] edges_used = new int[epochs];
        double[] r_used = new double[epochs];
        
        int sub_epoch_size = num_cycles > 2?original_epochs*(int)Math.pow(2, num_cycles-2): original_epochs;
        int base = sub_epoch_size;
        int seed_index = 0;
        //if r<pi and p then ig we def add for both, if r>p and pi then we def remove for both? i guess?
        Random random = new Random(seed);
        //System.out.println("edges: "+edges+" rs"+r_s);
        ArrayList<Set<Integer>> cc1 = new ArrayList<>();
        ArrayList<Set<Integer>> cc2 = new ArrayList<>();
        Set<Integer> comp = vg1.explore(i);
        cc1.add(comp);
        
        for (i = 0; i<epochs; i++){
            //need to store the seeds and access them as needed
            //seed 0 will be used for original epoch size. seed 1 will be for orig epoch * 2, etc.
            //double r = r_s.get(i);
            if(i >= sub_epoch_size){
                seed_index++;
                seed = seeds.get(seed_index);
                sub_epoch_size += original_epochs*(int)(base/2);
                base/=2;
                random = new Random(seed);
            }
            
            double r = random.nextDouble();
            if(edges1 == edges2){
                //coupling over
                //System.out.println("Coupling successful after " + t + " more epochs.");
                this.iterations += t;
                return true;
            }
            
            //ArrayList<Integer> _edge = edges.get(i);
            int[] edge = {random.nextInt(n), random.nextInt(n)};
            // // edge[0] = _edge.get(0); edge[1] = _edge.get(1);
            // if(edge[0] > edge[1]){
            //     int tm = edge[0]; edge[0] = edge[1]; edge[1] = tm;
            // }
            // System.out.println("NEW EDGE TIME "+ edge[0] + " " + edge[1]);
            // System.out.println("Current edges: g1: "+edges1+" g2: "+edges2);
            // System.out.println("p: "+p+" q: "+q+" r: "+r+" pi: "+pi);
            if(edge[0] == edge[1]){
                i--;
                continue; 
            }
            r_used[i] = r;
            t++;
            // ConnVertex u = vertices.get(edge[0]);
            // ConnVertex v = vertices.get(edge[1]);

            int u = edge[0], v = edge[1];

            //need to check if edge is a cut edge. This means we remove edge, if it is still connected, then it is not.
            
            //if it is not a tree edge and is in the graph, then it is def not a cut edge
            //boolean replace1 = g1.hasEdge(u, v), replace2 = g2.hasEdge(u, v);

            boolean replace1 = vg1.graph.get(edge[0]).contains(edge[1]), replace2 = vg2.graph.get(edge[0]).contains(edge[1]);

            //do i need to actually delete and replace it. Maybe i can just see if i find a replacement?
            //code to find a replacement exists, maybe I dublicate it to see if it returns soemthing?
            boolean cut_edge1 = false, cut_edge2 = false;
            
            if(replace1){
                vg1.removeEdge(u, v);
                boolean connected = vg1.connected(u, v);
                if(!connected) 
                    cut_edge1 = true;
                else 
                    cut_edge1 = false;
                vg1.addEdge(u, v);
            }
            else{
                boolean connected = vg1.connected(u, v);
                cut_edge1 = !connected;
            }

            if(replace2){
                vg2.removeEdge(u, v);
                boolean connected = vg2.connected(u, v);
                if(!connected) 
                    cut_edge2 = true;
                else 
                    cut_edge2 = false;
                vg2.addEdge(u, v);
            }
            else{
                boolean connected = vg2.connected(u, v);
                cut_edge2 = !connected;
            }
            
            
            if(cut_edge1){
                //System.out.println("cut edge for g1 "+edge[0]  + " " + edge[1]+ ", r = " + r + " pi = " + pi);
                if(r <= pi){ //we add it if it is a cut edge w probability pi
                   
                    if(!vg1.graph.get(u).contains(v)){
                        edges1++;
                        start = System.nanoTime();
                        vg1.addEdge(u, v); 
                        add_time += (System.nanoTime() - start);
                        num_add += 1;
                        //System.out.println("added edge to g1 "+edge[0]  + " " + edge[1]+ "------------------------152");
                    }
                    //dont need to mess with teh cc stuff since thats all handled
                }
                else{
   
                    if(vg1.graph.get(u).contains(v)){
                        start = System.nanoTime();
                        vg1.removeEdge(u, v);
                        del_time += (System.nanoTime() - start);
                        num_del += 1;
                        edges1--;
                        //System.out.println("deleted edge to g1 "+edge[0]  + " " + edge[1]+ "------------------------161");
                    }
                }
            }
            else if(r <= p){ //we still add it if its not a cut edge w prob p
              
                if(!vg1.graph.get(u).contains(v)){
                    edges1++;
                    start = System.nanoTime();
                    vg1.addEdge(u, v);
                    add_time += (System.nanoTime() - start);
                    num_add += 1;
                    //System.out.println("added edge to g1 "+edge[0]  + " " + edge[1]+ "------------------------169");
                }
            }
            else{ //we remove it
               
                if(vg1.graph.get(u).contains(v)){
                    start = System.nanoTime();
                    vg1.removeEdge(u, v);
                    del_time += (System.nanoTime() - start);
                    num_del += 1;
                    edges1--;
                    //System.out.println("deleted edge to g1 "+edge[0]  + " " + edge[1]+ "------------------------176");
                }
            }

            if(cut_edge2){
                //System.out.println("cut edge for g2 "+edge[0]  + " " + edge[1]+ ", r = " + r + " pi = " + pi);

                if(r <= pi){ //we add it if it is a cut edge w probability pi

                    if(!vg2.graph.get(u).contains(v)){
                        edges2++;
                        start = System.nanoTime();
                        vg2.addEdge(u, v); 
                        num_add += 1;
                        add_time += (System.nanoTime() - start);
                        //System.out.println("added edge to g2 "+edge[0]  + " " + edge[1]+ "------------------------185");
                    }
                    //else //System.out.println("already in g2 "+edge[0]  + " " + edge[1]+ "------------------------187");
                    //dont need to mess with teh cc stuff since thats all handled
                }
                else{
                    //remove it 

                    if(vg2.graph.get(u).contains(v)){
                        start = System.nanoTime();
                        vg2.removeEdge(u, v);
                        del_time += (System.nanoTime() - start);
                        num_del += 1;
                        edges2--;
                        //System.out.println("deleted edge to g2 "+edge[0]  + " " + edge[1]+ "------------------------194");

                    }
                }
            }
            else if(r <= p){ //we still add it if its not a cut edge w prob p
  
                // }
                if(!vg2.graph.get(u).contains(v)){
                    edges2++;
                    start = System.nanoTime();
                    vg2.addEdge(u, v);
                    add_time += (System.nanoTime() - start);
                    num_add += 1;
                    //System.out.println("added edge to g2 "+edge[0]  + " " + edge[1] + "------------------------203");

                }
            }
            else{ //we remove it

                if(vg2.graph.get(u).contains(v)){
                    start = System.nanoTime();
                    vg2.removeEdge(u, v);
                    del_time += (System.nanoTime() - start);
                    num_del += 1;
                    edges2--;
                    //System.out.println("deleted edge to g2 "+edge[0]  + " " + edge[1]+ "------------------------211");

                }
            }
            ////System.out.println("additions over");
            ////System.out.println("Graph rn:" +g2);
        }
        //System.out.println("rs used:"+r_used+"+++++++++++++++++++++++++++++++++++++++++++++++++");
        this.iterations+=t;
        return false;
    }


    public double[] couple(){
        int num = 1;
        // ArrayList<Double> total_epoch_r = generate_r();
        // ArrayList<ArrayList<Integer>> total_edges = generate_edges();
        Random random = new Random();
        int largest_component = -1;
        ArrayList<Double> times_per_epoch = new ArrayList<>();
        while(true){
        //for (int m = 0; m<num; m++){
            //just keep running until its true? i dont even need to generate anything it 
            //just generates right
            seeds.add(0, (int)(random.nextInt())); //add a new seed for randomness
            boolean done; 
            double start = System.nanoTime();
            if (vanilla){
                done = run_epochs_vanilla(null, null);
                largest_component = vg1.max_cc();}
            else{
                done = run_epochs(null, null);
                largest_component = g1.max_comp_size();}
            times_per_epoch.add((System.nanoTime() - start));
            if(done)
                break;
            
            epochs*=2;  
            num_cycles+=1; //go 1, 2, 4, 8, ... 
            
            //adding the new probabilities to the start of the existing ones, so were coupling from the past omg exciting
            
            ////System.out.println("so far: "+iterations+"------------------------------");

        }
        double avg = times_per_epoch.stream().mapToDouble(a -> a).average().orElse(0.0);
        System.out.println("Time  "+(times_per_epoch)+" ns");
        
        // for(int i = 1; i<=n; i++){
        //     for(int j = i; j<=n; j++){
        //         // if(g1.has_edge(i, j)){
        //         //     ////System.out.println("G1 :"+ i+" " + j + " lvl: "+g1.level(i, j));
        //         // }
        //         // if(g2.has_edge(i, j)){
        //         //     ////System.out.println("G2 :" +i+" " + j + " lvl: "+g2.level(i, j));
        //         // }
                

        //     }
        // }
        
        // System.out.println("edges: "+edges1+" "+edges2+" iterations: "+iterations);
        // System.out.println("Average add time: "+(add_time/num_add)+" ns over "+num_add+" operations");
        // System.out.println("Average del time: "+(del_time/num_del)+" ns over "+num_del+" operations");
        // System.out.println("Average conn time: "+(conn_time/num_conn)+" ns over "+num_conn+" operations");
         
        // System.out.println("Largest component size: "+largest_component+" out of "+n);
        // int size_res  = g1.max_cc();
        // if(size_res != largest_component){
        //     //System.out.println(" dfs: "+largest_component+" size: "+size_res);
        //     // g1.spf.get(0).print_tour();
        //     for(int i = 1; i<=n; i++){
        //         for(int j = i; j<=n; j++){
        //             if(g1.has_edge(i, j)){
        //                 //System.out.println("G1 :"+ i+" " + j + " lvl: "+g1.level(i, j));
        //             }
                                

        //         }
        //     }
        // }

        double[] result = {largest_component, iterations};
        if(!vanilla)
            System.out.println("Delete time:" + g1.timeForDeleteEdge +
                        "\n Add time:" + g1.timeForAddEdge +" \n Connected time:" + g1.timeForConnected + 
                        "\n Recomute time:" + g1.timeForRecompute);
        return result;
    }

}
