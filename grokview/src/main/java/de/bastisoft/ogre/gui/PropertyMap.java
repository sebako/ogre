/*
 * Copyright 2012 Sebastian Koppehel
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

package de.bastisoft.ogre.gui;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A simple string-only key-value store intended to hold configuration properties.
 * Both keys and values are always strings and guaranteed not to be <code>null</code>.
 * Insertion order is preserved when enumerating the keys.
 * 
 * <p>Convenience methods exists to extract sub-maps of properties that share a common
 * prefix, as well as numbered lists.
 * 
 * @author Sebastian Koppehel
 */
public class PropertyMap {

    private Map<String, String> map;
    
    public PropertyMap() {
        map = new LinkedHashMap<>();
    }
    
    public PropertyMap(Properties source) {
        this();
        for (String key : source.stringPropertyNames())
            set(key, source.getProperty(key));
    }
    
    /**
     * Retrieves the value of the property. If the given property is not in the map,
     * the value <code>null</code> is returned; otherwise <code>null</code> is never
     * returned.
     * 
     * @param key the key of the desired property
     * @return the value or <code>null</code> if the property is unknown
     */
    public String get(String key) {
        return map.get(key);
    }
    
    public String get(String key, String defaultValue) {
        String v = get(key);
        return v != null ? v : defaultValue;
    }
    
    public int getInt(String key, int defaultValue) {
        String v = get(key);
        
        if (v == null)
            return defaultValue;
        
        try {
            return Integer.parseInt(v);
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public boolean getBool(String key, boolean defaultValue) {
        String v = get(key);
        return v != null ? Boolean.parseBoolean(v) : defaultValue;
    }
    
    /**
     * Sets the value of a property. A property may be removed from the map by setting
     * the value to <code>null</code>.
     * 
     * @param key the key of the property to be set
     * @param value the value to be set, or <code>null</code> to remove a property
     * @throws NullPointerException if the key is <code>null</code>
     */
    public void set(String key, String value) {
        if (key == null)
            throw new NullPointerException("key");
        
        if (value == null)
            map.remove(key);
        else
            map.put(key, value);
    }
    
    /**
     * Sets the value of a property to a string representation of a boolean value, which
     * means either the string "true" or "false".
     * 
     * @param key the key of the property to be set
     * @param value the value to be set
     * @throws NullPointerException if the key is <code>null</code>
     */
    public void set(String key, boolean value) {
        set(key, Boolean.toString(value));
    }
    
    /**
     * Sets the value of a property to a string representation of an integer value.
     * 
     * @param key the key of the property to be set
     * @param value the value to be set
     * @throws NullPointerException if the key is <code>null</code>
     */
    public void set(String key, int value) {
        set(key, Integer.toString(value));
    }
    
    public Set<String> keys() {
        return Collections.unmodifiableSet(map.keySet());
    }
    
    /**
     * Constructs a subset of this property map that contains all the properties whose keys
     * share a given prefix. The prefix must be separated from the rest of the key by a dot
     * character. In the new map, the prefix (including the dot) is removed from the keys.
     * 
     * <p>For example, assume this map contains the following properties:
     * 
     * <ul>
     * <li>file.name
     * <li>file.date
     * <li>dir.name
     * </ul>
     * 
     * <p>Then a submap for the prefix "file" will contain the keys "name" and "date".
     * 
     * <p>There is no requirement that the prefix cannot contain dot characters at any
     * position, including at the end.
     * 
     * @param prefix a prefix that distinguishes a certain subset of properties
     * @return a property map containing all properties that originally had the given prefix
     */
    public PropertyMap submap(String prefix) {
        PropertyMap submap = new PropertyMap();
        for (Map.Entry<String, String> entry : map.entrySet())
            if (entry.getKey().startsWith(prefix + "."))
                submap.set(entry.getKey().substring(prefix.length() + 1), entry.getValue());
        return submap;
    }
    
    /**
     * Constructs an ordered list of subsets of this property map that contains all the
     * properties whose keys share a given prefix, followed by a numeric index notation.
     * For a property to be eligible to be included, its key must begin with the prefix,
     * followed by an integer number in square brackets, followed by a dot.
     * 
     * <p>For example, given the prefix "server", the following properties would be
     * found:
     * 
     * <ul>
     * <li>server[0].hostname
     * <li>server[-1].hostname
     * </ul>
     * 
     * <p>But not:
     * 
     * <ul>
     * <li>server.hostname
     * <li>server[0]hostname
     * <li>server[1.5].hostname
     * </ul>
     * 
     * <p>Note that, the similarity to the array index notation of many programming
     * languages not withstanding, whitespace is not ignored anywhere, including inside
     * the brackets, where it must not appear. Failure to comply with the format is not
     * an error, it simply means that a property will not be included in any of the
     * submaps. There is no requirement that the prefix cannot contain dot or bracket
     * characters at any position, including at the end.
     * 
     * <p>The properties are grouped into submaps by common index, and the submaps are
     * returned in the order of their indices; however, the actual values of the indices
     * are not preserved. In the submaps the prefix, including the index notation and
     * the dot, is removed from the keys. In the examples above, the new key would be
     * "hostname".
     * 
     * @param prefix a prefix that distinguishes a certain subset of properties
     * @return a list of property maps that contain all the properties that originally
     *             had the given prefix with a numeric index
     */
    public List<PropertyMap> submaps(String prefix) {
        return new ArrayList<>(indexedSubmaps(prefix).values());
    }
    
    private SortedMap<Integer, PropertyMap> indexedSubmaps(String prefix) {
        SortedMap<Integer, PropertyMap> instances = new TreeMap<>();
        
        for (String key : map.keySet())
            if (key.startsWith(prefix + "[")) {
                int p = key.indexOf("].", prefix.length() + 1);
                if (p > -1) {
                    String s = key.substring(prefix.length() + 1, p);
                    try {
                        Integer index = new Integer(s);
                        PropertyMap mapForIndex = instances.get(index);
                        if (mapForIndex == null)
                            instances.put(index, mapForIndex = new PropertyMap());
                        mapForIndex.set(key.substring(p + 2), map.get(key));
                    }
                    catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
        
        return instances;
    }
    
    public void add(PropertyMap submap, String prefix) {
        for (Entry<String, String> entry : submap.map.entrySet())
            set(prefix + "." + entry.getKey(), entry.getValue());
    }
    
    public void addIndexed(PropertyMap submap, String prefix, int index) {
        for (Entry<String, String> entry : submap.map.entrySet())
            set(prefix + "[" + index + "]." + entry.getKey(), entry.getValue());
    }
    
    public void addIndexed(PropertyMap submap, String prefix) {
        int nextIndex = 0;
        for (int index : indexedSubmaps(prefix).keySet())
            nextIndex = index;
        
        addIndexed(submap, prefix, nextIndex);
    }
    
    public void save(Path file) throws IOException {
        try (OutputStream out = Files.newOutputStream(file)) { save(out); }
    }
    
    public void save(OutputStream output) throws IOException {
        Writer out = new OutputStreamWriter(output, Charset.forName("ISO-8859-1"));
        for (Entry<String, String> entry : map.entrySet()) {
            out.write(escape(entry.getKey(), true));
            out.write(" = ");
            out.write(escape(entry.getValue(), false));
            out.write(System.lineSeparator());
        }
        out.flush();
    }
    
    private static String escape(String s, boolean key) {
        StringBuilder sb = new StringBuilder();
        
        boolean first = true;
        for (char ch : s.toCharArray()) {
            switch (ch) {
            
            // Backspace: Escape always
            case '\\':
                sb.append("\\\\");
                break;
            
            // Whitespace: Escape CR, LF, FF always, space and HT only when first character
            case '\r':
                sb.append("\\r");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\f':
                sb.append("\\f");
                break;
            case ' ':
                sb.append(first ? "\\ " : " ");
                break;
            case '\t':
                sb.append(first ? "\\t" : "\t");
                break;
            
            // Key-value separators: Escape only in key
            case ':':
                sb.append(key ? "\\:" : ":");
                break;
            case '=':
                sb.append(key ? "\\=" : "=");
                break;
            
            // Comment characters: Escape only when first character of key
            case '#':
                sb.append(first && key ? "\\#" : "#");
                break;
            case '!':
                sb.append(first && key ? "\\!" : "!");
                break;
            
            // All else: Escape everything outside printable ISO-8859-1
            default:
                if (ch < 0x20 || ch > 0x7E && ch < 0xA1 || ch == 0xAD || ch > 0xFF)
                    sb.append(String.format("\\u%04x", (int) ch));
                else
                    sb.append(ch);
            }
            
            first = false;
        }
        
        return sb.toString();
    }
    
    public static PropertyMap load(InputStream in) throws IOException {
        Properties source = new Properties();
        source.load(in);
        return new PropertyMap(source);
    }
    
    public static PropertyMap load(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) { return load(in); }
    }
    
}
