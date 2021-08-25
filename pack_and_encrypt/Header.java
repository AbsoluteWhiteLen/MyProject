package pack_and_encrypt;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Header implements Serializable{
    String fileName;
    long offset = 0;
    long length = 0;
    public Header(String fn,long os,long len){
        fileName = fn;
        offset = os;
        length = len;
    }
}
