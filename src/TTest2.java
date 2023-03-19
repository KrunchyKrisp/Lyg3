/** Simple system multithreading performance test.
 Version 1.3
 Testuojamas masyvas
 @author R.Vaicekauskas
 */
public class TTest2 extends Thread
{
	volatile boolean finished = false;
	int idThread;
	public TTest2(int idThread) {this.idThread = idThread;}

	public final static int SIZE = 10000000;
	static int[] ar = new int[SIZE];

	//-----------------------------------------
	// work unit for 1 thread
	boolean work ()
	{
		int count = SIZE / nThreads;
		int iFrom = count * idThread;
		int iTo   = iFrom + count;
		if (iTo >=SIZE) iTo = SIZE;

		int s = 0;
		for (int iPass = 0; iPass < TTest2.workload; iPass++)
		{
			for (int i = iFrom; i < iTo; i++)
			{
				s += ar[i];
			}
		}
		return s < 0;
	}

	//-----------------------------------------
	// Thread.run()
	public void run()
	{
		// To have total work independed of the number of threads

		boolean doPrint = work();
		if (doPrint) System.out.println("Never be printed");

		//System.out.println(this+" finished");
		this.finished = true;
	}

	//-----------------------------------------
	// Global parameters
	static int workload = 0;
	static int nThreads = 0; // Number working threads

	//-----------------------------------------
	// Make tests for given workload and number threads
	// Returns working time
	static double makePerformanceTest() throws Exception
	{
		long time0 = System.currentTimeMillis();

		// Create and start threads
		TTest2[] aThreads = new TTest2[nThreads];
		for (int i = 0; i < nThreads; i++)
		{
			(aThreads[i] = new TTest2(i)).start();
		}

		// Wait until all threads finish
		for (int i = 0; i < nThreads; i++)
		{
			while (!aThreads[i].finished)
			{
				aThreads[i].join();
			}
		}
		long time1 = System.currentTimeMillis();
		double dtime = (time1-time0)/1000.;
		return dtime;
	}

	//-----------------------------------------
	public static void main(String[] args)
	{
		try
		{
			if (args.length < 2
					|| ! ( (nThreads  = Integer.parseInt(args[0])) >=1 && nThreads  <= 16 &&
					(workload = Integer.parseInt(args[1])) >=1 && workload <= 100000000))
			{
				System.err.println("Simple system multithreading performance test. Ver 1.3");
				System.err.println("Parameters: <number threads 1..16> <workload: 1..100000000>");

				System.err.println("#Make auto test: find workload for > 1 sec...");

				TTest2.nThreads = 1;
				TTest2.workload = 1;

				for (double dtime = 0; ; workload *= 2)
				{
					dtime = makePerformanceTest();
					if (dtime > 1.) break; //>>>>>>>
				}

				System.out.println("#nThreads #workload #timeS #speedup");

				double dtime1=0.;
				for (nThreads = 1; nThreads <= 16; nThreads *= 2)
				{
					double dtime = makePerformanceTest();
					dtime1 = nThreads==1 ? dtime : dtime1;
					double speedup = dtime1 / dtime;
					System.out.println( nThreads + " " + workload  + " " +dtime + " " + speedup);
				}

				System.out.println("#completed");
				System.exit(1); //>>>>>>>
			}
			else
			{
				System.err.println("#Test for: nThreads="+nThreads+" workload="+workload);
				double dtime = makePerformanceTest();
				System.err.println("#Completed. Running time: " + dtime + "s");
				System.out.println( nThreads + " " + workload  + " " +dtime );
				System.exit(0); //>>>>>>>
			}
		}
		catch (Exception exc)
		{
			System.out.println(exc);
			exc.printStackTrace();
			System.exit(4);
		}
	}
}