/*
 * Copyright (c) 2018, Oracle and/or its affiliates.
 * Copyright (c) 2014, Regents of the University of California
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
package com.oracle.graal.python.builtins.modules;

import static com.oracle.graal.python.runtime.exception.PythonErrorType.NotImplementedError;
import static com.oracle.graal.python.runtime.exception.PythonErrorType.OSError;
import static com.oracle.graal.python.runtime.exception.PythonErrorType.TypeError;
import static com.oracle.graal.python.runtime.exception.PythonErrorType.ValueError;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.oracle.graal.python.builtins.Builtin;
import com.oracle.graal.python.builtins.CoreFunctions;
import com.oracle.graal.python.builtins.PythonBuiltins;
import com.oracle.graal.python.builtins.modules.PosixModuleBuiltinsFactory.StatNodeFactory;
import com.oracle.graal.python.builtins.objects.PNone;
import com.oracle.graal.python.builtins.objects.bytes.PByteArray;
import com.oracle.graal.python.builtins.objects.bytes.PBytes;
import com.oracle.graal.python.builtins.objects.dict.PDict;
import com.oracle.graal.python.builtins.objects.floats.PFloat;
import com.oracle.graal.python.builtins.objects.ints.PInt;
import com.oracle.graal.python.builtins.objects.str.PString;
import com.oracle.graal.python.builtins.objects.tuple.PTuple;
import com.oracle.graal.python.nodes.function.PythonBuiltinNode;
import com.oracle.graal.python.runtime.PythonCore;
import com.oracle.graal.python.runtime.exception.PException;
import com.oracle.graal.python.runtime.exception.PythonExitException;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

@CoreFunctions(defineModule = "posix")
public class PosixModuleBuiltins extends PythonBuiltins {
    private static final int TMPFILE = 4259840;
    private static final int TEMPORARY = 4259840;
    private static final int SYNC = 1052672;
    private static final int RSYNC = 1052672;
    private static final int DIRECT = 16384;
    private static final int DSYNC = 4096;
    private static final int NDELAY = 2048;
    private static final int NONBLOCK = 2048;
    private static final int APPEND = 1024;
    private static final int TRUNC = 512;
    private static final int EXCL = 128;
    private static final int CREAT = 64;
    private static final int RDWR = 2;
    private static final int WRONLY = 1;
    private static final int RDONLY = 0;

    private static final int SEEK_SET = 0;
    private static final int SEEK_CUR = 1;
    private static final int SEEK_END = 2;

    private static final int F_OK = 0;
    private static final int X_OK = 1;

    private static ArrayList<SeekableByteChannel> files = new ArrayList<>(Arrays.asList(new SeekableByteChannel[]{null, null, null}));
    private static ArrayList<String> filePaths = new ArrayList<>(Arrays.asList(new String[]{"stdin", "stdout", "stderr"}));

    private static PosixFilePermission[][] otherBitsToPermission = new PosixFilePermission[][]{
                    new PosixFilePermission[]{},
                    new PosixFilePermission[]{PosixFilePermission.OTHERS_EXECUTE},
                    new PosixFilePermission[]{PosixFilePermission.OTHERS_WRITE},
                    new PosixFilePermission[]{PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE},
                    new PosixFilePermission[]{PosixFilePermission.OTHERS_READ},
                    new PosixFilePermission[]{PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_EXECUTE},
                    new PosixFilePermission[]{PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE},
                    new PosixFilePermission[]{PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE, PosixFilePermission.OTHERS_EXECUTE},
    };
    private static PosixFilePermission[][] groupBitsToPermission = new PosixFilePermission[][]{
                    new PosixFilePermission[]{},
                    new PosixFilePermission[]{PosixFilePermission.GROUP_EXECUTE},
                    new PosixFilePermission[]{PosixFilePermission.GROUP_WRITE},
                    new PosixFilePermission[]{PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE},
                    new PosixFilePermission[]{PosixFilePermission.GROUP_READ},
                    new PosixFilePermission[]{PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_EXECUTE},
                    new PosixFilePermission[]{PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE},
                    new PosixFilePermission[]{PosixFilePermission.GROUP_READ, PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE},
    };
    private static PosixFilePermission[][] ownerBitsToPermission = new PosixFilePermission[][]{
                    new PosixFilePermission[]{},
                    new PosixFilePermission[]{PosixFilePermission.OWNER_EXECUTE},
                    new PosixFilePermission[]{PosixFilePermission.OWNER_WRITE},
                    new PosixFilePermission[]{PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE},
                    new PosixFilePermission[]{PosixFilePermission.OWNER_READ},
                    new PosixFilePermission[]{PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE},
                    new PosixFilePermission[]{PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE},
                    new PosixFilePermission[]{PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE},
    };

    @Override
    protected List<? extends NodeFactory<? extends PythonBuiltinNode>> getNodeFactories() {
        return PosixModuleBuiltinsFactory.getFactories();
    }

    private abstract static class PythonFileNode extends PythonBuiltinNode {
        protected SeekableByteChannel getFileChannel(int fd) {
            if (files.size() <= fd || fd < 3) {
                throw raise(OSError, "Bad file descriptor");
            }
            SeekableByteChannel channel = files.get(fd);
            if (channel == null) {
                throw raise(OSError, "Bad file descriptor");
            }
            return channel;
        }

        protected String getFilePath(int fd) {
            if (filePaths.size() <= fd || fd < 3) {
                throw raise(OSError, "Bad file descriptor");
            }
            String path = filePaths.get(fd);
            if (path == null) {
                throw raise(OSError, "Bad file descriptor");
            }
            return path;
        }

        protected void removeFile(int fd) {
            files.set(fd, null);
            filePaths.set(fd, null);
        }

        private static int nextFreeFd() {
            for (int i = 0; i < filePaths.size(); i++) {
                String openPath = filePaths.get(i);
                if (openPath == null) {
                    assert files.get(i) == null;
                    return i;
                }
            }
            files.add(null);
            filePaths.add(null);
            return filePaths.size() - 1;
        }

        protected int addFile(TruffleFile path, SeekableByteChannel fc) {
            int fd = nextFreeFd();
            files.set(fd, fc);
            filePaths.set(fd, path.getAbsoluteFile().getPath());
            return fd;
        }

        protected int dupFile(int fd) {
            String filePath = getFilePath(fd);
            SeekableByteChannel fileChannel = getFileChannel(fd);
            int fd2 = nextFreeFd();
            files.set(fd2, fileChannel);
            filePaths.set(fd2, filePath);
            return fd2;
        }
    }

    public PosixModuleBuiltins() {
        super();
        builtinConstants.put("O_RDONLY", RDONLY);
        builtinConstants.put("O_WRONLY", WRONLY);
        builtinConstants.put("O_RDWR", RDWR);
        builtinConstants.put("O_CREAT", CREAT);
        builtinConstants.put("O_EXCL", EXCL);
        builtinConstants.put("O_TRUNC", TRUNC);
        builtinConstants.put("O_APPEND", APPEND);
        builtinConstants.put("O_NONBLOCK", NONBLOCK);
        builtinConstants.put("O_NDELAY", NDELAY);
        builtinConstants.put("O_DSYNC", DSYNC);
        builtinConstants.put("O_DIRECT", DIRECT);
        builtinConstants.put("O_RSYNC", RSYNC);
        builtinConstants.put("O_SYNC", SYNC);
        builtinConstants.put("O_TEMPORARY", TEMPORARY);
        builtinConstants.put("O_TMPFILE", TMPFILE);
        builtinConstants.put("SEEK_SET", SEEK_SET);
        builtinConstants.put("SEEK_CUR", SEEK_CUR);
        builtinConstants.put("SEEK_END", SEEK_END);

        builtinConstants.put("F_OK", F_OK);
        builtinConstants.put("X_OK", X_OK);
    }

    @Override
    public void initialize(PythonCore core) {
        super.initialize(core);
        builtinConstants.put("_have_functions", core.factory().createList());

        Map<String, String> getenv = System.getenv();
        PDict environ = core.factory().createDict();
        for (Entry<String, String> entry : getenv.entrySet()) {
            environ.setItem(core.factory().createBytes(entry.getKey().getBytes()), core.factory().createBytes(entry.getValue().getBytes()));
        }
        builtinConstants.put("environ", environ);
    }

    @Builtin(name = "getcwd", fixedNumOfArguments = 0)
    @GenerateNodeFactory
    public abstract static class CwdNode extends PythonBuiltinNode {
        @Specialization
        String cwd() {
            return System.getProperty("user.dir");
        }

    }

    @Builtin(name = "fstat", fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class FstatNode extends PythonFileNode {
        @Child private StatNode statNode = StatNodeFactory.create(null);

        @Specialization(guards = {"fd >= 0", "fd <= 2"})
        @TruffleBoundary
        Object fstatStd(@SuppressWarnings("unused") int fd) {
            return factory().createTuple(new Object[]{
                            8592,
                            0, // ino
                            0, // dev
                            0, // nlink
                            0,
                            0,
                            0,
                            0,
                            0,
                            0
            });
        }

        @Specialization(guards = "fd > 2")
        @TruffleBoundary
        Object fstat(int fd) {
            return statNode.executeWith(getFilePath(fd).toString());
        }
    }

    @Builtin(name = "stat", fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class StatNode extends PythonBuiltinNode {
        private static final int S_IFIFO = 0010000;
        private static final int S_IFCHR = 0020000;
        private static final int S_IFBLK = 0060000;
        private static final int S_IFSOCK = 0140000;
        private static final int S_IFLNK = 0120000;
        private static final int S_IFDIR = 0040000;
        private static final int S_IFREG = 0100000;

        protected abstract Object executeWith(Object path);

        @Specialization
        @TruffleBoundary
        Object stat(String path) {
            TruffleFile f = getContext().getEnv().getTruffleFile(path);
            if (!f.exists()) {
                throw raise(OSError, "No such file or directory: '%s'", path);
            }
            int mode = 0;
            long size = 0;
            long ctime = 0;
            long atime = 0;
            long mtime = 0;
            int gid = 0;
            int uid = 0;
            if (f.isRegularFile()) {
                mode |= S_IFREG;
            } else if (f.isDirectory()) {
                mode |= S_IFDIR;
            } else if (f.isSymbolicLink()) {
                mode |= S_IFLNK;
            } else {
                // TODO: differentiate these
                mode |= S_IFSOCK | S_IFBLK | S_IFCHR | S_IFIFO;
            }
            try {
                mtime = f.getLastModifiedTime().toMillis();
            } catch (IOException e1) {
                mtime = 0;
            }
            try {
                ctime = f.getCreationTime().toMillis();
            } catch (IOException e1) {
                ctime = 0;
            }
            try {
                atime = f.getLastAccessTime().toMillis();
            } catch (IOException e1) {
                atime = 0;
            }
            gid = 1;
            uid = 1;
            try {
                final Set<PosixFilePermission> posixFilePermissions = f.getPosixPermissions();
                if (posixFilePermissions.contains(PosixFilePermission.OTHERS_READ)) {
                    mode |= 0004;
                }
                if (posixFilePermissions.contains(PosixFilePermission.OTHERS_WRITE)) {
                    mode |= 0002;
                }
                if (posixFilePermissions.contains(PosixFilePermission.OTHERS_EXECUTE)) {
                    mode |= 0001;
                }
                if (posixFilePermissions.contains(PosixFilePermission.GROUP_READ)) {
                    mode |= 0040;
                }
                if (posixFilePermissions.contains(PosixFilePermission.GROUP_WRITE)) {
                    mode |= 0020;
                }
                if (posixFilePermissions.contains(PosixFilePermission.GROUP_EXECUTE)) {
                    mode |= 0010;
                }
                if (posixFilePermissions.contains(PosixFilePermission.OWNER_READ)) {
                    mode |= 0400;
                }
                if (posixFilePermissions.contains(PosixFilePermission.OWNER_WRITE)) {
                    mode |= 0200;
                }
                if (posixFilePermissions.contains(PosixFilePermission.OWNER_EXECUTE)) {
                    mode |= 0100;
                }
            } catch (UnsupportedOperationException | IOException e1) {
                if (f.isReadable()) {
                    mode |= 0004;
                    mode |= 0040;
                    mode |= 0400;
                }
                if (f.isWritable()) {
                    mode |= 0002;
                    mode |= 0020;
                    mode |= 0200;
                }
                if (f.isExecutable()) {
                    mode |= 0001;
                    mode |= 0010;
                    mode |= 0100;
                }
            }
            try {
                size = f.size();
            } catch (IOException e) {
                size = 0;
            }
            return factory().createTuple(new Object[]{
                            mode,
                            0, // ino
                            0, // dev
                            0, // nlink
                            uid,
                            gid,
                            size,
                            atime,
                            mtime,
                            ctime,
            });
        }
    }

    @Builtin(name = "listdir", fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class ListdirNode extends PythonBuiltinNode {
        @Specialization
        @TruffleBoundary
        Object listdir(String path) {
            try {
                TruffleFile file = getContext().getEnv().getTruffleFile(path);
                Collection<TruffleFile> listFiles = file.list();
                if (listFiles.isEmpty()) {
                    throw raise(OSError, path);
                }
                Object[] filenames = new Object[listFiles.size()];
                int i = 0;
                for (TruffleFile f : listFiles) {
                    filenames[i] = f.getName();
                    i += 1;
                }
                return factory().createList(filenames);
            } catch (IOException e) {
                CompilerDirectives.transferToInterpreter();
                throw raise(OSError, path);
            }
        }
    }

    @Builtin(name = "dup", fixedNumOfArguments = 1)
    @GenerateNodeFactory
    abstract static class DupNode extends PythonFileNode {
        @Specialization
        @TruffleBoundary
        int dup(int fd) {
            return dupFile(fd);
        }
    }

    @Builtin(name = "open", minNumOfArguments = 2, maxNumOfArguments = 4, keywordArguments = {"mode", "dir_fd"})
    @GenerateNodeFactory
    public abstract static class OpenNode extends PythonFileNode {
        @Specialization(guards = {"isNoValue(mode)", "isNoValue(dir_fd)"})
        Object open(String pathname, int flags, @SuppressWarnings("unused") PNone mode, PNone dir_fd) {
            return open(pathname, flags, 0777, dir_fd);
        }

        @Specialization(guards = {"isNoValue(dir_fd)"})
        @TruffleBoundary
        Object open(String pathname, int flags, int fileMode, @SuppressWarnings("unused") PNone dir_fd) {
            Set<StandardOpenOption> options = new HashSet<>();
            if ((flags & WRONLY) != 0) {
                options.add(StandardOpenOption.WRITE);
            } else if ((flags & RDWR) != 0) {
                options.add(StandardOpenOption.READ);
                options.add(StandardOpenOption.WRITE);
            } else {
                options.add(StandardOpenOption.READ);
            }
            if ((flags & CREAT) != 0) {
                options.add(StandardOpenOption.WRITE);
                options.add(StandardOpenOption.CREATE);
            }
            if ((flags & EXCL) != 0) {
                options.add(StandardOpenOption.WRITE);
                options.add(StandardOpenOption.CREATE_NEW);
            }
            if ((flags & APPEND) != 0) {
                options.add(StandardOpenOption.WRITE);
                options.add(StandardOpenOption.APPEND);
            }
            if ((flags & NDELAY) != 0 || (flags & DIRECT) != 0) {
                options.add(StandardOpenOption.DSYNC);
            }
            if ((flags & SYNC) != 0) {
                options.add(StandardOpenOption.SYNC);
            }
            if ((flags & TRUNC) != 0) {
                options.add(StandardOpenOption.WRITE);
                options.add(StandardOpenOption.TRUNCATE_EXISTING);
            }
            if ((flags & TMPFILE) != 0) {
                options.add(StandardOpenOption.DELETE_ON_CLOSE);
            }
            FileAttribute<Set<PosixFilePermission>> fa1 = PosixFilePermissions.asFileAttribute(new HashSet<>(Arrays.asList(otherBitsToPermission[fileMode & 7])));
            FileAttribute<Set<PosixFilePermission>> fa2 = PosixFilePermissions.asFileAttribute(new HashSet<>(Arrays.asList(groupBitsToPermission[fileMode >> 3 & 7])));
            FileAttribute<Set<PosixFilePermission>> fa3 = PosixFilePermissions.asFileAttribute(new HashSet<>(Arrays.asList(ownerBitsToPermission[fileMode >> 6 & 7])));
            TruffleFile truffleFile = getContext().getEnv().getTruffleFile(pathname);
            try {
                SeekableByteChannel fc = truffleFile.newByteChannel(options, fa1, fa2, fa3);
                return addFile(truffleFile, fc);
            } catch (IOException e) {
                throw raise(OSError, e.getMessage());
            }
        }
    }

    @Builtin(name = "lseek", fixedNumOfArguments = 3)
    @GenerateNodeFactory
    public abstract static class LseekNode extends PythonFileNode {
        @Specialization
        @TruffleBoundary
        Object lseek(int fd, int pos, int how) {
            SeekableByteChannel fc = getFileChannel(fd);
            if (fc == null) {
                throw raise(OSError, "Illegal seek");
            }
            try {
                switch (how) {
                    case SEEK_CUR:
                        fc.position(fc.position() + pos);
                        break;
                    case SEEK_END:
                        fc.position(fc.size() + pos);
                        break;
                    case SEEK_SET:
                    default:
                        fc.position(pos);
                }
                return fc.position();
            } catch (IOException e) {
                throw raise(OSError, e.getMessage());
            }
        }
    }

    @Builtin(name = "close", fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class CloseNode extends PythonFileNode {
        @Specialization
        @TruffleBoundary
        Object close(int fd) {
            try {
                getFileChannel(fd).close();
            } catch (IOException e) {
                raise(OSError, e.getMessage());
            } finally {
                removeFile(fd);
            }
            return PNone.NONE;
        }
    }

    @Builtin(name = "unlink", fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class UnlinkNode extends PythonFileNode {
        @Specialization
        @TruffleBoundary
        Object unlink(String path) {
            try {
                getContext().getEnv().getTruffleFile(path).delete();
            } catch (RuntimeException | IOException e) {
                throw raise(OSError, e.getMessage());
            }
            return PNone.NONE;
        }
    }

    @Builtin(name = "remove", fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class RemoveNode extends UnlinkNode {
    }

    @Builtin(name = "rmdir", fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class RmdirNode extends UnlinkNode {
    }

    @Builtin(name = "mkdir", fixedNumOfArguments = 1, keywordArguments = {"mode", "dir_fd"})
    @GenerateNodeFactory
    public abstract static class MkdirNode extends PythonFileNode {
        @Specialization
        Object mkdir(String path, @SuppressWarnings("unused") PNone mode, PNone dirFd) {
            return mkdir(path, 511, dirFd);
        }

        @Specialization
        @TruffleBoundary
        Object mkdir(String path, @SuppressWarnings("unused") int mode, @SuppressWarnings("unused") PNone dirFd) {
            try {
                getContext().getEnv().getTruffleFile(path).createDirectory();
            } catch (RuntimeException | IOException e) {
                throw raise(OSError, e.getMessage());
            }
            return PNone.NONE;
        }
    }

    @Builtin(name = "write", fixedNumOfArguments = 2)
    @GenerateNodeFactory
    public abstract static class WriteNode extends PythonFileNode {
        @Specialization(guards = {"fd <= 2", "fd > 0"})
        @TruffleBoundary
        Object writeStd(int fd, byte[] data) {
            try {
                switch (fd) {
                    case 1:
                        getContext().getStandardOut().write(data);
                        break;
                    case 2:
                        getContext().getStandardErr().write(data);
                        break;
                }
            } catch (IOException e) {
                throw raise(OSError, e.getMessage());
            }
            return PNone.NONE;
        }

        @Specialization(guards = "fd == 0 || fd > 2")
        @TruffleBoundary
        Object write(int fd, byte[] data) {
            try {
                return getFileChannel(fd).write(ByteBuffer.wrap(data));
            } catch (NonWritableChannelException | IOException e) {
                throw raise(OSError, e.getMessage());
            }
        }

        @Specialization(guards = "fd == 0 || fd > 2")
        @TruffleBoundary
        Object write(int fd, String data) {
            return write(fd, data.getBytes());
        }

        @Specialization(guards = {"fd <= 2", "fd > 0"})
        @TruffleBoundary
        Object writeStd(int fd, String data) {
            return writeStd(fd, data.getBytes());
        }

        @Specialization(guards = "fd == 0 || fd > 2")
        Object write(int fd, PString data) {
            return write(fd, data.getValue());
        }

        @Specialization(guards = {"fd <= 2", "fd > 0"})
        Object writeStd(int fd, PString data) {
            return writeStd(fd, data.getValue());
        }

        @Specialization(guards = "fd == 0 || fd > 2")
        @TruffleBoundary
        Object write(int fd, PBytes data) {
            return write(fd, data.getInternalByteArray());
        }

        @Specialization(guards = {"fd <= 2", "fd > 0"})
        @TruffleBoundary
        Object writeStd(int fd, PBytes data) {
            return writeStd(fd, data.getInternalByteArray());
        }

        @Specialization(guards = "fd == 0 || fd > 2")
        @TruffleBoundary
        Object write(int fd, PByteArray data) {
            return write(fd, data.getInternalByteArray());
        }

        @Specialization(guards = {"fd <= 2", "fd > 0"})
        @TruffleBoundary
        Object writeStd(int fd, PByteArray data) {
            return writeStd(fd, data.getInternalByteArray());
        }
    }

    @Builtin(name = "read", fixedNumOfArguments = 2)
    @GenerateNodeFactory
    public abstract static class ReadNode extends PythonFileNode {
        @Specialization
        @TruffleBoundary
        Object read(int fd, int requestedSize) {
            SeekableByteChannel channel = getFileChannel(fd);
            try {
                long size = Math.min(requestedSize, channel.size() - channel.position());
                // cast below will always succeed, since requestedSize was an int,
                // and must thus will always be smaller than a long that cannot be
                // downcast
                ByteBuffer dst = ByteBuffer.allocate((int) size);
                getFileChannel(fd).read(dst);
                return factory().createBytes(dst.array());
            } catch (IOException e) {
                throw raise(OSError, e.getMessage());
            }
        }
    }

    @Builtin(name = "isatty", fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class IsATTYNode extends PythonBuiltinNode {
        @Specialization
        boolean isATTY(int fd) {
            // TODO: XXX: actually check
            switch (fd) {
                case 1:
                    return getContext().getStandardOut() == System.out;
                case 2:
                    return getContext().getStandardErr() == System.err;
                default:
                    return false;
            }
        }
    }

    @Builtin(name = "_exit", fixedNumOfArguments = 1)
    @GenerateNodeFactory
    public abstract static class ExitNode extends PythonBuiltinNode {
        @TruffleBoundary
        @Specialization
        Object exit(int status) {
            throw new PythonExitException(this, status);
        }
    }

    @Builtin(name = "chmod", minNumOfArguments = 2, keywordArguments = {"dir_fd", "follow_symlinks"})
    @GenerateNodeFactory
    abstract static class ChmodNode extends PythonBuiltinNode {
        @Specialization
        @TruffleBoundary
        Object chmod(String path, int mode, @SuppressWarnings("unused") PNone dir_fd, @SuppressWarnings("unused") PNone follow_symlinks) {
            Set<PosixFilePermission> permissions = new HashSet<>(Arrays.asList(otherBitsToPermission[mode & 7]));
            permissions.addAll(Arrays.asList(groupBitsToPermission[mode >> 3 & 7]));
            permissions.addAll(Arrays.asList(ownerBitsToPermission[mode >> 6 & 7]));
            try {
                TruffleFile truffleFile = getContext().getEnv().getTruffleFile(path);
                truffleFile.setPosixPermissions(permissions);
            } catch (IOException e) {
                throw raise(OSError, e.getMessage());
            }
            return PNone.NONE;
        }

        @SuppressWarnings("unused")
        @Fallback
        Object chmod(Object path, Object mode, Object dir_fd, Object follow_symlinks) {
            throw raise(NotImplementedError, "chmod");
        }
    }

    @Builtin(name = "utime", minNumOfArguments = 1, keywordArguments = {"times", "ns", "dir_fd", "follow_symlinks"})
    @GenerateNodeFactory
    abstract static class UtimeNode extends PythonBuiltinNode {
        @SuppressWarnings("unused")
        @Specialization
        Object utime(String path, PNone times, PNone ns, PNone dir_fd, PNone follow_symlinks) {
            long time = ((Double) TimeModuleBuiltins.timeSeconds()).longValue();
            setMtime(path, time);
            setAtime(path, time);
            return PNone.NONE;
        }

        @SuppressWarnings("unused")
        @Specialization
        Object utime(String path, PTuple times, PNone ns, PNone dir_fd, PNone follow_symlinks) {
            long atime = getTime(times, 0, "times");
            long mtime = getTime(times, 1, "times");
            setMtime(path, mtime);
            setAtime(path, atime);
            return PNone.NONE;
        }

        @SuppressWarnings("unused")
        @Specialization
        Object utime(String path, PNone times, PTuple ns, PNone dir_fd, PNone follow_symlinks) {
            long atime = getTime(ns, 0, "ns") / 1000;
            long mtime = getTime(ns, 1, "ns") / 1000;
            setMtime(path, mtime);
            setAtime(path, atime);
            return PNone.NONE;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isPNone(times)", "!isPTuple(times)"})
        Object utimeWrongTimes(String path, Object times, Object ns, Object dir_fd, Object follow_symlinks) {
            throw tupleError("times");
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isPTuple(ns)", "!isPNone(ns)"})
        Object utimeWrongNs(String path, PNone times, Object ns, Object dir_fd, Object follow_symlinks) {
            throw tupleError("ns");
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"!isPNone(ns)"})
        Object utimeWrongNs(String path, PTuple times, Object ns, Object dir_fd, Object follow_symlinks) {
            throw raise(ValueError, "utime: you may specify either 'times' or 'ns' but not both");
        }

        @SuppressWarnings("unused")
        @Fallback
        Object utimeError(Object path, Object times, Object ns, Object dir_fd, Object follow_symlinks) {
            throw raise(NotImplementedError, "utime");
        }

        private long getTime(PTuple times, int index, String argname) {
            if (times.len() <= index) {
                throw tupleError(argname);
            }
            Object mtimeObj = times.getItem(index);
            long mtime;
            if (mtimeObj instanceof Integer) {
                mtime = ((Integer) mtimeObj).longValue();
            } else if (mtimeObj instanceof Long) {
                mtime = ((Long) mtimeObj).longValue();
            } else if (mtimeObj instanceof PInt) {
                mtime = ((PInt) mtimeObj).longValue();
            } else if (mtimeObj instanceof Double) {
                mtime = ((Double) mtimeObj).longValue();
            } else if (mtimeObj instanceof PFloat) {
                mtime = (long) ((PFloat) mtimeObj).getValue();
            } else {
                throw tupleError(argname);
            }
            if (mtime < 0) {
                throw raise(ValueError, "time cannot be negative");
            }
            return mtime;
        }

        private PException tupleError(String argname) {
            return raise(TypeError, "utime: '%s' must be either a tuple of two ints or None", argname);
        }

        @TruffleBoundary
        private void setMtime(String path, long mtime) {
            try {
                getContext().getEnv().getTruffleFile(path).setLastModifiedTime(FileTime.fromMillis(mtime));
            } catch (IOException e) {
            }
        }

        @TruffleBoundary
        private void setAtime(String path, long mtime) {
            try {
                getContext().getEnv().getTruffleFile(path).setLastAccessTime(FileTime.fromMillis(mtime));
            } catch (IOException e) {
            }
        }
    }

    // FIXME: this is not nearly ready, just good enough for now
    @Builtin(name = "system", fixedNumOfArguments = 1)
    @GenerateNodeFactory
    abstract static class SystemNode extends PythonBuiltinNode {
        static final String[] shell = System.getProperty("os.name").toLowerCase().startsWith("windows") ? new String[]{"cmd.exe", "/c"}
                        : new String[]{(System.getenv().getOrDefault("SHELL", "sh")), "-c"};

        class StreamGobbler extends Thread {
            static final int bufsize = 4096;
            InputStream is;
            OutputStream type;
            private char[] buf;

            StreamGobbler(InputStream is, OutputStream outputStream) {
                this.is = is;
                this.type = outputStream;
                this.buf = new char[bufsize];
            }

            @Override
            public void run() {
                try {
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);
                    int readSz = 0;
                    while ((readSz = br.read(buf, 0, bufsize)) > 0) {
                        type.write(new String(buf).getBytes(), 0, readSz);
                    }
                } catch (IOException ioe) {
                }
            }
        }

        @TruffleBoundary
        @Specialization
        int system(String cmd) {
            String[] command = new String[]{shell[0], shell[1], cmd};
            try {
                Runtime rt = Runtime.getRuntime();
                Process proc = rt.exec(command);
                StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream(), getContext().getStandardErr());
                StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream(), getContext().getStandardOut());
                errorGobbler.start();
                outputGobbler.start();
                return proc.waitFor();
            } catch (IOException | InterruptedException e) {
                return -1;
            }
        }
    }
}
