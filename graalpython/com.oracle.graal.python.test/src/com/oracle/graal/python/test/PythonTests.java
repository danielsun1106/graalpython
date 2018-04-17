/*
 * Copyright (c) 2017, Oracle and/or its affiliates.
 * Copyright (c) 2013, Regents of the University of California
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.graal.python.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.junit.BeforeClass;

import com.oracle.graal.python.PythonLanguage;
import com.oracle.graal.python.runtime.PythonContext;
import com.oracle.graal.python.runtime.PythonParseResult;
import com.oracle.graal.python.runtime.exception.PException;
import com.oracle.graal.python.shell.GraalPythonEnvVars;
import com.oracle.graal.python.test.interop.JavaInteropTest;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.MissingMIMETypeException;
import com.oracle.truffle.api.source.MissingNameException;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.Source.Builder;

public class PythonTests {
    static {
        URLConnection openConnection;
        try {
            openConnection = JavaInteropTest.class.getProtectionDomain().getCodeSource().getLocation().openConnection();
            if (!(openConnection instanceof JarURLConnection)) {
                System.setProperty("python.home", GraalPythonEnvVars.graalpythonHome());
            }
        } catch (IOException e) {
        }
    }

    final static ByteArrayOutputStream errArray = new ByteArrayOutputStream();
    final static ByteArrayOutputStream outArray = new ByteArrayOutputStream();
    final static PrintStream errStream = new PrintStream(errArray);
    final static PrintStream outStream = new PrintStream(outArray);
    static Context context = null;

    public static void assertBenchNoError(Path scriptName, String[] args) {
        final ByteArrayOutputStream byteArrayErr = new ByteArrayOutputStream();
        final ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
        final PrintStream printErrStream = new PrintStream(byteArrayErr);
        final PrintStream printOutStream = new PrintStream(byteArrayOut);
        File source = getBenchFile(scriptName);
        if (args == null)
            PythonTests.runScript(new String[]{source.toString()}, source, printOutStream, printErrStream);
        else {
            args[0] = source.toString();
            PythonTests.runScript(args, source, printOutStream, printErrStream);
        }

        String err = byteArrayErr.toString().replaceAll("\r\n", "\n");
        String result = byteArrayOut.toString().replaceAll("\r\n", "\n");
        System.out.println(scriptName.toString() + "\n" + result + "\n");
        assertEquals("", err);
        assertNotEquals("", result);
    }

    public static void assertLastLineError(String expected, String code) {
        final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArray);
        String source = code;
        PythonTests.runThrowableScript(new String[0], source, System.out, printStream);
        String[] output = byteArray.toString().split("\n");
        // ignore the traceback
        assertEquals(expected.trim(), output[0].trim());
    }

    public static void assertLastLineErrorContains(String expected, String code) {
        final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArray);
        String source = code;
        PythonTests.runThrowableScript(new String[0], source, System.out, printStream);
        String[] output = byteArray.toString().split("\n");
        // ignore the traceback
        assertTrue(output[0].contains(expected.trim()));
    }

    public static void assertPrintContains(String expected, Path scriptName) {
        final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArray);

        String source = getSource(getTestFile(scriptName));
        PythonTests.runScript(new String[0], source, printStream, System.err);
        String result = byteArray.toString().replaceAll("\r\n", "\n");
        assertTrue(result.contains(expected));
    }

    public static void assertPrintContains(String expected, String code) {
        final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArray);
        String source = code;
        PythonTests.runScript(new String[0], source, printStream, System.err);
        String result = byteArray.toString().replaceAll("\r\n", "\n");
        assertTrue(result.contains(expected));
    }

    public static void assertPrints(Path expected, Path scriptName) {
        final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArray);

        File scriptFile = getTestFile(scriptName);
        String source = getSource(scriptFile);
        String output = getFileContent(getTestFile(expected));
        PythonTests.runScript(new String[0], source, printStream, System.err);
        String result = byteArray.toString().replaceAll("\r\n", "\n");
        assertEquals(output, result);
    }

    public static void assertPrints(String expected, Path scriptName) {
        final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArray);

        File scriptFile = getTestFile(scriptName);
        PythonTests.runScript(new String[]{scriptFile.toString()}, scriptFile, printStream, System.err);
        String result = byteArray.toString().replaceAll("\r\n", "\n");
        assertEquals(expected, result);
    }

    public static void assertPrints(String expected, String code) {
        final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(byteArray);
        String source = code;
        PythonTests.runScript(new String[0], source, printStream, System.err);
        String result = byteArray.toString().replaceAll("\r\n", "\n");
        assertEquals(expected, result);
    }

    public static VirtualFrame createVirtualFrame() {
        return Truffle.getRuntime().createVirtualFrame(null, new FrameDescriptor());
    }

    public static void ensureContext() {
        resetContext(new String[0]);
        // XXX
        Field field;
        try {
            field = PythonTests.context.getClass().getDeclaredField("impl");
            field.setAccessible(true);
            Object polyglotContextImpl = field.get(PythonTests.context);
            Method enterMethod = polyglotContextImpl.getClass().getDeclaredMethod("enter", new Class<?>[0]);
            enterMethod.setAccessible(true);
            enterMethod.invoke(polyglotContextImpl);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    static void flush(OutputStream out, OutputStream err) {
        PythonTests.outStream.flush();
        PythonTests.errStream.flush();
        try {
            out.write(PythonTests.outArray.toByteArray());
            err.write(PythonTests.errArray.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static File getBenchFile(Path filename) {
        Path path = Paths.get(GraalPythonEnvVars.graalpythonHome(), "benchmarks", "src");
        if (!Files.isDirectory(path))
            throw new RuntimeException("Unable to locate benchmarks/src/");

        Path fullPath = Paths.get(path.toString(), filename.toString());
        if (!Files.isReadable(fullPath)) {
            fullPath = Paths.get(path.toString(), "benchmarks", filename.toString());
            if (!Files.isReadable(fullPath)) {
                fullPath = Paths.get(path.toString(), "micro", filename.toString());
                if (!Files.isReadable(fullPath))
                    throw new IllegalStateException("Unable to locate " + path + " (benchmarks or micro) " + filename.toString());
            }
        }

        File file = new File(fullPath.toString());
        return file;
    }

    public static PythonContext getContext() {
        PythonTests.ensureContext();
        return PythonLanguage.getContext();
    }

    static Context getContext(String[] args) {
        resetContext(args);
        PythonTests.outArray.reset();
        PythonTests.errArray.reset();
        return PythonTests.context;
    }

    private static String getFileContent(File file) {
        String ret = null;
        Reader reader;
        try {
            reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
            final BufferedReader bufferedReader = new BufferedReader(reader);
            final StringBuilder content = new StringBuilder();
            final char[] buffer = new char[1024];

            try {
                int n = 0;
                while (n != -1) {
                    n = bufferedReader.read(buffer);
                    if (n != -1)
                        content.append(buffer, 0, n);
                }
            } finally {
                bufferedReader.close();
            }
            ret = content.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static PythonParseResult getParseResult(com.oracle.truffle.api.source.Source source, PrintStream out, PrintStream err) {
        PythonTests.ensureContext();
        PythonContext ctx = PythonLanguage.getContext();
        ctx.setOut(out);
        ctx.setErr(err);
        return ctx.getCore().getParser().parse(ctx.getCore(), source);
    }

    public static PythonParseResult getParseResult(String code) {
        final ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
        Builder<RuntimeException, MissingMIMETypeException, MissingNameException> newBuilder = Source.newBuilder(code);
        newBuilder.mimeType(PythonLanguage.MIME_TYPE);
        newBuilder.name("test");
        try {
            return PythonTests.getParseResult(newBuilder.build(), new PrintStream(byteArray), System.err);
        } catch (RuntimeException | MissingMIMETypeException | MissingNameException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getSource(File file) {
        try {
            return new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
        } catch (IOException e) {
            return null;
        }
    }

    public static File getTestFile(Path filename) {
        Path path = Paths.get(GraalPythonEnvVars.graalpythonHome(), "com.oracle.graal.python.test", "src", "tests", filename.toString());
        if (Files.isReadable(path)) {
            return new File(path.toString());
        } else
            throw new RuntimeException("Unable to locate " + path);
    }

    public static void resetContext(String[] args) {
        if (PythonTests.context != null) {
            PythonTests.context.close();
        }
        org.graalvm.polyglot.Context.Builder ctxBuilder = Context.newBuilder();
        ctxBuilder.allowAllAccess(true);
        PythonTests.outArray.reset();
        PythonTests.errArray.reset();
        ctxBuilder.out(PythonTests.outStream);
        ctxBuilder.err(PythonTests.errStream);
        ctxBuilder.arguments("python", args);
        PythonTests.context = ctxBuilder.build();
        PythonTests.context.initialize("python");
    }

    public static void runScript(String[] args, File path, OutputStream out, OutputStream err) {
        try {
            PythonTests.getContext(args).eval(org.graalvm.polyglot.Source.newBuilder("python", path).build());
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            flush(out, err);
        }
    }

    public static void runScript(String[] args, String source, OutputStream out, OutputStream err) {
        try {
            PythonTests.getContext(args).eval(org.graalvm.polyglot.Source.create("python", source));
        } finally {
            flush(out, err);
        }
    }

    public static void runThrowableScript(String[] args, String source, OutputStream out, OutputStream err) {
        try {
            runScript(args, source, out, err);
        } catch (PolyglotException t) {
            Object e;
            try {
                Field field = t.getClass().getDeclaredField("impl");
                field.setAccessible(true);
                Object object = field.get(t);
                Field field2 = object.getClass().getDeclaredField("exception");
                field2.setAccessible(true);
                e = field2.get(object);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
            if (e instanceof PException) {
                ((PException) e).printStackTrace(new PrintStream(err));
            } else if (e instanceof Exception) {
                throw new RuntimeException((Exception) e);
            } else {
                throw new RuntimeException(e.toString());
            }
        }
    }

    @BeforeClass
    public static void setUp() {
        PythonTests.resetContext(new String[0]);
    }

}
