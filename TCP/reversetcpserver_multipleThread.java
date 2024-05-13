import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class reversetcpserver_multipleThread {

    //多线程接收客户端信息
    static class ServerThread implements Runnable{
        private final Socket socket;
        public ServerThread(Socket s) {
            this.socket = s;
        }

        @Override
        public void run() {
            boolean isRunning = true;
            int maxN = 0;
            try {
                InputStream is = socket.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String msg;
                while ((msg = br.readLine()) != null && isRunning) {
                    tcpheader tcpheader = new tcpheader().decode(msg);
                    System.out.println(tcpheader);
                    int type = tcpheader.getType();
                    int N = tcpheader.getN();
                    int Length = tcpheader.getLength();
                    String Data = tcpheader.getData();

                    OutputStream os = socket.getOutputStream();
                    PrintStream ps = new PrintStream(os);
                    if (type == 1) {
                        System.out.println("收到Initialization报文");
                        maxN = N;
                        ps.println(new tcpheader(2, N, Length, "Accept"));
                    } else if (type == 3) {
                        if (N == maxN) {
                            isRunning = false;
                        }
                        ps.println(new tcpheader(4, N, Length, new StringBuilder(Data).reverse().toString()));
                    }
                    ps.flush();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            //创建一个服务器端的Socket，即ServerSocket，绑定需要监听的端口
            ServerSocket serverSocket = new ServerSocket(7777);
            Socket socket = null;
            while (true) {
                //调用accept()方法侦听，等待客户端的连接以获取Socket实例
                socket = serverSocket.accept();
                //创建新线程
                Thread thread = new Thread(new ServerThread(socket));
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}