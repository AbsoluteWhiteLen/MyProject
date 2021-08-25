package pack_and_encrypt;


//KMP
public class KMP {

    //改进next数组
    private int[][] nextArrImp;

    //静�?�内部类单例模式
    private static class InstanceHolder{
        private static final KMP INSTANCE = new KMP();
    }

    private KMP(){}

    //获取实例
    static public final KMP getInstance(){
        return InstanceHolder.INSTANCE;
    }

    //获取next数组
    public int[] getNext(byte[] pStr){
        int[] nextArr = new int[pStr.length];
        int i = 0, k = -1, pLen = pStr.length;
        nextArr[i] = k;
        int mLen = pLen - 1;
        while (i < mLen)
        {
            if (k == -1 || pStr[i] == pStr[k])
            {
                nextArr[++i] = ++k;
            }
            else k = nextArr[k];
        }
        improveNext(pStr,nextArr);
        return nextArr;
    }

    //改进next
    private void improveNext(byte[] pStr,int[] nextArr){
        nextArrImp = new int[pStr.length][2];
        nextArrImp[0][0] = 1;
        nextArrImp[0][1] = 0;
        nextArrImp[1][0] = 1;
        nextArrImp[1][1] = 0;
        if(pStr[1] == pStr[0]){
            nextArrImp[1][0] = 2;
        }
        for(int i = 2;i < nextArr.length;i++){
            int p = i - nextArr[i];
            if(pStr[i] == pStr[nextArr[i]]){
                nextArrImp[i][0] = p + nextArr[nextArr[i]];
                nextArrImp[i][1] = nextArrImp[nextArr[i]][1];
            }
            else{
                nextArrImp[i][0] = p;
                nextArrImp[i][1] = nextArr[i];
            }
        }
    }

    //从pos处开始寻找，返回第一个找到的位置，未找到返回-1
    public int findFirstIndex(byte[] l,byte[] s,int pos){
        if(s.length == 1){
            for(int i = pos;i < l.length;i++){
                if(l[i] == s[0])
                    return i;
            }
            return -1;
        }

        getNext(s);
        int compPos = 0;
        while(pos <= l.length - s.length){
            for(int j = compPos;j < s.length;j++){
                if(s[j] != l[pos + j]){
                    pos += nextArrImp[j][0];
                    compPos = nextArrImp[j][1];
                    break;
                }
                if(j == s.length -1){
                    return pos;
                }
            }
        }
        return -1;
    }
}
