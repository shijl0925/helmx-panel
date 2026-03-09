package com.helmx.tutorial.docker.utils;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DockerClientUtilTest {

    @Test
    void extractFileFromTar_returnsFileBytesForRegularEntry() throws Exception {
        DockerClientUtil util = new DockerClientUtil();
        Method method = DockerClientUtil.class.getDeclaredMethod("extractFileFromTar", java.io.InputStream.class);
        method.setAccessible(true);

        byte[] tarBytes = createTar("demo.txt", "hello");

        byte[] result = (byte[]) method.invoke(util, new ByteArrayInputStream(tarBytes));

        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), result);
    }

    @Test
    void extractFileFromTar_withoutRegularEntry_throwsIOException() throws Exception {
        DockerClientUtil util = new DockerClientUtil();
        Method method = DockerClientUtil.class.getDeclaredMethod("extractFileFromTar", java.io.InputStream.class);
        method.setAccessible(true);

        byte[] tarBytes = createDirectoryOnlyTar("folder/");

        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> method.invoke(util, new ByteArrayInputStream(tarBytes)));
        assertInstanceOf(IOException.class, exception.getCause());
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
            entry.setMode(TarArchiveEntry.DEFAULT_DIR_MODE);
            entry.setSize(0);
            tarOutputStream.putArchiveEntry(entry);
            tarOutputStream.closeArchiveEntry();
            tarOutputStream.finish();
        }
        return outputStream.toByteArray();
    }
}
