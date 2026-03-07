package com.helmx.tutorial.docker.dto;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.helmx.tutorial.docker.utils.ByteUtils;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ImageHistoryItem and the JSON-parsing logic used in
 * DockerClientUtil.getImageHistory().
 *
 * These tests focus on the bugs fixed in the PR-11 follow-up:
 *  1. Tags field is null in Docker API response for untagged layers.
 *  2. rawList from JSON.parseArray can be null.
 *  3. All numeric fields may be absent/null.
 */
class ImageHistoryItemTest {

    // ---------- DTO constructor / getters ----------

    @Test
    void constructor_allFields_setsCorrectly() {
        List<String> tags = List.of("nginx:latest");
        ImageHistoryItem item = new ImageHistoryItem(
                "sha256:abc", "2024-01-01T00:00:00Z", "/bin/sh -c apt-get install nginx",
                "12.00 MB", tags, "base layer");

        assertEquals("sha256:abc", item.getId());
        assertEquals("2024-01-01T00:00:00Z", item.getCreated());
        assertEquals("/bin/sh -c apt-get install nginx", item.getLayer());
        assertEquals("12.00 MB", item.getSize());
        assertEquals(tags, item.getTags());
        assertEquals("base layer", item.getComment());
    }

    @Test
    void noArgsConstructor_allFieldsNull() {
        ImageHistoryItem item = new ImageHistoryItem();
        assertNull(item.getId());
        assertNull(item.getCreated());
        assertNull(item.getLayer());
        assertNull(item.getSize());
        assertNull(item.getTags());
        assertNull(item.getComment());
    }

    // ---------- JSON-parsing logic (mirrors DockerClientUtil.getImageHistory) ----------

    /** Parses the raw Docker API JSON array into List<ImageHistoryItem>. */
    private List<ImageHistoryItem> parse(String json) {
        List<JSONObject> rawList = JSON.parseArray(json, JSONObject.class);
        if (rawList == null) {
            return new ArrayList<>();
        }
        List<ImageHistoryItem> result = new ArrayList<>();
        for (JSONObject item : rawList) {
            String createdBy = item.getString("CreatedBy");
            if (createdBy == null) createdBy = "";
            Long sizeBytes = item.getLong("Size");
            Long createdEpoch = item.getLong("Created");
            String id = item.getString("Id");
            String comment = item.getString("Comment");
            // Fix: null Tags → empty list
            List<String> rawTags = item.getList("Tags", String.class);
            List<String> tags = rawTags != null ? rawTags : new ArrayList<>();
            String created = createdEpoch != null ? Instant.ofEpochSecond(createdEpoch).toString() : "";
            String size = sizeBytes != null ? ByteUtils.formatBytes(sizeBytes) : "0 B";
            result.add(new ImageHistoryItem(id, created, createdBy, size, tags, comment));
        }
        return result;
    }

    @Test
    void parse_nullTagsInResponse_yieldsEmptyTagsList() {
        String json = "[{\"Id\":\"<missing>\",\"Created\":1700000000,\"CreatedBy\":\"/bin/sh\",\"Size\":0,\"Tags\":null,\"Comment\":\"\"}]";
        List<ImageHistoryItem> items = parse(json);

        assertEquals(1, items.size());
        // Must be an empty list, not null — prevents NPE when iterating tags
        assertNotNull(items.get(0).getTags());
        assertTrue(items.get(0).getTags().isEmpty());
    }

    @Test
    void parse_withTags_returnsTagsList() {
        String json = "[{\"Id\":\"sha256:abc\",\"Created\":1700000000,\"CreatedBy\":\"/bin/sh\",\"Size\":1024,\"Tags\":[\"nginx:latest\"],\"Comment\":\"\"}]";
        List<ImageHistoryItem> items = parse(json);

        assertEquals(1, items.size());
        assertEquals(List.of("nginx:latest"), items.get(0).getTags());
    }

    @Test
    void parse_emptyArray_returnsEmptyList() {
        List<ImageHistoryItem> items = parse("[]");
        assertTrue(items.isEmpty());
    }

    @Test
    void parse_nullJson_returnsEmptyList() {
        // JSON.parseArray("null", ...) may return null — must be handled
        List<ImageHistoryItem> items = parse("null");
        assertNotNull(items);
        assertTrue(items.isEmpty());
    }

    @Test
    void parse_epochCreated_convertsToIso8601() {
        String json = "[{\"Id\":\"id1\",\"Created\":0,\"CreatedBy\":\"\",\"Size\":0,\"Tags\":null,\"Comment\":\"\"}]";
        List<ImageHistoryItem> items = parse(json);

        assertEquals(1, items.size());
        assertEquals(Instant.ofEpochSecond(0).toString(), items.get(0).getCreated());
    }

    @Test
    void parse_missingCreated_yieldsEmptyString() {
        String json = "[{\"Id\":\"id1\",\"CreatedBy\":\"\",\"Size\":0,\"Tags\":null,\"Comment\":\"\"}]";
        List<ImageHistoryItem> items = parse(json);

        assertEquals("", items.get(0).getCreated());
    }

    @Test
    void parse_nullCreatedBy_yieldsEmptyString() {
        String json = "[{\"Id\":\"id1\",\"Created\":1700000000,\"CreatedBy\":null,\"Size\":0,\"Tags\":null,\"Comment\":\"\"}]";
        List<ImageHistoryItem> items = parse(json);

        assertEquals("", items.get(0).getLayer());
    }

    @Test
    void parse_nullSize_yields0B() {
        String json = "[{\"Id\":\"id1\",\"Created\":1700000000,\"CreatedBy\":\"\",\"Tags\":null,\"Comment\":\"\"}]";
        List<ImageHistoryItem> items = parse(json);

        assertEquals("0 B", items.get(0).getSize());
    }

    @Test
    void parse_multipleItems_allParsedCorrectly() {
        String json = "["
                + "{\"Id\":\"id1\",\"Created\":1700000000,\"CreatedBy\":\"CMD\",\"Size\":1024,\"Tags\":[\"v1\"],\"Comment\":\"c1\"},"
                + "{\"Id\":\"id2\",\"Created\":1700000001,\"CreatedBy\":\"RUN\",\"Size\":2048,\"Tags\":null,\"Comment\":\"c2\"}"
                + "]";
        List<ImageHistoryItem> items = parse(json);

        assertEquals(2, items.size());
        assertEquals("id1", items.get(0).getId());
        assertEquals(List.of("v1"), items.get(0).getTags());
        assertEquals("id2", items.get(1).getId());
        assertTrue(items.get(1).getTags().isEmpty());
    }
}
