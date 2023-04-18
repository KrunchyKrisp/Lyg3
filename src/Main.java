import java.util.*;

public class Main extends Thread {
	public static int seed = 2023;
	public static int nThreads;
	public static int workload;
	public static int grainSize;
	public static boolean slowMode;
	public static ArrayList<Integer> data = new ArrayList<>();
	public static final LinkedList<Integer[]> jobs = new LinkedList<>();
	public static ArrayList<Boolean> threadWaiting = new ArrayList<>();

	public volatile int id;
	public Main(int id) {
		this.id = id;
	}

	public static void main(String[] args) {
		try {
			if (args.length < 4 || !(
					(nThreads = Integer.parseInt(args[0])) >= 1 && nThreads <= 16 &&
							(workload = Integer.parseInt(args[1])) >= 16 && workload <= 1_000_000_000 &&
							(grainSize = Integer.parseInt(args[2])) >= 1 && grainSize <= workload
			)) {
				System.err.println("Parameters: <number threads 1..16> <workload: 16..100000000> <grainSize: 1..64> <slowMode: true/false>");
				System.err.println("Not enough parameters: " + Arrays.toString(args));
			} else {
				slowMode = Boolean.parseBoolean(args[3]);
				for (int i = 0; i < workload; ++i) {
					data.add(i);
				}
				jobs.add(new Integer[]{0, workload - 1});
				shuffle(data);
				if (slowMode)
					System.out.println(data.toString());

				System.err.println("#Test for: nThreads=" + nThreads + " workload=" + workload + " grainSize=" + grainSize + " slowMode=" + slowMode);
				double dtime = makePerformanceTest();
				System.err.println("#Completed. Running time: " + dtime + "s");

				if (slowMode)
					System.out.println(data.toString());
				ArrayList<Integer> copy = new ArrayList<>(data);
				Collections.sort(copy);
				System.out.println("Sorted: " + copy.equals(data));
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(4);
		}
	}

	public static void shuffle(ArrayList<Integer> data) {
		Collections.shuffle(data, new Random(Main.seed));
	}

	static double makePerformanceTest() throws Exception {
		long time0 = System.currentTimeMillis();

		if (nThreads > 1) {
			// Create and start threads
			Main[] aThreads = new Main[nThreads];
			for (int i = 0; i < nThreads; i++) {
				(aThreads[i] = new Main(i)).start();
				threadWaiting.add(false);
			}

			// Wait until all threads finish
			for (int i = 0; i < nThreads; i++) {
				aThreads[i].join();
			}
		} else {
			quicksortRecursive(0, workload-1);
		}

		long time1 = System.currentTimeMillis();
		return (time1 - time0) / 1000.;
	}

	public void run() {
		//kol yra darbų arba yra nelaukiančių (dirbančių)
		Integer[] job = null;
		while (!jobs.isEmpty() || threadWaiting.contains(false)) {
			synchronized (jobs) {
				if (!jobs.isEmpty()) {
					job = jobs.removeFirst();
					threadWaiting.set(id, false); //turime darba - nelaukiame
				}
			}
			if (job == null) continue; //negavome darbo, griztame
			if (job[1] - job[0] <= grainSize) {
				quicksortRecursive(job[0], job[1]);
			} else {
				quicksortParallel(job[0], job[1]);
			}
			threadWaiting.set(id, true); //baigeme darba - vel laukiame

			if (slowMode) {
				System.out.println(data + " " + Arrays.toString(job) + " " + this.getName());
			}
			job = null;
		}
	}

	public static void quicksortRecursive(int begin, int end) {
		if (begin < end) {
			int i = partition(begin, end);
			quicksortRecursive(begin, i-1);
			quicksortRecursive(i+1, end);
		}
	}

	public static void quicksortParallel(int begin, int end) {
		if (begin < end) {
			int i = partition(begin, end);
			synchronized (jobs) {
				jobs.add(new Integer[] {begin, i-1});
				jobs.add(new Integer[] {i+1, end});
			}
		}
	}

	public static int partition(int begin, int end) {
		int pivot = data.get(end);
		int i = (begin - 1);

		for (int j = begin; j < end; j++) {
			if (data.get(j) <= pivot) {
				i++;

				int swapTemp = data.get(i);
				data.set(i, data.get(j));
				data.set(j, swapTemp);
			}
		}

		int swapTemp = data.get(i + 1);
		data.set(i + 1, data.get(end));
		data.set(end, swapTemp);

		return i + 1;
	}
}