package Dynamic_Graph;

import java.util.Random;

/** A vertex in a ConnGraph. See the comments for ConnGraph. */
public class ConnVertex {
    /** The thread-local random number generator we use by default to set the "hash" field. */
    int value;
    private static final ThreadLocal<Random> random = new ThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            return new Random();  //see, why am i doing this
            //ok we;re getting a thread local random number generator so that we can use it to generate a random hash code for ConnVertex
            //why do we need it to be thread local
            //
            
        }
    };

    /**
     * A randomly generated integer to use as the return value of hashCode(). ConnGraph relies on random hash codes for
     * its performance guarantees.
     */
    private final int hash;

    public ConnVertex() {
        hash = random.get().nextInt();
    }
    public ConnVertex(int value) {
        this.value = value;
        hash = random.get().nextInt();
    }

    /**
     * Constructs a new ConnVertex.
     * @param random The random number generator to use to produce a random hash code. ConnGraph relies on random hash
     *     codes for its performance guarantees.
     */
    public ConnVertex(Random random) {
        hash = random.nextInt();
    }

    @Override
    public String toString() {
        return "ConnVertex(" + value + ")";
    }

    @Override
    public int hashCode() {
        return hash;
    }
}
