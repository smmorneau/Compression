/* Author: David Galles */

public class Assert {

    public static void notFalse(boolean flag, String message) {
	if (!flag) {
	    System.out.println(message);
	    throw new IllegalArgumentException();
	}
    }

    public static void notFalse(boolean flag) {
	if (!flag) {
	    System.out.println("Failed notFalse Assertion");
	    throw new  IllegalArgumentException();
	}
    }

    public static void notNull(Object obj, String message) {
	if (obj == null) {
	    System.out.println(message);
	    throw new IllegalArgumentException();
	}
    }

    public static void notNull(Object obj) {
	if (obj == null) {
	    System.out.println("Failed notNull Assertion");
	    throw new IllegalArgumentException();
	}
    }
}
