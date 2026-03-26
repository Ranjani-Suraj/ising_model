package VanillaGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Dfs {
    public Map<Integer, Set<Integer>> graph = new HashMap<>();
    int n;
    int cycles;
    
    //ArrayList<ConnVertex> dyn_vertices;
    public Dfs(int size){
        n = size;
    }

    public boolean hasEdge(int u, int v){
        return graph.get(u).contains(v) || graph.get(v).contains(u);
    }

    public void add_vertex(int v){
        graph.put(v, new HashSet<>());
    }

    public void addEdge(int u, int v){
        graph.get(u).add(v);
        graph.get(v).add(u);
    }
    public void removeEdge(int u, int v){
        graph.get(u).remove(v);
        graph.get(v).remove(u);
    }

    public boolean connected(int u, int v){
        Set<Integer> cc_u = explore(u);
        return cc_u.contains(v);
    }

    public Set<Integer> explore(int start){
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

    public int max_cc(){
        int max_size = 0;
        for(int i = 0; i < n; i++){
            Set<Integer> cc = explore(i);
            if(cc.size() > max_size){
                max_size = cc.size();
            }
        }
        return max_size;
    }
}
