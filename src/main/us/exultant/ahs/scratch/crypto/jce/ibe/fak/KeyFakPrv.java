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

package us.exultant.ahs.scratch.crypto.jce.ibe.fak;

import us.exultant.ahs.core.*;
import us.exultant.ahs.util.*;
import us.exultant.ahs.scratch.crypto.jce.ibe.*;
import us.exultant.ahs.codec.eon.pre.*;

public class KeyFakPrv implements KeyIbePrv {
	public KeyFakPrv(BitVector $bat) {
		$x = $bat;
		try {
			$serial = BitVectorDencoder.ENCODER.encode(KeySystemIbeFak.HACK, $x).serialize();
		} catch (TranslationException $e) {
			throw new MajorBug("what the hell.");
		}
	}
	
	private final BitVector $x;
	private final byte[] $serial;
	
	public String getAlgorithm() {
		return KeySystemIbeFak.ALGORITHM;
	}
	
	/** DNMR OMFG DNMR */
	public byte[] getEncoded() {
		return $serial;
	}
	
	public String getFormat() {
		return KeySystemIbeFak.ALGORITHM;
	}
	
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.$x == null) ? 0 : this.$x.hashCode());
		return result;
	}
	
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		KeyFakPrv other = (KeyFakPrv) obj;
		if (this.$x == null) {
			if (other.$x != null) return false;
		} else if (!this.$x.equals(other.$x)) return false;
		return true;
	}
}
