package Dynamic_Graph;


public class Splay_bst {
	static Node newNode(int name) {
        Node node = new Node(name);
        node.left = node.right = node.parent = null;
        return node;
    }

    static Node rightRotate(Node x) {
        Node y = x.left;
        //Node par = x.parent;
        x.left = y.right;
        y.right = x;
        return y;
    }

    static Node leftRotate(Node x) {
        Node y = x.right;
        x.right = y.left;
        y.left = x;
        return y;
    }

    static Node splay(Node root, int name) {
        if (root == null || root.name == name)
            return root;

        if (root.name > name) {
            if (root.left == null)
                return root;
            if (root.left.name > name) {
                root.left.left = splay(root.left.left, name);
                root = rightRotate(root);
            }
            else if (root.left.name < name) {
                root.left.right = splay(root.left.right, name);
                if (root.left.right != null)
                    root.left = leftRotate(root.left);
            }
            return (root.left == null) ? root : rightRotate(root);
        }
        else {
            if (root.right == null)
                return root;
            if (root.right.name > name) {
                root.right.left = splay(root.right.left, name);
                if (root.right.left != null)
                    root.right = rightRotate(root.right);
            }
            else if (root.right.name < name) {
                root.right.right = splay(root.right.right, name);
                root = leftRotate(root);
            }
            return (root.right == null) ? root : leftRotate(root);
        }
    }

    

    static Node insert(Node root, int key) {
        if (root == null)
            return newNode(key);

        root = splay(root, key);

        if (root.name == key)
            return root;

        Node node = newNode(key);
        if (root.name > key) {
            node.right = root;
            node.left = root.left;
            root.left = null;
        }
        else {
            node.left = root;
            node.right = root.right;
            root.right = null;
        }
        return node;
    }

    static void preOrder(Node node) {
        if (node != null) {
            System.out.println();
            System.out.print(node.name + " ");
            preOrder(node.left);
            preOrder(node.right);
        }
    }
}
