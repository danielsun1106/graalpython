# coding=utf-8
# Copyright (c) 2017, Oracle and/or its affiliates.
# Copyright (c) 2017, The PyPy Project
#
#     The MIT License
# Permission is hereby granted, free of charge, to any person
# obtaining a copy of this software and associated documentation
# files (the "Software"), to deal in the Software without
# restriction, including without limitation the rights to use,
# copy, modify, merge, publish, distribute, sublicense, and/or
# sell copies of the Software, and to permit persons to whom the
# Software is furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included
# in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
# OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
# THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
# FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
# DEALINGS IN THE SOFTWARE.

import sys
_warn = sys.modules["_warnings"]._warn
_os = sys.modules.get("posix", sys.modules.get("nt"))


DEFAULT_BUFFER_SIZE = 8192


class BlockingIOError(OSError):
    pass


class UnsupportedOperation(OSError, ValueError):
    pass


class _IOBase(object):
    def __init__(self):
        self.__IOBase_closed = False

    def __enter__(self):
        self._checkClosed()
        return self

    def __exit__(self, *args):
        self.close()

    def __iter__(self):
        self._checkClosed()
        return self

    def __next__(self):
        line = self.readline()
        if not line:
            raise StopIteration
        return line

    def __getstate__(self):
        raise TypeError("cannot serialize '%s' object" % type(self))

    def close(self):
        if not self.closed:
            try:
                self.flush()
            finally:
                self.__IOBase_closed = True

    def flush(self):
        self._checkClosed()

    def seek(self, offset, whence=None):
        raise UnsupportedOperation("seek")

    def tell(self):
        return self.seek(0, 1)

    def truncate(self):
        raise UnsupportedOperation("truncate")

    def fileno(self):
        raise UnsupportedOperation("fileno")

    def isatty(self):
        self._checkClosed()
        return False

    def readable(self):
        return False

    def writable(self):
        return False

    def seekable(self):
        return False

    def _checkReadable(self):
        if not self.readable():
            raise UnsupportedOperation("File or stream is not readable")

    def _checkWritable(self):
        if not self.writable():
            raise UnsupportedOperation("File or stream is not writable")

    def _checkSeekable(self):
        if not self.seekable():
            raise UnsupportedOperation("File or stream is not seekable")

    def _checkClosed(self):
        if self.closed:
            raise ValueError("I/O operation on closed file")

    @property
    def closed(self):
        return self.__IOBase_closed

    def __del__(self):
        if not self.closed:
            try:
                self._dealloc_warn(self)
                self.close()
            finally:
                # Ignore all errors
                return

    def _dealloc_warn(self, source):
        pass

    def readline(self, limit=-1):
        has_peek = hasattr(self, "peek")
        builder = []
        size = 0
        while limit < 0 or size < limit:
            nreadahead = 1
            if has_peek:
                readahead = self.peek(1)
                if not isinstance(readahead, bytes):
                    raise IOError("peek() should have returned a bytes object, not '%s'", type(readahead))
                length = len(readahead)
                if length > 0:
                    n = 0
                    buf = readahead
                    if limit >= 0:
                        while True:
                            if n >= length or n >= limit:
                                break
                            n += 1
                            if buf[n-1] == '\n':
                                break
                    else:
                        while True:
                            if n >= length:
                                break
                            n += 1
                            if buf[n-1] == '\n':
                                break
                    nreadahead = n
            read = self.read(nreadahead)
            if not isinstance(read, bytes):
                raise IOError("read() should have returned a bytes object, not '%s'" % type(read))
            if not read:
                break

            size += len(read)
            builder.append(read)

            if read[-1] == b'\n'[0]:
                break

        return b"".join(builder)

    def readlines(self, hint=-1):
        if hint <= 0:
            return [line for line in self]
        lines = []
        length = 0
        while True:
            line = self.readline()
            line_length = len(line)
            if line_length == 0:
                break

            lines.append(line)

            length += line_length
            if length > hint:
                break

        return lines

    def writelines(self, lines):
        self._checkClosed()
        for line in lines:
            self.write(line)


class _RawIOBase(_IOBase):
    def read(self, size=-1):
        if size < 0:
            return self.readall()

        buf = bytearray(size)
        length = self.readinto(buf)
        if length is None:
            return length
        del buf[length:-1]
        return bytes(buf)

    def readall(self):
        builder = []
        while True:
            data = self.read(DEFAULT_BUFFER_SIZE)
            if data is None:
                if not builder:
                    return data
                break
            if not isinstance(data, str):
                raise TypeError("read() should return bytes")
            if not data:
                break
            builder.append(data)
        return b"".join(builder)




class FileIO(_RawIOBase):
    @staticmethod
    def __isdir__(mode):
        # We cannot import the stat module here
        return (mode & 0o170000) == 0o040000

    @staticmethod
    def __decode_mode__(mode):
        O_BINARY = getattr(_os, "O_BINARY", 0)
        O_APPEND = getattr(_os, "O_APPEND", 0)

        _bad_mode = ValueError("Must have exactly one of read/write/create/append mode")

        flags = 0
        rwa = False
        readable = False
        writable = False
        created = False
        append = False
        plus = False

        for s in mode:
            if s == 'r':
                if rwa:
                    raise _bad_mode
                rwa = True
                readable = True
            elif s == 'w':
                if rwa:
                    raise _bad_mode
                rwa = True
                writable = True
                flags |= _os.O_CREAT | _os.O_TRUNC
            elif s == 'x':
                if rwa:
                    raise _bad_mode
                rwa = True
                created = True
                writable = True
                flags |= _os.O_EXCL | _os.O_CREAT
            elif s == 'a':
                if rwa:
                    raise _bad_mode
                rwa = True
                writable = True
                append = True
                flags |= O_APPEND | _os.O_CREAT
            elif s == 'b':
                pass
            elif s == '+':
                if plus:
                    raise _bad_mode
                readable = writable = True
                plus = True
            else:
                raise ValueError("invalid mode: %s" % mode)

        if not rwa:
            raise _bad_mode

        if readable and writable:
            flags |= _os.O_RDWR
        elif readable:
            flags |= _os.O_RDONLY
        else:
            flags |= _os.O_WRONLY

        flags |= O_BINARY

        return readable, writable, created, append, flags

    SMALLCHUNK = 8 * 1024
    BIGCHUNK = 512 * 1024

    def __init__(self, name, mode='r', closefd=True, opener=None):
        _RawIOBase.__init__(self)
        self.__fd__ = -1
        self.__readable__ = False
        self.__writable__ = False
        self.__created__ = False
        self.__appending__ = False
        self.__seekable__ = -1
        self.__closefd__ = True
        self.name = None

        if self.__fd__ >= 0:
            if self.__closefd__:
                self.close()
            else:
                self.__fd__ = -1

        if isinstance(name, float):
            raise TypeError("integer argument expected, got float")

        fd = -1
        try:
            fd = int(name)
        except:
            pass
        else:
            if fd < 0:
                raise ValueError("negative file descriptor")

        self.__readable__, self.__writable__, self.__created__, self.__appending__, flags = FileIO.__decode_mode__(mode)

        fd_is_own = False
        try:
            if fd >= 0:
                self.__fd__ = fd
                self.__closefd__ = bool(closefd)
            else:
                self.__closefd__ = True
                if not closefd:
                    raise ValueError("Cannot use closefd=False with file name")

                if opener is None:
                    self.__fd__ = _os.open(name, flags, 0o666)
                    fd_is_own = True
                else:
                    fd = getattr(opener, name)(flags)
                    try:
                        self.__fd__ = int(fd)
                        if self.__fd__ < 0:
                            # The opener returned a negative result instead
                            # of raising an exception
                            raise ValueError("opener returned %d" % self.__fd__)
                        fd_is_own = True
                    except TypeError:
                        raise TypeError("expected integer from opener")
            st = _os.fstat(self.__fd__)
            # On Unix, fopen will succeed for directories.
            # In Python, there should be no file objects referring to
            # directories, so we need a check.
            if FileIO.__isdir__(st.st_mode):
                raise OSError(21) # EISDIR
            self.__blksize__ = DEFAULT_BUFFER_SIZE
            self.name = name
            if self.__appending__:
                # For consistent behaviour, we explicitly seek to the end of file
                # (otherwise, it might be done only on the first write()).
                _os.lseek(self.__fd__, 0, _os.SEEK_END)
        except:
            if not fd_is_own:
                self.__fd__ = -1
            raise

    @property
    def closefd(self):
        return self.__closefd__

    @property
    def mode(self):
        if self.__created__:
            if self.__readable__:
                return 'xb+'
            else:
                return 'xb'
        if self.__appending__:
            if self.__readable__:
                return 'ab+'
            else:
                return 'ab'
        elif self.__readable__:
            if self.__writable__:
                return 'rb+'
            else:
                return 'rb'
        else:
            return 'wb'

    @property
    def blksize(self):
        return self.__blksize__

    def _checkClosed(self, message=None):
        if message is None:
            message = "I/O operation on closed file"
        if self.__fd__ < 0:
            raise ValueError(message)

    def _checkReadable(self):
        if not self.readable():
            raise UnsupportedOperation("File not open for reading")

    def _checkWritable(self):
        if not self.writable():
            raise UnsupportedOperation("File not open for writing")

    def close(self):
        got_e = None
        try:
            _RawIOBase.close(self)
        except Exception as e:
            got_e = e
        if not self.__closefd__:
            self.__fd__ = -1
        else:
            if self.__fd__ >= 0:
                fd = self.__fd__
                self.__fd__ = -1
                _os.close(fd)
            if got_e:
                raise got_e

    def _dealloc_warn(self, source):
        if self.__fd__ >= 0 and self.closefd:
            _warn("unclosed file %s" % repr(source), ResourceWarning)

    def seek(self, pos, whence=0):
        self._checkClosed()
        return _os.lseek(self.__fd__, pos, whence)

    def tell(self):
        self._checkClosed()
        return _os.lseek(self.__fd__, 0, 1)

    def readable(self):
        self._checkClosed()
        return self.__readable__

    def writable(self):
        self._checkClosed()
        return self.__writable__

    def seekable(self):
        self._checkClosed()
        if self.__seekable__ < 0:
            try:
                _os.lseek(self.__fd__, 0, _os.SEEK_CUR)
            except OSError:
                self.__seekable__ = 0
            else:
                self.__seekable__ = 1
        return self.__seekable__ == 1

    # ______________________________________________

    def fileno(self):
        self._checkClosed()
        return self.__fd__

    def isatty(self):
        self._checkClosed()
        return _os.isatty(self.__fd__)

    def __repr__(self):
        if self.__fd__ < 0:
            return "<_io.FileIO [closed]>"
        closefd = "True" if self.__closefd__ else "False"
        if self.name is None:
            return "<_io.FileIO fd=%d mode='%s' closefd=%s>" % (
                self.__fd__, self.mode, closefd
            )
        else:
            return "<_io.FileIO name=%s mode='%s' closefd=%s>" % (
                    repr(self.name), self.mode, closefd
            )

    # ______________________________________________

    def write(self, data):
        self._checkClosed()
        self._checkWritable()
        return _os.write(self.__fd__, data)

    def read(self, size=-1):
        self._checkClosed()
        self._checkReadable()
        if size < 0:
            return self.readall()
        return _os.read(self.__fd__, size)

    def readinto(self, rwbuffer):
        self._checkClosed()
        self._checkReadable()
        length = len(rwbuffer)
        buf = _os.read(self.__fd__, length)
        rwbuffer[:] = buf
        return len(buf)

    def readall(self):
        self._checkClosed()
        self._checkReadable()
        total = 0
        builder = []
        while True:
            newsize = FileIO.BIGCHUNK
            chunk = _os.read(self.__fd__, newsize - total)
            if len(chunk) == 0:
                break
            builder.append(chunk)
            total += len(chunk)
        return b"".join(builder)

    if sys.platform == "win32":
        def truncate(self, size):
            self._checkClosed()
            self._checkWritable()
            if size < 0:
                size = self.tell()
            raise NotImplementedError("truncate on win32")
    else:
        def truncate(self, size=-1):
            self._checkClosed()
            self._checkWritable()
            if size < 0:
                size = self.tell()
            _os.ftruncate(self.__fd__, size)
            return size


sys.stdout = FileIO(1, mode='w', closefd=False)
sys.__stdout__ = sys.stdout
sys.stderr = FileIO(2, mode='w', closefd=False)
sys.__stderr__ = sys.stderr


class _BufferedIOBase(_IOBase):
    """Base class for buffered IO objects.

The main difference with RawIOBase is that the read() method
supports omitting the size argument, and does not have a default
implementation that defers to readinto().

In addition, read(), readinto() and write() may raise
BlockingIOError if the underlying raw stream is in non-blocking
mode and not ready; unlike their raw counterparts, they will never
return None.

A typical implementation should not inherit from a RawIOBase
implementation, but wrap one.
"""

    def _check_init(self):
        raise NotImplementedError

    def read(self, size=None):
        """Read and return up to n bytes.

If the argument is omitted, None, or negative, reads and
returns all data until EOF.

If the argument is positive, and the underlying raw stream is
not 'interactive', multiple raw reads may be issued to satisfy
the byte count (unless EOF is reached first).  But for
interactive raw streams (as well as sockets and pipes), at most
one raw read will be issued, and a short result does not imply
that EOF is imminent.

Returns an empty bytes object on EOF.

Returns None if the underlying raw stream was open in non-blocking
mode and no data is available at the moment."""
        raise UnsupportedOperation("read")

    def read1(self, size):
        """Read and return up to n bytes, with at most one read() call
to the underlying raw stream. A short result does not imply
that EOF is imminent.

Returns an empty bytes object on EOF."""
        raise UnsupportedOperation("read1")

    def write(self, data):
        """Write the given buffer to the IO stream.

Returns the number of bytes written, which is always the length of b
in bytes.

Raises BlockingIOError if the buffer is full and the
underlying raw stream cannot accept more data at the moment."""
        raise UnsupportedOperation("write")

    def detach(self):
        """Disconnect this buffer from its underlying raw stream and return it.

After the raw stream has been detached, the buffer is in an unusable
state."""
        raise UnsupportedOperation("detach")

    def readinto(self, space, buffer):
        length = len(buffer)
        data = self.read(length)
        if not isinstance(data, bytes):
            raise TypeError("read() should return bytes")
        ldata = len(data)
        if ldata > length:
            raise ValueError("read() returned too much data: "
                             "%d bytes requested, %d returned" % (length, ldata))
        buffer[0:ldata] = data
        return ldata

    def readinto1(self, buffer):
        length = len(buffer)
        data = self.read1(length)
        if not isinstance(data, bytes):
            raise TypeError("read1() should return bytes")
        ldata = len(data)
        if ldata > length:
            raise ValueError("read1() returned too much data: "
                             "%d bytes requested, %d returned" % (length, ldata))
        buffer[0:ldata] = data
        return ldata


class BytesIO(_BufferedIOBase):
    def __init__(self, inital_bytes=None):
        if inital_bytes:
            self.write(inital_bytes)
            self.seek(0)


class _TextIOBase(_IOBase):
    pass


class StringIO(_TextIOBase):
    pass


class BufferedReader(_BufferedIOBase):
    pass


class BufferedWriter(_BufferedIOBase):
    pass


class BufferedRWPair(_BufferedIOBase):
    pass


class BufferedRandom(_BufferedIOBase):
    pass


class IncrementalNewlineDecoder(object):
    pass


class TextIOWrapper(_TextIOBase):
    pass


def open(file, mode="r", buffering=-1, encoding=None, errors=None, newline=None, closefd=True, opener=None):
    if not isinstance(file, (str, int, bytes)):
        raise TypeError("invalid file: %r" % file)

    if not isinstance(mode, str):
        raise TypeError("invalid mode: %r" % mode)

    if not isinstance(buffering, int):
        raise TypeError("invalid buffering: %r" % buffering)

    if encoding is not None and not isinstance(encoding, str):
        raise TypeError("invalid encoding: %r" % encoding)

    if errors is not None and not isinstance(errors, str):
        raise TypeError("invalid errors: %r" % errors)

    modes = set(mode)
    if modes - {'x', 'r', 'w', 'a', '+', 't', 'b', 'U'} or len(mode) > len(modes):
        raise ValueError("invalid mode: %r" % mode)

    # decode mode
    creating = "x" in modes
    reading = "r" in modes
    writing = "w" in modes
    appending = "a" in modes
    updating = "+" in modes
    text = "t" in modes
    binary = "b" in modes
    if 'U' in modes:
        if writing or appending:
            raise ValueError("can't use U and writing mode at once")
        universal = True
        reading = True
    else:
        universal = False

    # raw mode
    raw_mode = '%s%s%s%s%s' % (
        'x' if creating else '',
        'r' if reading else '',
        'w' if writing else '',
        'a' if appending else '',
        '+' if updating else ''
    )

    raw_mode = (reading and "r" or "") + (writing and "w" or "") + (appending and "a" or "") + (updating and "+" or "")

    # parameters validation
    if universal:
        if creating or writing or appending or updating:
            raise ValueError("mode U cannot be combined with x', 'w', 'a', or '+'")
        _warn("'U' mode is deprecated", DeprecationWarning)
        reading = True

    if text and binary:
        raise ValueError("can't have text and binary mode at once")

    if creating + reading + writing + appending > 1:
        raise ValueError("must have exactly one of create/read/write/append mode")

    if binary and encoding is not None:
        raise ValueError("binary mode doesn't take an encoding argument")

    if binary and errors is not None:
        raise ValueError("binary mode doesn't take an errors argument")

    if binary and newline is not None:
        raise ValueError("binary mode doesn't take a newline argument")

    # create the raw file stream
    # TODO: if ms windows type: RawIO_class = (PyObject *)&PyWindowsConsoleIO_Type and encoding = "utf-8"

    raw = FileIO(file, mode=raw_mode, closefd=closefd, opener=opener)
    result = raw

    try:
        # buffering
        if buffering == 1 or buffering < 0 and raw.isatty():
            buffering = -1
            line_buffering = True
        else:
            line_buffering = False

        if buffering < 0:
            buffering = DEFAULT_BUFFER_SIZE
            try:
                bs = _os.fstat(raw.fileno()).st_blksize
            except (OSError, AttributeError):
                pass
            else:
                if bs > 1:
                    buffering = bs

        if buffering < 0:
            raise ValueError("invalid buffering size")

        if buffering == 0:
            if binary:
                return result
            raise ValueError("can't have unbuffered text I/O")

        if updating:
            buffer = BufferedRandom(raw, buffering)
        elif creating or writing or appending:
            buffer = BufferedWriter(raw, buffering)
        elif reading:
            buffer = BufferedReader(raw, buffering)
        else:
            raise ValueError("unknown mode: %r" % mode)
        result = buffer

        if binary:
            return result

        text = TextIOWrapper(buffer, encoding, errors, newline, line_buffering)
        result = text

        text.mode = mode
        return result
    except:
        result.close()
        raise


# set the builtins open method
import builtins
setattr(builtins, 'open', open)
