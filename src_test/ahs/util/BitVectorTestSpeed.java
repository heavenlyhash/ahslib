package ahs.util;

import ahs.test.*;
import ahs.io.*;
import ahs.io.codec.*;
import ahs.io.codec.json.*;

import java.util.*;

public class BitVectorTestSpeed extends JUnitTestCase {
	public static final int LOTS = 1000000;
	
	//LOTS = 100000000 =>
	//0.00102737
	//0.00105995	// i think this one is funny but 'm too lazy to run it again
	//0.00172712
	
	//LOTS = 1000000 =>
	//0.001178
	//0.001106
	//0.00182
	
	
	public void testToByteArray_t1() {
		long $start = X.time();
		
		BitVector $bv;
		for (int $i = 0; $i < LOTS; $i++) {
			$bv = new BitVector("1");
			assertEquals(-128,$bv.toByteArray_t1()[0]);
			
			$bv = new BitVector("00000001");
			assertEquals(1,$bv.toByteArray_t1()[0]);
			
			$bv = new BitVector("00001011");
			assertEquals(11,$bv.toByteArray_t1()[0]);
			
			$bv = new BitVector("0000101100000000");
			assertEquals(11,$bv.toByteArray_t1()[0]);
			assertEquals(0,$bv.toByteArray_t1()[1]);
			
			$bv = new BitVector("000000010000001111111111");
			assertEquals(24,$bv.length());
			assertEquals(1,$bv.toByteArray_t1()[0]);
			assertEquals(3,$bv.toByteArray_t1()[1]);
			assertEquals(-1,$bv.toByteArray_t1()[2]);
		}
		
		double $long = (X.time()-$start)/(double)LOTS;
		X.say($long+"");
	}
	public void testToByteArray_t3() {
		long $start = X.time();
		
		BitVector $bv;
		for (int $i = 0; $i < LOTS; $i++) {
			$bv = new BitVector("1");
			assertEquals(-128,$bv.toByteArray_t3()[0]);
			
			$bv = new BitVector("00000001");
			assertEquals(1,$bv.toByteArray_t3()[0]);
			
			$bv = new BitVector("00001011");
			assertEquals(11,$bv.toByteArray_t3()[0]);
			
			$bv = new BitVector("0000101100000000");
			assertEquals(11,$bv.toByteArray_t3()[0]);
			assertEquals(0,$bv.toByteArray_t3()[1]);
			
			$bv = new BitVector("000000010000001111111111");
			assertEquals(24,$bv.length());
			assertEquals(1,$bv.toByteArray_t3()[0]);
			assertEquals(3,$bv.toByteArray_t3()[1]);
			assertEquals(-1,$bv.toByteArray_t3()[2]);
		}

		double $long = (X.time()-$start)/(double)LOTS;
		X.say($long+"");
	}
	
	public void testByteArrayConstructors() {
		long $start = X.time();
		
		byte[] $t;
		BitVector $bv;
		for (int $i = 0; $i < LOTS; $i++) {
			$t = new byte[] {0x0};
			$bv = new BitVector($t);
			assertEquals("00000000",$bv.toString());
			assertEquals($t, $bv.toByteArray());
			
			$t = new byte[] {0x21};
			$bv = new BitVector(new byte[] {0x21});
			assertEquals("00100001",$bv.toString());
			assertEquals($t, $bv.toByteArray());
			
			$t = new byte[] {0x21, -0x01, 0x21};
			$bv = new BitVector($t);
			assertEquals("001000011111111100100001",$bv.toString());
			assertEquals($t, $bv.toByteArray());
			
			$bv = new BitVector(new byte[] {0x21, -0x01, 0x21},4,15);
			assertEquals("000111111111001",$bv.toString());
		}

		double $long = (X.time()-$start)/(double)LOTS;
		X.say($long+"");
	}
}