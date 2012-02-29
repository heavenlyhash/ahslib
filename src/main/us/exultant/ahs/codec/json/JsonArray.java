/*
 * Copyright 2010, 2011 Eric Myhre <http://exultant.us>
 * 
 * This file is part of AHSlib.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * This file contains derivations of work copyrighted in 2006 to JSON.org.  The original licensing statement follows:
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * The Software shall be used for Good, not Evil.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package us.exultant.ahs.codec.json;

import us.exultant.ahs.core.*;
import us.exultant.ahs.util.*;
import us.exultant.ahs.codec.eon.*;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * A JsonArray is an ordered sequence of values. Its external text form is a string
 * wrapped in square brackets with commas separating the values. The internal form is an
 * object having <code>get</code> and <code>opt</code> methods for accessing the values by
 * index, and <code>put</code> methods for adding or replacing values. The values can be
 * any of these types: <code>Boolean</code>, <code>JsonArray</code>,
 * <code>JSONObject</code>, <code>Number</code>, <code>String</code>, or the
 * <code>JSONObject.NULL object</code>.
 * <p>
 * The constructor can convert a JSON text into a Java object. The <code>toString</code>
 * method converts to JSON text.
 * <p>
 * A <code>get</code> method returns a value if one can be found, and throws an exception
 * if one cannot be found. An <code>opt</code> method returns a default value instead of
 * throwing an exception, and so is useful for obtaining optional values.
 * <p>
 * The generic <code>get()</code> and <code>opt()</code> methods return an object which
 * you can cast or query for type. There are also typed <code>get</code> and
 * <code>opt</code> methods that do type checking and type coercion for you.
 * <p>
 * The texts produced by the <code>toString</code> methods strictly conform to JSON syntax
 * rules. The constructors are more forgiving in the texts they will accept:
 * <ul>
 * <li>An extra <code>,</code>&nbsp;<small>(comma)</small> may appear just before the
 * closing bracket.</li>
 * <li>The <code>null</code> value will be inserted when there is <code>,</code>
 * &nbsp;<small>(comma)</small> elision.</li>
 * <li>Strings may be quoted with <code>'</code>&nbsp;<small>(single quote)</small>.</li>
 * <li>Strings do not need to be quoted at all if they do not begin with a quote or single
 * quote, and if they do not contain leading or trailing spaces, and if they do not
 * contain any of these characters: <code>{ } [ ] / \ : , = ; #</code> and if they do not
 * look like numbers and if they are not the reserved words <code>true</code>,
 * <code>false</code>, or <code>null</code>.</li>
 * <li>Values can be separated by <code>;</code> <small>(semicolon)</small> as well as by
 * <code>,</code> <small>(comma)</small>.</li>
 * <li>Numbers may have the <code>0-</code> <small>(octal)</small> or <code>0x-</code>
 * <small>(hex)</small> prefix.</li>
 * </ul>
 * 
 * @author JSON.org
 * @version 2008-09-18
 */
public class JsonArray implements EonArray {
	
	/**
	 * The arrayList where the JsonArray's properties are kept.
	 */
	private ArrayList<Object>	myArrayList;
	
	
	/**
	 * Construct an empty JsonArray.
	 */
	public JsonArray() {
		this.myArrayList = new ArrayList<Object>();
	}
	
	/**
	 * Construct a JsonArray from a JSONTokener.
	 * 
	 * @param x
	 *                A JSONTokener
	 * @throws JsonException
	 *                 If there is a syntax error.
	 */
	public JsonArray(JsonTokener x) throws JsonException {
		this();
		char c = x.nextClean();
		char q;
		if (c == '[') {
			q = ']';
		} else if (c == '(') {
			q = ')';
		} else {
			throw x.syntaxError("A JsonArray text must start with '['");
		}
		if (x.nextClean() == ']') { return; }
		x.back();
		for (;;) {
			if (x.nextClean() == ',') {
				x.back();
				this.myArrayList.add(null);
			} else {
				x.back();
				this.myArrayList.add(x.nextValue());
			}
			c = x.nextClean();
			switch (c) {
				case ';':
				case ',':
					if (x.nextClean() == ']') { return; }
					x.back();
					break;
				case ']':
				case ')':
					if (q != c) { throw x.syntaxError("Expected a '" + new Character(q) + "'"); }
					return;
				default:
					throw x.syntaxError("Expected a ',' or ']'");
			}
		}
	}
	
	
	/**
	 * Construct a JsonArray from a source JSON text.
	 * 
	 * @param source
	 *                A string that begins with <code>[</code>&nbsp;<small>(left
	 *                bracket)</small> and ends with <code>]</code>&nbsp;<small>(right
	 *                bracket)</small>.
	 * @throws JsonException
	 *                 If there is a syntax error.
	 */
	public JsonArray(String source) throws JsonException {
		this(new JsonTokener(source));
	}
	
	
	/**
	 * Construct a JsonArray from a Collection.
	 * 
	 * @param collection
	 *                A Collection.
	 */
	public JsonArray(Collection<Object> collection) {
		this.myArrayList = (collection == null) ? new ArrayList<Object>() : new ArrayList<Object>(collection);
	}
	
	/**
	 * Construct a JsonArray from a collection of beans. The collection should have
	 * Java Beans.
	 */
	
	public JsonArray(Collection<Object> collection, boolean includeSuperClass) {
		this.myArrayList = new ArrayList<Object>();
		if (collection != null) {
			for (Iterator<Object> iter = collection.iterator(); iter.hasNext();) {
				this.myArrayList.add(new JsonObject(iter.next(), includeSuperClass));
			}
		}
	}
	
	
	/**
	 * Construct a JsonArray from an array
	 * 
	 * @throws JsonException
	 *                 If not an array.
	 */
	public JsonArray(Object array) throws JsonException {
		this();
		if (array.getClass().isArray()) {
			int length = Array.getLength(array);
			for (int i = 0; i < length; i += 1) {
				this.put(Array.get(array, i));
			}
		} else {
			throw new JsonException("JsonArray initial value should be a string or collection or array.");
		}

	}
	
	/**
	 * Construct a JsonArray from an array with a bean. The array should have Java
	 * Beans.
	 * 
	 * @throws JsonException
	 *                 If not an array.
	 */
	public JsonArray(Object array, boolean includeSuperClass) throws JsonException {
		this();
		if (array.getClass().isArray()) {
			int length = Array.getLength(array);
			for (int i = 0; i < length; i += 1) {
				this.put(new JsonObject(Array.get(array, i), includeSuperClass));
			}
		} else {
			throw new JsonException("JsonArray initial value should be a string or collection or array.");
		}
	}
	
	

	/**
	 * Get the object value associated with an index.
	 * 
	 * @param index
	 *                The index must be between 0 and length() - 1.
	 * @return An object value.
	 * @throws JsonException
	 *                 If there is no value for the index.
	 */
	public Object get(int index) throws JsonException {
		Object o = opt(index);
		if (o == null) { throw new JsonException("JsonArray[" + index + "] not found."); }
		return o;
	}
	
	
	/**
	 * Get the boolean value associated with an index. The string values "true" and
	 * "false" are converted to boolean.
	 * 
	 * @param index
	 *                The index must be between 0 and length() - 1.
	 * @return The truth.
	 * @throws JsonException
	 *                 If there is no value for the index or if the value is not
	 *                 convertable to boolean.
	 */
	public boolean getBoolean(int index) throws JsonException {
		Object o = get(index);
		if (o.equals(Boolean.FALSE) || (o instanceof String && ((String) o).equalsIgnoreCase("false"))) {
			return false;
		} else if (o.equals(Boolean.TRUE) || (o instanceof String && ((String) o).equalsIgnoreCase("true"))) { return true; }
		throw new JsonException("JsonArray[" + index + "] is not a Boolean.");
	}
	
	
	/**
	 * Get the double value associated with an index.
	 * 
	 * @param index
	 *                The index must be between 0 and length() - 1.
	 * @return The value.
	 * @throws JsonException
	 *                 If the key is not found or if the value cannot be converted to
	 *                 a number.
	 */
	public double getDouble(int index) throws JsonException {
		Object o = get(index);
		try {
			return o instanceof Number ? ((Number) o).doubleValue() : Double.valueOf((String) o).doubleValue();
		} catch (Exception e) {
			throw new JsonException("JsonArray[" + index + "] is not a number.");
		}
	}
	
	
	/**
	 * Get the int value associated with an index.
	 * 
	 * @param index
	 *                The index must be between 0 and length() - 1.
	 * @return The value.
	 * @throws JsonException
	 *                 If the key is not found or if the value cannot be converted to
	 *                 a number. if the value cannot be converted to a number.
	 */
	public int getInt(int index) throws JsonException {
		Object o = get(index);
		return o instanceof Number ? ((Number) o).intValue() : (int) getDouble(index);
	}
	
	
	/**
	 * Get the JsonArray associated with an index.
	 * 
	 * @param index
	 *                The index must be between 0 and length() - 1.
	 * @return A JsonArray value.
	 * @throws JsonException
	 *                 If there is no value for the index. or if the value is not a
	 *                 JsonArray
	 */
	public JsonArray getArr(int index) throws JsonException {
		Object o = get(index);
		if (o instanceof JsonArray) { return (JsonArray) o; }
		throw new JsonException("JsonArray[" + index + "] is not a JsonArray.");
	}
	
	
	/**
	 * Get the JSONObject associated with an index.
	 * 
	 * @param index
	 *                subscript
	 * @return A JSONObject value.
	 * @throws JsonException
	 *                 If there is no value for the index or if the value is not a
	 *                 JSONObject
	 */
	public JsonObject getObj(int index) throws JsonException {
		Object o = get(index);
		if (o instanceof JsonObject) { return (JsonObject) o; }
		throw new JsonException("JsonArray[" + index + "] is not a JsonObject.");
	}
	
	
	/**
	 * Get the long value associated with an index.
	 * 
	 * @param index
	 *                The index must be between 0 and length() - 1.
	 * @return The value.
	 * @throws JsonException
	 *                 If the key is not found or if the value cannot be converted to
	 *                 a number.
	 */
	public long getLong(int index) throws JsonException {
		Object o = get(index);
		return o instanceof Number ? ((Number) o).longValue() : (long) getDouble(index);
	}
	
	
	/**
	 * Get the string associated with an index.
	 * 
	 * @param index
	 *                The index must be between 0 and length() - 1.
	 * @return A string value.
	 * @throws JsonException
	 *                 If there is no value for the index.
	 */
	public String getString(int index) throws JsonException {
		return get(index).toString();
	}
	
	
	/**
	 * Determine if the value is null.
	 * 
	 * @param index
	 *                The index must be between 0 and length() - 1.
	 * @return true if the value at the index is null, or if there is no value.
	 */
	public boolean isNull(int index) {
		return JsonObject.NULL.equals(opt(index));
	}
	
	
	/**
	 * Make a string from the contents of this JsonArray. The <code>separator</code>
	 * string is inserted between each element. Warning: This method assumes that the
	 * data structure is acyclical.
	 * 
	 * @param separator
	 *                A string that will be inserted between the elements.
	 * @return a string.
	 * @throws JsonException
	 *                 If the array contains an invalid number.
	 */
	public String join(String separator) throws JsonException, UnencodableException {
		int len = length();
		StringBuffer sb = new StringBuffer();
		
		for (int i = 0; i < len; i += 1) {
			if (i > 0) {
				sb.append(separator);
			}
			sb.append(JsonObject.valueToString(this.myArrayList.get(i)));
		}
		return sb.toString();
	}
	
	
	/**
	 * Get the number of elements in the JsonArray, included nulls.
	 * 
	 * @return The length (or size).
	 */
	public int length() {
		return this.myArrayList.size();
	}
	public int size() {
		return this.myArrayList.size();
	}
	

	public void put(int index, EonObject value) {
		if (value.getClass() != JsonObject.class) throw new IllegalArgumentException("JsonArray isn't willing to deal with nested EonObject other than JsonObject.");
		put(index,(Object) value);
	}
	public void put(int index, EonArray value) {
		if (value.getClass() != JsonArray.class) throw new IllegalArgumentException("JsonArray isn't willing to deal with nested EonArray other than EonArray.");
		put(index,(Object) value);
	}
	public void put(int index, String value) {
		put(index,(Object) value);
	}
	public void put(int index, byte[] value) {
		put(index,Base64.encode(value));
	}
	public byte[] getBytes(int index) throws JsonException {
		return Base64.decode(getString(index));
	}
	public byte[] optBytes(int index) {
		String $s = optString(index);
		return $s == null ? null : Base64.decode($s);
	}
	
	public byte[] optBytes(int index, byte[] $default) {
		String $s = optString(index);
		if ($s == null) return $default;
		byte[] $try = Base64.decode($s);
		return $try == null ? $default : $try;
	}
	
	
	/**
	 * Get the optional object value associated with an index.
	 * 
	 * @param index
	 *                The index must be between 0 and length() - 1.
	 * @return An object value, or null if there is no object at that index.
	 */
	public Object opt(int index) {
		return (index < 0 || index >= length()) ? null : this.myArrayList.get(index);
	}
	
	
	/**
	 * Get the optional boolean value associated with an index. It returns false if
	 * there is no value at that index, or if the value is not Boolean.TRUE or the
	 * String "true".
	 * 
	 * @param index
	 *                The index must be between 0 and length() - 1.
	 * @return The truth.
	 */
	public boolean optBoolean(int index) {
		return optBoolean(index, false);
	}
	
	
	/**
	 * Get the optional boolean value associated with an index. It returns the
	 * defaultValue if there is no value at that index or if it is not a Boolean or
	 * the String "true" or "false" (case insensitive).
	 * 
	 * @param index
	 *                The index must be between 0 and length() - 1.
	 * @param defaultValue
	 *                A boolean default.
	 * @return The truth.
	 */
	public boolean optBoolean(int index, boolean defaultValue) {
		try {
			return getBoolean(index);
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	
	/**
	 * Get the optional double value associated with an index. NaN is returned if
	 * there is no value for the index, or if the value is not a number and cannot be
	 * converted to a number.
	 * 
	 * @param index
	 *                The index must be between 0 and length() - 1.
	 * @return The value.
	 */
	public double optDouble(int index) {
		return optDouble(index, Double.NaN);
	}
	
	
	/**
	 * Get the optional double value associated with an index. The defaultValue is
	 * returned if there is no value for the index, or if the value is not a number
	 * and cannot be converted to a number.
	 * 
	 * @param index
	 *                subscript
	 * @param defaultValue
	 *                The default value.
	 * @return The value.
	 */
	public double optDouble(int index, double defaultValue) {
		try {
			return getDouble(index);
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	
	/**
	 * Get the optional int value associated with an index. Zero is returned if there
	 * is no value for the index, or if the value is not a number and cannot be
	 * converted to a number.
	 * 
	 * @param index
	 *                The index must be between 0 and length() - 1.
	 * @return The value.
	 */
	public int optInt(int index) {
		return optInt(index, 0);
	}
	
	
	/**
	 * Get the optional int value associated with an index. The defaultValue is
	 * returned if there is no value for the index, or if the value is not a number
	 * and cannot be converted to a number.
	 * 
	 * @param index
	 *                The index must be between 0 and length() - 1.
	 * @param defaultValue
	 *                The default value.
	 * @return The value.
	 */
	public int optInt(int index, int defaultValue) {
		try {
			return getInt(index);
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	
	/**
	 * Get the optional JsonArray associated with an index.
	 * 
	 * @param index
	 *                subscript
	 * @return A JsonArray value, or null if the index has no value, or if the value
	 *         is not a JsonArray.
	 */
	public JsonArray optArr(int index) {
		Object o = opt(index);
		return o instanceof JsonArray ? (JsonArray) o : null;
	}
	
	
	/**
	 * Get the optional JSONObject associated with an index. Null is returned if the
	 * key is not found, or null if the index has no value, or if the value is not a
	 * JSONObject.
	 * 
	 * @param index
	 *                The index must be between 0 and length() - 1.
	 * @return A JSONObject value.
	 */
	public JsonObject optObj(int index) {
		Object o = opt(index);
		return o instanceof JsonObject ? (JsonObject) o : null;
	}
	
	
	/**
	 * Get the optional long value associated with an index. Zero is returned if there
	 * is no value for the index, or if the value is not a number and cannot be
	 * converted to a number.
	 * 
	 * @param index
	 *                The index must be between 0 and length() - 1.
	 * @return The value.
	 */
	public long optLong(int index) {
		return optLong(index, 0);
	}
	
	
	/**
	 * Get the optional long value associated with an index. The defaultValue is
	 * returned if there is no value for the index, or if the value is not a number
	 * and cannot be converted to a number.
	 * 
	 * @param index
	 *                The index must be between 0 and length() - 1.
	 * @param defaultValue
	 *                The default value.
	 * @return The value.
	 */
	public long optLong(int index, long defaultValue) {
		try {
			return getLong(index);
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	
	/**
	 * Get the optional string value associated with an index. It returns null if no
	 * value at that index. If the value is not a string and is not null, then it is
	 * coverted to a string.
	 * 
	 * @param index
	 *                The index must be between 0 and length() - 1.
	 * @return A String value.
	 */
	public String optString(int index) {
		return optString(index, null);
	}
	
	
	/**
	 * Get the optional string associated with an index. The defaultValue is returned
	 * if the key is not found.
	 * 
	 * @param index
	 *                The index must be between 0 and length() - 1.
	 * @param defaultValue
	 *                The default value.
	 * @return A String value.
	 */
	public String optString(int index, String defaultValue) {
		Object o = opt(index);
		return o != null ? o.toString() : defaultValue;
	}
	
	
	/**
	 * Append a boolean value. This increases the array's length by one.
	 * 
	 * @param value
	 *                A boolean value.
	 */
	public void put(boolean value) {
		put(value ? Boolean.TRUE : Boolean.FALSE);
	}
	
	
	/**
	 * Put a value in the JsonArray, where the value will be a JsonArray which is
	 * produced from a Collection.
	 * 
	 * @param value
	 *                A Collection value.
	 */
	public void put(Collection<Object> value) {
		put(new JsonArray(value));
	}
	
	
	/**
	 * Append a double value. This increases the array's length by one.
	 * 
	 * @param value
	 *                A double value.
	 * @throws UnencodableException
	 *                 if the value is infinite or NaN
	 */
	public void put(double value) throws UnencodableException {
		Double d = new Double(value);
		JsonObject.testValidity(d);
		put(d);
	}
	
	
	/**
	 * Append an int value. This increases the array's length by one.
	 * 
	 * @param value
	 *                An int value.
	 */
	public void put(int value) {
		put(new Integer(value));
	}
	
	
	/**
	 * Append an long value. This increases the array's length by one.
	 * 
	 * @param value
	 *                A long value.
	 */
	public void put(long value) {
		put(new Long(value));
	}
	
	
	/**
	 * Put a value in the JsonArray, where the value will be a JSONObject which is
	 * produced from a Map.
	 * 
	 * @param value
	 *                A Map value.
	 */
	public void put(Map<Object,Object> value) {
		put(new JsonObject(value));
	}
	
	
	/**
	 * Append an object value. This increases the array's length by one.
	 * 
	 * @param value
	 *                An object value. The value should be a Boolean, Double, Integer,
	 *                JsonArray, JSONObject, Long, or String, or the JSONObject.NULL
	 *                object.
	 */
	protected void put(Object value) {
		this.myArrayList.add(value);
	}
	
	
	/**
	 * Put or replace a boolean value in the JsonArray. If the index is greater than
	 * the length of the JsonArray, then null elements will be added as necessary to
	 * pad it out.
	 * 
	 * @param index
	 *                The subscript.
	 * @param value
	 *                A boolean value.
	 */
	public void put(int index, boolean value) {
		put(index, value ? Boolean.TRUE : Boolean.FALSE);
	}
	
	
	/**
	 * Put a value in the JsonArray, where the value will be a JsonArray which is
	 * produced from a Collection.
	 * 
	 * @param index
	 *                The subscript.
	 * @param value
	 *                A Collection value.
	 */
	public void put(int index, Collection<Object> value) {
		put(index, new JsonArray(value));
	}
	
	
	/**
	 * Put or replace a double value. If the index is greater than the length of the
	 * JsonArray, then null elements will be added as necessary to pad it out.
	 * 
	 * @param index
	 *                The subscript.
	 * @param value
	 *                A double value.
	 * @throws UnencodableException
	 *                 if infinite or NaN
	 */
	public void put(int index, double value) throws UnencodableException {
		putQuestionable(index, new Double(value));
	}
	
	
	/**
	 * Put or replace an int value. If the index is greater than the length of the
	 * JsonArray, then null elements will be added as necessary to pad it out.
	 * 
	 * @param index
	 *                The subscript.
	 * @param value
	 *                An int value.
	 */
	public void put(int index, int value) {
		put(index, new Integer(value));
	}
	
	
	/**
	 * Put or replace a long value. If the index is greater than the length of the
	 * JsonArray, then null elements will be added as necessary to pad it out.
	 * 
	 * @param index
	 *                The subscript.
	 * @param value
	 *                A long value.
	 */
	public void put(int index, long value) {
		put(index, new Long(value));
	}
	
	
	/**
	 * Put a value in the JsonArray, where the value will be a JSONObject which is
	 * produced from a Map.
	 * 
	 * @param index
	 *                The subscript.
	 * @param value
	 *                The Map value.
	 */
	public void put(int index, Map<Object,Object> value) {
		put(index, new JsonObject(value));
	}
	
	
	/**
	 * Put or replace an object value in the JsonArray. If the index is greater than
	 * the length of the JsonArray, then null elements will be added as necessary to
	 * pad it out.
	 * 
	 * @param index
	 *                The subscript.
	 * @param value
	 *                The value to put into the array. The value should be a Boolean,
	 *                Double, Integer, JsonArray, JSONObject, Long, or String, or the
	 *                JSONObject.NULL object.
	 */
	protected void put(int index, Object value) {
		putReally(index, value);
	}
	
	protected void putQuestionable(int index, Object value) throws UnencodableException {
		JsonObject.testValidity(value);
		putReally(index, value);
	}
	
	private void putReally(int index, Object value) {
		if (index < 0) { throw new IndexOutOfBoundsException("JsonArray[" + index + "] not found."); }
		if (index < length()) {
			this.myArrayList.set(index, value);
		} else {
			while (index != length()) {
				put(JsonObject.NULL);
			}
			put(value);
		}
	}
	
	
	/**
	 * Produce a JSONObject by combining a JsonArray of names with the values of this
	 * JsonArray.
	 * 
	 * @param names
	 *                A JsonArray containing a list of key strings. These will be
	 *                paired with the values.
	 * @return A JSONObject, or null if there are no names or if this JsonArray has no
	 *         values.
	 * @throws JsonException
	 *                 If any of the names are null.
	 */
	public JsonObject toJsonObject(JsonArray names) throws JsonException {
		if (names == null || names.length() == 0 || length() == 0) { return null; }
		JsonObject jo = new JsonObject();
		for (int i = 0; i < names.length(); i += 1) {
			jo.put(names.getString(i), this.opt(i));
		}
		return jo;
	}
	
	
	/**
	 * Make a JSON text of this JsonArray. For compactness, no unnecessary whitespace
	 * is added. If it is not possible to produce a syntactically correct JSON text
	 * then null will be returned instead. This could occur if the array contains an
	 * invalid number.
	 * <p>
	 * Warning: This method assumes that the data structure is acyclical.
	 * 
	 * @return a printable, displayable, transmittable representation of the array.
	 */
	public String toString() {
		try {
			return '[' + join(",") + ']';
		} catch (Exception e) {
			return null;
		}
	}
	
	
	/**
	 * Make a prettyprinted JSON text of this JsonArray. Warning: This method assumes
	 * that the data structure is acyclical.
	 * 
	 * @param indentFactor
	 *                The number of spaces to add to each level of indentation.
	 * @return a printable, displayable, transmittable representation of the object,
	 *         beginning with <code>[</code>&nbsp;<small>(left bracket)</small> and
	 *         ending with <code>]</code>&nbsp;<small>(right bracket)</small>.
	 * @throws JsonException
	 * @throws UnencodableException 
	 */
	public String toString(int indentFactor) throws JsonException, UnencodableException {
		return toString(indentFactor, 0);
	}
	
	
	/**
	 * Make a prettyprinted JSON text of this JsonArray. Warning: This method assumes
	 * that the data structure is acyclical.
	 * 
	 * @param indentFactor
	 *                The number of spaces to add to each level of indentation.
	 * @param indent
	 *                The indention of the top level.
	 * @return a printable, displayable, transmittable representation of the array.
	 * @throws JsonException
	 * @throws UnencodableException 
	 */
	String toString(int indentFactor, int indent) throws JsonException, UnencodableException {
		int len = length();
		if (len == 0) { return "[]"; }
		int i;
		StringBuffer sb = new StringBuffer("[");
		if (len == 1) {
			sb.append(JsonObject.valueToString(this.myArrayList.get(0), indentFactor, indent));
		} else {
			int newindent = indent + indentFactor;
			sb.append('\n');
			for (i = 0; i < len; i += 1) {
				if (i > 0) {
					sb.append(",\n");
				}
				for (int j = 0; j < newindent; j += 1) {
					sb.append(' ');
				}
				sb.append(JsonObject.valueToString(this.myArrayList.get(i), indentFactor, newindent));
			}
			sb.append('\n');
			for (i = 0; i < indent; i += 1) {
				sb.append(' ');
			}
		}
		sb.append(']');
		return sb.toString();
	}
	
	
	/**
	 * Write the contents of the JsonArray as JSON text to a writer. For compactness,
	 * no whitespace is added.
	 * <p>
	 * Warning: This method assumes that the data structure is acyclical.
	 * <p>
	 * Do not use a failable Writer. Write to a buffer first if you must deal with I/O
	 * outside of the program.
	 * 
	 * @return the writer.
	 * 
	 * @throws MajorBug
	 *                 if the Writer throws an IOException.
	 */
	Writer write(Writer writer) {
		try {
			boolean b = false;
			int len = length();
			
			writer.write('[');
			
			for (int i = 0; i < len; i += 1) {
				if (b) {
					writer.write(',');
				}
				Object v = this.myArrayList.get(i);
				if (v instanceof JsonObject) {
					((JsonObject) v).write(writer);
				} else if (v instanceof JsonArray) {
					((JsonArray) v).write(writer);
				} else {
					writer.write(JsonObject.valueToString(v));
				}
				b = true;
			}
			writer.write(']');
			return writer;
		} catch (IOException e) {
			/* don't pass me a shitty Writer. */
			throw new MajorBug("Unimaginably strange error while writing to an internal buffer.", e);
		}
	}
}
