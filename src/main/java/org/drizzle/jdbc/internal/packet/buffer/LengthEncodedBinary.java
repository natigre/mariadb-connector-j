package org.drizzle.jdbc.internal.packet.buffer;

import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: marcuse
 * Date: Mar 31, 2009
 * Time: 7:11:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class LengthEncodedBinary {
    private final int length;
    private final long value;

    public LengthEncodedBinary(byte[] rawBytes, int start) {
        if(start<rawBytes.length) {
            switch(rawBytes[start]) {
              case (byte)251:
                  this.length=1;
                  this.value=-1;
                  break;
              case (byte)252:
                  value = ReadUtil.readShort(rawBytes, start+1);
                  this.length=3;
                  break;
              case (byte)253:
                  value = ReadUtil.read24bitword(rawBytes, start+1);
                  this.length=4;
                  break;
              case (byte)254:
                  value = ReadUtil.readLong(rawBytes, start+1);
                  this.length=9;
                  break;
              default:
                  value=rawBytes[start];
                  this.length=1;
            }
        }
        else {
            length=0;
            value=0;
        }
    }

    public int getLength() {
        return length;
    }

    public long getValue() {
        return value;
    }
}