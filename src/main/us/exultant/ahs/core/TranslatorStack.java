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

import java.util.*;

/**
 * Helper class to construct a type-safe chain of Translator instances at runtime that can
 * then act as a single coherent Translator from end to end.
 *
 * @author Eric Myhre <tt>hash@exultant.us</tt>
 *
 * @param <$FROM>
 * @param <$TO>
 */
public class TranslatorStack<$FROM, $TO> implements Translator<$FROM, $TO> {
	// these factory methods are ugly in their repetitiveness, but also perfect in their spectacular genericness.
	// and if you want more than six Translator in a chain, then fuck you.
	public static <$I,$O,$T1> TranslatorStack<$I,$O> make(Translator<$I,$T1> $t0, Translator<$T1,$O> $t1) {
		TranslatorStack<$I,$O> $v = new TranslatorStack<$I,$O>(2);
		$v.$dat.add($t0);
		$v.$dat.add($t1);
		return $v;
	}
	public static <$I,$O,$T1,$T2> TranslatorStack<$I,$O> make(Translator<$I,$T1> $t0, Translator<$T1,$T2> $t1, Translator<$T2,$O> $t2) {
		TranslatorStack<$I,$O> $v = new TranslatorStack<$I,$O>(3);
		$v.$dat.add($t0);
		$v.$dat.add($t1);
		$v.$dat.add($t2);
		return $v;
	}
	public static <$I,$O,$T1,$T2,$T3> TranslatorStack<$I,$O> make(Translator<$I,$T1> $t0, Translator<$T1,$T2> $t1, Translator<$T2,$T3> $t2, Translator<$T3,$O> $t3) {
		TranslatorStack<$I,$O> $v = new TranslatorStack<$I,$O>(4);
		$v.$dat.add($t0);
		$v.$dat.add($t1);
		$v.$dat.add($t2);
		$v.$dat.add($t3);
		return $v;
	}
	public static <$I,$O,$T1,$T2,$T3,$T4> TranslatorStack<$I,$O> make(Translator<$I,$T1> $t0, Translator<$T1,$T2> $t1, Translator<$T2,$T3> $t2, Translator<$T3,$T4> $t3, Translator<$T4,$O> $t4) {
		TranslatorStack<$I,$O> $v = new TranslatorStack<$I,$O>(5);
		$v.$dat.add($t0);
		$v.$dat.add($t1);
		$v.$dat.add($t2);
		$v.$dat.add($t3);
		$v.$dat.add($t4);
		return $v;
	}
	public static <$I,$O,$T1,$T2,$T3,$T4,$T5> TranslatorStack<$I,$O> make(Translator<$I,$T1> $t0, Translator<$T1,$T2> $t1, Translator<$T2,$T3> $t2, Translator<$T3,$T4> $t3, Translator<$T4,$T5> $t4, Translator<$T5,$O> $t5) {
		TranslatorStack<$I,$O> $v = new TranslatorStack<$I,$O>(6);
		$v.$dat.add($t0);
		$v.$dat.add($t1);
		$v.$dat.add($t2);
		$v.$dat.add($t3);
		$v.$dat.add($t4);
		$v.$dat.add($t5);
		return $v;
	}

	private TranslatorStack(int $size) {
		$dat = new ArrayList<Translator<?,?>>($size);
	}
	private List<Translator<?,?>> $dat;

	@SuppressWarnings("unchecked")	// though runtime safety is unenforceable here because of generic type erasure, reasonable compilers enforce invariants at the factory methods.
	public $TO translate($FROM $x) throws TranslationException {
		Object $v = $x;
		for (Translator<?,?> $t : $dat) {
			$v = ((Translator<Object,Object>)$t).translate($v);
			if ($v == null) break;
		}
		return ($TO)$v;
	}
}
