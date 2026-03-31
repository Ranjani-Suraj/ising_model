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
                                     int depth, endpointType type,
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
            assertLeafNeighbors(hf, 0, d, endpointType.WITNESS,
                                Set.of(1), "testLevels leaf 0 depth " + d, result);
            assertLeafNeighbors(hf, 1, d, endpointType.WITNESS,
                                Set.of(0, 2), "testLevels leaf 1 depth " + d, result);
            assertLeafNeighbors(hf, 2, d, endpointType.WITNESS,
                                Set.of(1, 3), "testLevels leaf 2 depth " + d, result);
            assertLeafNeighbors(hf, 3, d, endpointType.WITNESS,
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
        int n = 100;
        int rounds = 1;
        int opsPerRound = 1000000;
        
        double total_time = 0.0;
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
                    double start_time = System.currentTimeMillis();
                    hf.add_edge(u, v);
                    double end_time = System.currentTimeMillis();
                    total_time += (end_time - start_time);
                    ng.insert(u, v);
                    currentEdges.add(key);

                    assertConnectivity(hf, ng, n,
                        "stress round=" + round + " op=" + op
                        + " insert(" + u + "," + v + ")", result);
                    // printForestNodes(hf);

                } else {
                    // Pick a random existing edge to delete
                    List<String> edgeList = new ArrayList<>(currentEdges);
                    String chosen = edgeList.get(rng.nextInt(edgeList.size()));
                    String[] parts = chosen.split("-");
                    int u = Integer.parseInt(parts[0]);
                    int v = Integer.parseInt(parts[1]);

                    double start_time = System.currentTimeMillis();
                    hf.delete_edge(u, v);
                    double end_time = System.currentTimeMillis();
                    total_time += (end_time - start_time);
                    ng.delete(u, v);
                    currentEdges.remove(chosen);
                    
                    assertConnectivity(hf, ng, n,
                        "stress round=" + round + " op=" + op
                        + " delete(" + u + "," + v + ")", result);
                    assertBitmapConsistency(hf, v, chosen, result);
                    assertRootCount(hf, ng, chosen, result);
                    // //assertWitnessSymmetry(hf, u, v, op, chosen, result);
                    // printForestNodes(hf);
                    if (op%500 == 0) {
                        System.out.println("Completed stress round " + op + "/" + opsPerRound + 
                        " (avg time per op: " + (total_time / (op+1)) + " ms) + " + total_time + " ms so far");
                    }
                }
            }

            // assertRootCount(hf, ng,
            //     "stress round=" + round + " final root count", result);
            
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

    
    public static HForest buildForestForTest() {
        int n = 16;
        HForest forest = HForest.emptyForTest(n);
        int dMax = forest.dMax;

        // -------------------------------------------------------
        // Step 1: Create all leaf nodes
        // -------------------------------------------------------
        for (int v = 1; v < 16; v++) {
            HNode leafNode = new HNode(n, dMax);
            leafNode.weight   = 1;
            leafNode.isRoot   = false;
            leafNode.leafData = new HLeaf(v, dMax, leafNode);
            forest.leaves[v]  = leafNode;
        }
        HNode leafNode0 = new HNode(n, dMax);
        leafNode0.weight = 1;
        leafNode0.isRoot = true;
        leafNode0.leafData = new HLeaf(0, dMax, leafNode0);
        forest.leaves[0] = leafNode0;
        forest.roots.add(leafNode0); // start with all vertices as separate roots

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
        addWitness(forest, 1, 2, 3);   // {1,2} promoted to depth 2
        addWitness(forest, 6, 7, 3);   // {6,7} promoted to depth 2
        addWitness(forest, 8, 9, 3);   // {8,9} promoted to depth 2
        addWitness(forest, 11,12, 3);  // {11,12} promoted to depth 2
        addWitness(forest, 14,15, 3);  // {14,15} promoted to depth 2

        // Depth-1 witness edges (spanning depth-2 components, within depth-1 nodes)
        addWitness(forest, 2, 3, 2);
        addWitness(forest, 3, 5, 2);
        addWitness(forest, 5, 6, 2);
        addWitness(forest, 8, 13, 2);
        addWitness(forest, 13, 10, 2);
        addWitness(forest, 9, 11, 2);

        // Primary edges (non-witness, within same component)
        addPrimary(forest, 3, 4, 2);
        addPrimary(forest, 10, 12, 2);

        // -------------------------------------------------------
        // Step 6: Recompute bitmaps bottom-up
        // -------------------------------------------------------
        for (int v = 0; v <= 15; v++) {
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
        f.leaves[u].leafData.add_edge_info(v, depth, endpointType.WITNESS);
        f.leaves[v].leafData.add_edge_info(u, depth, endpointType.WITNESS);
    }

    private static void addPrimary(HForest f, int u, int v, int depth) {
        f.leaves[u].leafData.add_edge_info(v, depth, endpointType.PRIMARY);
        f.leaves[v].leafData.add_edge_info(u, depth, endpointType.PRIMARY);
    }

    // -------------------------------------------------------------------------
    // ENTRY POINT
    // -------------------------------------------------------------------------
    public static void main(String[] args) {
        System.out.println("HForest Test Suite");
        System.out.println("==================");
        System.out.println("==================");

        TestResult result = new TestResult();

        // testBasicInsert(result);
        // testNonWitnessDelete(result);
        // testWitnessDeleteWithReplacement(result);
        // testWitnessDeleteNoReplacement(result);
        // testInsertAfterDelete(result);
        // testLeafIntegrity(result);
        // testEdgeCases(result);

        // HForest hf = buildForestForTest();
        // System.out.println("=== Initial Forest State ===");
        // printForestState(hf);
        // printForestNodes(hf);

        testStress(result);  // run stress last — noisiest output

        result.printSummary();

        // Uncomment to print state of a specific failing scenario:
        // HForest debug = new HForest(4);
        // debug.add_edge(0,1); debug.add_edge(1,2); debug.delete_edge(0,1);
        // printForestState(debug);
    }
    public static void printForestNodes(HForest hf) {
        System.out.println("=== HForest Structure ===");
        for (HNode root : hf.roots) {
            printNode(hf,root, 0);
        }
    }

    private static void printNode(HForest hf, HNode node, int indent) {
        String pad = "  ".repeat(indent);

        // Base case: leaf node
        if (node.leafData != null) {
            System.out.println(pad + "LEAF(vertex=" + node.leafData.vertex 
                + ", depth=" + node.depth + ")");
            // Print edges stored at this leaf
            for (int d = 1; d <= hf.dMax; d++) {
                if (node.leafData.isEndpoint[0][d]) {
                    for (int nb : node.leafData.witness_edges[d]) {
                        System.out.println(pad + "  witness edge to " + nb + " at depth " + d);
                    }
                }
                if (node.leafData.isEndpoint[1][d]) {
                    for (int nb : node.leafData.primary_edges[d]) {
                        System.out.println(pad + "  primary edge to " + nb + " at depth " + d);
                    }
                }
                if (node.leafData.isEndpoint[2][d]) {
                    for (int nb : node.leafData.secondary_edges[d]) {
                        System.out.println(pad + "  secondary edge to " + nb + " at depth " + d);
                    }
                }
            }
            return;
        }

        // Find witness edges at this node's depth that span between its children
        // These are the edges that "justify" this node's existence —
        // they connect the (depth+1)-components that are children of this node
        List<int[]> spanningEdges = findSpanningWitnessEdges(hf, node);

        StringBuilder edgeStr = new StringBuilder();
        if (!spanningEdges.isEmpty()) {
            edgeStr.append(" connects via: ");
            for (int[] e : spanningEdges) {
                edgeStr.append("{").append(e[0]).append(",").append(e[1]).append("} ");
            }
        }

        System.out.println(pad + "NODE(depth=" + node.depth 
            + ", weight=" + node.weight 
            + ", children=" + node.children.size() + ")"
            + edgeStr);

        for (HNode child : node.children) {
            printNode(hf, child, indent + 1);
        }
    }

    // Find all witness edges at this node's depth that span between 
    // two different children — these are what hold this component together
    private static List<int[]> findSpanningWitnessEdges(HForest hf, HNode node) {
        // Map each vertex to which direct child of this node contains it
        Map<Integer, HNode> vertexToChild = new HashMap<>();
        for (HNode child : node.children) {
            collectVertexToChild(child, child, vertexToChild);
        }

        List<int[]> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // Check every vertex under this node for witness edges at this depth
        // that cross to a vertex under a different child
        for (Map.Entry<Integer, HNode> entry : vertexToChild.entrySet()) {
            int v = entry.getKey();
            HNode childOfV = entry.getValue();
            HLeaf leaf = hf.leaves[v].leafData;

            if (leaf.isEndpoint[0][node.depth]) {
                for (int neighbor : leaf.witness_edges[node.depth]) {
                    HNode childOfNeighbor = vertexToChild.get(neighbor);
                    if (childOfNeighbor != null && childOfNeighbor != childOfV) {
                        // This edge spans two different children — it's a spanning edge
                        String key = Math.min(v, neighbor) + "-" + Math.max(v, neighbor);
                        if (seen.add(key)) {
                            result.add(new int[]{v, neighbor});
                        }
                    }
                }
            }
        }
        return result;
    }

    // Walk subtree rooted at 'node', recording which direct child of the 
    // parent contains each vertex
    private static void collectVertexToChild(HNode node, HNode directChild, 
                                    Map<Integer, HNode> result) {
        if (node.leafData != null) {
            result.put(node.leafData.vertex, directChild);
            return;
        }
        for (HNode child : node.children) {
            collectVertexToChild(child, directChild, result);
        }
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

