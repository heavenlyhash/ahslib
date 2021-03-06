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

package us.exultant.ahs.core;

import java.io.*;

public interface ExceptionHandler<$T extends Throwable> extends Listener<$T> {
	/**
	 * Hear (and respond to) the given Throwable.
	 *
	 * @param $e
	 */
	public void hear($T $e);




	// unfortunately, this isn't very useful unless you can accept a super-general handler, which is something you'd usually prefer to avoid.
	// or you can just cast it.
	public static final ExceptionHandler<? extends Throwable> STDERR = new ExceptionHandler<Throwable>() {
		/**
		 * Punts the Throwable's stack trace to the standard error stream.
		 */
		public void hear(Throwable $e) {
			$e.printStackTrace();
		}
	};

	public static final ExceptionHandler<IOException> STDERR_IOEXCEPTION = new ExceptionHandler<IOException>() {
		/**
		 * Punts the Throwable's stack trace to the standard error stream.
		 */
		public void hear(IOException $e) {
			$e.printStackTrace();
		}
	};

	// notice that there's no pre-made convenient "SHH" ExceptionHandler.  that's because it's a bad fucking idea.
	// if you really, really, REALLY think that it should be impossible to get an exception from a piece of code that needs one of these for interface's sake,
	//   then give it a STDERR one anyway (it'll be silent, right?), or just give it null so you can at least get a NullPointerException and know your assumption was wrong.
}
