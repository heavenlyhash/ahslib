package us.exultant.ahs.io;

import us.exultant.ahs.log.*;
import us.exultant.ahs.test.*;
import java.io.*;
import java.nio.channels.*;
import java.util.*;

public class ReadHeadSocketChannelTest extends TestCase {
	public static void main(String... $args) {					new ReadHeadSocketChannelTest().run();				}
	public ReadHeadSocketChannelTest() {						super(new Logger(Logger.LEVEL_DEBUG), true);	}
	public ReadHeadSocketChannelTest(Logger $log, boolean $enableConfirmation) {	super($log, $enableConfirmation);		}
	public List<Unit> getUnits() {
		List<Unit> $tests = new ArrayList<Unit>();
		$tests.add(new TestBasic());
		$tests.add(new TestSocketReceive());
		return $tests;
	}
	
	private class TestBasic extends TestCase.Unit {
		PumperSelector $ps;
		ReadHeadSocketChannel $rhsc;
		public Object call() throws IOException {
			$ps = new PumperSelector();
			$ps.start();
			
			$rhsc = new ReadHeadSocketChannel(null, $ps);
			return null;
		}
	}
	
	private class TestSocketReceive extends TestBasic {
		SocketChannel $sca0;
		public Object call() throws IOException {
			super.call();
			
			$sca0 = SocketChannel.open();
			$sca0.connect($rhsc.getServerSocketChannel().getLocalAddress());
			
			SocketChannel $sca1 = $rhsc.read();	// this may block forever if something is wrong
			
			return null;
		}
	}
}