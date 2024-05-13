import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class reversetcpserver_singleThread {

    //定义端口号
    private int port = 7777;   //定义服务器端的套接字

    public reversetcpserver_singleThread() {}

    public void service() {
        try {

            //定义变量，设置服务器端口为7777
            ServerSocket server = new ServerSocket(port);
            Socket socket = server.accept();
            try {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                boolean f = false;
                int maxN = 0;
                while (!f) {
                    tcpheader accpet = new tcpheader().decode(in.readUTF());
                    if (accpet.getN() == maxN && accpet.getType() == 3) {
                        f = true;
                    }
                    System.out.println(accpet);
                    tcpheader tcpheader;

                    //如果发送的是Initialization报文，则回应Accept，同时获取N以控制循环
                    if (accpet.getType() == 1) {
                        tcpheader = new tcpheader(2, accpet.getN(), accpet.getLength(), "Accept");
                        maxN = accpet.getN();
                    } else {
                        tcpheader = new tcpheader(4, accpet.getN(), accpet.getLength(), new StringBuffer(accpet.getData()).reverse().toString());
                    }

                    //发送回应报文
                    String send = tcpheader.toString();
                    System.out.println(send);
                    out.writeUTF(send);
                }
            } finally {
                socket.close();
                server.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new reversetcpserver_singleThread().service();
    }
}
