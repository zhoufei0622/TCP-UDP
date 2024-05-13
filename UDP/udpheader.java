public class udpheader {
    private int isConnected;
    private int seq_no;
    private int version = 2;
    private String sendTime;
    private String from;
    private String to;
    private String data;

    public udpheader() {}

    public udpheader(int isConnected, int seq_no, int version, String sendTime, String from, String to, String data) {
        this.isConnected = isConnected;
        this.seq_no = seq_no;
        this.version = version;
        this.sendTime = String.format("%13s", sendTime);
        this.from = String.format("%8s", from);
        this.to = String.format("%8s", to);
        this.data = String.format("%31s", data);
    }

    public int getConnected() {
        return isConnected;
    }

    public void setConnected(int isConnected) {
        this.isConnected = isConnected;
    }

    public int getSeq_no() {
        return seq_no;
    }

    public void setSeq_no(int seq_no) {
        this.seq_no = seq_no;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getSendTime() {
        return sendTime;
    }

    public void setSendTime(String sendTime) {
        this.sendTime = String.format("%13s", sendTime);
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = String.format("%8s", from);
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = String.format("%8s", to);
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = String.format("%31s", data);
    }

    @Override
    public String toString() {
        return isConnected + String.format("%02d", seq_no) + version + sendTime + from + to + data;
    }

    public udpheader decode(String str) {
        udpheader udpheader = new udpheader();
        udpheader.setConnected(Integer.parseInt(str.substring(0, 1)));
        udpheader.setSeq_no(Integer.parseInt(str.substring(1, 3)));
        udpheader.setVersion(Integer.parseInt(str.substring(3, 4)));
        udpheader.setSendTime(str.substring(4, 17));
        udpheader.setFrom(str.substring(17, 25));
        udpheader.setTo(str.substring(25, 33));
        udpheader.setData(str.substring(33, 64));
        return udpheader;
    }
}
