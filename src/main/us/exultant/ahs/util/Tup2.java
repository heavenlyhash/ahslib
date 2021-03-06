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

public class Tup2<A, B> {
	public Tup2(A $a, B $b) {
		this.$a = $a;
		this.$b = $b;
	}

	public final A $a;
	public final B $b;

	public A getA() { return $a; }
	public B getB() { return $b; }

	public String toString() {
		return "Tup2[$a=" + this.$a + ", $b=" + this.$b + "]";
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.$a == null) ? 0 : this.$a.hashCode());
		result = prime * result + ((this.$b == null) ? 0 : this.$b.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Tup2<?,?> other = (Tup2<?,?>) obj;
		if (this.$a == null) {
			if (other.$a != null) return false;
		} else if (!this.$a.equals(other.$a)) return false;
		if (this.$b == null) {
			if (other.$b != null) return false;
		} else if (!this.$b.equals(other.$b)) return false;
		return true;
	}
}
