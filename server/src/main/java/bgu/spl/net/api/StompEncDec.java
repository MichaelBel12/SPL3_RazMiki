package bgu.spl.net.api;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class StompEncDec implements MessageEncoderDecoder<String> {

    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;

    @Override
    public String decodeNextByte(byte nextByte) {
        if (nextByte == '\u0000') {
            return popString();
        }
        pushByte(nextByte);
        return null; //not a full message yet
    }

    @Override
    public byte[] encode(String message) {
        return (message+"\n\u0000").getBytes(); //uses utf8 by default
        
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }
        bytes[len++] = nextByte;
    }

    private String popString() {
        int actualLen = len;
        if (actualLen > 0 && bytes[actualLen - 1] == '\n') {
            actualLen--; 
        }
        String result = new String(bytes, 0, actualLen, StandardCharsets.UTF_8);
        len = 0; 
        return result;
    }
}
