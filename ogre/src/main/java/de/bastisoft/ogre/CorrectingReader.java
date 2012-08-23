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

package de.bastisoft.ogre;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Deals with all the discrepancies between the XHTML returned by OpenGrok servers and
 * actual valid XML documents.
 *
 * @author Sebastian Koppehel
 */
class CorrectingReader {

    private InputStream input;
    private Charset charset;
    
    public CorrectingReader(InputStream input, Charset charset) {
        this.input = input;
        this.charset = charset;
    }
    
    public Document parse() throws IOException, ParserConfigurationException, SAXException {
        String data = readData();
        
        data = fix(data);
        
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        builder.setEntityResolver(new EntityResolver() {
            @Override
            public InputSource resolveEntity(String arg0, String arg1) throws SAXException, IOException {
                return new InputSource(new StringReader(""));
            }
        });
        
        return builder.parse(new InputSource(new StringReader(data)));
    }
    
    private String readData() throws IOException {
        if (charset == null)
            guessCharset();
        
        try (Reader r = new InputStreamReader(input, charset)) {
            StringWriter sw = new StringWriter();
            int c; while ((c = r.read()) >= 0) sw.write(c);
            return sw.toString();
        }
    }
    
    private void guessCharset() {
        charset = Charset.forName("UTF-8");
    }
    
    private static final Pattern UNQUOTED_HREF = Pattern.compile("<a href=([^\"'][^>]*)>");
    private static final Pattern HREF = Pattern.compile("<a href=((?:\"[^\"]*\"|'[^']*'))>");
    
    private String fix(String s) {
        // Some sites may add custom meta tags without closing tags - invalid XML.
        s = s.replaceAll("<meta[^>]*>", "");
        
        // On "no matches" pages, the "results" div isn't properly closed.
        s = s.replaceAll("</ul>\\s*<div id=\"footer\">", "</ul></div><div id=\"footer\">");
        
        // When the slider goes to page 11, it starts with "<<" or, in OpenGrok-style "strict XHTML": &lt;&lt
        s = s.replaceAll("&lt<", "&lt;<");
        
        Matcher mat;
        
        /* Some "href" attributes for <a> tags are not properly enclosed in quotes. */
        
        mat = UNQUOTED_HREF.matcher(s);
        s = mat.replaceAll("<a href=\"$1\">");
        
        
        /* In some cases the URLs in "href" attributes are not properly XML escaped. This is somewhat
         * more complicated to determine. */
        
        StringBuffer sb = new StringBuffer();
        mat = HREF.matcher(s);
        while (mat.find()) {
            String repl = mat.group(1);
            
            /* All of this is a pretty awful hack, but whatever. We're not building an XHTML parser,
             * we're just working around bugs in OpenGrok. */
            
            int p = repl.indexOf('&');
            if (p >= 0) {
                int q = repl.indexOf(';', p);
                if (q == -1 || q - p > 4) {
                    repl = repl.replaceAll("&", "&amp;");
                    repl = repl.replaceAll("<", "&lt;");   // for good measure
                    repl = repl.replaceAll(">", "&gt;");
                }
            }
            
            /* In addition to XML escaping we may also have to fix URL escaping in links that contain
             * spaces (and who knows what else, but we can't escape everything, as proper escape
             * sequences do occur in the links). */
            
            repl = repl.replaceAll(" ", "%20");
            
            mat.appendReplacement(sb, "<a href=" + repl + ">");
        }
        mat.appendTail(sb);
        s = sb.toString();
        
        
        /* Some sites add space before the XML prolog. */
        
        s = s.trim();
        return s;
    }
    
}
