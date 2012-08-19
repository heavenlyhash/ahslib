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

package us.exultant.ahs.io;

import us.exultant.ahs.core.*;
import us.exultant.ahs.util.*;
import us.exultant.ahs.test.*;
import us.exultant.ahs.thread.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

/**
 * General tests that any InputSystem and OutputSystem with matching framers attached to
 * connected channels should be able to pass... and only for networky stuff that uses
 * schedulers and selectors, because the signatures of the factory methods makes it hard
 * to unite tests for those with tests for simpler systems. Does not include tests for
 * framers like HTTP that have metadata as well as a straight data channel, because it's
 * impossible to generalize those.
 * 
 * @author Eric Myhre <tt>hash@exultant.us</tt>
 * 
 */
public class IOSystemTest extends TestCase {
	public static void main(String... $args) { new IOSystemTest().run(); }
	public List<Unit> getUnits() {
		return Arrays.asList(new Unit[] {
				new TestBasic(),
				new TestBasicMultimessage(),
				new TestBasicBig(),
				new TestBidiBig()
		});
	}
	
	
	
	abstract class TestTemplate extends TestCase.Unit {
		protected WorkScheduler $scheduler = new WorkSchedulerFlexiblePriority(8);
		protected SelectionSignaller $selector = new SelectionSignaller(10000);
		private WorkScheduler $ssws = new WorkSchedulerFlexiblePriority(1).start();
		private WorkFuture<Void> $sswf;
		{ $sswf = $selector.schedule($ssws, ScheduleParams.NOW); }
		
		/**
		 * call this at the end of a test in order to stop the schedulers that
		 * were created for this unit. failure to call this method results in
		 * wasted resources and threads outliving their usefulness, but is
		 * otherwise not actually very important.
		 */
		protected void cleanup() {
			$log.debug("cleaning up:");
			$log.trace("cancelling selector...");
			$sswf.cancel(true);
			$log.trace("stopping selector scheduler...");
			$ssws.stop(true);
			$log.trace("stopping main scheduler...");
			$scheduler.stop(true);
			$log.debug("cleanup done");
		}
		
		protected Tup2<SocketChannel,SocketChannel> makeSocketChannelPair() throws IOException {
			ServerSocketChannel $ssc = ServerSocketChannel.open();
			$ssc.socket().bind(null);
			SocketChannel $outsock = SocketChannel.open($ssc.socket().getLocalSocketAddress());
			SocketChannel $insock = $ssc.accept();
			$ssc.close();
			return new Tup2<SocketChannel,SocketChannel>($outsock, $insock);
		}
	}
	
	
	
	/**
	 * 
	 * A pair of (TCP) SocketChannel are constructed; one is read from, and one is
	 * written to (each is used unidirectionally).
	 * 
	 * @author Eric Myhre <tt>hash@exultant.us</tt>
	 * 
	 */
	private class TestBasic extends TestTemplate {
		public Object call() throws IOException {
			// set up ye olde sockets to stuff to test against
			Tup2<SocketChannel,SocketChannel> $socks = makeSocketChannelPair();
			SocketChannel $outsock = $socks.getA();
			SocketChannel $insock = $socks.getB();
			
			// set up the input system!
			$log.debug("setting up InputSystem");
			final DataPipe<ByteBuffer> $incomingPipe = new DataPipe<ByteBuffer>();
			final InputSystem<ByteBuffer> $insys = InputSystem.setup(
					$scheduler,
					$selector,
					$incomingPipe.sink(),
					$insock,
					new ChannelReader.BinaryFramer()
			);
			$insys.getFuture().addCompletionListener(new Listener<WorkFuture<?>>() {
				public void hear(WorkFuture<?> $x) {
					$incomingPipe.source().close();
				}
			});
			
			// set up the output system!
			$log.debug("setting up OutputSystem");
			final DataPipe<ByteBuffer> $outgoingPipe = new DataPipe<ByteBuffer>();
			final OutputSystem<ByteBuffer> $outsys = OutputSystem.setup(
					$scheduler,
					$selector,
					$outgoingPipe.source(),
					$outsock,
					new ChannelWriter.BinaryFramer()
			);
			
			// make test messages
			ByteBuffer $m1 = ByteBuffer.wrap(new byte[] {0x10, 0x20, 0x30, 0x40, 0x50});
			
			// start scheduler behind the IO systems
			$scheduler.start();
			
			// do some writes
			$log.debug("writing chunks...");
			$outgoingPipe.sink().write($m1);
			
			// do some reads, and make assertion
			$log.debug("reading chunks...");
			assertEquals($m1.array(), $incomingPipe.source().read().array());
			
			cleanup();
			return null;
		}
	}
	
	
	
	/**
	 * 
	 * A pair of (TCP) SocketChannel are constructed; one is read from, and one is
	 * written to (each is used unidirectionally); two messages are sent.
	 * 
	 * @author Eric Myhre <tt>hash@exultant.us</tt>
	 * 
	 */
	private class TestBasicMultimessage extends TestTemplate {
		public Object call() throws IOException {
			// set up ye olde sockets to stuff to test against
			Tup2<SocketChannel,SocketChannel> $socks = makeSocketChannelPair();
			SocketChannel $outsock = $socks.getA();
			SocketChannel $insock = $socks.getB();
			
			// set up the input system!
			$log.debug("setting up InputSystem");
			final DataPipe<ByteBuffer> $incomingPipe = new DataPipe<ByteBuffer>();
			final InputSystem<ByteBuffer> $insys = InputSystem.setup(
					$scheduler,
					$selector,
					$incomingPipe.sink(),
					$insock,
					new ChannelReader.BinaryFramer()
			);
			$insys.getFuture().addCompletionListener(new Listener<WorkFuture<?>>() {
				public void hear(WorkFuture<?> $x) {
					$incomingPipe.source().close();
				}
			});
			
			// set up the output system!
			$log.debug("setting up OutputSystem");
			final DataPipe<ByteBuffer> $outgoingPipe = new DataPipe<ByteBuffer>();
			final OutputSystem<ByteBuffer> $outsys = OutputSystem.setup(
					$scheduler,
					$selector,
					$outgoingPipe.source(),
					$outsock,
					new ChannelWriter.BinaryFramer()
			);
			
			// make test messages
			ByteBuffer $m1 = ByteBuffer.wrap(new byte[] {0x10, 0x20, 0x30, 0x40, 0x50});
			ByteBuffer $m2 = ByteBuffer.wrap(new byte[] {0x70, 0x7F, 0x10, 0x00, -0x80});
			
			// start scheduler behind the IO systems
			$scheduler.start();
			
			// do some writes
			$log.debug("writing chunks...");
			$outgoingPipe.sink().write($m1);
			$outgoingPipe.sink().write($m2);
			
			// do some reads, and make assertion
			$log.debug("reading chunks...");
			assertEquals($m1.array(), $incomingPipe.source().read().array());
			assertEquals($m2.array(), $incomingPipe.source().read().array());
			
			cleanup();
			return null;
		}
	}
	
	
	
	/**
	 * 
	 * A pair of (TCP) SocketChannel are constructed; one is read from, and one is
	 * written to (each is used unidirectionally); the data size is sufficient that we
	 * should definitely be filling up the write buffers at some point.
	 * 
	 * @author Eric Myhre <tt>hash@exultant.us</tt>
	 * 
	 */
	private class TestBasicBig extends TestTemplate {
		public Object call() throws IOException {
			// set up ye olde sockets to stuff to test against
			Tup2<SocketChannel,SocketChannel> $socks = makeSocketChannelPair();
			SocketChannel $outsock = $socks.getA();
			SocketChannel $insock = $socks.getB();
			
			// set up the input system!
			$log.debug("setting up InputSystem");
			final DataPipe<ByteBuffer> $incomingPipe = new DataPipe<ByteBuffer>();
			final InputSystem<ByteBuffer> $insys = InputSystem.setup(
					$scheduler,
					$selector,
					$incomingPipe.sink(),
					$insock,
					new ChannelReader.BinaryFramer()
			);
			$insys.getFuture().addCompletionListener(new Listener<WorkFuture<?>>() {
				public void hear(WorkFuture<?> $x) {
					$incomingPipe.source().close();
				}
			});
			
			// set up the output system!
			$log.debug("setting up OutputSystem");
			final DataPipe<ByteBuffer> $outgoingPipe = new DataPipe<ByteBuffer>();
			final OutputSystem<ByteBuffer> $outsys = OutputSystem.setup(
					$scheduler,
					$selector,
					$outgoingPipe.source(),
					$outsock,
					new ChannelWriter.BinaryFramer()
			);
			
			// make test messages
			ByteBuffer $m1 = TestData.getFreshTestData().bb10m.duplicate();
			
			// start scheduler behind the IO systems
			$scheduler.start();
			
			// do some writes
			$log.debug("writing chunks...");
			$outgoingPipe.sink().write($m1.duplicate());
			
			// do some reads, and make assertion
			// note it's tough to use assertEquals on these byte arrays because they're just huge
			$log.debug("reading chunks...");
			assertTrue(Arr.equals(TestData.getFreshTestData().bb10m.array(), $incomingPipe.source().read().array()));
			
			cleanup();
			return null;
		}
	}
	
	
	
	/**
	 * A pair of (TCP) SocketChannel are constructed; each is used for both writing
	 * and reading the other and the same time; the data size is sufficient that we
	 * should definitely be filling up the write buffers at some point.
	 * 
	 * @author Eric Myhre <tt>hash@exultant.us</tt>
	 * 
	 */
	private class TestBidiBig extends TestTemplate {
		public Object call() throws IOException {
			// set up ye olde sockets to stuff to test against
			Tup2<SocketChannel,SocketChannel> $socks = makeSocketChannelPair();
			SocketChannel $sockA = $socks.getA();
			SocketChannel $sockB = $socks.getB();
			
			// set up the input systems!
			$log.debug("setting up InputSystem A");
			final DataPipe<ByteBuffer> $incomingPipeA = new DataPipe<ByteBuffer>();
			final InputSystem<ByteBuffer> $insysA = InputSystem.setup(
					$scheduler,
					$selector,
					$incomingPipeA.sink(),
					$sockA,
					new ChannelReader.BinaryFramer()
			);
			$insysA.getFuture().addCompletionListener(new Listener<WorkFuture<?>>() {
				public void hear(WorkFuture<?> $x) {
					$incomingPipeA.source().close();
				}
			});
			$log.debug("setting up InputSystem B");
			final DataPipe<ByteBuffer> $incomingPipeB = new DataPipe<ByteBuffer>();
			final InputSystem<ByteBuffer> $insysB = InputSystem.setup(
					$scheduler,
					$selector,
					$incomingPipeB.sink(),
					$sockB,
					new ChannelReader.BinaryFramer()
			);
			$insysB.getFuture().addCompletionListener(new Listener<WorkFuture<?>>() {
				public void hear(WorkFuture<?> $x) {
					$incomingPipeB.source().close();
				}
			});
			
			// set up the output systems!
			$log.debug("setting up OutputSystem A");
			final DataPipe<ByteBuffer> $outgoingPipeA = new DataPipe<ByteBuffer>();
			final OutputSystem<ByteBuffer> $outsysA = OutputSystem.setup(
					$scheduler,
					$selector,
					$outgoingPipeA.source(),
					$sockA,
					new ChannelWriter.BinaryFramer()
			);
			$log.debug("setting up OutputSystem B");
			final DataPipe<ByteBuffer> $outgoingPipeB = new DataPipe<ByteBuffer>();
			final OutputSystem<ByteBuffer> $outsysB = OutputSystem.setup(
					$scheduler,
					$selector,
					$outgoingPipeB.source(),
					$sockB,
					new ChannelWriter.BinaryFramer()
			);
			
			// make test messages
			ByteBuffer $m1 = TestData.getFreshTestData().bb10m.duplicate();
			
			// start scheduler behind the IO systems
			$scheduler.start();
			
			// do some writes
			$log.debug("writing chunks...");
			$outgoingPipeA.sink().write($m1.duplicate());
			$outgoingPipeB.sink().write($m1.duplicate());
			$outgoingPipeB.sink().write($m1.duplicate());
			$outgoingPipeA.sink().write($m1.duplicate());
			
			// do some reads, and make assertion
			$log.debug("reading chunks...");
			assertTrue(Arr.equals($m1.array(), $incomingPipeA.source().read().array()));
			assertTrue(Arr.equals($m1.array(), $incomingPipeA.source().read().array()));
			assertFalse($incomingPipeA.source().hasNext());
			assertTrue(Arr.equals($m1.array(), $incomingPipeB.source().read().array()));
			assertTrue(Arr.equals($m1.array(), $incomingPipeB.source().read().array()));
			assertFalse($incomingPipeB.source().hasNext());
			
			cleanup();
			return null;
		}
	}
}