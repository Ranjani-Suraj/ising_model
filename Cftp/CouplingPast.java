package Cftp; 

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

import DynGraph.ConnGraph;
import VanillaGraph.Dfs;
//import dyn_connectivity.*;

//stress test for all values of n, p, 1<=q<=2
//check why the time behaviour is weird
//test without time checks

//


public class CouplingPast {
    ConnGraph g1, g2;
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
    // ArrayList<ConnVertex> vertices = new ArrayList<ConnVertex>();
    public CouplingPast(int epochs, int n, double p, double q, boolean warm_start) {
        this.epochs = epochs;
        this.original_epochs = epochs;
        this.n = n;
        this.g1 = new ConnGraph();
        this.g2 = new ConnGraph();
        this.vg1 = new Dfs(n);
        this.vg2 = new Dfs(n);
        this.p = p;
        this.q = q;
        this.pi = p/(p+q*(1-p));
        Random r = new Random(); 
        seeds = new ArrayList<Integer>();
        //make g1 a complete graph
        //make g2 an empty graph
        for (int i = 0; i<n; i++){
            // vertices.add(new ConnVertex());
            vg1.add_vertex(i);
            vg2.add_vertex(i);
            //System.out.println("Added vertex " + i + "-> " + vertices.get(i));
            
        }
        ////System.out.println("NODES ADDED");
        for(int i = 0; i<n; i++){
            for(int j = i+1; j<n; j++){
                ////System.out.println("ADD EDGE: "+j);
                double ch = r.nextDouble();
                if (warm_start){
                    if(ch < p){
                            g1.addEdge(i, j);
                            // g1.addEdge(vertices.get(j), vertices.get(i));
                            edges1+=1;
                            vg1.addEdge(i, j);
                    }
                    if(ch < pi){
                            g2.addEdge(i, j);
                            //g2.addEdge(j, i);
                            edges2+=1;
                            vg2.addEdge(i, j);
                    }
                }
                else{
                    g1.addEdge(i, j);
                    // g1.addEdge(vertices.get(j), vertices.get(i));
                    edges1+=1;
                    vg1.addEdge(i, j);
                }
                    //add_time += (System.nanoTime() - start);
            }
        }


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
        double[] r_used = new double[epochs];
        
        int sub_epoch_size = num_cycles > 2?original_epochs*(int)Math.pow(2, num_cycles-2): original_epochs;
        int base = sub_epoch_size;
        int seed_index = 0;
        //if r<pi and p then ig we def add for both, if r>p and pi then we def remove for both? i guess?
        Random random = new Random(seed);
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
            r_used[i] = r;
            t++;
            int u = (edge[0]);
            int v = (edge[1]);
            //need to check if edge is a cut edge. This means we remove edge, if it is still connected, then it is not.
            
            //if it is not a tree edge and is in the graph, then it is def not a cut edge
            boolean replace1 = g1.hasEdge(u, v), replace2 = g2.hasEdge(u, v);
            //do i need to actually delete and replace it. Maybe i can just see if i find a replacement?
            //code to find a replacement exists, maybe I dublicate it to see if it returns soemthing?
            boolean cut_edge1 = false, cut_edge2 = false;
            cut_edge1 = true;//g1.isCutEdge(u, v);
            // cut_edge2 = g2.isCutEdge(u, v);
            // if(g1.is_tree_edge(u, v)){
            //     //System.out.println("edge is a tree edge in g1");
            //     if(replace1){
            //         //System.out.println("edge is in g1");
            //         //start = System.nanoTime();
            //         g1.deleteEdge(u, v);
            //         //del_time += (System.nanoTime() - start);
            //         num_del += 1;
            //     }
                
                
            //     //if they are not connected, then removing the edge
            //     //splits the cc so it IS a cut edge
            //     //start = System.nanoTime();
            //     boolean connected = g1.connected(u, v);
            //     //conn_time += (System.nanoTime() - start);
            //     num_conn += 1;
            //     if(!connected){
            //         //edge is not a cut edge, so we can add it to g2
            //         ////System.out.println("edge is a cut edge for g1");
            //         cut_edge1 = true;
            //     }
            //     else{
            //         ////System.out.println("edge is not a cut edge for g1");
            //         cut_edge1 = false;
            //     }
            //     if(replace1){
            //         //System.out.println("adding edge back to g1 "+edge[0]  + " " + edge[1]);
            //         //start = System.nanoTime();
            //         g1.addEdge(u, v); //add it back
            //         //add_time += (System.nanoTime() - start);
            //         num_add += 1;
            //     }
            // }
            // else if (replace1){
            //     cut_edge1 = false;
            //     //System.out.println("edge is not a tree edge in g1 but it is IN g1 so it cannot be a cut edge");
            // }
            // else{
            //     //it is not in the graph, but we need to check if they are alr connected
            //     //start = System.nanoTime();
            //     boolean connected = g1.connected(u, v);
            //     //conn_time += (System.nanoTime() - start);
            //     num_conn += 1;
            //     //System.out.println("nodes u: "+edge[0]+" and v: "+edge[1]+" are "+connected);
            //     cut_edge1 = !connected;
            // }

            // if(g2.is_tree_edge(u, v)){
            //     //System.out.println("edge is a tree edge in g1");
            //     if(replace2){
            //         //System.out.println("edge is in g1");
            //         //start = System.nanoTime();
            //         g2.deleteEdge(u, v);
            //         //del_time += (System.nanoTime() - start);
            //         num_del += 1;                
            //     }
                
                
            //     //if they are not connected, then removing the edge
            //     //splits the cc so it IS a cut edge
            //     //start = System.nanoTime();
            //     boolean connected = g2.connected(u, v);
            //     //conn_time += (System.nanoTime() - start);
            //     num_conn += 1;
            //     if(!connected){
            //         //edge is not a cut edge, so we can add it to g2
            //         ////System.out.println("edge is a cut edge for g1");
            //         cut_edge2 = true;
            //     }
            //     else{
            //         ////System.out.println("edge is not a cut edge for g1");
            //         cut_edge2 = false;
            //     }
            //     if(replace2){
            //         //System.out.println("adding edge back to g1 "+edge[0]  + " " + edge[1]);
            //         //start = System.nanoTime();
            //         g2.addEdge(u, v); //add it back
            //         //add_time += (System.nanoTime() - start);
            //         num_add += 1;
            //     }
            // }
            // else if (replace2){
            //     cut_edge2 = false;
            //     //System.out.println("edge is not a tree edge in g1 but it is IN g1 so it cannot be a cut edge");
            // }
            // else{
            //     //it is not in the graph, but we need to check if they are alr connected
            //     //start = System.nanoTime();
            //     boolean connected = g2.connected(u, v);
            //     //conn_time += (System.nanoTime() - start);
            //     num_conn += 1;
            //     //System.out.println("nodes u: "+edge[0]+" and v: "+edge[1]+" are "+connected);
            //     cut_edge2 = !connected;
            // }
            
            ////System.out.println("starting to add/delete!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!11");
            //need to do the markov stuff lol i forgor
            //need to keep track of number of edges whoops
            if(cut_edge1){
                //System.out.println("cut edge for g1 "+edge[0]  + " " + edge[1]+ ", r = " + r + " pi = " + pi);
                if(r <= pi){ //we add it if it is a cut edge w probability pi
                    if(!g1.hasEdge(u, v)){
                        edges1++;
                        //start = System.nanoTime();
                        g1.addEdge(u, v); 
                        //add_time += (System.nanoTime() - start);
                        num_add += 1;
                        //System.out.println("added edge to g1 "+edge[0]  + " " + edge[1]+ "------------------------152");
                    }
                    //dont need to mess with teh cc stuff since thats all handled
                }
                else{
                    //remove it 
                    if(g1.hasEdge(u, v)){
                        //start = System.nanoTime();
                        g1.deleteEdge(u, v);
                        //del_time += (System.nanoTime() - start);
                        num_del += 1;
                        edges1--;
                        //System.out.println("deleted edge to g1 "+edge[0]  + " " + edge[1]+ "------------------------161");
                    }
                }
            }
            else if(r <= p){ //we still add it if its not a cut edge w prob p
                ////System.out.println("not cut edge for g1 "+edge[0]  + " " + edge[1]+ ", r = " + r + " p = " + p);
                if(!g1.hasEdge(u, v)){
                    edges1++;
                    //start = System.nanoTime();
                    g1.addEdge(u, v);
                    //add_time += (System.nanoTime() - start);
                    num_add += 1;
                    //System.out.println("added edge to g1 "+edge[0]  + " " + edge[1]+ "------------------------169");
                }
            }
            else{ //we remove it
                ////System.out.println("not cut edge for g1 "+edge[0]  + " " + edge[1]+ ", r = " + r + " p = " + p);
                if(g1.hasEdge(u, v)){
                    //start = System.nanoTime();
                    g1.deleteEdge(u, v);
                    //del_time += (System.nanoTime() - start);
                    num_del += 1;
                    edges1--;
                    //System.out.println("deleted edge to g1 "+edge[0]  + " " + edge[1]+ "------------------------176");
                }
            }

            if(cut_edge2){
                //System.out.println("cut edge for g2 "+edge[0]  + " " + edge[1]+ ", r = " + r + " pi = " + pi);

                if(r <= pi){ //we add it if it is a cut edge w probability pi
                    if(!g2.hasEdge(u, v)){
                        edges2++;
                        //start = System.nanoTime();
                        g2.addEdge(u, v); 
                        num_add += 1;
                        //add_time += (System.nanoTime() - start);
                        //System.out.println("added edge to g2 "+edge[0]  + " " + edge[1]+ "------------------------185");
                    }
                    //else //System.out.println("already in g2 "+edge[0]  + " " + edge[1]+ "------------------------187");
                    //dont need to mess with teh cc stuff since thats all handled
                }
                else{
                    //remove it 
                    if(g2.hasEdge(u, v)){
                        //start = System.nanoTime();
                        g2.deleteEdge(u, v);
                        //del_time += (System.nanoTime() - start);
                        num_del += 1;
                        edges2--;
                        //System.out.println("deleted edge to g2 "+edge[0]  + " " + edge[1]+ "------------------------194");

                    }
                }
            }
            else if(r <= p){ //we still add it if its not a cut edge w prob p
                if(!g2.hasEdge(u, v)){
                    edges2++;
                    //start = System.nanoTime();
                    g2.addEdge(u, v);
                    //add_time += (System.nanoTime() - start);
                    num_add += 1;
                    //System.out.println("added edge to g2 "+edge[0]  + " " + edge[1] + "------------------------203");

                }
            }
            else{ //we remove it
                if(g2.hasEdge(u, v)){
                    //start = System.nanoTime();
                    g2.deleteEdge(u, v);
                    //del_time += (System.nanoTime() - start);
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

    
    public boolean run_epochs_vanilla(ArrayList<ArrayList<Integer>> edges, ArrayList<Double> r_s){
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


    public double[] couple(boolean vanilla){
        int num = 1;
        ArrayList<Double> total_epoch_r = generate_r();
        ArrayList<ArrayList<Integer>> total_edges = generate_edges();
        Random random = new Random();
        int largest_component = -1;
        while(true){
        //for (int m = 0; m<num; m++){
            //just keep running until its true? i dont even need to generate anything it 
            //just generates right
            seeds.add(0, (int)(random.nextInt())); //add a new seed for randomness
            boolean done; 
            if (vanilla){
                done = run_epochs_vanilla(total_edges, total_epoch_r);
                largest_component = vg1.max_cc();}
            else{
                done = run_epochs(total_edges, total_epoch_r);
                largest_component = g1.max_comp_size();}
            if(done)
                break;
            
            epochs*=2;  
            num_cycles+=1; //go 1, 2, 4, 8, ... 
            
            //adding the new probabilities to the start of the existing ones, so were coupling from the past omg exciting
            for(int i = 0; i<2; i++){
                // ArrayList<Double> add_r = generate_r();
                // ArrayList<ArrayList<Integer>> add_edges = generate_edges();
                // total_epoch_r.addAll(0, add_r);
                // total_edges.addAll(0, add_edges);
            }
            ////System.out.println("so far: "+iterations+"------------------------------");

        }
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
        return result;
    }

}
