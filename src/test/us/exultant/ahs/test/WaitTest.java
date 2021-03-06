/*
 * Copyright 2010 - 2013 Eric Myhre <http://exultant.us>
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

package us.exultant.ahs.test;

import us.exultant.ahs.util.*;
import java.util.*;

public class WaitTest extends TestCase {
	public static void main() { new SanityTest().run(); }

	public List<Unit> getUnits() {
		List<Unit> $tests = new ArrayList<Unit>();
		$tests.add(new TestA());
		$tests.add(new TestA2());
		$tests.add(new TestA3());
		$tests.add(new TestB());
		$tests.add(new TestB10());
		$tests.add(new TestC());
		return $tests;
	}



	private abstract class LoopedWaitTemplate extends TestCase.Unit {
		public void wait(final int $pause_ms, final int $loop_times, final int $error_margin) {
			long $start = X.time();
			for (int $i = 0; $i < $loop_times; $i++)
				X.chill($pause_ms);
			assertEquals($start+($pause_ms*$loop_times), X.time(), $error_margin);
		}
	}



	private class TestA extends LoopedWaitTemplate {
		public void call() {
			wait(50,10,10);
		}
	}



	private class TestA2 extends LoopedWaitTemplate {
		public void call() {
			wait(50,10,3);
		}
	}



	private class TestA3 extends LoopedWaitTemplate {
		public void call() {
			wait(50,10,1);
		}
	}



	private class TestB extends LoopedWaitTemplate {
		public void call() {
			wait(20,20,2);
		}
	}



	private class TestB10 extends LoopedWaitTemplate {
		public void call() {
			for (int $i = 0; $i < 10; $i++)
				wait(20,20,2);
		}
	}



	private class TestC extends LoopedWaitTemplate {
		public void call() {
			wait(500,10,5);
		}
	}
}
