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

import static java.lang.Math.max;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

public class Strings {
	public static final Charset	UTF_8		= Charset.forName("UTF-8");
	public static final Charset	ASCII		= Charset.forName("ASCII");
	public static final char[]	HEX_CHARS	= new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };



//////////////////////////////////////////////////////////////// TRANSLATION FUCTIONS
	// some of this functionality is already readily available, but these differ in that default charset always means utf-8

	public static final String fromBytes(byte[] $bats, Charset $cs) {
		return new String($bats, $cs);
	}

	public static final String fromBytes(byte[] $bats) {
		return new String($bats, UTF_8);
	}

	public static final String fromBytes(ByteBuffer $bats, Charset $cs) {
		return new String(Arr.toArray($bats), $cs);
	}

	public static final String fromBytes(ByteBuffer $bats) {
		return new String(Arr.toArray($bats), UTF_8);
	}

	public static final byte[] toBytes(String $s) {
		return $s.getBytes(UTF_8);
	}

//////////////////////////////////////////////////////////////// PARTING FUCTIONS
	// default to returning the original string if the pattern is not found

	public static final String getPartAfter(String $source, String $pattern) {
		int $index = $source.indexOf($pattern);
		if ($index < 0) return $source;
		return $source.substring($index+$pattern.length());
	}

	public static final String getPartAfterOrEmpty(String $source, String $pattern) {
		int $index = $source.indexOf($pattern);
		if ($index < 0) return "";
		return $source.substring($index+$pattern.length());
	}

	public static final String getPartBefore(String $source, String $pattern) {
		int $index = $source.indexOf($pattern);
		if ($index < 0) return $source;
		return $source.substring(0,$index);
	}

	public static final String getPartBeforeOrEmpty(String $source, String $pattern) {
		int $index = $source.indexOf($pattern);
		if ($index < 0) return "";
		return $source.substring(0,$index);
	}

	public static final String getPartBetween(String $source, String $startPattern, String $endPattern) {
		return getPartBeforeLast(getPartAfter($source,$startPattern),$endPattern);
	}

	public static final String getPartAfterLast(String $source, String $pattern) {
		int $index = $source.lastIndexOf($pattern);
		if ($index < 0) return $source;
		return $source.substring($index+$pattern.length());
	}

	public static final String getPartAfterLastOrEmpty(String $source, String $pattern) {
		int $index = $source.lastIndexOf($pattern);
		if ($index < 0) return "";
		return $source.substring($index+$pattern.length());
	}

	public static final String getPartBeforeLast(String $source, String $pattern) {
		int $index = $source.lastIndexOf($pattern);
		if ($index < 0) return $source;
		return $source.substring(0,$index);
	}

	public static final String getPartBeforeLastOrEmpty(String $source, String $pattern) {
		int $index = $source.lastIndexOf($pattern);
		if ($index < 0) return "";
		return $source.substring(0,$index);
	}

	public static final String[] splitOnNext(String $source, String $pattern) {
		int $index = $source.indexOf($pattern);
		if ($index < 0) return new String[] {"",$source};
		return new String[] {$source.substring(0,$index),$source.substring($index+$pattern.length())};
	}

	public static final String[] splitOnLast(String $source, String $pattern) {
		int $index = $source.lastIndexOf($pattern);
		if ($index < 0) return new String[] {"",$source};
		return new String[] {$source.substring(0,$index),$source.substring($index+$pattern.length())};
	}

////////////////////////////////////////////////////////////////

	public static final String merge(String[] $r) {
		return merge($r,"\n");
	}

	public static final String merge(String[] $r, String $dlim) {
		StringBuilder $sb = new StringBuilder();
		for (String $s : $r) $sb.append($s).append($dlim);
		return $sb.toString();
	}

	public static String indent(Object x) {
		return indent(x, "\t");
	}

	public static String indent(Object x, String dent) {
		String y = (x == null) ? "null" : x.toString();
		boolean trailing = y.endsWith("\n");
		String[] z = y.split("\n");
		StringBuilder b = new StringBuilder(y.length()+z.length*dent.length());
		for (String a : z)
			b.append(dent).append(a).append('\n');
		return b.substring(0, b.length() - (trailing ? 0 : 1));
	}

	public static String join(Collection<? extends CharSequence> collection, CharSequence infix) {
		return join(collection, infix, null, null);
	}

	public static String join(Collection<? extends CharSequence> collection, CharSequence infix, CharSequence prefix, CharSequence suffix) {
		if (prefix == null) prefix = "";
		if (infix == null) infix = "";
		if (suffix == null) suffix = "";
		StringBuilder s = new StringBuilder();
		s.append(prefix);
		Iterator<? extends CharSequence> i = collection.iterator();
		while (i.hasNext()) {
			s.append(i.next());
			if (i.hasNext()) s.append(infix);
		}
		s.append(suffix);
		return s.toString();
	}

	/** See {@link #chooseFieldWidth(Collection, int)} &mdash; this is exactly as calling that method with a policy of 8. */
	public static int chooseFieldWidth(Collection<String> samples) {
		return chooseFieldWidth(samples, 8);
	}

	/**
	 * Return the smallest number of characters that a printf should reserve when
	 * printing a table with a columnn containing the given sample of values, if the
	 * next field must start aligned to the given 'policy' multiple of characters.
	 */
	public static int chooseFieldWidth(Collection<String> samples, int policy) {
		int width = 0;
		for (String val : samples)
			width = max(width, val.length());
		// divide into blocks of size 'policy', then turn that back into characters and add one more block.
		return (width / policy) * policy + policy;
	}

	/**
	 * Mutates the given array of strings to contain only interned strings.
	 *
	 * @return an array of intern'd strings (=== the arg)
	 */
	public static final String[] intern(String[] $r) {
		for (int $i = 0; $i < $r.length; $i++)
			$r[$i] = $r[$i].intern();
		return $r;
	}

	/**
	 * @return an empty string if the argument was null; otherwise the argument.
	 */
	public static String noNull(String s) {
		return (s == null ? "" : s);
	}

//////////////////////////////////////////////////////////////// FILE NAME MANIPULATION FUNCTIONS

	public static final String dirname(String $path) {
		if ($path.endsWith("/")) return $path;
		return $path.substring(0, $path.lastIndexOf("/")+1);
	}

	public static final String fullname(String $path) {
		return getPartAfterLast($path, "/");
	}

	public static final String basename(String $path) {
		return getPartBefore(fullname($path),".");
	}

	/**
	 *
	 * @param $path a string representing a path for a filesystem
	 * @return the string following the last period
	 */
	public static final String extension(String $path) {
		return getPartAfterLastOrEmpty(fullname($path),".");
	}

//////////////////////////////////////////////////////////////// PADDING FUNCTIONS

	public static String repeat(char $pad, int $count) {
		if ($count < 0) throw new IllegalArgumentException("count must be >= 0");
		char[] $ch = new char[$count];
		Arr.fill($ch, $pad);
		return new String($ch);
	}

	public static String repeat(String $pad, int $count) {
		if ($count < 0) throw new IllegalArgumentException("count must be >= 0");
		StringBuffer $buf = new StringBuffer($count*$pad.length());
		for (int i = 0; i < $count; ++i)
			$buf.append($pad);
		return $buf.toString();
	}

	/**
	 * Buff a number with leading zeros, and return it as a string. (Effectively,
	 * <code>padLeftToWidth(String.valueOf($n), '0', $desiredWidth)</code>.)
	 */
	public static String frontZeroBuff(int $n, int $desiredWidth) {
		return padLeftToWidth(String.valueOf($n), '0', $desiredWidth);
	}

	/**
	 * Buff a number with leading zeros, and return it as a string. (Effectively,
	 * <code>padLeftToWidth(String.valueOf($n), '0', $desiredWidth)</code>.)
	 */
	public static String frontZeroBuff(long $n, int $desiredWidth) {
		return padLeftToWidth(String.valueOf($n), '0', $desiredWidth);
	}

	/**
	 * Appends $padCount space characters to the end of a string.
	 *
	 * @param $s
	 * @param $padCount
	 * @return the padded string
	 */
	public static String padRight(String $s, int $padCount) {
		return padRight($s, ' ', $padCount);
	}

	/**
	 * Appends $padCount space characters to the beginning of a string.
	 *
	 * @param $s
	 * @param $padCount
	 * @return the padded string
	 */
	public static String padLeft(String $s, int $padCount) {
		return padLeft($s, ' ', $padCount);
	}

	/**
	 * Appends $padCount iterations of the "$pad" string to the end of a string "$s".
	 *
	 * @param $s
	 * @param $pad
	 * @param $padCount
	 * @return the padded string
	 */
	public static String padRight(String $s, String $pad, int $padCount) {
		if ($padCount < 0) throw new IllegalArgumentException("padCount must be >= 0");
		StringBuffer $buf = new StringBuffer($s.length()+$padCount*$pad.length());
		$buf.append($s);
		for (int i = 0; i < $padCount; ++i)
			$buf.append($pad);
		return $buf.toString();
	}

	/**
	 * Appends $padCount iterations of the "$pad" string to the beginning of a string
	 * "$s".
	 *
	 * @param $s
	 * @param $pad
	 * @param $padCount
	 * @return the padded string
	 */
	public static String padLeft(String $s, String $pad, int $padCount) {
		if ($padCount < 0) throw new IllegalArgumentException("padCount must be >= 0");
		StringBuffer $buf = new StringBuffer($s.length()+$padCount*$pad.length());
		for (int i = 0; i < $padCount; ++i)
			$buf.append($pad);
		$buf.append($s);
		return $buf.toString();
	}

	/**
	 * Appends $padCount iterations of the "$pad" string to the end of a string "$s".
	 *
	 * @param $s
	 * @param $pad
	 * @param $padCount
	 * @return the padded string
	 */
	public static String padRight(String $s, char $pad, int $padCount) {
		return $s+repeat($pad,$padCount);
	}

	/**
	 * Appends $padCount iterations of the "$pad" string to the beginning of a string
	 * "$s".
	 *
	 * @param $s
	 * @param $pad
	 * @param $padCount
	 * @return the padded string
	 */
	public static String padLeft(String $s, char $pad, int $padCount) {
		return repeat($pad,$padCount)+$s;
	}

	/**
	 * If the string "$s" is shorter than $desiredWidth, space characters are
	 * appeneded to the end of the string until it has a length matching
	 * $desiredWidth.
	 *
	 * @param $s
	 * @param $desiredWidth
	 * @return the padded string
	 */
	public static String padRightToWidth(String $s, int $desiredWidth) {
		if ($s.length() < $desiredWidth) return padRight($s, $desiredWidth - $s.length());
		return $s;
	}

	/**
	 * If the string "$s" is shorter than $desiredWidth, the "$pad" string is appended
	 * to the end of the string the number of times that would be necessary to make
	 * the original string's length match $desiredWidth (assuming that the $pad string
	 * is a single character in length).
	 *
	 * @param $s
	 * @param $desiredWidth
	 * @return the padded string
	 */
	public static String padRightToWidth(String $s, char $pad, int $desiredWidth) {
		if ($s.length() < $desiredWidth) return padRight($s, $pad, $desiredWidth - $s.length());
		return $s;
	}

	/**
	 * If the string "$s" is shorter than $desiredWidth, space characters are
	 * appeneded to the beginning of the string until it has a length matching
	 * $desiredWidth.
	 *
	 * @param $s
	 * @param $desiredWidth
	 * @return the padded string
	 */
	public static String padLeftToWidth(String $s, int $desiredWidth) {
		if ($s.length() < $desiredWidth) return padLeft($s, $desiredWidth - $s.length());
		return $s;
	}

	/**
	 * If the string "$s" is shorter than $desiredWidth, the "$pad" string is appended
	 * to the beginning of the string the number of times that would be necessary to
	 * make the original string's length match $desiredWidth (assuming that the $pad
	 * string is a single character in length).
	 *
	 * @param $s
	 * @param $desiredWidth
	 * @return the padded string
	 */
	public static String padLeftToWidth(String $s, char $pad, int $desiredWidth) {
		if ($s.length() < $desiredWidth) return padLeft($s, $pad, $desiredWidth - $s.length());
		return $s;
	}

	public static List<String> wrapToList(String $s, int $width) {
		List<String> $v = new LinkedList<String>();
		if (($s != null) && ($s.length() > 0)) {
			StringBuffer $buf = new StringBuffer();
			int $lastSpaceBufIndex = -1;
			for (int i = 0; i < $s.length(); ++i) {
				char c = $s.charAt(i);
				if (c == '\n') {
					$v.add($buf.toString());
					$buf.setLength(0);
					$lastSpaceBufIndex = -1;
				} else {
					if (c == ' ') {
						if ($buf.length() >= $width - 1) {
							$v.add($buf.toString());
							$buf.setLength(0);
							$lastSpaceBufIndex = -1;
						}
						if ($buf.length() > 0) {
							$lastSpaceBufIndex = $buf.length();
							$buf.append(c);
						}
					} else {
						if ($buf.length() >= $width) {
							if ($lastSpaceBufIndex != -1) {
								$v.add($buf.substring(0, $lastSpaceBufIndex));
								$buf.delete(0, $lastSpaceBufIndex + 1);
								$lastSpaceBufIndex = -1;
							}
						}
						$buf.append(c);
					}
				}
			}
			if ($buf.length() > 0) $v.add($buf.toString());
		}
		return $v;
	}

//////////////////////////////////////////////////////////////// HEX ENCODING FUNCTIONS

	public static final byte[] fromHex(String $hex) { return decHex($hex); }
	public static final byte[] decHex(String $hex) {
		if ($hex == null) return null;
	        byte[] $bah = new byte[$hex.length() / 2];
		for (int $i = 0; $i < $hex.length(); $i += 2) {
			int $j = Integer.parseInt($hex.substring($i, $i + 2), 16);
			$bah[$i / 2] = (byte) ($j & 0xFF);
		}
		return $bah;
	}

	public static final String toHex(byte[] $bah) { return encHex($bah); }
	public static final String encHex(byte[] $bah) {
		if ($bah == null) return null;
		char[] $chars = new char[2 * $bah.length];
		for (int $i = 0; $i < $bah.length; ++$i) {	// could probably save cpu at the cost of a 4 bytes of memory by just having two counters, since it would remove the need to multiply
			$chars[2 * $i] = HEX_CHARS[($bah[$i] & 0xF0) >>> 4];
			$chars[2 * $i + 1] = HEX_CHARS[$bah[$i] & 0x0F];
		}
		return new String($chars);
	}

//////////////////////////////////////////////////////////////// OTHER CRAP

	/**
	 * Returns a string of comma separated values. Bytes which are non-whitespace
	 * printable ascii characters are printed as themselves prefixed by a single
	 * quotation mark; other bytes are printed as their base-10 value. This is
	 * expected to be occationally useful in debugging, but there should be no reason
	 * to ever use it in a production context.
	 */
	public static final String semireadable(byte[] $bats) {
		StringBuilder $sb = new StringBuilder();
		for (int $i = 0; $i < $bats.length; $i++) {
			byte $b = $bats[$i];
			if ($b < 127 && $b > 32) $sb.append('\'').append((char) $b);
			else $sb.append($b);
			$sb.append(',');
		}
		$sb.setLength($sb.length()-1);
		return $sb.toString();
	}
}
