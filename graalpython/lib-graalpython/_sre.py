# Copyright (c) 2018, Oracle and/or its affiliates.
#
# The Universal Permissive License (UPL), Version 1.0
#
# Subject to the condition set forth below, permission is hereby granted to any
# person obtaining a copy of this software, associated documentation and/or data
# (collectively the "Software"), free of charge and under any and all copyright
# rights in the Software, and any and all patent rights owned or freely
# licensable by each licensor hereunder covering either (i) the unmodified
# Software as contributed to or provided by such licensor, or (ii) the Larger
# Works (as defined below), to deal in both
#
# (a) the Software, and
# (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
#     one is included with the Software (each a "Larger Work" to which the
#     Software is contributed by such licensors),
#
# without restriction, including without limitation the rights to copy, create
# derivative works of, display, perform, and distribute the Software and make,
# use, sell, offer for sale, import, export, have made, and have sold the
# Software and the Larger Work(s), and to sublicense the foregoing rights on
# either these or other terms.
#
# This license is subject to the following condition:
#
# The above copyright notice and either this complete permission notice or at a
# minimum a reference to the UPL must be included in all copies or substantial
# portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.

import polyglot as _interop
try:
    TREGEX_ENGINE = _interop.eval(string="", language="regex")()
except NotImplementedError as e:
    def TREGEX_ENGINE(*args): raise e

CODESIZE = 4

MAGIC = 20140917
MAXREPEAT = 4294967295
MAXGROUPS = 2147483647
FLAG_NAMES = ["re.TEMPLATE", "re.IGNORECASE", "re.LOCALE", "re.MULTILINE",
              "re.DOTALL", "re.UNICODE", "re.VERBOSE", "re.DEBUG",
              "re.ASCII"]

_FLAGS_TO_JS = ["", "i", "", "m",
                 "s", "u", "", "",
                 ""]


class SRE_Match():
    def __init__(self, pattern, pos, endpos, result):
        self.result = result
        self.re = pattern
        self.pos = pos
        self.endpos = endpos

    def end(self, groupnum=0):
        return self.result.end[groupnum]

    def group(self, *args):
        if not args:
            return self.result.input[self.result.start[0]:self.result.end[0]]
        elif len(args) == 1:
            return self.result.input[self.result.start[args[0]]:self.result.end[args[0]]]
        else:
            lst = []
            for arg in args:
                lst.append(self.result.input[self.result.start[arg]:self.result.end[arg]])
            return tuple(lst)

    def groups(self, default=None):
        lst = []
        for arg in range(1, self.result.groupCount):
            lst.append(self.result.input[self.result.start[arg]:self.result.end[arg]])
        return tuple(lst)

    def span(self, groupnum=0):
        return (self.result.start[groupnum], self.result.end[groupnum])

    def start(self, groupnum=0):
        return self.result.start[groupnum]

    @property
    def string(self):
        return self.result.input

    @property
    def lastgroup(self):
        return self.result.groupCount

    @property
    def lastindex(self):
        return self.result.end[0]


class SRE_Pattern():
    def __init__(self, pattern, flags, code, groups=0, groupindex=None, indexgroup=None):
        self.pattern = self._decode_string(pattern)
        self.flags = flags
        self.code = code
        self.num_groups = groups
        self.groupindex = groupindex
        self.indexgroup = indexgroup
        jsflags = []
        for i,jsflag in enumerate(_FLAGS_TO_JS):
            if flags & (1 << i):
                jsflags.append(jsflag)
        self.jsflags = "".join(jsflags)

    def _decode_string(self, string):
        if isinstance(string, str):
            pattern = string
        elif isinstance(string, bytes):
            pattern = string.decode()
        else:
            raise TypeError("invalid search pattern {!r}".format(string))
        # TODO: fix this in the regex engine
        return pattern.replace(r'\"', '"').replace(r"\'", "'")

    def __repr__(self):
        flags = self.flags
        for i,name in enumerate(FLAG_NAMES):
            if flags & (1 << i):
                flags -= (1 << i)
                flag_items.append(name)
        if flags != 0:
            flag_items.append("0x%x" % flags)
        if len(flag_items) == 0:
            sep = ""
            sflags = ""
        else:
            sep = ", "
            sflags = "|".join(flag_items)
        return "re.compile(%s%s%s)" % (self.pattern, sep, sflags)

    def _search(self, pattern, string, pos, endpos):
        pattern = TREGEX_ENGINE(self.pattern, self.jsflags)
        string = self._decode_string(string)
        if endpos == -1 or endpos >= len(string):
            result = pattern.exec(string, pos)
        else:
            result = pattern.exec(string[:endpos], pos)
        if result.isMatch:
            return SRE_Match(self, pos, endpos, result)
        else:
            return None

    def search(self, string, pos=0, endpos=-1):
        return self._search(self.pattern, string, pos, endpos)

    def match(self, string, pos=0, endpos=-1):
        if not self.pattern.startswith("^"):
            return self._search("^" + self.pattern, string, pos, endpos)
        else:
            return self._search(self.pattern, string, pos, endpos)

    def fullmatch(self, string, pos=0, endpos=-1):
        pattern = self.pattern
        if not pattern.startswith("^"):
            pattern = "^" + pattern
        if not pattern.endswith("$"):
            pattern = pattern + "$"
        return self._search(pattern, string, pos, endpos)

    def findall(self, string, pos=0, endpos=-1):
        pattern = TREGEX_ENGINE(self.pattern, self.jsflags)
        string = self._decode_string(string)
        if endpos > len(string):
            endpos = len(string)
        elif endpos < 0:
            endpos = endpos % len(string) + 1
        matchlist = []
        while pos < endpos:
            result = pattern.exec(string, pos)
            if not result.isMatch:
                break
            elif self.num_groups == 0:
                matchlist.append("")
            elif self.num_groups == 1:
                matchlist.append(string[result.start[1]:result.end[1]])
            else:
                matchlist.append(SRE_Match(self, pos, endpos, result).groups())
            no_progress = (result.start[0] == result.end[0])
            pos = result.end[0] + no_progress
        return matchlist

    def sub(self, repl, string, count=0):
        n = 0
        pattern = TREGEX_ENGINE(self.pattern, self.jsflags)
        string = self._decode_string(string)
        result = []
        pos = 0
        while count == 0 or n < count:
            match = pattern.exec(string, pos)
            if not match.isMatch:
                break
            n += 1
            start = match.start[0]
            end = match.end[0]
            result.append(string[pos:start])
            if isinstance(repl, str):
                # TODO: backslash replace groups
                result.append(repl)
            else:
                result.append(repl(SRE_Match(self, pos, -1, match)))
            no_progress = (start == end)
            pos = end + no_progress
        result.append(string[pos:])
        return "".join(result)


compile = SRE_Pattern


def getcodesize(*args, **kwargs):
    raise NotImplementedError("_sre.getcodesize is not yet implemented")


def getlower(char_ord, flags):
    return ord(chr(char_ord).lower())
