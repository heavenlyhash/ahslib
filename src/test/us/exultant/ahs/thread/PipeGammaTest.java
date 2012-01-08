/*
 * Copyright 2010, 2011 Eric Myhre <http://exultant.us>
 * 
 * This file is part of AHSlib.
 *
 * AHSlib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License, or
 * (at the original copyright holder's option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package us.exultant.ahs.thread;

import us.exultant.ahs.util.*;
import us.exultant.ahs.log.*;
import us.exultant.ahs.test.*;
import java.util.*;

public class PipeGammaTest extends TestCase {
	public static void main(String... $args) {			new PipeGammaTest().run();				}
	public PipeGammaTest() {						super(new Logger(Logger.LEVEL_DEBUG), true);	}
	public PipeGammaTest(Logger $log, boolean $enableConfirmation) {	super($log, $enableConfirmation);		}
	public List<Unit> getUnits() {
		List<Unit> $tests = new ArrayList<Unit>();
		$tests.add(new TestBasic());
		$tests.add(new TestBasic_WriteAll());
		TestBasic_WriteAllPartial $wap = new TestBasic_WriteAllPartial();
		$tests.add($wap.new Part1());
		$tests.add($wap.new Part2());
		$tests.add(new TestBasicClose_WriteAfter());
		$tests.add(new TestBasicClose_ReadAfterCloseReturns());
		$tests.add(new TestConcurrent_ReadBlockBeforeWrite());
		$tests.add(new TestConcurrent_ReadWriteBlocking());
		$tests.add(new TestConcurrent_Close());
		return $tests;
	}
	private static final TestData TD = TestData.getFreshTestData();
	
	/** Just tests mixed read and writes in a single thread. */
	private class TestBasic extends TestCase.Unit {
		public Object call() {
			Pipe<String> $pipe = new Pipe<String>();
			$pipe.SINK.write(TD.s1);
			$pipe.SINK.write(TD.s2);
			assertEquals(2, $pipe.size());
			breakIfFailed();
			assertEquals(TD.s1, $pipe.SRC.read());
			$pipe.SINK.write(TD.s3);
			assertEquals(2, $pipe.size());
			assertEquals(TD.s2, $pipe.SRC.read());
			assertEquals(TD.s3, $pipe.SRC.read());
			assertEquals(0, $pipe.size());
			return null;
		}
	}
	
	/** Tests the group writing of collected chunks. */
	private class TestBasic_WriteAll extends TestCase.Unit {
		public Object call() {
			Pipe<String> $pipe = new Pipe<String>();
			$pipe.SINK.write(TD.s1);
			$pipe.SINK.writeAll(Arr.asList(TD.s2,TD.s2,TD.s3));
			assertEquals(4, $pipe.size());
			breakIfFailed();
			List<String> $arr = $pipe.SRC.readAllNow();
			assertEquals(0, $pipe.size());
			assertEquals(TD.s1, $arr.get(0));
			assertEquals(TD.s2, $arr.get(1));
			assertEquals(TD.s2, $arr.get(2));
			assertEquals(TD.s3, $arr.get(3));
			return null;
		}
	}
	
	/** Tests the consistency after a group write throws an exception from the middle of the operation. */
	private class TestBasic_WriteAllPartial {
		Pipe<String> $pipe = new Pipe<String>();
		
		/** Tests that yes, an exception is thrown. */
		private class Part1 extends TestCase.Unit {
			@SuppressWarnings("unchecked")
			public Class<NullPointerException> expectExceptionType() {
				return NullPointerException.class;
			}
			public Object call() {
				$pipe.SINK.write(TD.s1);
				$pipe.SINK.writeAll(Arr.asList(TD.s2,null,TD.s3));
				return null;
			}
		}
		
		/** Tests that the Pipe's size and contents are still consistent, and that it contains exactly the elements preceeding the one that caused the exception. */
		private class Part2 extends TestCase.Unit {
			public Object call() {
				assertEquals(2, $pipe.size());
				breakIfFailed();
				List<String> $arr = $pipe.SRC.readAllNow();
				assertEquals(0, $pipe.size());
				assertEquals(TD.s1, $arr.get(0));
				assertEquals(TD.s2, $arr.get(1));
				return null;
			}
		}
	}
	
	/** Tests that attempting to write after closing a pipe throws an exception. */
	private class TestBasicClose_WriteAfter extends TestCase.Unit {
		@SuppressWarnings("unchecked")
		public Class<IllegalStateException> expectExceptionType() {
			return IllegalStateException.class;
		}
		public Object call() {
			Pipe<String> $pipe = new Pipe<String>();
			$pipe.SINK.write(TD.s1);
			$pipe.SINK.write(TD.s2);
			assertEquals(TD.s1, $pipe.SRC.read());
			$pipe.SINK.close();
			$pipe.SINK.write(TD.s3);	// this should throw
			return null;
		}
	}
	
	private class TestBasicClose_ReadAfterCloseReturns extends TestCase.Unit {
		public Object call() {
			Pipe<String> $pipe = new Pipe<String>();
			$pipe.SINK.write(TD.s1);
			$pipe.SINK.write(TD.s2);
			assertEquals(2, $pipe.size());
			assertEquals(TD.s1, $pipe.SRC.read());
			$pipe.SINK.close();
			assertEquals(TD.s2, $pipe.SRC.read());
			assertEquals(0, $pipe.size());
			assertEquals(null, $pipe.SRC.readNow());
			breakIfFailed();	// we'd rather not block on this next call if we already know there's something wrong.
			assertEquals(null, $pipe.SRC.read());		// this may block forever if something's broken
			assertEquals(0, $pipe.SRC.readAll().size());	// this may block forever if something's broken
			return null;
		}
	}
	
	private class TestConcurrent_ReadBlockBeforeWrite extends TestCase.Unit {
		Pipe<String> $pipe = new Pipe<String>();
		volatile boolean $won = false;
		
		public Object call() {
			new Thread() { public void run() {
					$pipe.source().read();
					$won = true;
			}}.start();
			$pipe.SINK.write(TD.s1);
			while (!$won) X.chill(5);
			// honestly, just making it out of here alive is test enough.
			return null;
		}	
	}
	
	private class TestConcurrent_ReadWriteBlocking extends TestCase.Unit {
		Pipe<String> $pipe = new Pipe<String>();
		ConcurrentCounter<String> $counter = ConcurrentCounter.make(Arr.asList(TD.s1, TD.s2, TD.s3));
		final int n = 100000000;
		
		// VAGUE PERFORMANCE OBSERVATIONS (at n=1000000):
		//  with the modern generation of flippable-semaphore-based pipes:
		//   about (min;432k; max:663k; ave:541k)/sec on a 2.7ghz+4core+ubuntu11.04; about 95% of all cores utilized (~5% kernel, ~90% userspace).
		//   performance remains in that range when increasing n another 100x, as well, if you're wondering.
		//   of course if you drop n it goes to crap: n=100:~7k/sec; n=1000:~20k/sec; n=10000:~40k/sec; n=100000:~174k/sec; 
		//  with the older generation of interrupt-based pipes:
		//   about (min:23k;  max:35k;  ave:27k)/sec  on a 2.7ghz+4core+ubuntu10.10; only about 50% of 2 cores utilized (20% userspace, 30% kernel). 
		
		public Object call() {
			long $start = X.time();
			Runnable[] $tasks = new Runnable[4];
			$tasks[0] = new Reader();
			$tasks[1] = new Reader();
			$tasks[2] = new Writer(TD.s1);
			$tasks[3] = new Writer(TD.s2);
			ThreadUtil.doAll($tasks);
			assertEquals(n, $counter.getCount(TD.s1));
			assertEquals(n, $counter.getCount(TD.s2));
			assertEquals(0, $counter.getCount(TD.s3));
			long $time = X.time() - $start;
			$log.info("performance", ((n/1000.0)/($time/1000.0))+"k/sec");
			return null;
		}
		

		private class Writer implements Runnable {
			public Writer(String $str) { this.$str = $str; }
			private String $str;
			public void run() {
				for (int $i = 0; $i < n; $i++) {
					$pipe.SINK.write($str);
					$log.trace(this, "wrote \""+$str+"\", pipe size now "+$pipe.size());
				}
				$log.trace(this, "writing thread done.");
			}
		}
		private class Reader implements Runnable {
			public Reader() {}
			public void run() {
				for (int $i = 0; $i < n; $i++) {
					String $lol = $pipe.SRC.read();
					$log.trace(this, "read \""+$lol+"\", pipe size now "+$pipe.size());
					$counter.hear($lol);
				}
				$log.trace(this, "reading thread done.");
			}
		}
	}

	private class TestConcurrent_Close extends TestCase.Unit {
		Pipe<String> $pipe = new Pipe<String>();
		ConcurrentCounter<String> $counter = ConcurrentCounter.make(Arr.asList(TD.s1));
		final int n = 10000000;
		final int n2 = 500;
		
		public Object call() {
			Runnable[] $tasks = new Runnable[4];
			$tasks[0] = new Writer(TD.s1);	// puts 2n+n2
			$tasks[1] = new Reader();	// consumes up to n
			$tasks[2] = new Reader();	// consumes up to n
			$tasks[3] = new FinalReader();	// consumes some arbitrary amount based on thread scheduling, minimum n2.
			ThreadUtil.doAll($tasks);
			assertEquals(2*n+n2, $counter.getCount(TD.s1));
			return null;
		}
		

		private class Writer implements Runnable {
			public Writer(String $str) { this.$str = $str; }
			private String $str;
			public void run() {
				for (int $i = 0; $i < (2*n)+n2; $i++)
					$pipe.SINK.write($str);
				$pipe.close();
			}
		}
		private class Reader implements Runnable {
			public Reader() {}
			public void run() {
				for (int $i = 0; $i < n; $i++)
					$counter.hear($pipe.SRC.read());
			}
		}
		private class FinalReader implements Runnable {
			public FinalReader() {}
			public void run() {
				for (String $s : $pipe.SRC.readAll())
					$counter.hear($s);
			}
		}
	}
	
	// if a pipe is fed, closed, and then drained, we should see exactly n+2 events (one for the closure, one for the final drain, and one for each of the (unbatched) writes)... even if there is more than one person trying to get that last read.
	//  jk, that's all impossible because pipes can't be arsed to check that that final drain event is a once-only.
}