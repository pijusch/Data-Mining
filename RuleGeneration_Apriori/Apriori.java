

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class Apriori {

	// the maximal level reached by Apriori 
	protected int k;

	// For statistics
	protected int totalCandidateCount = 0; // total number of candidates generated
	private int itemsetCount;   // number of itemsets found
	private int hash_tree_branch_count;  // the number of branches in the hash tree
	private int minsupRelative;
	// an in-memory representation of the transaction database
	private List<int[]> database = null;
	BufferedWriter writer = null;
	/*
	 1.handicapped-infants=n
	 2.handicapped-infants=y
	 3.water-project-cost-sharing=n
	 4.water-project-cost-sharing=y
	 5.adoption-of-the-budget-resolution=n
	 6.adoption-of-the-budget-resolution=y
	 7.physician-fee-freeze=n
	 8.physician-fee-freeze=y
	 9.el-salvador-aid=n
	 10.el-salvador-aid=y
	 11.religious-groups-in-schools=n
	 12.religious-groups-in-schools=y
	 13.anti-satellite-test-ban=n
	 14.anti-satellite-test-ban=y
	 15.aid-to-nicaraguan-contras=n
	 16.aid-to-nicaraguan-contras=y
	 17.mx-missile=n
	 18.mx-missile=y
	 19.immigration=n
	 20.immigration=y
	 21.synfuels-corporation-cutback=n
	 22.synfuels-corporation-cutback=y
	 23.education-spending=n
	 24.education-spending=y
	 25.superfund-right-to-sue=n
	 26.superfund-right-to-sue=y
	 27.crime=n
	 28.crime=y
	 29.duty-free-exports=n
	 30.duty-free-exports=y
	 31.export-administration-act-south-africa=n
	 32.export-administration-act-south-africa=y
	 33.Class=republican
	 34.Class=democrat
	 */
	public Apriori() {
	}
	public void runAlgorithm(double minsup, String input, String output, int hash_tree_branch_count) throws IOException {
		writer = new BufferedWriter(new FileWriter(output));
		itemsetCount = 0;
		totalCandidateCount = 0;
		this.hash_tree_branch_count = hash_tree_branch_count;
		Map<Integer, Integer> mapItemCount = new HashMap<Integer, Integer>(); 
		database = new ArrayList<int[]>(); 
		BufferedReader reader = new BufferedReader(new FileReader(input));
		String line;
		while (((line = reader.readLine()) != null)) {
			if (line.isEmpty() == true ||
					line.charAt(0) == '#' || line.charAt(0) == '%'
							|| line.charAt(0) == '@') {
				continue;
			} 
			String[] lineSplited = line.split(",");
			int transaction[] = new int[lineSplited.length];
			Integer h =0; 
			for (int i=0; i< lineSplited.length; i++) {
				if(lineSplited[i].charAt(0)== '?'){
					h=h+2;
					transaction[i]=9999;
					continue;
				}
				Integer item = 8000;
				if(lineSplited[i].charAt(1) == 'n'){
					item = h;
				}
				else if(lineSplited[i].charAt(1) == 'y'){
					item = h+1;
				}
				else if(lineSplited[i].charAt(1) == 'r'){
					item = h;
				}
				else if(lineSplited[i].charAt(1) == 'd'){
					item = h+1;
				}
				h= h+2;
				transaction[i] = item;
				Integer count = mapItemCount.get(item);
				if (count == null) {
					mapItemCount.put(item, 1);
				} else {
					mapItemCount.put(item, ++count);
				}	
			}
			for(int u =0 ; u < transaction.length ; u++){
				System.out.print(transaction[u] + " ");
			}
			System.out.println();
			database.add(transaction);
		} 
		reader.close();
		this.minsupRelative = (int) Math.ceil(minsup * 435);
		k = 1;
		List<Integer> frequent1 = new ArrayList<Integer>();
		for(Entry<Integer, Integer> entry : mapItemCount.entrySet()){
			if(entry.getValue() >= minsupRelative){
				frequent1.add(entry.getKey());
				saveItemsetToFile(entry.getKey(), entry.getValue());
			}
		}
		mapItemCount = null; 
		Collections.sort(frequent1, new Comparator<Integer>() {
			public int compare(Integer o1, Integer o2) {
				return o1 - o2;
			}
		});
		
		// if no frequent item
		if(frequent1.size() == 0){
			return;
		}
		totalCandidateCount += frequent1.size();
		k = 2;
		int previousItemsetCount = itemsetCount;
		ItemsetHashTree candidatesK = null;
		do{
			if(k ==2){
				candidatesK = generateCandidate2(frequent1);
			}
			else{
				candidatesK = generateCandidateSizeK(candidatesK, k);
			}
			if(candidatesK.candidateCount ==0 ){
				break;
			}
			totalCandidateCount += candidatesK.candidateCount;
			for(int[] transaction: database){
				if(transaction.length >= k) {
					candidatesK.updateSupportCount(transaction);
				}
			}
			// We next save to file all the candidates that have a support higher than the minsup threshold 
			for(LeafNode node  = candidatesK.lastInsertedNode;node != null; node = node.nextLeafNode){
				for(List<Itemset> listCandidate: node.candidates){
					if(listCandidate != null){
						for(int i=0; i<listCandidate.size(); i++){
							Itemset candidate = listCandidate.get(i);
							if (candidate.getAbsoluteSupport() >= minsupRelative) {
								//System.out.println(candidate);
								StringBuffer bf = new StringBuffer();
								for(int h = 0 ; h < candidate.size() ; h ++ ){
				//					System.out.println(candidate.get(h));
									bf.append(candidate.get(h));
									bf.append(",");
								}
								saveItemsetToFile(bf.toString() , candidate);
							}
							else{
								listCandidate.remove(i);  
							}
						}
					}	
				}
				
			}
			k++;
		}while(previousItemsetCount != itemsetCount);
		// close the file
		writer.close();
	}
	
	// Method to generate candidates of size k, where k > 2
	
	private ItemsetHashTree generateCandidateSizeK(ItemsetHashTree candidatesK_1, int k) {
		// create the hash-tree to store the candidates of size K
		ItemsetHashTree newCandidates = new ItemsetHashTree(k, hash_tree_branch_count);
		
		// For each leaf node
		for(LeafNode node  = candidatesK_1.lastInsertedNode; node != null; node = node.nextLeafNode){
			List<Itemset> subgroups [] = node.candidates;
			// For each sets of itemsets in this node
			for(int i=0; i< subgroups.length; i++){
				if(subgroups[i] == null){
					continue;
				}
			
				// For each sets of itemsets in this node
				for(int j=i; j< subgroups.length; j++){
					if(subgroups[j] == null){
						continue;
					}
					// try to use these list of itemsets to generate candidates.
					generate(subgroups[i], subgroups[j], candidatesK_1, newCandidates);
				}
			}
		}
		return newCandidates; 
	}

	 // Method to generate candidates of size k from two list of itemsets of size k-1
	 
	private void generate(List<Itemset> list1, List<Itemset> list2, ItemsetHashTree candidatesK_1, ItemsetHashTree newCandidates) {
		loop1: for (int i = 0; i < list1.size(); i++) {
			int[] itemset1 = list1.get(i).itemset;
			
			// if the two lists are the same, we will start from i+1 in the second list
			// to avoid comparing pairs of itemsets twice.
			int j = (list1 == list2)?  i+1 : 0;
			// For each itemset in list 2
			loop2: for (; j < list2.size(); j++) {
				int[] itemset2 = list2.get(j).itemset;

				// we compare items of itemset1 and itemset2.
				// If they have all the same k-1 items and the last item of
				// itemset1 is smaller than
				// the last item of itemset2, we will combine them to generate a
				// candidate
				for (int k = 0; k < itemset1.length; k++) {
					// if k is not the last item
					if (k != itemset1.length - 1) {
						if (itemset2[k] > itemset1[k]) {  
							continue loop1; // we continue searching
						} 
						if (itemset1[k] > itemset2[k]) {  
							continue loop2; // we continue searching
						} 
					}
					
				}

				int newItemset[] = new int[itemset1.length+1];
				if(itemset2[itemset2.length -1] < itemset1[itemset1.length -1]){
					System.arraycopy(itemset2, 0, newItemset, 0, itemset2.length);
					newItemset[itemset1.length] = itemset1[itemset1.length -1];
				}else{
					// Create a new candidate by combining itemset1 and itemset2
					System.arraycopy(itemset1, 0, newItemset, 0, itemset1.length);
					newItemset[itemset1.length] = itemset2[itemset2.length -1];
				}
				if (allSubsetsOfSizeK_1AreFrequent(newItemset, candidatesK_1)) {
					// If yes, we add the candidate to the hash-tree
					newCandidates.insertCandidateItemset(new Itemset(newItemset));
				}
			}
		}
	}


	private ItemsetHashTree generateCandidate2(List<Integer> frequent1) {
		ItemsetHashTree candidates = new ItemsetHashTree(2, hash_tree_branch_count);
		for (int i = 0; i < frequent1.size(); i++) {
			Integer item1 = frequent1.get(i);
			for (int j = i + 1; j < frequent1.size(); j++) {
				Integer item2 = frequent1.get(j);
				candidates.insertCandidateItemset(new Itemset(new int []{item1, item2}));
			}
		}
		return candidates;
	}

	protected boolean allSubsetsOfSizeK_1AreFrequent(int[] itemset, ItemsetHashTree hashtreeCandidatesK_1) {
		for(int posRemoved=0; posRemoved< itemset.length; posRemoved++){

			if(hashtreeCandidatesK_1.isInTheTree(itemset, posRemoved) == false){ 
				return false;
			}
		}
		return true;
	}
	
	void saveItemsetToFile(String s , Itemset itemset) throws IOException {
		writer.write(s+ " #SUP: " + itemset.getAbsoluteSupport()/435.0);
		writer.newLine();
		itemsetCount++;
	}

	void saveItemsetToFile(Integer item, Integer support) throws IOException {
		writer.write(item + " #SUP: " + support/435.0);
		writer.newLine();
		itemsetCount++;
	}
	
	public void printStats() {
		System.out.println("***********************APRIORI**********************************");
		System.out.println(" Candidates count : " + totalCandidateCount);
		System.out.println(" The algorithm stopped at size " + (k - 1) + ", because there is no candidate");
		System.out.println(" Frequent itemsets count : " + itemsetCount);
		System.out.println("****************************************************************");
	}


}

class ItemsetHashTree {

	// this constant indicates how many child nodes a node should have
	private int branch_count = 30;
	
	// the size of the itemsets that are inserted into this tree
	private int itemsetSize;
	
	// the number of itemsets that have been inserted into this tree
	int candidateCount;
	
	// the root node of the tree
	InnerNode root;
	
	// the last leaf node that was added to the tree
	LeafNode lastInsertedNode = null; 
	
	public ItemsetHashTree(int itemsetSize, int branch_count){
		this.itemsetSize = itemsetSize;
		this.branch_count = branch_count;
		root = new InnerNode(); 
	}
	
	public void insertCandidateItemset(Itemset itemset){
		candidateCount++; 
		insertCandidateItemset(root, itemset, 0);
	}
	
	//* Inserts an itemset in the hash-tree (this is called recursively to search where to insert the itemset)
	private void insertCandidateItemset(Node node, Itemset itemset, int level){	
		int branchIndex = itemset.itemset[level] % branch_count;
		if(node instanceof LeafNode){
			List<Itemset> list = ((LeafNode)node).candidates[branchIndex];
			if(list == null){
				list = new ArrayList<Itemset>();
				((LeafNode)node).candidates[branchIndex] = list;
			}
			list.add(itemset);
		}else{
			Node nextNode = ((InnerNode)node).childs[branchIndex];
			if(nextNode == null){
				if(level == itemsetSize - 2){
					nextNode = new LeafNode();
					((LeafNode)nextNode).nextLeafNode = lastInsertedNode;
					lastInsertedNode = (LeafNode)nextNode;
				}else{
					nextNode = new InnerNode();
				}
				((InnerNode)node).childs[branchIndex] = nextNode;
			}
			insertCandidateItemset(nextNode, itemset, level+1);
		}
	}



 //	 * This method increase the support count of all itemsets contained in the hash-tree

	public void updateSupportCount(int[] transaction) {
		updateSupportCount(transaction, root, 0, new int[]{});
	}

	/**
	 * Recursive method for increasing the support count of all itemsets contained in the hash-tree that are contained in a transaction.
	 */
	private void updateSupportCount(int[] transaction, InnerNode node, int firstPositionToCheck, int [] prefix) {
		int lastPosition = transaction.length -1;
		int lastPositionToCheck = transaction.length - itemsetSize + prefix.length;
		for(int i=firstPositionToCheck; i <= lastPositionToCheck; i++){ 
			int itemI = transaction[i];
			int branchIndex = itemI% branch_count;
			Node nextNode = node.childs[branchIndex];
			if(nextNode == null){
			}
			else if(nextNode instanceof InnerNode){
				int [] newPrefix = new int[prefix.length+1];
				System.arraycopy(prefix, 0, newPrefix, 0, prefix.length);
				newPrefix[prefix.length] = itemI;
				// we call the method recursively
				updateSupportCount(transaction, (InnerNode) nextNode, i+1, newPrefix);
			}else{
				// if the node is a leaf node
				LeafNode theNode = (LeafNode) nextNode;
				// we search for an additional item that could be added
				for(int j= i+1; j <= lastPosition; j++){
					int itemJ = transaction[j];
					// we check which branch
					int branchIndexNextNode = itemJ% branch_count;
					List<Itemset> listCandidates = theNode.candidates[branchIndexNextNode];
					// if the branch is not null
					if(listCandidates != null){
						// we check if the resulting itemset is in this branch.
						for(Itemset candidate: listCandidates){
							
							// if so, we increase its support count
							if(sameAsPrefix(candidate.itemset, prefix, itemI, itemJ)){
								candidate.support++;
							}
						}
					}
				}
			}
		}
	}
	
	 // @return 0 if they are the same, 1 if itemset is larger according to lexical order, -1 if smaller.

	public static int sameAs(int [] itemset1, int [] itemsets2, int posRemoved) {
		int j=0;
		for(int i=0; i<itemset1.length; i++){
			if(j == posRemoved){
				j++;
			}
			if(itemset1[i] == itemsets2[j]){
				j++;

			}else if (itemset1[i] > itemsets2[j]){
				return 1;
			}else{
				return -1;
			}
		}
		return 0;
	}
	
	public boolean isInTheTree(int[] itemset, int posRemoved) {
		Node node = root;
		int count = 0;
loop:	for(int i=0; i< itemset.length; i++){
			if(i== posRemoved){
				continue;
			}
			count++;
			int branchIndex = itemset[i] % branch_count;
			if(count == itemsetSize){
				if(node == null){
					return false;
				}
				List<Itemset> list = ((LeafNode)node).candidates[branchIndex];
				if(list == null){
					return false;
				}
		        int first = 0;
		        int last = list.size() - 1;
		       
		        while( first <= last )
		        {
		        	int middle = ( first + last ) / 2;

		            if(sameAs(list.get(middle).getItems(), itemset, posRemoved)  < 0 ){
		            	first = middle + 1;  
		            }
		            else if(sameAs(list.get(middle).getItems(), itemset, posRemoved)  > 0 ){
		            	last = middle - 1; 
		            }
		            else{
		            	break loop;  
		            }
		        }
				return false; 
				
			}
			else{
				if(node == null){
					return false;
				}
				node = ((InnerNode)node).childs[branchIndex];
			}
		}
		return true; 
	}


	 // A method that check if an itemset is equal to another itemset called "prefix" + itemI + itemJ
	private boolean sameAsPrefix(int [] itemset1, int [] prefix, int itemI, int itemJ) {
		for(int i=0; i < prefix.length; i++){
			if(itemset1[i] != prefix[i]){
				return false;
			}
		}
		return itemset1[itemset1.length -2] == itemI 
				&& itemset1[itemset1.length -1] == itemJ;
	}
}
	abstract class Node{
		protected int branch_count = 30;
	}
		
	class InnerNode extends Node{
		Node childs[ ] = new Node[branch_count]; 
	}
		
	class LeafNode extends Node{
		@SuppressWarnings("unchecked")
		final List<Itemset> [] candidates = new ArrayList[branch_count ];
		LeafNode nextLeafNode = null;
	}
	

