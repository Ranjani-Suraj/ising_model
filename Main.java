import Cftp.CouplingPast;
import DynGraph.ConnGraph;
import DynGraph.HForest;
import ETTree.Treap;
import ETTree.TreapNode;
import java.util.ArrayList;
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
        HForest hf = new HForest(5);
        hf.add_edge(1, 2);
        hf.add_edge(2, 3);
        hf.add_edge(3, 4);
        hf.add_edge(4, 5);
        hf.drawHForest();

    }

    void test_dyn_grp(){
        TreapNode.random = new Random(42);
        test_dyn t = new test_dyn(10, 100000);
        boolean res = t.test();
        System.out.println("Result: "+res);
        System.out.println("****************************");


        System.out.println("Let us begin (battle music)");

        // long start = System.nanoTime();
        // int x = 1000;
        // CouplingPast cp1 = new CouplingPast(x*x, x , 0.0015, 2);
        // cp1.couple();
        // long end = System.nanoTime();
        // System.out.println("Time taken: " + (end - start) + " ns");
        // System.out.println();



        //sum adjacent is sometimes negative which shouldtn be possible so thats a problem
        //so its failing because im updating twice since its in add_edge_level
        //obv size isnt working which is a thing in and of itself  


        int iters = 2000;
        Map<Double, ArrayList<double[]>> results = new HashMap<>();
        long[] times = new long[iters];
        Random random = new Random(42);
        int p_num = 10;
        double[] p_choices = new double[p_num];//{0.0015, 0.0016, 0.0017, 0.0018, 0.0019, 0.002, 0.0021, 0.0022, 0.0023, 0.0024, 0.0025};
        int n = 1000;
        p_choices[0] = 0.5/n; //0.5, 0.6, 0.7, 0.8 ,0.9, 1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 
        HashMap<Double, ArrayList<Double>> sizes = new HashMap<>();
         sizes.put(p_choices[0], new ArrayList<>());
        for(int i = 1; i<=p_num-1;  i+=1){
            p_choices[i] = p_choices[i-1] + 0.2/n;
            sizes.put(p_choices[i], new ArrayList<>());
        }
        int[] graph_sizes = {n};//, 2500, 500};
        int[] q_options = {2}; //{1, 2};
        Map<Integer, Map<Integer, Map<Double, ArrayList<double[]>>>> final_results = new HashMap<>();
        // {n, {q, {p, {largest comp, time}}}}
        for(int i = 0; i < iters; i++){
            int ch = Math.abs((int)(random.nextDouble()*p_choices.length));
            int n_index = random.nextInt(graph_sizes.length);
            n = graph_sizes[n_index];
            int q_index = (int)((random.nextInt(q_options.length)));
            int q = q_options[q_index];
            //int q = 2;
            if (final_results.containsKey(n)){ //if n has not been hit yet
                if (final_results.get(n).containsKey(q)){ //if this q fro this n has not been hit yet
                    if (!final_results.get(n).get(q).containsKey(p_choices[ch])){ //if this p for this q for this n has not been hit yet
                        final_results.get(n).get(q).put(p_choices[ch], new ArrayList<>());
                    }
                    //if this p exists, then we do nothing
                    
                } 
                else {
                    final_results.get(n).put(q, new HashMap<>());
                    final_results.get(n).get(q).put(p_choices[ch], new ArrayList<>());
                }
            } else {
                final_results.put(n, new HashMap<>());
                final_results.get(n).put(q, new HashMap<>());
                final_results.get(n).get(q).put(p_choices[ch], new ArrayList<>());
            }
            System.out.println("n = "+n+", epochs = "+n*n+", p = "+p_choices[ch] + " q = "+q);
            long start1 = System.nanoTime();
            CouplingPast cp = new CouplingPast(n*n, n, p_choices[ch], q, false);
            double[] output = cp.couple(false); //largest component, iterations
            
            long end1 = System.nanoTime();
            times[i] = end1-start1;
            
            System.out.println("Run "+i+"took "+times[i]+" nanoseconds, "+output[1]+" iterations, and gave largest cc "+output[0]+" for p = "+p_choices[ch]);
            output[1] = times[i];
            results.putIfAbsent(p_choices[ch], new ArrayList<>());
            results.get(p_choices[ch]).add(output);
            final_results.get(n).get(q).get(p_choices[ch]).add(output);
        }
        //results: {p, {largest comp, iterations}}
        double avg_time = 0.0, avg_size = 0.0, avg_overall_time = 0.0;
        int index = 0;
        

        for(int _n = 0; _n < graph_sizes.length; _n++){
            n = graph_sizes[_n];
            if (final_results.containsKey(n)){
                System.out.print("For n = "+n+"-----------------------\n");
                for(int _q = 0; _q < q_options.length; _q++){
                    int q = q_options[_q];
                    if (final_results.get(n).containsKey(q)){
                        System.out.println(" For q = "+q+"------------------------\n");
                        for(int i = 0; i<p_choices.length; i++){
                            //double avg_time = final_results.get(n).get(q).get(p_choices[i])
                            int size = 0;
                            avg_size = 0.0; avg_time = 0.0;
                            int index2 = 0;
                            if (final_results.get(n).get(q).containsKey(p_choices[i])){
                                for(index2 = 0; index2 < final_results.get(n).get(q).get(p_choices[i]).size(); index2++){
                                    double t_c[] = new double[2] ;
                                    size = final_results.get(n).get(q).get(p_choices[i]).size();
                                    t_c = final_results.get(n).get(q).get(p_choices[i]).get(index2);
                                    //index2 ++;
                                    //System.out.println(""+p_choices[i]+" "+t_c[0]+ " ");
                                    sizes.get(p_choices[i]).add(t_c[0]);
                                    avg_size += t_c[0];
                                    avg_time += t_c[1];
                                    avg_overall_time += t_c[1];
                                }
                            }
                            
                            System.out.println("  for p = "+p_choices[i]+" , time "+avg_time/size+" avg size "+avg_size/size+" \\");
                        }
                    }
                }
            }
        }
        // for(int i = 0; i<p_choices.length; i++){
            
        //     double t_c[] = new double[2] ;
        //     int size = results.get(p_choices[i]).size();
        //     for(int j = 0; j< size; j++){
        //         ArrayList<double[]> t = results.get(p_choices[i]);
        //         t_c = t.get(j);
        //         //System.out.println(""+p_choices[i]+" "+t_c[0]+ " ");
        //         avg_size+=t_c[0];
        //         avg_time += t_c[1];
        //         avg_overall_time += t_c[1];
        //     }
        //     avg_size/=size; avg_time/=size;
        //     System.out.println("for p = "+p_choices[i]+" , avg time "+avg_time+" avg size "+avg_size+" \\");
        //     avg_size = 0.0; avg_time = 0.0;
        // }
        System.out.println("Average overall time: "+avg_overall_time/iters);
        for (int qi = 2; qi <=3; qi++){
            int q = qi;
            System.out.println("Q = "+q+" : ");
            for (int i = 0; i<p_choices.length; i++){
                System.out.println("Sizes for p = "+p_choices[i]+" : \\");
                for (int j = 0; j < sizes.get(p_choices[i]).size(); j++){
                    System.out.print(""+sizes.get(p_choices[i]).get(j)+" ");
                }
                System.out.println("\\");
            }
        }
        //System.out.println("Sizes" + sizes.toString());
    }
    

    static void from_ett(){
        ConnGraph g = new ConnGraph();
        g.addEdge(1, 2);
        g.addEdge(2, 3);
        g.addEdge(3, 4);
        g.addEdge(4, 5);
        // g.addEdge(1, 4);
        System.out.println("After linking edges (1,2), (2,3), (3,4), (4,5):");
        // g.printForest();
        System.out.println("----------------------------");

        g.deleteEdge(2, 3);
        System.out.println("After cutting edge (2,3):");
        // g.printForest();
        System.out.println("----------------------------");

        g.addEdge(1, 3);
        System.out.println("After linking edge (1,3):");
        // g.printForest();
        System.out.println("----------------------------");
        g.addEdge(2, 3);
        System.out.println("After linking edge (2,3):");
        // g.printForest();
        g.deleteEdge(1, 2);
        System.out.println("After cutting edge (2,1):");
        // g.printForest();
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
        // Treap.inorder(root);
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
        // Treap.inorder(root);
        System.out.println("----------------------------");
        
        //now to cut edge 1-2
        TreapNode[] edgeNodes = edges.get(new map_edge(1, 2));
        int i = Math.min(Treap.getIndex(edgeNodes[0]), Treap.getIndex(edgeNodes[1]));
        int j = Math.max(Treap.getIndex(edgeNodes[0]), Treap.getIndex(edgeNodes[1]));
        TreapNode[] split1 = Treap.split(root, i);
        // Treap.inorder(split1[1]);
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
        // Treap.inorder(root);
        TreapNode cutroot = split2[0];
        System.out.println("-------");
        cutroot = Treap.delete_root_and_leaf(cutroot);
        //Treap.inorder(cutroot); 
        System.out.println("-------");
    }

}
