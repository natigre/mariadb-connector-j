/*
 *
 * MariaDB Client for Java
 *
 * Copyright (c) 2012-2014 Monty Program Ab.
 * Copyright (c) 2015-2017 MariaDB Ab.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to Monty Program Ab info@montyprogram.com.
 *
 * This particular MariaDB Client for Java file is work
 * derived from a Drizzle-JDBC. Drizzle-JDBC file which is covered by subject to
 * the following copyright and notice provisions:
 *
 * Copyright (c) 2009-2011, Marcus Eriksson
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of the driver nor the names of its contributors may not be
 * used to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS  AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 */

package org.mariadb.jdbc;

import org.mariadb.jdbc.internal.util.exceptions.ExceptionMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.SQLException;

public class MariaDbClob extends MariaDbBlob implements Clob, NClob, Serializable {
    private static final long serialVersionUID = -2006825230517923067L;

    /**
     * Creates a Clob with content.
     *
     * @param bytes the content for the Clob.
     */
    public MariaDbClob(byte[] bytes) {
        super(bytes);
    }

    /**
     * Creates a Clob with content.
     *
     * @param bytes     the content for the Clob.
     * @param offset    offset
     * @param length    length
     */
    public MariaDbClob(byte[] bytes, int offset, int length) {
        super(bytes, offset, length);
    }

    /**
     * Creates an empty Clob.
     */
    public MariaDbClob() {
        super();
    }

    /**
     * ToString implementation.
     *
     * @return string value of blob content.
     */
    public String toString() {
        try {
            return new String(data, offset, length, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Get sub string.
     *
     * @param pos    position
     * @param length length of sub string
     * @return substring
     * @throws SQLException if pos is less than 1 or length is less than 0
     */
    public String getSubString(long pos, int length) throws SQLException {

        if (pos < 1) {
            throw ExceptionMapper.getSqlException("position must be >= 1");
        }

        if (length < 0) {
            throw ExceptionMapper.getSqlException("length must be > 0");
        }

        try {
            String val = toString();
            return val.substring((int) pos - 1, Math.min((int) pos - 1 + length, val.length()));
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    public Reader getCharacterStream() throws SQLException {
        return new StringReader(toString());
    }

    /**
     * Returns a Reader object that contains a partial Clob value, starting with the character specified by pos, which
     * is length characters in length.
     *
     * @param pos    the offset to the first character of the partial value to be retrieved. The first character in the
     *               Clob is at position 1.
     * @param length the length in characters of the partial value to be retrieved.
     * @return Reader through which the partial Clob value can be read.
     * @throws SQLException if pos is less than 1 or if pos is greater than the number of characters in the Clob or
     * if pos + length is greater than the number of characters in the Clob
     */
    public Reader getCharacterStream(long pos, long length) throws SQLException {
        String val = toString();
        if (val.length() < (int) pos - 1 + length) {
            throw ExceptionMapper.getSqlException("pos + length is greater than the number of characters in the Clob");
        }
        String sub = val.substring((int) pos - 1, (int) pos - 1 + (int) length);
        return new StringReader(sub);
    }

    /**
     * Set character stream.
     *
     * @param pos position
     * @return writer
     * @throws SQLException if position is invalid
     */
    public Writer setCharacterStream(long pos) throws SQLException {
        int bytePosition = utf8Position((int) pos - 1);
        OutputStream stream = setBinaryStream(bytePosition + 1);
        return new OutputStreamWriter(stream, StandardCharsets.UTF_8);
    }

    public InputStream getAsciiStream() throws SQLException {
        return getBinaryStream();
    }

    public long position(String searchStr, long start) throws SQLException {
        return toString().indexOf(searchStr, (int) start - 1) + 1;
    }

    public long position(Clob searchStr, long start) throws SQLException {
        return position(searchStr.toString(), start);
    }

    /**
     * Convert character position into byte position in UTF8 byte array.
     *
     * @param charPosition charPosition
     * @return byte position
     */
    private int utf8Position(int charPosition) {
        int pos = offset;
        for (int i = 0; i < charPosition; i++) {
            int byteValue = data[pos] & 0xff;
            if (byteValue < 0x80) {
                pos += 1;
            } else if (byteValue < 0xC2) {
                throw new AssertionError("invalid UTF8");
            } else if (byteValue < 0xE0) {
                pos += 2;
            } else if (byteValue < 0xF0) {
                pos += 3;
            } else if (byteValue < 0xF8) {
                pos += 4;
            } else {
                throw new AssertionError("invalid UTF8");
            }
        }
        return pos;
    }

    /**
     * Set String.
     *
     * @param pos position
     * @param str string
     * @return string length
     * @throws SQLException if UTF-8 conversion failed
     */
    public int setString(long pos, String str) throws SQLException {
        int bytePosition = utf8Position((int) pos - 1);
        super.setBytes(bytePosition + 1 - offset, str.getBytes(StandardCharsets.UTF_8));
        return str.length();
    }

    public int setString(long pos, String str, int offset, int len) throws SQLException {
        return setString(pos, str.substring(offset, offset + len));
    }

    public OutputStream setAsciiStream(long pos) throws SQLException {
        return setBinaryStream(utf8Position((int) pos - 1) + 1);
    }

    /**
     * Return character length of the Clob. Assume UTF8 encoding.
     */
    @Override
    public long length() {
        long len = 0;
        for (int i = offset; i < offset + length; ) {
            int byteValue = data[i] & 0xff;
            if (byteValue < 0x80) {
                i += 1;
            } else if (byteValue < 0xC2) {
                throw new AssertionError("invalid UTF8");
            } else if (byteValue < 0xE0) {
                i += 2;
            } else if (byteValue < 0xF0) {
                i += 3;
            } else if (byteValue < 0xF8) {
                i += 4;
            } else {
                throw new AssertionError("invalid UTF8");
            }
            len++;
        }
        return len;
    }

    @Override
    public void truncate(final long len) throws SQLException {
        int pos = offset;
        for (; pos < offset + Math.min(length, len); ) {
            int byteValue = data[pos] & 0xff;
            if (byteValue < 0x80) {
                pos += 1;
            } else if (byteValue < 0xC2) {
                throw new AssertionError("invalid UTF8");
            } else if (byteValue < 0xE0) {
                pos += 2;
            } else if (byteValue < 0xF0) {
                pos += 3;
            } else if (byteValue < 0xF8) {
                pos += 4;
            } else {
                throw new AssertionError("invalid UTF8");
            }
        }
        length = pos - offset;
    }
}
