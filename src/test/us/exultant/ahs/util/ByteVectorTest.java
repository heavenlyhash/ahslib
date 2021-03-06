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

package us.exultant.ahs.util;

import us.exultant.ahs.test.*;
import java.util.*;

public class ByteVectorTest extends TestCase {
	public static void main(String... $args) {
		new BitVectorTest().run();
	}

	public List<Unit> getUnits() {
		return Arr.asList(
				new TestSanity(),
				new TestGet(),
				new TestShift(),
				new TestUnshift(),
				new TestPop(),
				new TestPush(),
				new TestCatConstructor()
		);
	}



	private static final byte[] TARR0	= new byte[] { 0x12, 0x34, (byte)0xFF, -0x80, (byte)0xE4, 0x00 };
	private static final byte[] TARR0_0_3	= new byte[] { 0x12, 0x34, (byte)0xFF };
	private static final byte[] TARR0_3_6	= new byte[] { -0x80, (byte)0xE4, 0x00 };
	private static final byte[] TARR1	= new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09 };

	private class TestSanity extends TestCase.Unit {
		public void call() {
			assertEquals(1,new byte[new byte[1].length].length);
		}
	}

	private class TestGet extends TestCase.Unit {
		public void call() {
			ByteVector $bv = new ByteVector(TARR0);
			assertEquals(TARR0_0_3,$bv.get(0,3).getByteArray());
			assertEquals(TARR0_3_6,$bv.get(3,3).getByteArray());
			assertEquals(TARR0_3_6,$bv.getRange(3,6).getByteArray());
			assertEquals(TARR0_3_6,$bv.getRangeArray(3,6));
		}
	}

	private class TestShift extends TestCase.Unit {
		public void call() {
			ByteVector $bv = new ByteVector(TARR1);
			assertEquals(0x01,$bv.shift());
			assertEquals(0x02,$bv.shift());
			assertEquals(7,$bv.length());
			assertEquals(new byte[] { 0x03, 0x04, 0x05 },$bv.shift(3).getByteArray());
		}
	}

	private class TestUnshift extends TestCase.Unit {
		public void call() {
			ByteVector $bv = new ByteVector(TARR1);
			$bv.unshift((byte)12);
			$bv.unshift((byte)13);
			$bv.unshift((byte)14);
			assertEquals(12,$bv.length());
			assertEquals(new byte[] { 14, 13, 12, 0x01, 0x02 },$bv.get(0,5).getByteArray());
			$bv.unshift(new ByteVector(new byte[] { 34, 35 }));
			assertEquals(new byte[] { 34, 35, 14, 13, 12 },$bv.get(0,5).getByteArray());
		}
	}

	private class TestPop extends TestCase.Unit {
		public void call() {
			ByteVector $bv = new ByteVector(TARR1);
			assertEquals(0x09,$bv.pop());
			assertEquals(0x08,$bv.pop());
			assertEquals(7,$bv.length());
			assertEquals(new byte[] { 0x05, 0x06, 0x07 },$bv.pop(3).getByteArray());
		}
	}

	private class TestPush extends TestCase.Unit {
		public void call() {
			ByteVector $bv = new ByteVector(TARR1);
			$bv.push((byte)12);
			$bv.push((byte)13);
			$bv.push((byte)14);
			assertEquals(12,$bv.length());
			assertEquals(new byte[] { 8, 9, 12, 13, 14 },$bv.getRange(7,12).getByteArray());
			$bv.push(new ByteVector(new byte[] { 34, 35 }));
			assertEquals(new byte[] { 13, 14, 34, 35 },$bv.getRange(10,14).getByteArray());
		}
	}

	private class TestCatConstructor extends TestCase.Unit {	// purr
		public void call() {
			ByteVector $bv = new ByteVector(TARR0_0_3, TARR0_3_6, TARR1);
			assertEquals(Arr.cat(TARR0, TARR1), $bv.getByteArray());
		}
	}
}
