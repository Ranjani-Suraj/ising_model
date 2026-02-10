package ETTree;

public class TreapKey implements Comparable<TreapKey> {
    int from, to;
    int index;
    TreapKey(int from, int to) {
        this.from = from;
        this.to = to;
        index = -1;
    }
    @Override
    public int compareTo(TreapKey other) {
        if (this.from != other.from) {
            return Integer.compare(this.from, other.from);
        }
        return Integer.compare(this.to, other.to);
    }
    @Override
    public String toString() {
        return "(" + from + " -> " + to + ")";
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TreapKey other = (TreapKey) obj;
        return this.from == other.from && this.to == other.to;
    }
}