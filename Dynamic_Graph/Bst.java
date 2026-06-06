package Dynamic_Graph;
// import java.util.ArrayList;
// import java.util.*;
public class Bst {
    //ArrayList<Node> current_tour;

    static void rotate_right(Node u){
        Node par = u.parent;
        Node gpar = par!=null? par.parent : null;
        Node rchild = u.right;
        u.parent = gpar;
        if(gpar != null){
            if(par == gpar.left)
                gpar.left = u;
            else
                gpar.right = u;
        }
        u.right = par;
        par.parent = u;
        par.left = rchild;
        if(rchild != null){
            rchild.parent = par;
        }
        par.update();
        u.update();
    }

    static void rotate_left(Node u){
        Node par = u.parent;
        Node gpar = par!=null? par.parent : null;
        Node lchild = u.left;
        u.parent = gpar;

        if(gpar != null){
            if(par == gpar.left)
                gpar.left = u;
            else
                gpar.right = u;
        }
        u.left = par;
        par.parent = u;
        par.right = lchild;
        if(lchild != null){
            lchild.parent = par;
        }
        par.update();
        u.update();
    }

    static void rotate(Node u){
        Node par = u.parent;
        if(par == null){
            return;
        }
        if(u == par.left){
            rotate_right(u);
        }
        else
            rotate_left(u);
    }

    //since this is a splay bst, we need to change the root every time we access a node
    //this is done by rotating the node to the root
    public static void change_root(Node u){     //making u the root
        // if(u == null){
        //     return; //u is null, nothing to do
        // }
        if(u == null){
            return; //u is already the root
        }
        
        while(u.parent != null){
            Node par = u.parent, gpar = par.parent;
            if(gpar == null){
                rotate(u);
                break;
            }
            if((u == par.left) == (par == gpar.left)){ //both are left or both are right
                rotate(par);              //go up 2 levels
                rotate(u);
            }
            else{                  //one left one right
                rotate(u);
                rotate(u);   
            }
            
        }
        
    }

    //splay u to root
    //go to rightmost node and insert a new node as a child of u, so in same tree
    public static Node insert_node(Node u){       //inserting a new node to parent u
        if(u == null){
            return new Node(0); //if u is null, we create a new node
        }
        change_root(u);   //make u the root
        while(u.right!=null)
            u = u.right; //keep going down until you become the rightmost node
        Node newnode = new Node(0);
        newnode.parent = u;
        u.right = newnode;    //put the newnode as the right child of the rightmost node
        u.update();
        change_root(newnode);       //make the newnode the root
        return newnode;
    }
    //...huh

    //detatch a node from its parent
    //this is used when we want to remove a node from the tree, 
    //but keep its subtree
    static void remove_child_node(Node u){
        Node par = u.parent;
        if(par == null){
            return;
        }
        if(u == par.left){
            par.left = null;
        }
        else{
            par.right = null;
        }
        u.parent = null;
        par.update();
    }

    //deletes u from the bst by splaying it to the root, 
    //separating the resulting left and right children
    //make the leftmost of the right subtree the new root and attach the left 
    static void delete_node(Node u){
        // if(u == null){
        //     return; //nothing to delete
        // }
        change_root(u);
        Node lchild = u.left;
        Node rchild = u.right;
        if(lchild == null && rchild == null){     // if there is only 1 node and we delete it
             return;
        }
        if(lchild == null){
            //only a right child exists, so the remaining tree is j the right
            remove_child_node(rchild); //detatch u from its parent
        }
        else if(rchild == null){
            //only left child
            remove_child_node(lchild); //detatch u from its parent
        }
        else{
            //both children exist, so need to attach them
            //so we put the left subtree at the bottom of the right subtree
            remove_child_node(lchild); //detatch u from its parent
            remove_child_node(rchild); //detatch u from its parent
            Node front = leftmost(rchild); //make the leftmost of the right subtree the root
            front.left = lchild; //connects the leftmost of the subtree to the leftmost at root
            //lchild.parent = front; //set the parent of the left child to the new root
            front.update();
        }

    }
    

    static Node leftmost(Node u){
        change_root(u);
        while(u.left != null){
            u = u.left;
        }
        change_root(u);
        return u;
    }

    static Node rightmost(Node u){
        change_root(u);
        while(u.right != null){
            u = u.right;
        }
        change_root(u);
        return u;
    }

    //returns successor of u, splays the node to the root
    static Node next(Node u){
        if(u == null)
            return null;
        change_root(u);
        
        u = u.right;
        if(u == null)
            return null;
        while(u.left != null){
            u = u.left;
        }
        change_root(u);
        return u;
    }

    public static void print_bst(Node u){
        if (u == null){
            return;
        }
        print_bst(u.left);
        System.out.print(u.name + " ");

        print_bst(u.right);
    }
}
