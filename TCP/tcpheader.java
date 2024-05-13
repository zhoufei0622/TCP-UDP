public class tcpheader {
    private int type;
    private int N;
    private int Length;
    private String Data;   //注意String类型数据的处理

    public tcpheader() {}
    public tcpheader(int type, int N, int Length, String Data) {
        this.type = type;
        this.N = N;
        this.Length = Length;
        this.Data = Data;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getN() {
        return N;
    }

    public void setN(int n) {
        N = n;
    }

    public int getLength() {
        return Length;
    }

    public void setLength(int length) {
        Length = length;
    }

    public String getData() {
        return Data;
    }

    public void setData(String data) {
        Data = data;
    }

    @Override
    public String toString() {
        return type + String.format("%04d", N) + String.format("%03d", Length) + Data;
    }

    public tcpheader decode(String str) {
        tcpheader tcpheader = new tcpheader();
        tcpheader.setType(Integer.parseInt(str.substring(0, 1)));
        tcpheader.setN(Integer.parseInt(str.substring(1, 5)));
        tcpheader.setLength(Integer.parseInt(str.substring(5, 8)));
        tcpheader.setData(str.substring(8));
        return tcpheader;
    }
}
