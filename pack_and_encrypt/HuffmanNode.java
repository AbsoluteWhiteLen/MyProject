package pack_and_encrypt;

import java.io.Serializable;

    //霍夫曼树节点
    //value作为byte的�?�，0xffff代表�?
    @SuppressWarnings("serial")
    public class HuffmanNode implements Serializable{
        int value = 0xffff;
        int weight = 0;
        HuffmanNode left = null;
        HuffmanNode right = null;
        HuffmanNode parent = null;
    }