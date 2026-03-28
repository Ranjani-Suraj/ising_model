package DynGraph;
import java.util.Objects;

class EdgeRecord {
    public int neighbor; // the other endpoint of the edge
    public int depth;    // the depth at which this edge is currently stored
    public EndpointType type; // PRIMARY, SECONDARY, or WITNESS
    public int source; // the vertex at which this record is stored (for easy reference)
    public EdgeRecord(int source, int neighbor, int depth, EndpointType type) {
        this.source = source;
        this.neighbor = neighbor;
        this.depth = depth;
        this.type = type;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EdgeRecord that = (EdgeRecord) o;
        return source == that.source &&
               neighbor == that.neighbor &&
               depth == that.depth &&
               type == that.type;
    }
    @Override
    public int hashCode() {
        return Objects.hash(source, neighbor, depth, type);
    }
}