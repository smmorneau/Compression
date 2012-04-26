import java.util.Comparator;
import java.util.Map;


/* Compile with: javac -Xlint:unchecked *.java */

class MapValueComparator implements Comparator {

	Map<?, ?> original;

	public MapValueComparator(Map<?, ?> unsorted) {
		this.original = unsorted;
	}

	public int compare(Object a, Object b) {
		// lowest to highest
		if ((Integer) original.get(a) < (Integer) original.get(b)) {
			return -1;
		} else if ((Integer) original.get(a) > (Integer) original.get(b)) {
			return 1;
		} else {
			if((Character) a < (Character) b) {
				return -1;
			} else if((Character) a > (Character) b) {
				return 1;
			} else {
				return 0;
			}
		}
	}
}