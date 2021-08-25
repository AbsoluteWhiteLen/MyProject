package pack_and_encrypt;


//bit操作数据
final public class BitOperatableData implements Cloneable{

    //存储数据的byte数组，主�?
    public byte[] data;

    //�?后一个有效字节的位置
    public int lastBytePos;

    //�?后一个字节的有效bit位置
    public int lastByteValidBit;

    //是否满载
    public boolean isFull;

    //�?写入bit数超过剩余空间，超出的部分字节长�?
    public final int overWriteBitLen = 1024;

    //用于存放操作之后超出的部分bit的变�?
    public BitOperatableData overWriteBit;

    //指向下一个读入数据的的byte序号
    public int pByte;

    //指向下一个读入数据的的bit序号
    public int pBit;

    private BitOperatableData(int size){
        data = new byte[size];
        dataClear();
    }

    private BitOperatableData(byte[] aData){
        //copy�?个aData副本
        int len = aData.length;
        data = new byte[len];
        System.arraycopy(aData, 0, data, 0, len);

        isFull = true;
        lastBytePos = len - 1;
        lastByteValidBit = 7;
        overWriteBit = null;
    }

    //获取实例新建
    static public final BitOperatableData getInstance(int size){
        return new BitOperatableData(size);
    }

    //获取实例通过�?个byte[]参数
    static public final BitOperatableData getInstance(byte[] aData){
        return new BitOperatableData(aData);
    }

    //向末尾写入一个byte中的几位bit
    public int writeBitOfByte(byte b,int s,int e){
        if(isFull)
            return -1;
        int isFullFlg = 0;
        int restBitLen = getRestBitNum();
        for(int i = s;i <= e;i++){
            if(Common.getByteBitOnPos(b, i) == 1){
                if((isFullFlg = writeBitOne()) == 1){
                    break;
                }
            }
            else{
                if((isFullFlg = writeBitZero()) == 1){
                    break;
                }
            }
        }
        if(isFullFlg == 1){
            overWriteBit = BitOperatableData.getInstance(overWriteBitLen);
            int voerBitLen = e - s + 1 - restBitLen;
            overWriteBit.writeBitOfByte(b,e - s + 1 - voerBitLen, e);
            return 1;
        }
        return 0;
    }

    //向末尾写入一个byte中的几位，从s位开始到结束
    public int writeBitOfByte(byte b,int s){
        if(isFull)
            return -1;
        int isFullFlg = 0;
        int restBitLen = getRestBitNum();
        for(int i = s;i <= 7;i++){
            if(Common.getByteBitOnPos(b, i) == 1){
                if((isFullFlg = writeBitOne()) == 1){
                    break;
                }
            }
            else{
                if((isFullFlg = writeBitZero()) == 1){
                    break;
                }
            }
        }
        if(isFullFlg == 1){
            overWriteBit = BitOperatableData.getInstance(overWriteBitLen);
            int voerBitLen = 8 - s - restBitLen;
            overWriteBit.writeBitOfByte(b,8 - voerBitLen);
            return 1;
        }
        return 0;
    }

    //写一个bit�?1
    public int writeBitOne(){
        if(isFull)
            return 1;
        byte temp;
        if(this.moveValidPoint(1)){
            temp = (byte)(1 << (7 - lastByteValidBit));
            data[lastBytePos] = (byte)(data[lastBytePos] | temp);
        }
        if(lastByteValidBit == 7 && lastBytePos == data.length - 1){
            isFull = true;
        }
        return 0;
    }

    //写一个bit�?0
    public int writeBitZero(){
        if(isFull)
            return 1;
        if(this.moveValidPoint(1)){
            if(lastByteValidBit == 7 && lastBytePos == data.length - 1){
                isFull = true;
            }
            return 0;
        }
        else
            return 1;
    }

    //定义加法
    public int plus(BitOperatableData tBod) throws CloneNotSupportedException{
        if(isFull){
            if(overWriteBit == null){
                overWriteBit = BitOperatableData.getInstance(overWriteBitLen);
            }
            overWriteBit.plus(tBod);
            return 1;
        }
        if(tBod.lastByteValidBit == -1)
            return 0;
        BitOperatableData bod;
        if(tBod == this){
            //深拷�?
            bod = (BitOperatableData)tBod.clone();
        }
        else{
            bod = tBod;
        }
        int rIsFull = 0;
        for(int i = 0;i < bod.lastBytePos;i++){
            if(rIsFull == 0){
                rIsFull = writeBitOfByte(bod.data[i],0);
            }
            else{
                if(overWriteBit.writeBitOfByte(bod.data[i],0) == 1)
                    return -1;
            }
        }
        if(rIsFull == 0){
            writeBitOfByte(bod.data[bod.lastBytePos],0, bod.lastByteValidBit);
        }
        else{
            isFull = true;
            if(overWriteBit.writeBitOfByte(bod.data[bod.lastBytePos],0,bod.lastByteValidBit) == 1)
                return -1;
            return 1;
        }
        return 0;
    }

    //定位当前read位置
    public boolean seek(int n,int i){
        pByte = n;
        pBit = i;
        return !this.isPosOver(n, i);
    }

    //将read位置(n,i)移动l的bit数，正向后，负向�?
    public boolean moveReadPoint(int len){

        if(pBit + len >= 0){
            pByte += (int)((pBit + len) / 8);
            pBit = pBit + len - (int)((pBit + len) / 8) * 8;
        }

        else if(pBit + len < 0){
            pByte--;
            pByte -= (int)((-1 - pBit - len) / 8);
            pBit = 8 + pBit + len + (int)((-1 - pBit - len) / 8) * 8;
        }
        if(this.isPosOver(pByte, pBit)){
            return false;
        }
        return true;
    }

    //将bit计数的位�?(n,i)移动l的bit数，正向后，负向�?
    public boolean moveValidPoint(int len){

        if(lastByteValidBit + len >= 0){
            lastBytePos += (int)((lastByteValidBit + len) / 8);
            lastByteValidBit = lastByteValidBit + len - (int)((lastByteValidBit + len) / 8) * 8;
        }

        else if(lastByteValidBit + len < 0){
            lastBytePos--;
            lastBytePos -= (int)((-1 - lastByteValidBit - len) / 8);
            lastByteValidBit = 8 + lastByteValidBit + len + (int)((-1 - lastByteValidBit - len) / 8) * 8;
        }
        if(this.isPosOver(lastBytePos, lastByteValidBit)){
            return false;
        }
        return true;
    }

    //判断(n,i)位置是否超出有效范围
    public boolean isPosOver(int n,int i){
        return (n  * 8 + i + 1 > this.getValidBitNum() || n < 0 || n >= data.length || i < 0 || i > 7);
    }

    //从序号为n的字节的序号为i的bit�?始，向后读取m个字�?
    public BitOperatableData read(int n,int i,int m){
        pByte = n;
        pBit = i;

        if(this.isPosOver(pByte,pBit) || this.isPosOver(pByte,pBit + m - 1)){
            return null;
        }

        BitOperatableData res = BitOperatableData.getInstance(m / 8 + 1);
        int bit;
        for(int j = 0;j < m;j++){
            if((bit = this.readBitOnPos()) == -1)
                break;
            if(bit == 1)
                res.writeBitOne();
            else
                res.writeBitZero();
        }
        return res;
    }

    //从当前指向的bit�?始，向后读取m个字�?
    public BitOperatableData read(int m){

        if(this.isPosOver(pByte,pBit) || pByte * 8 + pBit + m > this.getValidBitNum()){
            return null;
        }

        BitOperatableData res = BitOperatableData.getInstance((m - 1) / 8 + 1);
        int bit;
        for(int j = 0;j < m;j++){
            if((bit = this.readBitOnPos()) == -1)
                break;
            if(bit == 1)
                res.writeBitOne();
            else
                res.writeBitZero();
        }
        return res;
    }

    //从序号为n的字节的序号为i的bit�?始，读到data结束位置
    public BitOperatableData read(int n,int i){
        pByte = n;
        pBit = i;

        if(this.isPosOver(pByte,pBit)){
            return null;
        }

        BitOperatableData res = BitOperatableData.getInstance((this.getValidBitNum() - (pByte * 8 + pBit)) / 8 + 1);
        int bit;
        while((bit = this.readBitOnPos()) != -1){
            if(bit == 1)
                res.writeBitOne();
            else
                res.writeBitZero();
        }
        return res;
    }

    //从当前指向的bit�?始，读到data结束位置
    public BitOperatableData read(){

        if(this.isPosOver(pByte,pBit)){
            return null;
        }

        BitOperatableData res = BitOperatableData.getInstance((this.getValidBitNum() - (pByte * 8 + pBit) - 1) / 8 + 1);
        int bit;
        while((bit = this.readBitOnPos()) != -1){
            if(bit == 1)
                res.writeBitOne();
            else
                res.writeBitZero();
        }
        return res;
    }

    //返回序号为n的字节的序号为i的bit位，1或�??0
    public int readBitOnPos(int n,int i){
        pByte = n;
        pBit = i;
        if(this.isPosOver(pByte,pBit)){
            return -1;
        }
        int res = Common.getByteBitOnPos(data[pByte], pBit);
        this.moveReadPoint(1);
        return res;
    }

    //返回当前位置的bit位，1或�??0
    public int readBitOnPos(){
        if(this.isPosOver(pByte,pBit)){
            return -1;
        }
        int res = Common.getByteBitOnPos(data[pByte], pBit);
        this.moveReadPoint(1);
        return res;
    }

    //返回�?(n,i)位置起有连续1的个�?
    public int readNumOfContinuousOne(int n,int i){
        pByte = n;
        pBit = i;
        if(this.isPosOver(pByte,pBit)){
            return -1;
        }
        int res = 0;
        while(!this.isPosOver(pByte,pBit)){
            if(this.readBitOnPos(pByte, pBit) == 1)
                res++;
            else
                return res;
        }
        return res;
    }

    //返回从当前指向位置起有连�?1的个�?
    public int readNumOfContinuousOne(){
        if(this.isPosOver(pByte,pBit)){
            return -1;
        }
        int res = 0;
        while(!this.isPosOver(pByte,pBit)){
            if(this.readBitOnPos(pByte, pBit) == 1)
                res++;
        }
        return res;
    }

    //将有效bit转化为最大长度为31bit的int型，超过31bit返回-1
    public int intoInt(){
        int len;
        if((len = this.getValidBitNum()) > 31)
            return -1;
        int res = 0;
        for(int i = 0;i < len;i++){
            if(this.readBitOnPos() == 1){
                res += Math.pow(2, len - 1 - i);
            }
        }
        return res;
    }

    //清除数据
    public void dataClear(){
        pBit = 0;
        pByte = 0;
        isFull = false;
        lastBytePos = 0;
        lastByteValidBit = -1;
        overWriteBit = null;
        for(int i = 0;i < data.length;i++){
            data[i] = 0;
        }
    }

    //清除数据但是保留上一次的超出的部分bit
    public void dataClearButSaveOverBit() throws CloneNotSupportedException{
        pBit = 0;
        pByte = 0;
        isFull = false;
        lastBytePos = 0;
        lastByteValidBit = -1;
        for(int i = 0;i < data.length;i++){
            data[i] = 0;
        }
        if(overWriteBit != null){
            BitOperatableData bod = overWriteBit;
            overWriteBit = null;
            this.plus(bod);
        }
    }

//    //check是否�?
//    private int checkIsFull(){
//        if(lastByteValidBit == 7 && lastBytePos == data.length - 1){
//            isFull = true;
//            return 1;
//        }
//        return 0;
//    }

    //获取已用bit�?
    public int getValidBitNum(){
        return lastBytePos * 8 + lastByteValidBit + 1;
    }

    //获取空余bit�?
    public int getRestBitNum(){
        return (data.length * 8 - getValidBitNum());
    }

    //获得有效data
    //不足的字节填�?0，再在末尾加上一个字节用于表示前个字节填�?0的数�?
    public byte[] getPaddingData(){
        byte[] res;
        if(lastByteValidBit == -1){
            res = new byte[1];
            res[0] = 0;
        }
        else{
            res = new byte[lastBytePos + 2];
            res[res.length - 1] = (byte)(7 - lastByteValidBit);
        }
        System.arraycopy(data, 0, res, 0, res.length - 1);
        return res;
    }

    //填充后的byte[]转为bod
    public void paddingDataToBOD(byte[] b){
        if(b.length <= 1){
            data = new byte[1];
            dataClear();
        }
        byte[] c = new byte[b.length - 1];
        System.arraycopy(b, 0, c, 0, c.length);
        dataClear();
        this.data = c;
        isFull = false;
        lastBytePos = c.length - 1;
        lastByteValidBit = 7 - b[b.length - 1];
    }

    //深拷�?
    @Override
    public Object clone() throws CloneNotSupportedException
    {
        int len = data.length;
        BitOperatableData bod = (BitOperatableData)super.clone();
        byte[] temp = new byte[len];
        System.arraycopy(data, 0, temp, 0, len);
        bod.data = temp;
        if(bod.overWriteBit != null){
            bod.overWriteBit = (BitOperatableData)overWriteBit.clone();
        }
        return bod;
    }
}
