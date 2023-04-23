/*
 * Main.java
 * Matas Damidavičius INF 3k2g1p 1910621
 * ND3 variantas 1 - quick sort
 *
 * Masyvo elementų rūšiavimas “quick sort” metodu.
 *
 * Algoritmo išlygiagretinimas: kiekviena gija pasiema darbą iš sinchronizuotos eilės.
 * Darbas: bendro masyvo rėžiai, kuriuos reikia išrikiuoti “quick sort” metodu.
 * Rezultatas:
 *      Jei rėžių dydis mažiau-lygus pasirinktam grūdo dydžiui - rėžis išrikiuojamas nuosekliai.
 *      Kitu atveju - rėžis dalijamas su “pivot” į 2 dalis, gauti 2 rėžiai talpinami į eilę.
 * Jei eilė tučia - gija laukia, tai pažymima atskirame bendrame "gijų laukimo" masyve.
 * Gijos dirba, kol eilė tampa tučia IR visos gijos laukia.
 */

import java.util.*;

public class Main extends Thread {
	public static int seed = 2023; //Sėkla naudojama masyvo sumaišymui
	public static int nThreads; //Gijų skaičius
	public static int workload; //Masyvo ilgis
	public static int grainSize; //Mažiausio darbo dydis (right-left)
	public static boolean slowMode; //Lėtasis rėžimas
	public static ArrayList<Integer> data = new ArrayList<>(); //Masyvas rikiavimui
	public static final LinkedList<Integer[]> jobs = new LinkedList<>(); //Darbų eilė (FIFO)
	public static ArrayList<Boolean> threadWaiting = new ArrayList<>(); //Laukiančių gijų būsenos masyvas

	public volatile int id; //Gijos ID, naudojamas keisti savo threadWaiting reikšmę

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
				System.err.println("Parameters: <number threads 1..16> <workload: 16..1_000_000_000> <grainSize: 1..workload> <slowMode: true/false>");
				System.err.println("Not enough parameters: " + Arrays.toString(args));
				workload = 10_000_000;
				slowMode = false;
				double dtime1 = 0d;
				for (int i = 0; i < workload; ++i) { //Masyvo inicializacija 0..workload-1
					data.add(i);
				}
				for (grainSize = 16; grainSize <= 1024; grainSize *= 4) { //Iteruojame grainSize ir nThreads
					System.out.println("#nThreads #workload #grainSize #timeS #speedup");
					for (nThreads = 1; nThreads <= 8; nThreads *= 2) {
						shuffle(data); //Sumaišome masyvą
						double dtime = makePerformanceTest(); //Išrikiuojame masyvą
						dtime1 = nThreads == 1 ? dtime : dtime1;
						double speedup = dtime1 / dtime;
						System.out.println(nThreads + " " + workload + " " + grainSize + " " + dtime + String.format(" %.2f", speedup));
					}
				}

			} else {
				slowMode = Boolean.parseBoolean(args[3]);
				for (int i = 0; i < workload; ++i) { //Masyvo inicializacija 0..workload-1
					data.add(i);
				}
				shuffle(data); //Sumaišome masyvą
				if (slowMode)
					System.out.println(data.toString()); //Išrašome pradinį masyvą

				System.err.println("#Test for: nThreads=" + nThreads + " workload=" + workload + " grainSize=" + grainSize + " slowMode=" + slowMode);
				double dtime = makePerformanceTest();//Išrikiuojame masyvą
				System.err.println("#Completed. Running time: " + dtime + "s");

				if (slowMode)
					System.out.println(data.toString()); //Išrašome išrikiuotą masyvą
				ArrayList<Integer> copy = new ArrayList<>(data);
				Collections.sort(copy);
				System.out.println("Sorted: " + copy.equals(data)); //Patikriname ar masyvas išrikiuotas
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			System.exit(4);
		}
	}

	public static void shuffle(ArrayList<Integer> data) { //Išmaišome masyvą pagal sėklą
		Collections.shuffle(data, new Random(Main.seed));
	}

	static double makePerformanceTest() throws Exception {
		threadWaiting = new ArrayList<>();
		long time0 = System.currentTimeMillis();

		if (nThreads > 1) {
			jobs.add(new Integer[]{0, workload - 1}); //Įdedame pradinį darbą (0, workload-1)
			// Sukuriame ir pradedame gijas
			Main[] aThreads = new Main[nThreads];
			for (int i = 0; i < nThreads; i++) {
				threadWaiting.add(false);
				(aThreads[i] = new Main(i)).start();
			}

			// Laukiame kol visos gijos užbaigs darbą
			for (int i = 0; i < nThreads; i++) {
				aThreads[i].join();
			}
		} else { //Jei dirbs tik 1 gija - darbą atliekame rekursyviai
			quicksortRecursive(0, workload - 1);
		}

		long time1 = System.currentTimeMillis();
		return (time1 - time0) / 1000d;
	}

	public void run() {
		Integer[] job = null;
		while (!jobs.isEmpty() || threadWaiting.contains(false)) { //kol yra darbų arba yra nelaukiančių (dirbančių) gijų
			synchronized (jobs) {
				if (!jobs.isEmpty()) {
					job = jobs.removeFirst();
					threadWaiting.set(id, false); //turime darba - nelaukiame
				}
			}
			if (job == null) continue; //negavome darbo, grįžtame į pradžią, patikriname darbo sąlygas
			if (job[1] - job[0] + 1 <= grainSize) { //jei darbas pakankamai mažas - užbaigiame rekursyviai
				quicksortRecursive(job[0], job[1]);
			} else { //kitu atveju - lygiagrečiai
				quicksortParallel(job[0], job[1]);
			}
			threadWaiting.set(id, true); //baigėme darbą - vėl laukiame

			if (slowMode)
				System.out.println(data + " " + Arrays.toString(job) + " " + this.getName());
			job = null;
		}
	}

	public static void quicksortRecursive(int begin, int end) {
		if (begin < end) {
			int i = partition(begin, end);
			if (begin < i-1)
				quicksortRecursive(begin, i - 1);
			if (i+1 < end)
				quicksortRecursive(i + 1, end);
		}
	}

	public static void quicksortParallel(int begin, int end) {
		if (begin < end) {
			int i = partition(begin, end);
			synchronized (jobs) {
				if (begin < i-1)
					jobs.add(new Integer[]{begin, i - 1});
				if (i+1 < end)
					jobs.add(new Integer[]{i + 1, end});
			}
		}
	}

	public static int partition(int begin, int end) { //klasikinis end-pivot quick-sort partition algoritmas
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