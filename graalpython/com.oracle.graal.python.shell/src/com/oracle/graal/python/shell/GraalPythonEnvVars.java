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
package com.oracle.graal.python.shell;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;

public class GraalPythonEnvVars {
    private static final boolean AOT = Boolean.getBoolean("com.oracle.truffle.aot") || Boolean.getBoolean("com.oracle.graalvm.isaot");
    private static final String LIB_GRAALPYTHON = "lib-graalpython";
    private static final String NO_CORE = "Fatal: You need to pass --python.CoreHome because its location could not be discovered.";

    public static String graalpythonHome() {
        try {
            return discoverHome();
        } catch (IOException e) {
            graalpythonExit(NO_CORE);
        }
        return null;
    }

    private static void graalpythonExit(String msg) {
        System.err.println("GraalPython unexpected failure: " + msg);
        System.exit(1);
    }

    private static String discoverHome() throws IOException {
        if (AOT) {
            final String executablePath = (String) Compiler.command(new Object[]{"com.oracle.svm.core.posix.GetExecutableName"});
            final Path parentDirectory = Paths.get(executablePath).getParent().getParent();

            // Root of the GraalVM distribution.
            Path candidate = parentDirectory.resolve("languages").resolve("python");
            if (isGraalPythonHome(candidate)) {
                return candidate.toFile().getCanonicalPath().toString();
            }

            // Root of the GraalPython source tree.
            candidate = parentDirectory.resolve("graalpython");
            if (isGraalPythonHome(parentDirectory)) {
                return parentDirectory.toFile().getCanonicalPath().toString();
            }

            // we cache the parse trees above, so we don't need to return anything here
            return "";
        } else {
            return discoverHomeFromSource();
        }
    }

    private static String discoverHomeFromSource() throws IOException {
        final CodeSource codeSource = GraalPythonEnvVars.class.getProtectionDomain().getCodeSource();

        if (codeSource != null && codeSource.getLocation().getProtocol().equals("file")) {
            final Path codeLocation = Paths.get(codeSource.getLocation().getFile());
            final Path codeDir = codeLocation.getParent();

            // executing from jar file in source tree
            if (codeDir.endsWith(Paths.get("mxbuild", "dists"))) {
                final Path candidate = codeDir.getParent().getParent().resolve("graalpython");
                if (isGraalPythonHome(candidate)) {
                    // Jar source build
                    return candidate.toFile().getCanonicalPath().toString();
                }
            }

            // executing from jar file in source main
            Path mainDirCandidate = codeDir.resolve("graalpython");
            if (isGraalPythonHome(mainDirCandidate)) {
                // Jar source build
                return mainDirCandidate.toFile().getCanonicalPath().toString();
            }

            // executing from class files in source tree
            if (codeDir.getParent().endsWith(Paths.get("mxbuild", "graalpython"))) {
                final Path candidate = codeDir.getParent().getParent().getParent().resolve("graalpython");
                if (isGraalPythonHome(candidate)) {
                    // Jar source build
                    return candidate.toFile().getCanonicalPath().toString();
                }
            }

            // executing from jar file in GraalVM build or distribution
            if (codeDir.getFileName().toString().equals("python")) {
                if (isGraalPythonHome(codeDir)) {
                    // GraalVM build or distribution
                    return codeDir.toFile().getCanonicalPath().toString();
                }
            }

            // executing from binary import
            if (codeDir.endsWith(Paths.get("mx.imports", "binary", "graalpython"))) {
                final Path candidate = codeDir.resolve(Paths.get("mxbuild", "graalpython-zip"));
                if (isGraalPythonHome(candidate)) {
                    return candidate.toFile().getCanonicalPath().toString();
                }
            } else if (codeDir.endsWith(Paths.get("mx.imports", "binary", "graalpython", "mxbuild", "dists"))) {
                // executing from another binary import layout
                final Path candidate = codeDir.resolveSibling(Paths.get("graalpython-zip"));
                if (isGraalPythonHome(candidate)) {
                    return candidate.toFile().getCanonicalPath().toString();
                }
            }
        }
        throw new IOException();
    }

    private static boolean isGraalPythonHome(Path src) {
        return Files.isDirectory(src.resolve(LIB_GRAALPYTHON));
    }

    public static String includeDirectory() {
        Path graalpythonHome = Paths.get(graalpythonHome());

        Path candidate = graalpythonHome.resolve("include");
        if (Files.exists(candidate)) {
            return candidate.toString();
        }

        candidate = graalpythonHome.resolve("com.oracle.graal.python.cext").resolve("include");
        if (Files.exists(candidate)) {
            return candidate.toString();
        }

        return ".";
    }
}
