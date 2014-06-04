/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.bind.DatatypeConverter;

import org.apache.logging.log4j.core.jackson.Log4jJsonObjectMapper;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.*;

/**
 *
 */
public class ThrowableProxyTest {

    static class Fixture {
        @JsonProperty
        ThrowableProxy proxy = new ThrowableProxy(new IOException("test"));
    }

    @Test
    public void testJsonIoContainer() throws IOException {
        ObjectMapper objectMapper = new Log4jJsonObjectMapper();
        Fixture expected = new Fixture();
        final String s = objectMapper.writeValueAsString(expected);
        Fixture actual = objectMapper.readValue(s, Fixture.class);
        assertEquals(expected.proxy.getName(), actual.proxy.getName());
        assertEquals(expected.proxy.getMessage(), actual.proxy.getMessage());
        assertEquals(expected.proxy.getLocalizedMessage(), actual.proxy.getLocalizedMessage());
        assertEquals(expected.proxy.getCommonElementCount(), actual.proxy.getCommonElementCount());
        assertArrayEquals(expected.proxy.getExtendedStackTrace(), actual.proxy.getExtendedStackTrace());
        assertEquals(expected.proxy, actual.proxy);
    }

    @Test
    public void testStack() {
        final Map<String, ThrowableProxy.CacheEntry> map = new HashMap<String, ThrowableProxy.CacheEntry>();
        final Stack<Class<?>> stack = new Stack<Class<?>>();
        final Throwable throwable = new IllegalStateException("This is a test");
        final ThrowableProxy proxy = new ThrowableProxy(throwable);
        final ExtendedStackTraceElement[] callerPackageData = proxy.toExtendedStackTrace(stack, map, null,
                throwable.getStackTrace());
        assertNotNull("No package data returned", callerPackageData);
    }

    @Test
    public void testSerialization() throws Exception {
        final Throwable throwable = new IllegalArgumentException("This is a test");
        final ThrowableProxy proxy = new ThrowableProxy(throwable);
        final byte[] binary = serialize(proxy);
        final ThrowableProxy proxy2 = deserialize(binary);

        assertEquals(proxy.getName(), proxy2.getName());
        assertEquals(proxy.getMessage(), proxy2.getMessage());
        assertEquals(proxy.getCauseProxy(), proxy2.getCauseProxy());
        assertArrayEquals(proxy.getExtendedStackTrace(), proxy2.getExtendedStackTrace());
    }

    // DO NOT REMOVE THIS COMMENT:
    // UNCOMMENT WHEN GENERATING SERIALIZED THROWABLEPROXY FOR #testSerializationWithUnknownThrowable
    // public static class DeletedException extends Exception {
    // private static final long serialVersionUID = 1L;
    //
    // public DeletedException(String msg) {
    // super(msg);
    // }
    // };

    @Test
    public void testSerializationWithUnknownThrowable() throws Exception {

        final String msg = "OMG I've been deleted!";
        
        // DO NOT DELETE THIS COMMENT:
        // UNCOMMENT TO RE-GENERATE SERIALIZED EVENT WHEN UPDATING THIS TEST.
        // final Exception thrown = new DeletedException(msg);
        // final ThrowableProxy proxy = new ThrowableProxy(thrown);
        // final byte[] binary = serialize(proxy);
        // String base64 = DatatypeConverter.printBase64Binary(binary);
        // System.out.println("final String base64 = \"" + base64.replaceAll("\r\n", "\\\\r\\\\n\" +\r\n\"") + "\";");

         final String base64 = "rO0ABXNyADFvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLlRocm93YWJsZVByb3h52cww1Zp7rPoCAAdJABJjb21tb25FbGVtZW50Q291bnRMAApjYXVzZVByb3h5dAAzTG9yZy9hcGFjaGUvbG9nZ2luZy9sb2c0ai9jb3JlL2ltcGwvVGhyb3dhYmxlUHJveHk7WwASZXh0ZW5kZWRTdGFja1RyYWNldAA/W0xvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovY29yZS9pbXBsL0V4dGVuZGVkU3RhY2tUcmFjZUVsZW1lbnQ7TAAQbG9jYWxpemVkTWVzc2FnZXQAEkxqYXZhL2xhbmcvU3RyaW5nO0wAB21lc3NhZ2VxAH4AA0wABG5hbWVxAH4AA1sAEXN1cHByZXNzZWRQcm94aWVzdAA0W0xvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovY29yZS9pbXBsL1Rocm93YWJsZVByb3h5O3hwAAAAAHB1cgA/W0xvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkV4dGVuZGVkU3RhY2tUcmFjZUVsZW1lbnQ7ys+II6XHz7wCAAB4cAAAABhzcgA8b3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRqLmNvcmUuaW1wbC5FeHRlbmRlZFN0YWNrVHJhY2VFbGVtZW504d7Pusa2kAcCAAJMAA5leHRyYUNsYXNzSW5mb3QANkxvcmcvYXBhY2hlL2xvZ2dpbmcvbG9nNGovY29yZS9pbXBsL0V4dGVuZGVkQ2xhc3NJbmZvO0wAEXN0YWNrVHJhY2VFbGVtZW50dAAdTGphdmEvbGFuZy9TdGFja1RyYWNlRWxlbWVudDt4cHNyADRvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLkV4dGVuZGVkQ2xhc3NJbmZvAAAAAAAAAAECAANaAAVleGFjdEwACGxvY2F0aW9ucQB+AANMAAd2ZXJzaW9ucQB+AAN4cAF0AA10ZXN0LWNsYXNzZXMvdAABP3NyABtqYXZhLmxhbmcuU3RhY2tUcmFjZUVsZW1lbnRhCcWaJjbdhQIABEkACmxpbmVOdW1iZXJMAA5kZWNsYXJpbmdDbGFzc3EAfgADTAAIZmlsZU5hbWVxAH4AA0wACm1ldGhvZE5hbWVxAH4AA3hwAAAAaHQANW9yZy5hcGFjaGUubG9nZ2luZy5sb2c0ai5jb3JlLmltcGwuVGhyb3dhYmxlUHJveHlUZXN0dAAXVGhyb3dhYmxlUHJveHlUZXN0LmphdmF0ACV0ZXN0U2VyaWFsaXphdGlvbldpdGhVbmtub3duVGhyb3dhYmxlc3EAfgAIc3EAfgAMAHEAfgAPdAAIMS43LjBfNTVzcQB+ABD////+dAAkc3VuLnJlZmxlY3QuTmF0aXZlTWV0aG9kQWNjZXNzb3JJbXBscHQAB2ludm9rZTBzcQB+AAhzcQB+AAwAcQB+AA9xAH4AF3NxAH4AEP////9xAH4AGXB0AAZpbnZva2VzcQB+AAhzcQB+AAwAcQB+AA9xAH4AF3NxAH4AEP////90AChzdW4ucmVmbGVjdC5EZWxlZ2F0aW5nTWV0aG9kQWNjZXNzb3JJbXBscHEAfgAec3EAfgAIc3EAfgAMAHEAfgAPcQB+ABdzcQB+ABD/////dAAYamF2YS5sYW5nLnJlZmxlY3QuTWV0aG9kcHEAfgAec3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAAAvdAApb3JnLmp1bml0LnJ1bm5lcnMubW9kZWwuRnJhbWV3b3JrTWV0aG9kJDF0ABRGcmFtZXdvcmtNZXRob2QuamF2YXQAEXJ1blJlZmxlY3RpdmVDYWxsc3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAAAMdAAzb3JnLmp1bml0LmludGVybmFsLnJ1bm5lcnMubW9kZWwuUmVmbGVjdGl2ZUNhbGxhYmxldAAXUmVmbGVjdGl2ZUNhbGxhYmxlLmphdmF0AANydW5zcQB+AAhzcQB+AAwBdAAOanVuaXQtNC4xMS5qYXJxAH4AD3NxAH4AEAAAACx0ACdvcmcuanVuaXQucnVubmVycy5tb2RlbC5GcmFtZXdvcmtNZXRob2RxAH4ALHQAEWludm9rZUV4cGxvc2l2ZWx5c3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAAARdAAyb3JnLmp1bml0LmludGVybmFsLnJ1bm5lcnMuc3RhdGVtZW50cy5JbnZva2VNZXRob2R0ABFJbnZva2VNZXRob2QuamF2YXQACGV2YWx1YXRlc3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAAEPdAAeb3JnLmp1bml0LnJ1bm5lcnMuUGFyZW50UnVubmVydAARUGFyZW50UnVubmVyLmphdmF0AAdydW5MZWFmc3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAABGdAAob3JnLmp1bml0LnJ1bm5lcnMuQmxvY2tKVW5pdDRDbGFzc1J1bm5lcnQAG0Jsb2NrSlVuaXQ0Q2xhc3NSdW5uZXIuamF2YXQACHJ1bkNoaWxkc3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAAAycQB+AE1xAH4ATnEAfgBPc3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAADudAAgb3JnLmp1bml0LnJ1bm5lcnMuUGFyZW50UnVubmVyJDNxAH4AR3EAfgA0c3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAAA/dAAgb3JnLmp1bml0LnJ1bm5lcnMuUGFyZW50UnVubmVyJDFxAH4AR3QACHNjaGVkdWxlc3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAADscQB+AEZxAH4AR3QAC3J1bkNoaWxkcmVuc3EAfgAIc3EAfgAMAXQADmp1bml0LTQuMTEuamFycQB+AA9zcQB+ABAAAAA1cQB+AEZxAH4AR3QACmFjY2VzcyQwMDBzcQB+AAhzcQB+AAwBdAAOanVuaXQtNC4xMS5qYXJxAH4AD3NxAH4AEAAAAOV0ACBvcmcuanVuaXQucnVubmVycy5QYXJlbnRSdW5uZXIkMnEAfgBHcQB+AEFzcQB+AAhzcQB+AAwBdAAOanVuaXQtNC4xMS5qYXJxAH4AD3NxAH4AEAAAATVxAH4ARnEAfgBHcQB+ADRzcQB+AAhzcQB+AAwBdAAELmNwL3EAfgAPc3EAfgAQAAAAMnQAOm9yZy5lY2xpcHNlLmpkdC5pbnRlcm5hbC5qdW5pdDQucnVubmVyLkpVbml0NFRlc3RSZWZlcmVuY2V0ABhKVW5pdDRUZXN0UmVmZXJlbmNlLmphdmFxAH4ANHNxAH4ACHNxAH4ADAF0AAQuY3AvcQB+AA9zcQB+ABAAAAAmdAAzb3JnLmVjbGlwc2UuamR0LmludGVybmFsLmp1bml0LnJ1bm5lci5UZXN0RXhlY3V0aW9udAASVGVzdEV4ZWN1dGlvbi5qYXZhcQB+ADRzcQB+AAhzcQB+AAwBdAAELmNwL3EAfgAPc3EAfgAQAAAB03QANm9yZy5lY2xpcHNlLmpkdC5pbnRlcm5hbC5qdW5pdC5ydW5uZXIuUmVtb3RlVGVzdFJ1bm5lcnQAFVJlbW90ZVRlc3RSdW5uZXIuamF2YXQACHJ1blRlc3Rzc3EAfgAIc3EAfgAMAXQABC5jcC9xAH4AD3NxAH4AEAAAAqtxAH4AgnEAfgCDcQB+AIRzcQB+AAhzcQB+AAwBdAAELmNwL3EAfgAPc3EAfgAQAAABhnEAfgCCcQB+AINxAH4ANHNxAH4ACHNxAH4ADAF0AAQuY3AvcQB+AA9zcQB+ABAAAADFcQB+AIJxAH4Ag3QABG1haW50ABZPTUcgSSd2ZSBiZWVuIGRlbGV0ZWQhcQB+AJJ0AEZvcmcuYXBhY2hlLmxvZ2dpbmcubG9nNGouY29yZS5pbXBsLlRocm93YWJsZVByb3h5VGVzdCREZWxldGVkRXhjZXB0aW9udXIANFtMb3JnLmFwYWNoZS5sb2dnaW5nLmxvZzRqLmNvcmUuaW1wbC5UaHJvd2FibGVQcm94eTv67QHghaLrOQIAAHhwAAAAAA==";

        byte[] binaryDecoded = DatatypeConverter.parseBase64Binary(base64);
        final ThrowableProxy proxy2 = deserialize(binaryDecoded);

        assertEquals(this.getClass().getName() + "$DeletedException", proxy2.getName());
        assertEquals(msg, proxy2.getMessage());
    }

    private byte[] serialize(ThrowableProxy proxy) throws IOException {
        final ByteArrayOutputStream arr = new ByteArrayOutputStream();
        final ObjectOutputStream out = new ObjectOutputStream(arr);
        out.writeObject(proxy);
        return arr.toByteArray();
    }

    private ThrowableProxy deserialize(byte[] binary) throws IOException, ClassNotFoundException {
        final ByteArrayInputStream inArr = new ByteArrayInputStream(binary);
        final ObjectInputStream in = new ObjectInputStream(inArr);
        return (ThrowableProxy) in.readObject();
    }
}
