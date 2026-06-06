package Dynamic_Graph;
//import java.lang.reflect.Array;
import java.util.*;

public class Connectivity {
    Set<Integer> vertices;
    ArrayList<ET_tour> spf;
    ArrayList<Map<Integer, Set<Integer>>> adj; //non tree edges at levels
    ArrayList<Map<Integer, Set<Integer>>> tree_adj;
    Map<Integer, Map<Integer, Set<Integer>>> edge_level; //for node u node v, we store level of edge??? what 
    Map<Integer, Set<Integer>> edges;
    //this is supposed to be unordered_map<pair<int, int>, multiset<int>, hash_pair> edge_level;
    //who knows if this is gonna work wtf is a hash pair even
    // struct hash_pair {
    // 	template <class T1,class T2> 
    //     size_t operator()(const pair<T1,T2> &p) const {
    //         auto h1 = hash<T1>{}(p.first);
    //         auto h2 = hash<T2>{}(p.second);
    //         return int(((long long)(1000000007)) * h1 + h2);
    // }
    // };
    //what does this DO^^

    public Connectivity(){
        vertices = new HashSet<>();
        spf = new ArrayList<>(); //et tours for eahc level
        adj = new ArrayList<>();
        tree_adj = new ArrayList<>();
        edge_level = new HashMap<>();
        //initialize the first level
        spf.add(new ET_tour());
        adj.add(new HashMap<>());
        tree_adj.add(new HashMap<>());
        edges = new HashMap<>();
        //there are logn levels, so we can just. initialize all of them i guess?
        //int num_levels = (int)(Math.floor(Math.log(5)/Math.log(2)));
        // for(int i = 0; i < n; i++){
        //     tree_adj.add(new HashMap<>());
        // }
    }

    public  void add_vertex(int u){
        vertices.add(u);
        spf.add(spf.size()-1, new ET_tour());
        adj.add(adj.size()-1, new HashMap<Integer, Set<Integer>>());
        tree_adj.add(tree_adj.size()-1, new HashMap<Integer, Set<Integer>>());
        //System.out.println("Added vertex " + u);
    }

    void add_edge_level(int u, int v, int level, boolean is_tree_edge){
        // if(u > v){
        //     int temp = u;
        //     u = v;
        //     v = temp;
        // }
        
        edge_level.putIfAbsent(u, new HashMap<>());
        edge_level.get(u).putIfAbsent(v, new HashSet<>());
        edge_level.get(u).get(v).add(level);
        
        edge_level.putIfAbsent(v, new HashMap<>());
        edge_level.get(v).putIfAbsent(u, new HashSet<>());
        edge_level.get(v).get(u).add(level);

        if(is_tree_edge){
            tree_adj.get(level).putIfAbsent(u, new HashSet<>());
            tree_adj.get(level).putIfAbsent(v, new HashSet<>());

            tree_adj.get(level).get(u).add(v);
            tree_adj.get(level).get(v).add(u);
            //tree_adj.get(level).get(v).add(u);
        }
        else{
            adj.get(level).putIfAbsent(u, new HashSet<>());
            adj.get(level).putIfAbsent(v, new HashSet<>());

            adj.get(level).get(u).add(v);
            adj.get(level).get(v).add(u);
        }
        System.out.println("At level "+level);
        spf.get(level).update_adjacent(u, 1, is_tree_edge);
        //spf.get(level).update_adjacent(v, 1, is_tree_edge);
        // //System.out.println("non tree adjacency list after adding edge: " + adj.get(level));
        // System.out.println("tree adjacency list after adding edge: " + tree_adj.get(level));
        // System.out.println("At level "+level);
        // System.out.println("Tree edges: " + tree_adj.get(level));
        // System.out.println("Non tree edges: " + adj.get(level));
        // System.out.println("Adding edge " + u + "-" + v + " at level " + level);
        spf.get(level).update_adjacent(v, 1, is_tree_edge);
    }

    void remove_edge_level(int u, int v, int level, boolean is_tree_edge){
        // if(u > v){
        //     int temp = u;
        //     u = v;
        //     v = temp;
        // }

        // edge_level.putIfAbsent(u, new HashMap<>());
        // edge_level.get(u).putIfAbsent(v, new HashSet<>());
        edge_level.get(u).get(v).remove(level); //remove the edge from the set of edges of x level
        // edge_level.putIfAbsent(v, new HashMap<>());
        // edge_level.get(v).putIfAbsent(u, new HashSet<>());
        edge_level.get(v).get(u).remove(level);
        //edge_level.get(v).get(u).remove(level); //remove the edge from the set of edges of y level
                //its sometimes taking tree edges as non tree edges

        
        if(is_tree_edge){
            // if(tree_adj.get(level) != null && tree_adj.get(level).containsKey(u))
            //     tree_adj.get(level).get(u).remove(v); //if itis a tree edge then we remove it from its level
            // else{
            //     System.out.println("At level "+level);
            //     System.out.println("Tree edges: " + tree_adj.get(level));
            //     System.out.println("Non tree edges: " + adj.get(level));
            //     System.out.println("Removing edge " + u + "-" + v + " at level " + level+" for tree edge = "+is_tree_edge);
                
            // }
            tree_adj.get(level).get(u).remove(v);
            tree_adj.get(level).get(v).remove(u);
        }
        else{
            //System.out.println("Removing edge lelvel " + u + "-" + v + " at level " + level + " "+adj.get(level) + " " + adj.get(level).get(u)); //+ " " + adj.get(level).containsKey(v));
            // if(adj.get(level) != null && adj.get(level).containsKey(u)) //this should not... be possible why is that failing
            //     {
            //         adj.get(level).get(u).remove(v);

            //     }
            // else{
            //     System.out.println("At level "+level);
            //     System.out.println("Tree edges: " + tree_adj.get(level));
            //     System.out.println("Non tree edges: " + adj.get(level));
            //     System.out.println("Removing edge " + u + "-" + v + " at level " + level+" for tree edge = "+is_tree_edge);
            // }
            //System.out.println("adjacency list after removing edge: " + adj.get(level));
            // System.out.println("At level "+level);
            // System.out.println("Tree edges: " + tree_adj.get(level));
            // System.out.println("Non tree edges: " + adj.get(level));
            // System.out.println("Removing edge " + u + "-" + v + " at level " + level+" for tree edge = "+is_tree_edge);
            adj.get(level).get(u).remove(v);
            adj.get(level).get(v).remove(u);
        }
        System.out.println("At level "+level);
        spf.get(level).update_adjacent(u, -1, is_tree_edge); //reduce level of edge by 1
        spf.get(level).update_adjacent(v, -1, is_tree_edge);
        
        //System.out.println("Tree edges: " + tree_adj.get(level));
        //System.out.println("Non tree edges: " + adj.get(level));
        //System.out.println("Removing edge " + u + "-" + v + " at level " + level);
    }

    public int level(int u, int v){
        // if(u > v){
        //     int temp = u;
        //     u = v;
        //     v = temp;
        // }
        if(!edge_level.containsKey(u) || !edge_level.get(u).containsKey(v)){
            return -1; //edge does not exist
        }
        int lev = -1;
        if(edge_level.get(u).get(v).iterator().hasNext()){
            lev = edge_level.get(u).get(v).iterator().next();
            //System.out.println("found level = "+lev);
        }

        return lev;//return the first level of the edge
    }

    public  boolean add_edge(int u, int v){
        if(!vertices.contains(u) || !vertices.contains(v)){
            return false; //one of the vertices does not exist
        }
        // if(v>u){
        //     int t = u;
        //     u = v; v = t;
        // }
        if(has_edge(u, v) || has_edge(v, u)){
            //System.out.println("Edge " + u + "," + v + " already exists");
            return true; //edge already exists
        }
        //we add it at the lowest level
        if(!spf.get(0).connected(u, v)){
           //they are not already connected, so we add the edge
           System.out.println("Adding edge " + u + "-" + v);
           spf.get(0).link(u, v);
           //spf.get(0).link(v, u);
           add_edge_level(u, v, 0, true);
           //add_edge_level(v, u, 0, true);
           System.out.println("tree edges at level 0:" + tree_adj.get(0));
           System.out.println("non tree edges at level 0: " + adj.get(0));
        }
        else{
            //System.out.println("alr connected so j adding tree");
            add_edge_level(u, v, 0, false);
            //add_edge_level(v, u, 0, false);
            
            //System.out.println("edges at level 0: " + adj.get(0));

        }
        edges.putIfAbsent(u, new HashSet<>());
        edges.get(u).add(v);
        edges.putIfAbsent(v, new HashSet<>());
        edges.get(v).add(u);
        return true;
    }

    public boolean has_edge(int u, int v){
        if(!vertices.contains(u) || !vertices.contains(v)){
            return false; //one of the vertices does not exist
        }
        int lvl = level(u, v);
        if(lvl == -1)
            return false;
        return (tree_adj.get(lvl).containsKey(u) && tree_adj.get(lvl).get(u).contains(v)) || (adj.get(lvl).containsKey(u) && adj.get(lvl).get(u).contains(v));

        //return edges.get(u) != null && edges.get(u).contains(v);
    }

    public boolean is_tree_edge(int u, int v){
        if(!vertices.contains(u) || !vertices.contains(v)){
            return false; //one of the vertices does not exist
        }
        // if(v>u){
        //     int t = u;
        //     u = v; v = t;
        // }
        int level = level(u, v);
        if(level == -1){
            return false; //edge does not exist
        }
        return tree_adj.get(level).containsKey(u) && tree_adj.get(level).get(u).contains(v);
    }

    public boolean delete_edge(int u, int v){
        if(!vertices.contains(u) || !vertices.contains(v)){
            return false; //one of the vertices does not exist
        }

        int level = level(u, v);
        if(level == -1){
            return false; //edge does not exist
        }
        System.out.println("edge: "+u+", "+v);
                System.out.println("At level "+level);
                System.out.println("Tree edges: " + tree_adj.get(level));
                System.out.println("Non tree edges: " + adj.get(level));
                System.out.println("edges overall:"+edges);
                System.out.println("edgemap:" + spf.get(0).edgemap);
                spf.get(0).print_tour(u);
                //System.out.println("tree_Edge: "+tree_edge+", cut:  "+cut);
                System.out.println(spf.get(0).edgemap);
        //need to cut for all levels?

        // boolean tree_edge = spf.get(0).cut(u, v);
        boolean tree_edge = is_tree_edge(u, v);
        boolean cut = spf.get(0).cut(u, v);
        if(tree_edge != cut){ //because cut = false if it is not a tree edge
                System.out.println("edge: "+u+", "+v);
                System.out.println("At level "+level);
                System.out.println("Tree edges: " + tree_adj.get(level));
                System.out.println("Non tree edges: " + adj.get(level));
                System.out.println("edges overall:"+edges);
                System.out.println("edgemap:" + spf.get(0).edgemap);
                spf.get(0).print_tour(u);
                System.out.println("tree_Edge: "+tree_edge+", cut:  "+cut);
                System.out.println(spf.get(0).edgemap);

       
        }
        if(tree_edge){
            for(int i = level; i>0; i--){
                spf.get(i).cut(u, v);
            }
        }
        else{ //if it is not a tree edge
            edges.get(u).remove(v);
            edges.get(v).remove(u);
            remove_edge_level(u, v, level, false);
            //remove_edge_level(v, u, level, false);

            return true;
        }
        remove_edge_level(u, v, level, true);
        //remove_edge_level(v, u, level, true);
        
        
        edges.get(u).remove(v);
        edges.get(v).remove(u);
        

        //System.out.println("Cut succeeded now we search for a replacement");
        
        //for every level, 
        for(int i = level; i>=0; i--){
            //System.out.println("checking level "+ i);
            //System.out.println("sizes of subtrees at level "+i+": u: "+spf.get(i).size(u)+" v: "+spf.get(i).size(v));
            if(spf.get(i).size(u) > spf.get(i).size(v)){
                //make u the smaller subset

                int temp = u;
                u = v;
                v = temp;
            }
            //this isnt working 
            //it is not able to find replacements that are not directly connected to u.
            while(true){

                int x = spf.get(i).get_adjacent(u, true);
                //System.out.println("checking adj tree nodes of u: "+u+" x: "+x);
                //System.out.println("Tree adjacency list at level "+i+": "+tree_adj.get(i));
                //System.out.println("adj list at level "+i+": "+adj.get(i));

                if(x == -1){
                    //System.out.println("no adjacent nodes of "+u+" at level "+i+" so we break");
                    break; //no more adjacent nodes
                }
                //System.out.println("adjacent nodes at level "+i+" x: "+x+" are: "+ tree_adj.get(i).get(x));
                // if(tree_adj.get(i).get(x).isEmpty()){
                //     System.out.println("no adjacent nodes at level "+i+" so we break");
                //     // remove_edge_level(x, u, i, false);
                //     // add_edge_level(x, u, i+1, false);
                //     continue; //no more adjacent nodes
                // }
                //increase the level of every tree
                while(!tree_adj.get(i).get(x).isEmpty()){ //adjacency list of tree nodes, so adjacent tree nodes to x at level i are increased level

                    int y = tree_adj.get(i).get(x).iterator().next();
                    //System.out.println("checked adj edge edges? y = "+y);
                    remove_edge_level(x, y, i, true);
                    //remove_edge_level(y, x, i, true);
                    
                    add_edge_level(x, y, i+1, true);
                    //add_edge_level(y, x, i+1, true);
                    //
                    System.out.println("linking "+x+" and "+y+" at level"+(i+1));
                    spf.get(i+1).link(x, y); //link x and y at a higher level???? now that the edges exist just on a higher level??? on the ET tree???
                    //System.out.println("linking "+x+" and "+y+" on a higher level");
                    //wait but it should still exist on level 0. so i should then. add it at level i+1? but... no? wtf 
                }
            
            }

            //its not able to find its own adjacent nodes what even
            System.out.println("Searching for replacements:");

            //non tree edges: getting adjacent nodes of u that are non tree edges to replace removed one
            boolean flag = false;
            while(!flag){
                
                int x = spf.get(i).get_adjacent(u, false);
                //System.out.println("adj non tree nodes of u:"+spf.get(i).adj_map);
                //System.out.println("edgemap at level "+i+": "+spf.get(i).edgemap);
                //System.out.println("adjacent node at level "+i+" non tree: "+x);
                if(x == -1){
                    break;
                }

                // if(adj.get(i).get(x).isEmpty()){
                //     System.out.println("no adjacent nodes at level "+i+" ????");
                    
                //     break; //no more adjacent nodes
                // }
                //System.out.println("adjacent nodes at level "+i+": "+ adj.get(i));

                while(!adj.get(i).get(x).isEmpty()){
                    //System.out.println("if there is a non tree edge at level "+i+" that is adjacent to u, ");
                    int y = adj.get(i).get(x).iterator().next();
                    //System.out.println("we check if the adjacent incident node y: "+y+" also connects to v: we check if x: "+x+"which is adjacent to a node in the cc of :"+u+" is also adjacent to a node in the cc of :"+v);
                    
                    if(spf.get(i).connected(y, v)){
                        //System.out.println("edge "+y+" "+x+" works");
                        for(int j = 0; j<=i; j++){ //connect them in all levels lower than i
                            spf.get(j).link(x, y);
                        }
                        flag = true;
                        remove_edge_level(x, y, i, false);
                        //remove_edge_level(y, x, i, false);

                        add_edge_level(x, y, i, true);
                        //add_edge_level(y, x, i, true);

                        break;
                    }
                    else{
                        remove_edge_level(x, y, i, false);
                        //remove_edge_level(y, x, i, false);
                        
                        add_edge_level(y, x, i+1, false);
                        //add_edge_level(x, y, i+1, false);
                    }
                    
                }
            }
            if(flag){
                //we found a replacement
                //System.out.println("Found a replacement for edge " + u + "-" + v + " at level " + i);
                break;
            }
        }
        //}
        return true;
    }


    public  boolean connected(int u, int v){
        if(!vertices.contains(u) || !vertices.contains(v)){
            return false; //one of the vertices does not exist
        }
        if(u == v){
            return true; //they are the same vertex
        }
        boolean connected = spf.get(0).connected(u, v);
        //System.out.println("Are " + u + " and " + v + " connected? " + connected);
        return connected;
    }

    public int max_cc(){
        int largest_component = 0, index = 0;
        for(int i = 1; i<vertices.size(); i++){
            if(spf.get(0).size(i) > largest_component){
                largest_component = spf.get(0).size(i);
                index = i;
            }
        }
        System.out.println("largest comp: "+largest_component);
        spf.get(0).print_tour(index);
        return largest_component;
    }

    public int dfs(int source){
        int n = edges.size();
        ArrayList<Integer> stack = new ArrayList<>();
        stack.add(source);
        int max_size = 0;
        Set<Integer> visited = new HashSet<>();
        visited.add(source);
        Set<Integer> unvisited = new HashSet<>();
        for(int i = 1; i<=n; i++){
            if(!visited.contains(i))
                unvisited.add(i);
        }
        Set <Integer> cc = new HashSet<>();
        
        while(unvisited.size() > 0){
            int ns = max_size;
            int start = unvisited.iterator().next();
            unvisited.remove(start);
            Map<Integer, Set<Integer>> newly = explore(start, max_size);

            Set<Integer> vis = newly.get(newly.keySet().iterator().next());
            
            visited.addAll(vis);
            max_size = newly.keySet().iterator().next();
            if(ns != max_size){
                cc = vis;
            }
            for(int i = 1; i<=n; i++){
                if(!visited.contains(i))
                    unvisited.add(i);
                else if(!unvisited.contains(i))
                    unvisited.remove(i);
            }
        }
        System.out.println("LARGEST COMP:"+cc);
        return max_size;
    }

    Map<Integer, Set<Integer>> explore(int start, Integer max_cc){
        ArrayList<Integer> stack = new ArrayList<>();
        stack.add(start);
        Set<Integer> visited = new HashSet<>();
        visited.add(start);
        //int size = 0;
        while (stack.size() > 0){
            int v = stack.remove(stack.size()-1);
            //System.out.println("adj of v:"+edges.get(v));
            for(int u : edges.get(v)){
                // System.out.println("u="+u);
                // System.out.println("stack:"+stack);
                // System.out.println("visited:"+visited);
                if(!visited.contains(u)){
                    visited.add(u);
                    stack.add(u);
                }

                if(visited.size() == edges.size()){
                    stack.clear();
                    break;
                }
            }
        }
        // System.out.println("size: "+visited.size()+" for v: "+start);
        // System.out.println("set: "+visited);
        if(visited.size() > max_cc){
            max_cc = (visited.size());
        }
        Map<Integer, Set<Integer>> result = new HashMap<>();
        result.put(max_cc, visited);
        return result;
    }
}
