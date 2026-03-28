package DynGraph;

import java.util.*;


// =============================================================================
// MAIN TEST CLASS
// =============================================================================
public class HForestTest {

    // -------------------------------------------------------------------------
    // ASSERTION HELPERS
    // -------------------------------------------------------------------------

    // Check that HForest and NaiveGraph agree on connectivity for all pairs
    static void assertConnectivity(HForest hf, NaiveGraph ng, int n,
                                    String context, TestResult result) {
        for (int u = 0; u < n; u++) {
            for (int v = u + 1; v < n; v++) {
                boolean hfConn  = hf.connected(u, v);
                boolean ngConn  = ng.connected(u, v);
                if (hfConn != ngConn) {
                    result.fail(context + " — connected(" + u + "," + v + "): "
                                + "HForest=" + hfConn + " NaiveGraph=" + ngConn);
                    return;  // one failure per context is enough
                }
            }
        }
        result.pass();
    }

    // Check a specific pair
    static void assertPair(HForest hf, NaiveGraph ng,
                            int u, int v, String context, TestResult result) {
        boolean hfConn = hf.connected(u, v);
        boolean ngConn = ng.connected(u, v);
        if (hfConn != ngConn) {
            result.fail(context + " — connected(" + u + "," + v + "): "
                        + "HForest=" + hfConn + " expected=" + ngConn);
            printForestState(hf);
        } else {
            result.pass();
        }
    }

    // Check that HForest leaf stores the expected neighbors at a depth/type
    static void assertLeafNeighbors(HForest hf, int vertex,
                                     int depth, EndpointType type,
                                     Set<Integer> expected,
                                     String context, TestResult result) {
        Set<Integer> actual = hf.leaves[vertex].leafData
                                  .witness_edges[depth];  // adjust per type
        switch (type) {
            case PRIMARY   -> actual = hf.leaves[vertex].leafData.primary_edges[depth];
            case SECONDARY -> actual = hf.leaves[vertex].leafData.secondary_edges[depth];
            case WITNESS   -> actual = hf.leaves[vertex].leafData.witness_edges[depth];
        }
        if (!actual.equals(expected)) {
            result.fail(context + " — leaf " + vertex
                        + " depth=" + depth + " type=" + type
                        + ": actual=" + actual + " expected=" + expected);
        } else {
            result.pass();
        }
    }

    // Check that both endpoints of a witness edge agree on each other
    static void assertWitnessSymmetry(HForest hf, int u, int v,
                                       int depth, String context, TestResult result) {
        boolean uHasV = hf.leaves[u].leafData.witness_edges[depth].contains(v);
        boolean vHasU = hf.leaves[v].leafData.witness_edges[depth].contains(u);
        if (uHasV != vHasU) {
            result.fail(context + " — witness asymmetry for {" + u + "," + v
                        + "} at depth " + depth
                        + ": u→v=" + uHasV + " v→u=" + vHasU);
        } else {
            result.pass();
        }
    }

    // Check that the root counts match the component counts
    static void assertRootCount(HForest hf, NaiveGraph ng,
                                  String context, TestResult result) {
        int expected = ng.components().size();
        int actual   = hf.roots.size();
        if (actual != expected) {
            result.fail(context + " — root count: HForest=" + actual
                        + " expected=" + expected);
            
        } else {
            result.pass();
        }
    }

    // Check isEndpoint bitmap matches actual leaf edge content
    static void assertBitmapConsistency(HForest hf, int vertex,
                                         String context, TestResult result) {
        HLeaf leaf = hf.leaves[vertex].leafData;
        for (int d = 1; d < hf.dMax; d++) {
            boolean wBit = leaf.isEndpoint[0][d];
            boolean wActual = !leaf.witness_edges[d].isEmpty();
            if (wBit != wActual) {
                result.fail(context + " — leaf " + vertex
                            + " isEndpoint[WITNESS][" + d + "]=" + wBit
                            + " but witness_edges[" + d + "].isEmpty()=" + leaf.witness_edges[d].isEmpty());
                printForestState(hf);
                return;
            }
            boolean pBit = leaf.isEndpoint[1][d];
            boolean pActual = !leaf.primary_edges[d].isEmpty();
            if (pBit != pActual) {
                result.fail(context + " — leaf " + vertex
                            + " isEndpoint[PRIMARY][" + d + "]=" + pBit
                            + " but primary_edges[" + d + "].isEmpty()=" + leaf.primary_edges[d].isEmpty());
                printForestState(hf);
                return;
            }
        }
        result.pass();
    }

    static void testLevels(TestResult result) {
        HForest hf = new HForest(4);
        hf.add_edge(0, 1);
        hf.add_edge(1, 2);
        hf.add_edge(2, 3);
        // This should create a chain of witnesses at increasing depths
        for (int d = 1; d < hf.dMax; d++) {
            assertLeafNeighbors(hf, 0, d, EndpointType.WITNESS,
                                Set.of(1), "testLevels leaf 0 depth " + d, result);
            assertLeafNeighbors(hf, 1, d, EndpointType.WITNESS,
                                Set.of(0, 2), "testLevels leaf 1 depth " + d, result);
            assertLeafNeighbors(hf, 2, d, EndpointType.WITNESS,
                                Set.of(1, 3), "testLevels leaf 2 depth " + d, result);
            assertLeafNeighbors(hf, 3, d, EndpointType.WITNESS,
                                Set.of(2), "testLevels leaf 3 depth " + d, result);
        }
    }

    // -------------------------------------------------------------------------
    // SUITE 1 — BASIC INSERT
    // -------------------------------------------------------------------------
    static void testBasicInsert(TestResult result) {
        System.out.println("\n--- Suite 1: Basic Insert ---");

        // 1a: single edge makes two vertices connected
        {
            HForest hf = new HForest(4);
            NaiveGraph ng = new NaiveGraph(4);
            hf.add_edge(0, 1); 
            ng.insert(0, 1);
            assertPair(hf, ng, 0, 1, "1a: insert(0,1) connected", result);
            assertPair(hf, ng, 0, 2, "1a: insert(0,1) unrelated pair", result);
            assertWitnessSymmetry(hf, 0, 1, 1, "1a: witness symmetry", result);
            printForestState(hf);
        }

        // 1b: edge within same component is secondary, not witness
        {
            HForest hf = new HForest(4);
            NaiveGraph ng = new NaiveGraph(4);
            hf.add_edge(0, 1); ng.insert(0, 1);
            hf.add_edge(1, 2); ng.insert(1, 2);
            hf.add_edge(0, 2); ng.insert(0, 2);  // cycle edge — should be secondary
            // HNode n = new HNode(0, 0);
            boolean isSecondary = hf.leaves[0].leafData.primary_edges[1].contains(2)
                               || hf.leaves[2].leafData.primary_edges[1].contains(0);
            if (!isSecondary) {
                result.fail("1b: cycle edge {0,2} should be primary");
                printForestState(hf);
            } else {
                result.pass();
            }
            printForestState(hf);
        }

        // 1c: chain inserts — all pairs in chain connected
        {
            int n = 5;
            HForest hf = new HForest(n);
            NaiveGraph ng = new NaiveGraph(n);
            for (int i = 0; i < n - 1; i++) {
                hf.add_edge(i, i + 1);
                ng.insert(i, i + 1);
            }
            assertConnectivity(hf, ng, n, "1c: chain", result);
            assertRootCount(hf, ng, "1c: chain root count", result);
        }

        // 1d: star topology
        {
            int n = 6;
            HForest hf = new HForest(n);
            NaiveGraph ng = new NaiveGraph(n);
            for (int i = 1; i < n; i++) {
                hf.add_edge(0, i);
                ng.insert(0, i);
            }
            assertConnectivity(hf, ng, n, "1d: star", result);
            assertRootCount(hf, ng, "1d: star root count", result);
        }

        // 1e: self-loop guard — inserting {u,u} should not crash or connect anything
        {
            HForest hf = new HForest(3);
            NaiveGraph ng = new NaiveGraph(3);
            try {
                hf.add_edge(1, 1);
                // After a self-loop, 0 and 1 should still be disconnected
                assertPair(hf, ng, 0, 1, "1e: self-loop isolation", result);
            } catch (Exception e) {
                result.fail("1e: self-loop threw exception: " + e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // SUITE 2 — BASIC DELETE (non-witness edges)
    // -------------------------------------------------------------------------
    static void testNonWitnessDelete(TestResult result) {
        System.out.println("\n--- Suite 2: Non-Witness Delete ---");

        // 2a: deleting a secondary edge changes nothing about connectivity
        {
            HForest hf = new HForest(4);
            NaiveGraph ng = new NaiveGraph(4);
            hf.add_edge(0, 1); ng.insert(0, 1);
            hf.add_edge(1, 2); ng.insert(1, 2);
            hf.add_edge(0, 2); ng.insert(0, 2); // secondary
            hf.delete_edge(0, 2); ng.delete(0, 2);
            assertConnectivity(hf, ng, 4, "2a: delete secondary edge", result);
        }

        // 2b: after deleting secondary, leaf no longer records it
        {
            HForest hf = new HForest(4);
            NaiveGraph ng = new NaiveGraph(4);
            hf.add_edge(0, 1); ng.insert(0, 1);
            hf.add_edge(1, 2); ng.insert(1, 2);
            hf.add_edge(0, 2); ng.insert(0, 2);
            hf.delete_edge(0, 2); ng.delete(0, 2);
            boolean stillHas = hf.leaves[0].leafData.secondary_edges[1].contains(2)
                            || hf.leaves[2].leafData.secondary_edges[1].contains(0);
            if (stillHas) {
                result.fail("2b: secondary edge still in leaf after delete");
                printForestState(hf);
            } else {
                result.pass();
            }
        }

        // 2c: bitmap updated after secondary delete
        {
            HForest hf = new HForest(3);
            NaiveGraph ng = new NaiveGraph(3);
            hf.add_edge(0, 1); ng.insert(0, 1);
            hf.add_edge(1, 2); ng.insert(1, 2);
            hf.add_edge(0, 2); ng.insert(0, 2);
            hf.delete_edge(0, 2); ng.delete(0, 2);
            assertBitmapConsistency(hf, 0, "2c: bitmap after secondary delete v=0", result);
            assertBitmapConsistency(hf, 2, "2c: bitmap after secondary delete v=2", result);
        }
    }

    // -------------------------------------------------------------------------
    // SUITE 3 — WITNESS DELETE WITH REPLACEMENT
    // -------------------------------------------------------------------------
    static void testWitnessDeleteWithReplacement(TestResult result) {
        System.out.println("\n--- Suite 3: Witness Delete (replacement exists) ---");

        // 3a: triangle — deleting one witness edge, another non-witness replaces it
        {
            HForest hf = new HForest(3);
            NaiveGraph ng = new NaiveGraph(3);
            hf.add_edge(0, 1); ng.insert(0, 1);
            hf.add_edge(1, 2); ng.insert(1, 2);
            hf.add_edge(0, 2); ng.insert(0, 2); // forms cycle
            // Delete one witness edge
            hf.delete_edge(0, 1); ng.delete(0, 1);
            assertConnectivity(hf, ng, 3, "3a: triangle delete", result);
            assertRootCount(hf, ng, "3a: triangle root count", result);
        }

        // 3b: 4-cycle — delete one witness edge, graph stays connected
        {
            HForest hf = new HForest(4);
            NaiveGraph ng = new NaiveGraph(4);
            hf.add_edge(0, 1); ng.insert(0, 1);
            hf.add_edge(1, 2); ng.insert(1, 2);
            hf.add_edge(2, 3); ng.insert(2, 3);
            hf.add_edge(3, 0); ng.insert(3, 0); // closes cycle
            hf.delete_edge(0, 1); ng.delete(0, 1);
            assertConnectivity(hf, ng, 4, "3b: 4-cycle delete", result);
            assertRootCount(hf, ng, "3b: 4-cycle root count", result);
        }

        // 3c: multiple cycles — delete witness, correct replacement chosen
        {
            int n = 5;
            HForest hf = new HForest(n);
            NaiveGraph ng = new NaiveGraph(n);
            // Build a complete graph on 5 vertices
            for (int u = 0; u < n; u++)
                for (int v = u + 1; v < n; v++) {
                    hf.add_edge(u, v); ng.insert(u, v);
                }
            // Delete some witness edges
            hf.delete_edge(0, 1); ng.delete(0, 1);
            printForestState(hf);
            hf.delete_edge(0, 2); ng.delete(0, 2);
            assertConnectivity(hf, ng, n, "3c: complete graph partial delete", result);
            assertRootCount(hf, ng, "3c: complete graph root count", result);
        }
    }

    // -------------------------------------------------------------------------
    // SUITE 4 — WITNESS DELETE WITHOUT REPLACEMENT (disconnection)
    // -------------------------------------------------------------------------
    static void testWitnessDeleteNoReplacement(TestResult result) {
        System.out.println("\n--- Suite 4: Witness Delete (no replacement) ---");

        // 4a: single edge — delete it, vertices disconnected
        {
            HForest hf = new HForest(3);
            NaiveGraph ng = new NaiveGraph(3);
            hf.add_edge(0, 1); ng.insert(0, 1);
            printForestState(hf);
            hf.delete_edge(0, 1); ng.delete(0, 1);
            assertPair(hf, ng, 0, 1, "4a: single edge delete", result);
            assertRootCount(hf, ng, "4a: root count after disconnect", result);
        }

        // 4b: path — remove middle edge, two components
        {
            HForest hf = new HForest(4);
            NaiveGraph ng = new NaiveGraph(4);
            hf.add_edge(0, 1); ng.insert(0, 1);
            hf.add_edge(1, 2); ng.insert(1, 2);
            hf.add_edge(2, 3); ng.insert(2, 3);
            hf.delete_edge(1, 2); ng.delete(1, 2);
            assertConnectivity(hf, ng, 4, "4b: path middle delete", result);
            assertRootCount(hf, ng, "4b: path root count", result);
        }

        // 4c: path — remove end edge
        {
            HForest hf = new HForest(4);
            NaiveGraph ng = new NaiveGraph(4);
            hf.add_edge(0, 1); ng.insert(0, 1);
            hf.add_edge(1, 2); ng.insert(1, 2);
            hf.add_edge(2, 3); ng.insert(2, 3);
            hf.delete_edge(0, 1); ng.delete(0, 1);
            assertConnectivity(hf, ng, 4, "4c: path end delete", result);
            assertRootCount(hf, ng, "4c: path root count", result);
        }

        // 4d: star — remove all edges, n components
        {
            int n = 5;
            HForest hf = new HForest(n);
            NaiveGraph ng = new NaiveGraph(n);
            for (int i = 1; i < n; i++) {
                hf.add_edge(0, i); ng.insert(0, i);
            }
            for (int i = 1; i < n; i++) {
                hf.delete_edge(0, i); ng.delete(0, i);
            }
            assertConnectivity(hf, ng, n, "4d: star all edges deleted", result);
            assertRootCount(hf, ng, "4d: star root count", result);
        }
    }

    // -------------------------------------------------------------------------
    // SUITE 5 — INSERT AFTER DELETE
    // -------------------------------------------------------------------------
    static void testInsertAfterDelete(TestResult result) {
        System.out.println("\n--- Suite 5: Insert After Delete ---");

        // 5a: insert, delete, re-insert same edge
        {
            HForest hf = new HForest(3);
            NaiveGraph ng = new NaiveGraph(3);
            hf.add_edge(0, 1); ng.insert(0, 1);
            hf.delete_edge(0, 1); ng.delete(0, 1);
            hf.add_edge(0, 1); ng.insert(0, 1);
            assertPair(hf, ng, 0, 1, "5a: re-insert same edge", result);
        }

        // 5b: insert, delete, insert different edge restoring connectivity
        {
            HForest hf = new HForest(3);
            NaiveGraph ng = new NaiveGraph(3);
            hf.add_edge(0, 1); ng.insert(0, 1);
            hf.add_edge(1, 2); ng.insert(1, 2);
            hf.delete_edge(0, 1); ng.delete(0, 1);
            assertConnectivity(hf, ng, 3, "5b: after delete mid-chain", result);
            hf.add_edge(0, 2); ng.insert(0, 2);
            assertConnectivity(hf, ng, 3, "5b: after re-connect via 0-2", result);
        }

        // 5c: build, tear down, rebuild — full connectivity check at each step
        {
            int n = 4;
            HForest hf = new HForest(n);
            NaiveGraph ng = new NaiveGraph(n);
            int[][] edges = {{0,1},{1,2},{2,3},{0,3}};
            for (int[] e : edges) { hf.add_edge(e[0],e[1]); ng.insert(e[0],e[1]); }
            assertConnectivity(hf, ng, n, "5c: built", result);
            for (int[] e : edges) { hf.delete_edge(e[0],e[1]); ng.delete(e[0],e[1]); }
            assertConnectivity(hf, ng, n, "5c: torn down", result);
            for (int[] e : edges) { hf.add_edge(e[0],e[1]); ng.insert(e[0],e[1]); }
            assertConnectivity(hf, ng, n, "5c: rebuilt", result);
        }
    }

    // -------------------------------------------------------------------------
    // SUITE 6 — LEAF DATA INTEGRITY
    // Tests that edges are stored symmetrically and bitmaps stay consistent
    // -------------------------------------------------------------------------
    static void testLeafIntegrity(TestResult result) {
        System.out.println("\n--- Suite 6: Leaf Data Integrity ---");

        // 6a: after inserting {u,v}, both leaves reference each other
        {
            HForest hf = new HForest(4);
            hf.add_edge(0, 1);
            boolean uHasV = hf.leaves[0].leafData.witness_edges[1].contains(1)
                         || hf.leaves[0].leafData.secondary_edges[1].contains(1);
            boolean vHasU = hf.leaves[1].leafData.witness_edges[1].contains(0)
                         || hf.leaves[1].leafData.secondary_edges[1].contains(0);
            if (!uHasV) 
                {result.fail("6a: leaf 0 doesn't reference vertex 1"); printForestState(hf);}
            else result.pass();
            if (!vHasU) 
                {result.fail("6a: leaf 1 doesn't reference vertex 0"); printForestState(hf);}
            else result.pass();
        }

        // 6b: after deleting {u,v}, neither leaf references the other
        {
            HForest hf = new HForest(4);
            hf.add_edge(0, 1);
            hf.add_edge(1, 2);
            hf.delete_edge(0, 1);
            boolean uHasV = false, vHasU = false;
            for (int d = 1; d < hf.dMax; d++) {
                uHasV |= hf.leaves[0].leafData.witness_edges[d].contains(1)
                       || hf.leaves[0].leafData.primary_edges[d].contains(1)
                       || hf.leaves[0].leafData.secondary_edges[d].contains(1);
                vHasU |= hf.leaves[1].leafData.witness_edges[d].contains(0)
                       || hf.leaves[1].leafData.primary_edges[d].contains(0)
                       || hf.leaves[1].leafData.secondary_edges[d].contains(0);
            }
            if (uHasV) 
                {result.fail("6b: leaf 0 still references 1 after delete"); printForestState(hf);}
            else result.pass();
            if (vHasU) 
                {result.fail("6b: leaf 1 still references 0 after delete"); printForestState(hf);}
            else result.pass();
        }

        // 6c: bitmaps match actual edge sets across all vertices
        {
            int n = 5;
            HForest hf = new HForest(n);
            hf.add_edge(0, 1); hf.add_edge(1, 2);
            hf.add_edge(2, 3); hf.add_edge(0, 2); // 0-2 is secondary
            hf.delete_edge(1, 2);
            for (int v = 0; v < n; v++)
                assertBitmapConsistency(hf, v, "6c: bitmap v=" + v, result);
        }
    }

    // -------------------------------------------------------------------------
    // SUITE 7 — STRESS TEST (randomized)
    // -------------------------------------------------------------------------
    static void testStress(TestResult result) {
        System.out.println("\n--- Suite 7: Stress Test ---");

        Random rng = new Random(42); // fixed seed for reproducibility
        int n = 8;
        int rounds = 3;
        int opsPerRound = 90;

        for (int round = 0; round < rounds; round++) {
            HForest hf = new HForest(n);
            NaiveGraph ng = new NaiveGraph(n);
            Set<String> currentEdges = new HashSet<>(); // track live edges

            for (int op = 0; op < opsPerRound; op++) {
                boolean doInsert = currentEdges.isEmpty() || rng.nextBoolean();

                if (doInsert) {
                    int u = rng.nextInt(n);
                    int v = rng.nextInt(n);
                    if (u == v) continue;

                    String key = Math.min(u,v) + "-" + Math.max(u,v);
                    if (currentEdges.contains(key)) continue; // no multi-edges

                    hf.add_edge(u, v);
                    ng.insert(u, v);
                    currentEdges.add(key);

                    assertConnectivity(hf, ng, n,
                        "stress round=" + round + " op=" + op
                        + " insert(" + u + "," + v + ")", result);

                } else {
                    // Pick a random existing edge to delete
                    List<String> edgeList = new ArrayList<>(currentEdges);
                    String chosen = edgeList.get(rng.nextInt(edgeList.size()));
                    String[] parts = chosen.split("-");
                    int u = Integer.parseInt(parts[0]);
                    int v = Integer.parseInt(parts[1]);

                    hf.delete_edge(u, v);
                    ng.delete(u, v);
                    currentEdges.remove(chosen);
                    //failed round 0 operation 29
                    assertConnectivity(hf, ng, n,
                        "stress round=" + round + " op=" + op
                        + " delete(" + u + "," + v + ")", result);
                    assertBitmapConsistency(hf, v, chosen, result);
                    assertRootCount(hf, ng, chosen, result);
                    //assertWitnessSymmetry(hf, u, v, op, chosen, result);
                }
            }

            assertRootCount(hf, ng,
                "stress round=" + round + " final root count", result);
        }
    }

    // -------------------------------------------------------------------------
    // SUITE 8 — EDGE CASES
    // -------------------------------------------------------------------------
    static void testEdgeCases(TestResult result) {
        System.out.println("\n--- Suite 8: Edge Cases ---");

        // 8a: n=1, no edges
        {
            HForest hf = new HForest(1);
            NaiveGraph ng = new NaiveGraph(1);
            assertRootCount(hf, ng, "8a: n=1 root count", result);
        }

        // 8b: n=2, insert then delete
        {
            HForest hf = new HForest(2);
            NaiveGraph ng = new NaiveGraph(2);
            hf.add_edge(0, 1); ng.insert(0, 1);
            assertPair(hf, ng, 0, 1, "8b: n=2 connected", result);
            hf.delete_edge(0, 1); ng.delete(0, 1);
            assertPair(hf, ng, 0, 1, "8b: n=2 disconnected", result);
        }

        // 8c: delete non-existent edge — should not crash or corrupt state
        {
            HForest hf = new HForest(4);
            NaiveGraph ng = new NaiveGraph(4);
            hf.add_edge(0, 1); ng.insert(0, 1);
            try {
                hf.delete_edge(0, 2); // 0-2 doesn't exist
                assertConnectivity(hf, ng, 4, "8c: delete non-existent edge", result);
            } catch (Exception e) {
                result.fail("8c: delete non-existent edge threw: " + e.getMessage());
            }
        }

        // 8d: duplicate insert — should not corrupt state
        {
            HForest hf = new HForest(3);
            NaiveGraph ng = new NaiveGraph(3);
            hf.add_edge(0, 1); ng.insert(0, 1);
            try {
                hf.add_edge(0, 1); // duplicate
                assertPair(hf, ng, 0, 1, "8d: duplicate insert connected", result);
            } catch (Exception e) {
                result.fail("8d: duplicate insert threw: " + e.getMessage());
            }
        }

        // 8e: many sequential inserts on same vertex pair, then delete
        {
            HForest hf = new HForest(4);
            NaiveGraph ng = new NaiveGraph(4);
            hf.add_edge(0, 1); ng.insert(0, 1);
            hf.add_edge(0, 2); ng.insert(0, 2);
            hf.add_edge(0, 3); ng.insert(0, 3);
            hf.delete_edge(0, 1); ng.delete(0, 1);
            hf.delete_edge(0, 2); ng.delete(0, 2);
            assertConnectivity(hf, ng, 4, "8e: sequential deletes", result);
        }
    }

    // -------------------------------------------------------------------------
    // DIAGNOSTIC PRINTER
    // Print the full state of the HForest for a small graph — use when
    // a test fails and you need to see exactly what went wrong
    // -------------------------------------------------------------------------
    static void printForestState(HForest hf) {
        System.out.println("\n=== HForest State ===");
        System.out.println("n=" + hf.n + " dMax=" + hf.dMax
                           + " roots=" + hf.roots.size());

        System.out.println("Leaves:");
        for (int v = 0; v < hf.n; v++) {
            HLeaf leaf = hf.leaves[v].leafData;
            System.out.print("  vertex " + v + ": ");
            for (int d = 1; d < hf.dMax; d++) {
                if (!leaf.witness_edges[d].isEmpty())
                    System.out.print("W@" + d + "=" + leaf.witness_edges[d] + " ");
                if (!leaf.primary_edges[d].isEmpty())
                    System.out.print("P@" + d + "=" + leaf.primary_edges[d] + " ");
                if (!leaf.secondary_edges[d].isEmpty())
                    System.out.print("S@" + d + "=" + leaf.secondary_edges[d] + " ");
            }
            System.out.println();
        }

        System.out.println("Roots:");
        for (HNode root : hf.roots) {
            System.out.println("  HNode#" + root.depth
                               + " weight=" + root.weight
                               + " children=" + root.children.size());
        }
        System.out.println("===================");
    }

    
    // public static HForest buildForestForTest(){
    //     HForest forest = new HForest(16);
        
    //     forest.add_edge(1, 2);
    //     forest.add_edge(2, 3);
    //     forest.add_edge(3, 5);
    //     forest.add_edge(4, 5);
    //     forest.add_edge(6, 5);
    //     forest.add_edge(6, 7);
    //     forest.add_edge(3, 4);
    //     forest.add_edge(1, 6);
    //     forest.add_edge(1, 7);
    //     forest.add_edge(8, 9);
    //     forest.add_edge(11, 12);
    //     forest.add_edge(8, 13);
    //     forest.add_edge(13, 10);
    //     forest.add_edge(11, 9);
    //     forest.add_edge(11, 12);
    //     forest.add_edge(13, 14);
    //     forest.add_edge(14, 15);
    //     forest.add_edge(12, 15);
    //     forest.add_edge(5, 8);
    //     forest.add_edge(6, 9);
    //     //----
    //     forest.leaves[1].leafData.promote_witness_edge(2, 1);
    //     forest.leaves[1].leafData.promote_witness_edge(2, 2);
    //     //--
    //     forest.leaves[2].leafData.promote_witness_edge(3, 1);
    //     //---
    //     forest.leaves[3].leafData.promote_witness_edge(5, 1);
    //     forest.leaves[5].leafData.promote_witness_edge(6, 1);
    //     //--
    //     forest.leaves[6].leafData.promote_witness_edge(7, 1);
    //     forest.leaves[6].leafData.promote_witness_edge(7, 2);
    //     //--
    //     forest.leaves[8].leafData.promote_witness_edge(9, 1);
    //     forest.leaves[8].leafData.promote_witness_edge(9, 2);
    //     //
    //     forest.leaves[8].leafData.promote_witness_edge(13, 1);
    //     forest.leaves[13].leafData.promote_witness_edge(10, 1);
    //     forest.leaves[9].leafData.promote_witness_edge(11, 1);
    //     //
    //     forest.leaves[11].leafData.promote_witness_edge(12, 1);
    //     forest.leaves[11].leafData.promote_witness_edge(12, 2);
    //     forest.leaves[15].leafData.promote_witness_edge(14, 1);
    //     forest.leaves[15].leafData.promote_witness_edge(14, 2);
    //     //Now for the other directions of ALL OF THESE
    //     forest.leaves[2].leafData.promote_witness_edge(1, 1);
    //     forest.leaves[2].leafData.promote_witness_edge(1, 2);
    //     //--
    //     forest.leaves[3].leafData.promote_witness_edge(2, 1);
    //     //---
    //     forest.leaves[5].leafData.promote_witness_edge(3, 1);
    //     forest.leaves[6].leafData.promote_witness_edge(5, 1);
    //     //--
    //     forest.leaves[7].leafData.promote_witness_edge(6, 1);
    //     forest.leaves[7].leafData.promote_witness_edge(6, 2);
    //     //--
    //     forest.leaves[9].leafData.promote_witness_edge(8, 1);
    //     forest.leaves[9].leafData.promote_witness_edge(8, 2);
    //     //
    //     forest.leaves[13].leafData.promote_witness_edge(8, 1);
    //     forest.leaves[10].leafData.promote_witness_edge(13, 1);
    //     forest.leaves[11].leafData.promote_witness_edge(9, 1);
    //     //
    //     forest.leaves[12].leafData.promote_witness_edge(11, 1);
    //     forest.leaves[12].leafData.promote_witness_edge(11, 2);
    //     forest.leaves[14].leafData.promote_witness_edge(15, 1);
    //     forest.leaves[14].leafData.promote_witness_edge(15, 2);
    //     //////
    //     /// Now for primary edges
    //     /// 
    //     forest.leaves[3].leafData.promote_primary_edge(4, 1);
    //     forest.leaves[4].leafData.promote_primary_edge(3, 1);
    //     forest.leaves[10].leafData.promote_primary_edge(12, 1);
    //     forest.leaves[12].leafData.promote_primary_edge(10, 1);
    //     ///
    //     /// Now to create the node representations
    //     /// 
    //     HNode _1_2 = new HNode(16, 3);
    //     forest.leaves[1].leafData.node = _1_2;
    //     forest.leaves[2].leafData.node = _1_2;
    //     _1_2.children.add(forest.leaves[1]);
    //     _1_2.children.add(forest.leaves[2]);
    //     forest.leaves[1].parent = _1_2;
    //     forest.leaves[2].parent = _1_2;
    //     HNode _3 = new HNode(16, 3);
    //     forest.leaves[3].leafData.node = _3;
    //     _3.children.add(forest.leaves[3]);
    //     forest.leaves[3].parent = _3;
    //     HNode _4_5 = new HNode(16, 3);
    //     forest.leaves[4].leafData.node = _4_5;
    //     _4_5.children.add(forest.leaves[4]);
    //     forest.leaves[4].parent = _4_5;
    //     forest.leaves[5].leafData.node = _4_5;
    //     _4_5.children.add(forest.leaves[5]);
    //     forest.leaves[5].parent = _4_5;
    //     HNode _6_7 = new HNode(16, 3);
    //     forest.leaves[6].leafData.node = _6_7;
    //     _6_7.children.add(forest.leaves[6]);
    //     forest.leaves[6].parent = _6_7;
    //     forest.leaves[7].leafData.node = _6_7;
    //     _6_7.children.add(forest.leaves[7]);
    //     forest.leaves[7].parent = _6_7;
    //     HNode _8_9 = new HNode(16, 3);
    //     /////
    //     forest.leaves[8].leafData.node = _8_9;
    //     _8_9.children.add(forest.leaves[8]);
    //     forest.leaves[8].parent = _8_9;
    //     forest.leaves[9].leafData.node = _8_9;
    //     _8_9.children.add(forest.leaves[9]);
    //     forest.leaves[9].parent = _8_9;
    //     HNode _10 = new HNode(16, 3);
    //     forest.leaves[10].leafData.node = _10;
    //     _10.children.add(forest.leaves[10]);
    //     forest.leaves[10].parent = _10;
    //     HNode _11_12 = new HNode(16, 3);
    //     forest.leaves[11].leafData.node = _11_12;
    //     _11_12.children.add(forest.leaves[11]);
    //     forest.leaves[11].parent = _11_12;
    //     forest.leaves[12].leafData.node = _11_12;
    //     _11_12.children.add(forest.leaves[12]);
    //     forest.leaves[12].parent = _11_12;
    //     HNode _13 = new HNode(16, 3);
    //     forest.leaves[13].leafData.node = _13;
    //     _13.children.add(forest.leaves[13]);
    //     forest.leaves[13].parent = _13;
    //     ///
    //     HNode _14_15 = new HNode(16, 3);
    //     forest.leaves[14].leafData.node = _14_15;
    //     _14_15.children.add(forest.leaves[14]);
    //     forest.leaves[14].parent = _14_15;
    //     forest.leaves[15].leafData.node = _14_15;
    //     _14_15.children.add(forest.leaves[15]);
    //     forest.leaves[15].parent = _14_15;
    //     ///Now for level 2 nodes
    //     HNode _1_2_3_4_5_6_7 = new HNode(16, 2);
    //     _1_2_3_4_5_6_7.children.add(_1_2);
    //     _1_2.parent = _1_2_3_4_5_6_7;
    //     _1_2_3_4_5_6_7.children.add(_3);
    //     _3.parent = _1_2_3_4_5_6_7;
    //     _1_2_3_4_5_6_7.children.add(_4_5);
    //     _4_5.parent = _1_2_3_4_5_6_7;
    //     _1_2_3_4_5_6_7.children.add(_6_7);
    //     _6_7.parent = _1_2_3_4_5_6_7;
    //     HNode _8_9_10_11_12_13 = new HNode(16, 2);
    //     _8_9_10_11_12_13.children.add(_8_9);
    //     _8_9.parent = _8_9_10_11_12_13;
    //     _8_9_10_11_12_13.children.add(_10); 
    //     _10.parent = _8_9_10_11_12_13;
    //     _8_9_10_11_12_13.children.add(_11_12);
    //     _11_12.parent = _8_9_10_11_12_13;
    //     _8_9_10_11_12_13.children.add(_13); 
    //     _13.parent = _8_9_10_11_12_13;
    //     HNode _2_14_15 = new HNode(16, 2);
    //     _2_14_15.children.add(_14_15);
    //     _14_15.parent = _2_14_15;
    //     //Now the root level 1 node
    //     HNode _root = new HNode(16, 1);
    //     _root.children.add(_1_2_3_4_5_6_7);
    //     _1_2_3_4_5_6_7.parent = _root;
    //     _root.children.add(_8_9_10_11_12_13);   
    //     _8_9_10_11_12_13.parent = _root;
    //     _root.children.add(_2_14_15);
    //     _2_14_15.parent = _root;
    //     _root.isRoot = true;
    //     /////
    //     /// Now we need to recompute the bitmaps for all of these nodes, starting from the bottom up.
    //     _1_2.recomputeBitmap();
    //     _3.recomputeBitmap();
    //     _4_5.recomputeBitmap();
    //     _6_7.recomputeBitmap();
    //     _8_9.recomputeBitmap();
    //     _10.recomputeBitmap();
    //     _11_12.recomputeBitmap();
    //     _13.recomputeBitmap();
    //     _14_15.recomputeBitmap();
    //     _1_2_3_4_5_6_7.recomputeBitmap();
    //     _8_9_10_11_12_13.recomputeBitmap();
    //     _2_14_15.recomputeBitmap();
    //     _root.recomputeBitmap();
    //     /////Now to remove all the leaves as roots and add the main root
    //     for (HNode root : forest.roots){
    //         if (root.leafData != null && root.leafData.vertex != 0){
    //             root.isRoot = false;
    //             forest.roots.remove(root);
    //             root.depth = 3;
    //         }
    //     }
    //     forest.roots.add(_root);
    //     return forest;
        
    // }

    public static HForest buildForestForTest() {
        int n = 16;
        HForest forest = HForest.emptyForTest(n);
        int dMax = forest.dMax;

        // -------------------------------------------------------
        // Step 1: Create all leaf nodes
        // -------------------------------------------------------
        for (int v = 1; v <= 15; v++) {
            HNode leafNode = new HNode(n, dMax);
            leafNode.weight   = 1;
            leafNode.isRoot   = false;
            leafNode.leafData = new HLeaf(v, dMax, leafNode);
            forest.leaves[v]  = leafNode;
        }

        // -------------------------------------------------------
        // Step 2: Create depth-2 (i+1 component) nodes
        // These group vertices that share depth-2 witness edges
        // -------------------------------------------------------
        HNode _1_2   = makeInternalNode(n, 3, 2, forest.leaves[1], forest.leaves[2]);
        HNode _3     = makeInternalNode(n, 3, 1, forest.leaves[3]);
        HNode _4_5   = makeInternalNode(n, 3, 2, forest.leaves[4], forest.leaves[5]);
        HNode _6_7   = makeInternalNode(n, 3, 2, forest.leaves[6], forest.leaves[7]);
        HNode _8_9   = makeInternalNode(n, 3, 2, forest.leaves[8], forest.leaves[9]);
        HNode _10    = makeInternalNode(n, 3, 1, forest.leaves[10]);
        HNode _11_12 = makeInternalNode(n, 3, 2, forest.leaves[11], forest.leaves[12]);
        HNode _13    = makeInternalNode(n, 3, 1, forest.leaves[13]);
        HNode _14_15 = makeInternalNode(n, 3, 2, forest.leaves[14], forest.leaves[15]);

        // -------------------------------------------------------
        // Step 3: Create depth-1 (component) nodes
        // -------------------------------------------------------
        HNode _left  = makeInternalNode(n, 2, 7,  _1_2, _3, _4_5, _6_7);
        HNode _mid   = makeInternalNode(n, 2, 7,  _8_9, _10, _11_12, _13);
        HNode _right = makeInternalNode(n, 2, 2,  _14_15);

        // -------------------------------------------------------
        // Step 4: Create depth-0 root
        // -------------------------------------------------------
        HNode root = makeInternalNode(n, 1, 16, _left, _mid, _right);
        root.isRoot = true;
        forest.roots.add(root);

        // -------------------------------------------------------
        // Step 5: Register edge info at leaves
        // All edges at the leaf level — depth is the edge depth, 
        // not the node depth
        // -------------------------------------------------------
        // Depth-1 witness edges (within depth-2 nodes)
        addWitness(forest, 1, 2, 2);   // {1,2} promoted to depth 2
        addWitness(forest, 6, 7, 2);   // {6,7} promoted to depth 2
        addWitness(forest, 8, 9, 2);   // {8,9} promoted to depth 2
        addWitness(forest, 11,12, 2);  // {11,12} promoted to depth 2
        addWitness(forest, 14,15, 2);  // {14,15} promoted to depth 2

        // Depth-1 witness edges (spanning depth-2 components, within depth-1 nodes)
        addWitness(forest, 2, 3, 1);
        addWitness(forest, 3, 5, 1);
        addWitness(forest, 5, 6, 1);
        addWitness(forest, 8, 13, 1);
        addWitness(forest, 13, 10, 1);
        addWitness(forest, 9, 11, 1);

        // Primary edges (non-witness, within same component)
        addPrimary(forest, 3, 4, 1);
        addPrimary(forest, 10, 12, 1);

        // -------------------------------------------------------
        // Step 6: Recompute bitmaps bottom-up
        // -------------------------------------------------------
        for (int v = 1; v <= 15; v++) {
            forest.leaves[v].recomputeBitmapsUp();
        }

        return forest;
    }

    // -------------------------------------------------------
    // Helpers
    // -------------------------------------------------------
    private static HNode makeInternalNode(int n, int depth, int weight, HNode... children) {
        HNode node = new HNode(n, depth);
        node.weight = weight;
        node.isRoot = false;
        for (HNode child : children) {
            child.parent = node;
            node.children.add(child);
        }
        node.recomputeBitmap();
        return node;
    }

    private static void addWitness(HForest f, int u, int v, int depth) {
        f.leaves[u].leafData.add_edge_info(v, depth, EndpointType.WITNESS);
        f.leaves[v].leafData.add_edge_info(u, depth, EndpointType.WITNESS);
    }

    private static void addPrimary(HForest f, int u, int v, int depth) {
        f.leaves[u].leafData.add_edge_info(v, depth, EndpointType.PRIMARY);
        f.leaves[v].leafData.add_edge_info(u, depth, EndpointType.PRIMARY);
    }

    // -------------------------------------------------------------------------
    // ENTRY POINT
    // -------------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("HForest Test Suite");
        System.out.println("==================");

        TestResult result = new TestResult();

        // testBasicInsert(result);
        // testNonWitnessDelete(result);
        // testWitnessDeleteWithReplacement(result);
        // testWitnessDeleteNoReplacement(result);
        // testInsertAfterDelete(result);
        // testLeafIntegrity(result);
        // testEdgeCases(result);

        HForest hf = buildForestForTest();
        printForestState(hf);


        //testStress(result);  // run stress last — noisiest output

        result.printSummary();

        // Uncomment to print state of a specific failing scenario:
        // HForest debug = new HForest(4);
        // debug.add_edge(0,1); debug.add_edge(1,2); debug.delete_edge(0,1);
        // printForestState(debug);
    }
}

// =============================================================================
// NAIVE REFERENCE IMPLEMENTATION
// Simple union-find + adjacency list. This is the ground truth.
// Every test compares HForest output against this.
// =============================================================================
class NaiveGraph {
    private final int n;
    private final Map<Integer, Set<Integer>> adj = new HashMap<>();

    public NaiveGraph(int n) {
        this.n = n;
        for (int i = 0; i < n; i++) adj.put(i, new HashSet<>());
    }

    public void insert(int u, int v) {
        adj.get(u).add(v);
        adj.get(v).add(u);
    }

    public void delete(int u, int v) {
        adj.get(u).remove(v);
        adj.get(v).remove(u);
    }

    public boolean connected(int u, int v) {
        if (u == v) return true;
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        queue.add(u);
        visited.add(u);
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            if (cur == v) return true;
            for (int nb : adj.get(cur))
                if (!visited.contains(nb)) { visited.add(nb); queue.add(nb); }
        }
        return false;
    }

    // Returns all connected components as sets of vertex IDs
    public List<Set<Integer>> components() {
        Set<Integer> visited = new HashSet<>();
        List<Set<Integer>> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!visited.contains(i)) {
                Set<Integer> comp = new HashSet<>();
                Queue<Integer> q = new LinkedList<>();
                q.add(i); visited.add(i); comp.add(i);
                while (!q.isEmpty()) {
                    int cur = q.poll();
                    for (int nb : adj.get(cur))
                        if (!visited.contains(nb)) {
                            visited.add(nb); comp.add(nb); q.add(nb);
                        }
                }
                result.add(comp);
            }
        }
        return result;
    }
}

// =============================================================================
// TEST RESULT TRACKING
// =============================================================================
class TestResult {
    int passed = 0, failed = 0;
    List<String> failures = new ArrayList<>();

    void pass() { passed++; }

    void fail(String msg) {
        failed++;
        failures.add(msg);
        System.out.println("  FAIL: " + msg);
    }

    void printSummary() {
        System.out.println("\n========================================");
        System.out.println("RESULTS: " + passed + " passed, " + failed + " failed");
        if (!failures.isEmpty()) {
            System.out.println("FAILURES:");
            failures.forEach(f -> System.out.println("  - " + f));
        }
        System.out.println("========================================");
    }
}

