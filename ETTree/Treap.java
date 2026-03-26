
package ETTree;

import java.util.ArrayList;

/* T1, T2 and T3 are subtrees of the tree rooted with y
  (on left side) or x (on right side)
                y                               x
               / \     Right Rotation          /  \
              x   T3   – – – – – – – >        T1   y
             / \       < - - - - - - -            / \
            T1  T2     Left Rotation            T2  T3 */

// A utility function to right rotate subtree rooted with y
// See the diagram given above.
public class Treap
{
    
    public static TreapNode root(TreapNode x) {
        if (x == null) return null;
        TreapNode t = x;
        while (t.parent != null) t = t.parent;
        return t;
    }
    

    public static TreapNode rightRotate(TreapNode y) {
        TreapNode x = y.left;
        TreapNode T2 = x.right;

        x.right = y;
        y.left = T2;

        if (T2 != null) T2.parent = y;

        x.parent = y.parent;
        y.parent = x;

        update(y);
        update(x);

        return x;
    }


    // A utility function to left rotate subtree rooted with x
// See the diagram given above.
    public static TreapNode leftRotate(TreapNode x) {
        TreapNode y = x.right;
        TreapNode T2 = y.left;

        y.left = x;
        x.right = T2;

        if (T2 != null) T2.parent = x;

        y.parent = x.parent;
        x.parent = y;

        update(x);
        update(y);

        return y;
    }


    public static int size(TreapNode node) {
        return (node == null) ? 0 : node.size;
    }

    public static int getIndex(TreapNode node) {
        int index = size(node.left); // nodes in left subtree
        TreapNode current = node;
        while (current.parent != null) {
            if (current == current.parent.right) {
                // add left subtree + parent itself
                index += size(current.parent.left) + 1;
            }
            current = current.parent;
        }
        node.key.index = index;
        return index;
    }

    public static boolean contains(TreapNode root, TreapKey key) {
        if (root == null) return false;
        if (root.key.equals(key)) return true;
        if (key.compareTo(root.key) < 0) {
            return contains(root.left, key);
        } else {
            return contains(root.right, key);
        }
    }
 
    public static boolean contains(TreapNode root, int x){
         if (root == null) return false;
        if (root.key.from == x || root.key.to == x) return true;
        if (x < root.key.from) {
            return contains(root.left, x);
        } else {
            return contains(root.right, x);
        }
    }

    public static ArrayList<TreapNode> inorderToList(TreapNode root, ArrayList<TreapNode> list) {
        if (root == null) 
            return list;
        inorderToList(root.left, list);
        list.add(root);
        inorderToList(root.right, list);
        return list;
    }

    public static TreapNode reroot(TreapNode v) {
        TreapNode root = Treap.root(v);
        int idx = getIndex(v);
        TreapNode[] parts = Treap.split(root, idx);
        TreapNode newRoot = Treap.merge(parts[1], parts[0]);
        newRoot.parent = null;
        return newRoot;
    }

    public static TreapNode getNode(int from, int to, TreapNode v){
        TreapNode root = Treap.root(v);
        TreapNode current = root;
        while (current != null) {
            if (current.key.from == from && current.key.to == to) {
                return current;
            } else if (new TreapKey(from, to).compareTo(current.key) < 0) {
                current = current.left;
            } else {
                current = current.right;
            }
        }
        return null;
    }
    // intntion is to remove the u-v and v-u nodes from the treap, given that we have removed this edge. 
    // here, the resulting treap will have these edges as extrema in the inorder traversal, so we can just assume their indices are 0 and len-1
    public static TreapNode delete_root_and_leaf(TreapNode v) {
        TreapNode[] split1 = split(v, 1);
        int len = size(split1[1]);
        TreapNode[] split2 = split(split1[1], len - 1);
        if (split2[0] != null) split2[0].parent = null;
        return split2[0];
    }


    public static ArrayList<Integer> getAllNodes(TreapNode root) {
        ArrayList<Integer> nodes = new ArrayList<>();
        inorderCollect(root, nodes);
        return nodes;
    }
    private static void inorderCollect(TreapNode node, ArrayList<Integer> nodes) {
        if (node != null) {
            inorderCollect(node.left, nodes);
            nodes.add(node.key.from);
            inorderCollect(node.right, nodes);
        }
    }

    static void update(TreapNode t) {
        if (t != null) {
            t.size = 1 + TreapNode.size(t.left) + TreapNode.size(t.right);
            if (t.left != null) t.left.parent = t;
            if (t.right != null) t.right.parent = t;
        }
    }

    // static Treap merge_treaps(Treap a, Treap b) {
    //     Treap res = new Treap();
    //     res.root = merge(a.root, b.root);
    //     return res;
    // }

    public static TreapNode merge(TreapNode a, TreapNode b) {
        // reroot(a);
        // reroot(b);
        TreapNode merged = merge_(a, b);
        if (merged != null) {
            merged.parent = null;
        }
        return merged;
    }

    public static TreapNode merge_(TreapNode a, TreapNode b) {
        if (a == null) return b;
        if (b == null) return a;
        
        if (a.priority > b.priority) {
            a.right = merge_(a.right, b);
            update(a);
            return a;
        } else {
            b.left = merge_(a, b.left);
            update(b);
            return b;
        }
    }

    public static TreapNode[] split_node(TreapNode t, TreapNode v){
        int k = getIndex(v);
        return split(t, k);
    }

    public static TreapNode[] split(TreapNode t, int k) {
        if (t == null) {
            return new TreapNode[]{null, null};
        }

        if (TreapNode.size(t.left) >= k) {
            TreapNode[] leftSplit = split(t.left, k);

            t.left = leftSplit[1];
            if (t.left != null) t.left.parent = t;

            update(t);

            if (leftSplit[0] != null) leftSplit[0].parent = null;

            return new TreapNode[]{leftSplit[0], t};
        } else {
            TreapNode[] rightSplit =
                split(t.right, k - TreapNode.size(t.left) - 1);

            t.right = rightSplit[0];
            if (t.right != null) t.right.parent = t;

            update(t);

            if (rightSplit[1] != null) rightSplit[1].parent = null;

            return new TreapNode[]{t, rightSplit[1]};
        }
    }

    public static TreapNode[] peekSplit(TreapNode t, int k) {  //simulates teh result of a split without actually changing anything in the treap
        if (t == null) {
            return new TreapNode[]{null, null};
        }

        if (TreapNode.size(t.left) >= k) {
            TreapNode[] leftSplit = peekSplit(t.left, k);
            return new TreapNode[]{leftSplit[0], t};
        } else {
            TreapNode[] rightSplit =
                peekSplit(t.right, k - TreapNode.size(t.left) - 1);
            return new TreapNode[]{t, rightSplit[1]};
        }
    }


    
    // Java function to search a given key in a given BST
    // public static TreapNode search(TreapNode root, int key)
    // {
    //     // Base Cases: root is null or key is present at root
    //     if (root == null || root.id == key)
    //         return root;

    //     // Key is greater than root's key
    //     if (root.id < key)
    //         return search(root.right, key);

    //     // Key is smaller than root's key
    //     return search(root.left, key);
    // }
    public static void inorder(TreapNode root)
    {   if (root == null) return;
        if (root != null)
        {
            if (root.left != null)
                inorder(root.left);
            System.out.print("key: " + root.key + " | priority: " + root.priority);
            if (root.left != null)
                System.out.print(" | left child: " + root.left.key);
            if (root.right != null)
                System.out.print(" | right child: " + root.right.key);
            System.out.println();
            if (root.right != null)
                inorder(root.right);
        }
    }

    public static TreapNode peekMerge(TreapNode a, TreapNode b) {
        // reroot(a);
        // reroot(b);
        TreapNode merged = peekMerge_(a, b);
        return merged;
    }

    public static TreapNode peekMerge_(TreapNode a, TreapNode b) {
        if (a == null) return b;
        if (b == null) return a;
        
        if (a.priority > b.priority) {
            a.right = peekMerge_(a.right, b);
            return a;
        } else {
            b.left = peekMerge_(a, b.left);
            return b;
        }
    }
    
}