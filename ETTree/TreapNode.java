package ETTree;

import java.util.Random;

// public class TreapNode
// {
//     int key,priority,size;
//     TreapNode left,right,parent;
//     public TreapNode(int key)
//     {
//         this.key = key;
//         this.priority = (int)(Math.random()*100);
//         left = right = parent = null;
//         size = 1;
//     }
//     static int size(TreapNode t) {
//         return t == null ? 0 : t.size;
//     }

    

// }


public class TreapNode {
    int id;        // Euler tour edge (u → v)
    TreapKey key;
    int priority;
    int size;
    public static Random random = new Random();
    TreapNode left, right, parent;

    public TreapNode(int from, int to) {
        this.key = new TreapKey(from, to);
        this.priority = (int)(random.nextInt(100));
        this.size = 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TreapNode other = (TreapNode) obj;
        return this.key.equals(other.key);
    }
    

//     int indexOf(TreapNode node) {
//         if (this == node) {
//             return size(this.left);
//         } else if (node.key < this.key) {
//             return this.left == null ? -1 : this.left.indexOf(node);
//         } else {
//             int rightIndex = this.right == null ? -1 : this.right.indexOf(node);
//             if (rightIndex == -1) return -1;
//             return size(this.left) + 1 + rightIndex;
//         }
//     }
    

    static int size(TreapNode t) {
        return t == null ? 0 : t.size;
    }

//     void update() {
//         if (this != null) {
//             this.size = 1 + size(this.left) + size(this.right);
//             if (this.left != null) this.left.parent = this;
//             if (this.right != null) this.right.parent = this;
//         }
//     }

//     TreapNode[] split(int k) {
       
//         if (size(this.left) >= k) {
//             if (this.left == null) {
//                 return new TreapNode[]{null, this};
//             }
//             TreapNode[] res = this.left.split(k);
//             this.left = res[1];
//             update();
//             return new TreapNode[]{res[0], this};
//         } else {
//             if (this.right == null) {
//                 return new TreapNode[]{this, null};
//             }
//             TreapNode[] res = this.right.split(k - size(this.left) - 1);
//             this.right = res[0];
//             update();
//             return new TreapNode[]{this, res[1]};
//         }
//     }

//     TreapNode merge(TreapNode b) {
//         if (b == null) return this;

//         if (this.priority > b.priority) {
//             if (this.right == null) 
//                 this.right = b;
//             else 
//                 this.right = this.right.merge(b);
//             update();
//             return this;
//         } 
//         else {
            
//             b.left = this.merge(b.left);
//             b.update();
//             return b;
//         }
//     }

//     TreapNode root() {
//         TreapNode t = this;
//         while (t.parent != null) t = t.parent;
//         return t;
//     }

//     public static TreapNode rightRotate(TreapNode y) {
//         TreapNode x = y.left;
//         TreapNode T2 = x.right;

//         // Perform rotation
//         x.right = y;
//         y.left = T2;

//         // Return new root
//         return x;
//     }

//     // A utility function to left rotate subtree rooted with x
// // See the diagram given above.
//     public static TreapNode leftRotate(TreapNode x) {
//         TreapNode y = x.right;
//         TreapNode T2 = y.left;

//         // Perform rotation
//         y.left = x;
//         x.right = T2;

//         // Return new root
//         return y;
//     }




//     /* Recursive implementation of insertion in Treap */
//     public static TreapNode insert(TreapNode root, int key) {
//         // If root is null, create a new node and return it
//         if (root == null) {
//             return new TreapNode(key);
//         }

//         // If key is smaller than root
//         if (key <= root.key) {
//             // Insert in left subtree
//             root.left = insert(root.left, key);

//             // Fix Heap property if it is violated
//             if (root.left.priority > root.priority) {
//                 root = rightRotate(root);
//             }
//         } else { // If key is greater
//             // Insert in right subtree
//             root.right = insert(root.right, key);

//             // Fix Heap property if it is violated
//             if (root.right.priority > root.priority) {
//                 root = leftRotate(root);
//             }
//         }
//         return root;
//     } 
//     /* Recursive implementation of Delete() */
//     public static TreapNode deleteNode(TreapNode root, int key) {
//         if (root == null)
//             return root;

//         if (key < root.key)
//             root.left = deleteNode(root.left, key);
//         else if (key > root.key)
//             root.right = deleteNode(root.right, key);

//         // IF KEY IS AT ROOT

//         // If left is NULL
//         else if (root.left == null)
//         {
//             TreapNode temp = root.right;
//             root = temp;  // Make right child as root
//         }
//         // If Right is NULL
//         else if (root.right == null)
//         {
//             TreapNode temp = root.left;
//             root = temp;  // Make left child as root
//         }
//         // If key is at root and both left and right are not NULL
//         else if (root.left.priority < root.right.priority)
//         {
//             root = leftRotate(root);
//             root.left = deleteNode(root.left, key);
//         }
//         else
//         {
//             root = rightRotate(root);
//             root.right = deleteNode(root.right, key);
//         }

//         return root;
//     }




}
