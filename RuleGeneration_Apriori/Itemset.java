import java.util.Arrays;

public class Itemset{
	public int[] itemset; 
	public int support = 0; 
	public int[] getItems() {
		return itemset;
	}
	public Itemset(){
		itemset = new int[]{};
	}
	public Itemset(int item){
		itemset = new int[]{item};
	}
	public Itemset(int [] items){
		this.itemset = items;
	}
	public int getAbsoluteSupport(){
		return support;
	}
	public int size() {
		return itemset.length;
	}
	public Integer get(int position) {
		return itemset[position];
	}
	public void setAbsoluteSupport(Integer support) {
		this.support = support;
	}
		public void increaseTransactionCount() {
		this.support++;
	}	
	public Itemset cloneItemSetMinusOneItem(Integer itemToRemove) {
		int[] newItemset = new int[itemset.length -1];
		int i=0;
		for(int j =0; j < itemset.length; j++){
			if(itemset[j] != itemToRemove){
				newItemset[i++] = itemset[j];
			}
		}
		return new Itemset(newItemset);
	}
	public boolean contains(Integer item) {
		for (int i=0; i< size(); i++) {
			if (get(i).equals(item)) {
				return true;
			} else if (get(i) > item) {
				return false;
			}
		}
		return false;
	}
	public Itemset cloneItemSetMinusAnItemset(Itemset itemsetToNotKeep) {
		int[] newItemset = new int[itemset.length - itemsetToNotKeep.size()];
		int i=0;
		for(int j =0; j < itemset.length; j++){
			if(itemsetToNotKeep.contains(itemset[j]) == false){
				newItemset[i++] = itemset[j];
			}
		}
		return new Itemset(newItemset); // return the copy
	}
	
	public static int[] intersectTwoSortedArrays(int[] array1, int[] array2){
	    final int newArraySize = (array1.length < array2.length) ? array1.length : array2.length;
	    int[] newArray = new int[newArraySize];

	    int pos1 = 0;
	    int pos2 = 0;
	    int posNewArray = 0;
	    while(pos1 < array1.length && pos2 < array2.length) {
	    	if(array1[pos1] < array2[pos2]) {
	    		pos1++;
	    	}else if(array2[pos2] < array1[pos1]) {
	    		pos2++;
	    	}else { 
	    		newArray[posNewArray] = array1[pos1];
	    		posNewArray++;
	    		pos1++;
	    		pos2++;
	    	}
	    }
	    return Arrays.copyOfRange(newArray, 0, posNewArray); 
	}
	
	public Itemset intersection(Itemset itemset2) {
		int [] intersection =intersectTwoSortedArrays(this.getItems(), itemset2.getItems());
		return new Itemset(intersection);
	}
}
