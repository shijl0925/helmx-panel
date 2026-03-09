package com.helmx.tutorial.docker.utils;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DockerClientUtilTest {

    @Test
    void extractFileFromTar_returnsFileBytesForRegularEntry() throws Exception {
        DockerClientUtil util = new DockerClientUtil();
        byte[] tarBytes = createTar("demo.txt", "hello");

        byte[] result = util.extractFileFromTar(new ByteArrayInputStream(tarBytes));

        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    void extractFileFromTar_withoutRegularEntry_throwsIOException() throws Exception {
        DockerClientUtil util = new DockerClientUtil();
        byte[] tarBytes = createDirectoryOnlyTar("folder/");

        IOException exception = assertThrows(IOException.class,
                () -> util.extractFileFromTar(new ByteArrayInputStream(tarBytes)));
        assertInstanceOf(IOException.class, exception);
    }

    private byte[] createTar(String name, String content) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(outputStream)) {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            TarArchiveEntry entry = new TarArchiveEntry(name);
            entry.setSize(bytes.length);
            tarOutputStream.putArchiveEntry(entry);
            tarOutputStream.write(bytes);
            tarOutputStream.closeArchiveEntry();
            tarOutputStream.finish();
        }
        return outputStream.toByteArray();
    }

    private byte[] createDirectoryOnlyTar(String name) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(outputStream)) {
            TarArchiveEntry entry = new TarArchiveEntry(name);
            entry.setSize(0);
            tarOutputStream.putArchiveEntry(entry);
            tarOutputStream.closeArchiveEntry();
            tarOutputStream.finish();
        }
        return outputStream.toByteArray();
    }
}
