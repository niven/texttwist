package org.interdictor.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utils {

	public interface Mapping<T> {
		List<T> map(T item);
	}

	public interface Filter<T> {
		boolean passes(T item);
	}

	// tests
	public static void main(String[] args) {

		String[] s = new String[] { "foo", "bar", "rez" };
		String join = join(s, ",");
		System.out.println("Join: " + join.equals("foo,bar,rez") + "( " + join + " )\n\n");
		s = new String[] { "foo", "bar", "rez", "qux" };
		join = join(s, ",");
		System.out.println("Join: " + join.equals("foo,bar,rez,qux") + "( " + join + " )");
	}

	public interface Combiner<T> {
		T combine(T t1, T t2);
	}

	public static String join(String[] strings, final String seperator) {
		return join(Arrays.asList(strings), seperator);
	}

	private static <T> T reduce(List<T> things, Combiner<T> combiner) {
		if (things.size() == 0) {
			return null;
		}
		if (things.size() == 1) {
			return things.get(0);
		}
		return reduce(things.subList(0, things.size() - 1), combiner, things.get(things.size() - 1));
	}

	/**
	 * Right-reduce an array for things to 1 thing
	 * 
	 * @param things
	 * @param combiner
	 * @return
	 */
	public static <T> T reduce(List<T> things, Combiner<T> combiner, T initial) {
		if (things.size() == 0) {
			return initial;
		}
		if (things.size() == 1) {
			return combiner.combine(things.get(0), initial);
		}

		T combined = combiner.combine(things.get(things.size() - 1), initial);
		return reduce(things.subList(0, things.size() - 1), combiner, combined);

	}

	public static String join(Collection<String> strings, final String seperator) {
		return reduce(new ArrayList<String>(strings), new Combiner<String>() {

			@Override
			public String combine(String t1, String t2) {
				return t1 + seperator + t2;
			}

		});
	}

	public static <T> Collection<T> uniq(List<T> things) {
		Set<T> out = new HashSet<T>();
		for (T t : things) {
			out.add(t);
		}
		return out;
	}

	public static <T> List<T> map(Mapping<T> mapping, List<T> in) {
		List<T> out = new ArrayList<T>();
		for (T t : in) {
			List<T> result = mapping.map(t);
			if (result != null) {
				out.addAll(result);
			}
		}
		return out;
	}

	public static <T> List<T> grep(Filter<T> filter, List<T> in) {
		List<T> out = new ArrayList<T>();
		for (T t : in) {
			if (filter.passes(t)) {
				out.add(t);
			}
		}
		return out;
	}
}
