package ETTree;

class Edge {
    int u, v;
    Edge(int u, int v) {
        this.u = u;
        this.v = v;
    }

    @Override
    public int hashCode() {
        return 31 * u + v;
    }

    @Override
    public boolean equals(Object o) {
        Edge e = (Edge)o;
        return u == e.u && v == e.v;
    }
}

