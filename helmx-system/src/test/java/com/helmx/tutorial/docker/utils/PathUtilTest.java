package com.helmx.tutorial.docker.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PathUtilTest {

    @Test
    void sanitizeDirectoryName_null_returnsNull() {
        assertNull(PathUtil.sanitizeDirectoryName(null));
    }

    @Test
    void sanitizeDirectoryName_empty_returnsNull() {
        assertNull(PathUtil.sanitizeDirectoryName(""));
    }

    @Test
    void sanitizeDirectoryName_validName_returnsUnchanged() {
        assertEquals("my-project", PathUtil.sanitizeDirectoryName("my-project"));
        assertEquals("my_folder_123", PathUtil.sanitizeDirectoryName("my_folder_123"));
    }

    @Test
    void sanitizeDirectoryName_slashesAndSpecialChars_areRemoved() {
        // / \ < > : " | ? * should all be stripped
        assertEquals("dangerousname", PathUtil.sanitizeDirectoryName("dangerous/name"));
        assertEquals("dangerousname", PathUtil.sanitizeDirectoryName("dangerous\\name"));
        assertEquals("name", PathUtil.sanitizeDirectoryName("<>:|?*name"));
    }

    @Test
    void sanitizeDirectoryName_dotDotTraversal_isRemoved() {
        // "../etc" → after removing "/" → "..etc" → after removing ".." → "etc"
        assertEquals("etc", PathUtil.sanitizeDirectoryName("../etc"));
    }

    @Test
    void sanitizeDirectoryName_dotPrefix_isStripped() {
        assertEquals("hidden", PathUtil.sanitizeDirectoryName(".hidden"));
    }

    @Test
    void sanitizeDirectoryName_onlyDangerousChars_returnsNull() {
        assertNull(PathUtil.sanitizeDirectoryName("///"));
        assertNull(PathUtil.sanitizeDirectoryName(".."));
    }

    @Test
    void sanitizeDirectoryName_longName_isTruncatedTo100() {
        String longName = "a".repeat(150);
        String result = PathUtil.sanitizeDirectoryName(longName);
        assertNotNull(result);
        assertEquals(100, result.length());
    }
}
