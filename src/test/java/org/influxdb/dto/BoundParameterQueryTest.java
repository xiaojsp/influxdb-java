package org.influxdb.dto;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.influxdb.dto.BoundParameterQuery.QueryBuilder;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import com.squareup.moshi.JsonReader;

import okio.Buffer;

/**
 * Test for the BoundParameterQuery DTO.
 */
@RunWith(JUnitPlatform.class)
public class BoundParameterQueryTest {

    @Test
    public void testSingleCharacterPlaceHolderParsing() throws IOException {
        BoundParameterQuery query = QueryBuilder.newQuery("SELECT * FROM abc WHERE a > $a AND b < $b")
                .forDatabase("foobar")
                .bind("a", 0)
                .bind("b", 10)
                .create();
        Map<String, Object> params = readObject(decode(query.getParameterJsonWithUrlEncoded()));
        Assert.assertEquals(2, params.size());
        Assert.assertEquals(params.get("a"), 0.0);
        Assert.assertEquals(params.get("b"), 10.0);
    }

    @Test
    public void testPlaceHolderParsing() throws IOException {
        BoundParameterQuery query = QueryBuilder.newQuery("SELECT * FROM abc WHERE a > $abc AND b < $bcd")
                .forDatabase("foobar")
                .bind("abc", 0)
                .bind("bcd", 10)
                .create();
        Map<String, Object> params = readObject(decode(query.getParameterJsonWithUrlEncoded()));
        Assert.assertEquals(2, params.size());
        Assert.assertEquals(params.get("abc"), 0.0);
        Assert.assertEquals(params.get("bcd"), 10.0);
    }

    @Test
    public void testPlaceHolderParsingWithLimitClause() throws IOException {
        BoundParameterQuery query = QueryBuilder.newQuery("SELECT * FROM abc WHERE a > $abc AND b < $bcd LIMIT 10")
                .forDatabase("foobar")
                .bind("abc", 0)
                .bind("bcd", 10)
                .create();
        Map<String, Object> params = readObject(decode(query.getParameterJsonWithUrlEncoded()));
        Assert.assertEquals(2, params.size());
        Assert.assertEquals(params.get("abc"), 0.0);
        Assert.assertEquals(params.get("bcd"), 10.0);
    }

    @Test
    public void testIgnoreInvalidPlaceholders() throws UnsupportedEncodingException {
        BoundParameterQuery query = QueryBuilder.newQuery("SELECT * FROM abc WHERE a > $")
                .forDatabase("foobar")
                .create();
        Assert.assertEquals(decode(query.getParameterJsonWithUrlEncoded()), "{}");

        query = QueryBuilder.newQuery("SELECT * FROM abc WHERE a > $abc$cde").forDatabase("foobar").create();
        Assert.assertEquals(decode(query.getParameterJsonWithUrlEncoded()), "{}");
    }

    @Test
    public void testUnbalancedQuery() throws UnsupportedEncodingException {
        // too many placeholders
        try {
            BoundParameterQuery query = QueryBuilder.newQuery("SELECT * FROM abc WHERE a > $abc AND b < $bcd")
                    .forDatabase("foobar")
                    .bind("abc", 0)
                    .create();
            query.getParameterJsonWithUrlEncoded();
            Assert.fail("Expected RuntimeException because of unbalanced placeholders and parameters");
        } catch (RuntimeException rte) {
            // expected
        }

        // too many parameters
        try {
            BoundParameterQuery query = QueryBuilder.newQuery("SELECT * FROM abc WHERE a > $abc")
                    .forDatabase("foobar")
                    .bind("abc", 0)
                    .bind("bcd", 10)
                    .create();
            query.getParameterJsonWithUrlEncoded();
            Assert.fail("Expected RuntimeException because of unbalanced placeholders and parameters");
        } catch (RuntimeException rte) {
            // expected
        }
    }

    private Map<String, Object> readObject(String json) throws IOException {
        Buffer source = new Buffer();
        source.writeString(json, Charset.forName("utf-8"));
        Map<String, Object> params = new HashMap<>();
        JsonReader reader = JsonReader.of(source);
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            Object value = reader.readJsonValue();
            params.put(name, value);
        }
        reader.endObject();
        return params;
    }

    private static String decode(String str) throws UnsupportedEncodingException {
        return URLDecoder.decode(str, StandardCharsets.UTF_8.toString());
    }
}
