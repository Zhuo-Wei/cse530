package hw3;


import java.util.ArrayList;

import hw1.Field;
import hw1.RelationalOperator;

public class BPlusTree {
	int pInner;
	int pLeaf;
	Node root;
	
    
    public BPlusTree(int pInner, int pLeaf) {
    	//your code here
    	this.pInner = pInner;
    	this.pLeaf = pLeaf;
    	this.root = null;
    }
    
    public LeafNode search(Field f) {
    	//your code here
    	//corner case
    	if (root == null) {
    		return null;
    	}
    	return searchHelper(f, this.root);
    }
    private LeafNode searchHelper(Field f, Node root) {
    	//base case
    	if (root.isLeafNode() == true) {
    		//iterate all entries in this leafNode to check whether f exist
    		for (Entry entry: ((LeafNode)root).getEntries()) {
    			if (entry.getField().equals(f)) {
    				return (LeafNode)root;
    			}
    		}
    		return null;
    	}
    	ArrayList<Field> keys = ((InnerNode)root).getKeys();
    	ArrayList<Node> children = ((InnerNode)root).getChildren();
    	for (int i = 0; i < keys.size(); i++) {	
    		if (keys.get(i).compare(RelationalOperator.GTE, f) == true) {
    			return searchHelper(f, children.get(i));
    		}	
    	}
    	return searchHelper(f, children.get(keys.size()));
    	
    }
    
    
    public void insert(Entry e) {
    	if (search(e.getField()) != null) {
    		return;
    	}
    	if (this.getRoot() == null) {
    		// first insert
    		LeafNode theLeaf = new LeafNode(this.pLeaf);
    		theLeaf.getEntries().add(e);
    		this.root = theLeaf;
    		return;
    	}
    	LeafNode theLeaf = findleaf(e,this.getRoot());
    	theLeaf.addToEntries(e);
		
		if (theLeaf.getEntries().size() <= theLeaf.getDegree()) {
			// simple case, if there is empty spot, directly insert;
			return;
    	} else {
    		// need to split;
    		LeafNode rightNode = theLeaf.split();
    		if (theLeaf.getParent() != null) {
    			theLeaf.getParent().addNewChild(theLeaf, rightNode);
    			InnerNode parent = theLeaf.getParent();
    			while (parent.getChildren().size() > parent.getDegree()) {
    				InnerNode newInnerRight = parent.split();
    				if (parent.parent != null) {
    					parent.parent.addNewChild(parent, newInnerRight);
    					parent = parent.parent;
    				} else {
    					InnerNode newRoot = new InnerNode(this.pInner);
    					newRoot.addNewChild(parent, newInnerRight);
    					this.root = newRoot;
    					parent = newRoot;
    				}
    			}
    		} else {
    			InnerNode newInner = new InnerNode(this.pInner);
    			newInner.addNewChild(theLeaf, rightNode);
    			theLeaf.setParent(newInner);
    			rightNode.setParent(newInner);
    			this.root = newInner;
    		}	
    	}  	
    	//your code here
    }
    
    // find the leafNode that e needs to get insert into, this is a helper function
    // for insert(Entry e) and delete
    public LeafNode findleaf(Entry e, Node cur) {
    	if (cur.isLeafNode()) {
    		return (LeafNode) cur;
    	}
    	InnerNode curInner = (InnerNode) cur;
    	for (int i = 0 ; i < curInner.getKeys().size() ; i ++) {
    		if (e.getField().compare(RelationalOperator.LTE, curInner.keys.get(i))) {
    			return findleaf(e,curInner.getChildren().get(i));
    		}
    	}
    	return findleaf(e,curInner.getChildren().get(curInner.getChildren().size() - 1));
    }
    
    public void delete(Entry e) {
    	//your code here
    	if (search(e.getField()) == null) {
    		return;
    	}
    	LeafNode theLeaf = findleaf(e,this.getRoot());
    	theLeaf.removeEntry(e);
    	if (theLeaf.getEntries().size() < theLeaf.getMiniEntry() && theLeaf != root) {
    		// need to adjust the structure
    		// try to borrow
    		if (theLeaf.getPrev() != null && theLeaf.getPrev().getEntries().size() > theLeaf.getPrev().getMiniEntry()
    				&& theLeaf.getPrev().getParent() == theLeaf.getParent()) {
				// borrow from left
				LeafNode prev = theLeaf.getPrev();
				Entry toMove = prev.getEntries().get(prev.getEntries().size() - 1);
				prev.getEntries().remove(prev.getEntries().size() - 1);
				theLeaf.addToEntries(toMove);
				// update parent's key
				InnerNode theParent = prev.getParent();
				for (int i = 0 ; i < theParent.getChildren().size() ; i++) {
					if (theParent.getChildren().get(i) == prev) {
						theParent.getKeys().set(i, prev.getEntries().get(prev.getEntries().size() - 1).getField());
					}
				}
    		} else if (theLeaf.getNext() != null && theLeaf.getNext().getEntries().size() > theLeaf.getNext().getMiniEntry()
    				&& theLeaf.getNext().getParent() == theLeaf.getParent()) {
    			// borrow from right
    			LeafNode next = theLeaf.getNext();
				Entry toMove = next.getEntries().get(0);
				next.getEntries().remove(0);
				theLeaf.addToEntries(toMove);
				// update parent's key
				InnerNode theParent = theLeaf.getParent();
				for (int i = 0 ; i < theParent.getChildren().size() ; i++) {
					if (theParent.getChildren().get(i) == theLeaf) {
						theParent.getKeys().set(i, theLeaf.getEntries().get(theLeaf.getEntries().size() - 1).getField());
					}
				}
    			
    		} else {
    			// borrow failed, need to merge
    			// try merge with left
    			LeafNode merged = null;
    			if (theLeaf.getPrev() != null && theLeaf.getPrev().getParent() == theLeaf.getParent()) {
    				merged = theLeaf.getPrev().merge(theLeaf);
    				InnerNode parent = merged.getParent();
    				for (int i = 0 ; i < parent.getChildren().size(); i ++) {
    					if (parent.getChildren().get(i) == theLeaf) {
    						// delete the leaf and corresponding key
    						parent.getKeys().remove(i);
    						parent.getChildren().remove(theLeaf);
    						break;
    					}
    				}
    
    				if (parent.getChildren().size() < parent.minPointer) {
    					if (parent == root) {
    						if (parent.getChildren().size() == 1) {
    							// no longer needed
    							merged.setParent(null);
    							this.root = merged;
    							return;
    						}
    					} else {
    						// borrow first
        					InnerNode parentLeft = null;
        					InnerNode parentRight = null;
        					for (int i = 0 ; i < parent.parent.getChildren().size() ; i++) {
        						if (parent.parent.getChildren().get(i) == parent) {
        							if (i - 1 >= 0) {
        								parentLeft = (InnerNode) parent.parent.getChildren().get(i - 1);
        							}
        							if (i + 1 < parent.parent.getChildren().size()) {
        								parentRight = (InnerNode) parent.parent.getChildren().get(i + 1);
        							}
        							break;
        						}
        					}
        					// borrow from left
        					if (parentLeft != null && parentLeft.getChildren().size() > parentLeft.minPointer) {
        						
        					}
    					}
    					
    				}
    			}else if (theLeaf.getNext() != null && theLeaf.getNext().getParent() == theLeaf.getParent()) {
    				merged = theLeaf.getNext().merge(theLeaf);
    			}
    			
    			
    			
    			
    		}
    	}
    	
    	
    }
    
    public Node getRoot() {
    	//your code here
    	return this.root;
    }
    


	
}
