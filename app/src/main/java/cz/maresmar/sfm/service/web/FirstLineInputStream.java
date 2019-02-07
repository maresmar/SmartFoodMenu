/*
 * SmartFoodMenu - Android application for canteens extendable with plugins
 *
 * Copyright © 2016-2018  Martin Mareš <mmrmartin[at]gmail[dot]com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cz.maresmar.sfm.service.web;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;


/**
 * Filters standard input stream. The FirstLineInputStream provides access only to first line
 * of source input stream (ended with `\n`), the rest is is treated as end of the stream.
 */
public class FirstLineInputStream extends InputStream {
    private boolean mFirstLine = true;
    private InputStream mInputStream;

    /**
     * Create new FirstLineInputStream from internal InputStream
     * @param is internal valid InputStream
     */
    public FirstLineInputStream(@NonNull InputStream is) {
        mInputStream = is;
    }

    /**
     * Reads the next byte of data from the input stream. The value byte is
     * returned as an <code>int</code> in the range <code>0</code> to
     * <code>255</code>. If no byte is available because the end of the stream or
     * end of the line has been reached, the value <code>-1</code> is returned.
     * This method blocks until input data is available, the end of the stream is detected,
     * or an exception is thrown.
     *
     * <p> A subclass must provide an implementation of this method.
     *
     * @return     the next byte of data, or <code>-1</code> if the end of the
     *             stream  or end of fist line is reached.
     * @exception  IOException  if an I/O error occurs
     */
    @Override
    public int read() throws IOException {
        if(mFirstLine) {
            int ch = mInputStream.read();
            if(ch != '\n')
                return ch;
            else
                mFirstLine = false;
        }
        return -1;
    }

    /**
     * Closes the internal input stream
     *
     * @exception  IOException  if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        // According to docs close method of InputStream did nothing
        super.close();
        mInputStream.close();
    }
}
