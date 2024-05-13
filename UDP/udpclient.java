import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class udpclient {

    /*
    * 设置全局变量，用来记录输出的信息以及重要的标志位
    */
    public static long[] sendTime = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    public static long[] receiveTime = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    public static long[] RTT = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    /*
    * status=0，未发送数据包
    * status=1，已发送数据包，未收到reply
    * status=2，已发送数据包，收到reply
    * status=-1，重发第一次，未收到reply
    * status=-2，重发第二次，未收到reply
    * status=-3，超时
    * */
    public static int[] status = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    public static int sendPacket = 0;
    public static int receivePacket = 0;

    //定义超时时间，单位ms
    public static long TTL = 100;

    //定义服务器端响应时间
    public static long startResponseServer = 0;
    public static long endResponseServer = 0;

    /*
     * send线程表示首次发送数据包至服务端
     * UDP在java里面用DatagramSocket和DatagramPacket实现，packet数据包要指定对应服务器的IP地址和端口号，二者为程序开始时输入的值
     */
    static class send extends Thread {
        public String serverIP;
        public int serverPort;
        public send(String serverIP, String serverPort) {
            this.serverIP = serverIP;
            this.serverPort = Integer.parseInt(serverPort);
        }
        @Override
        public void run() {

            //声明必要的参数变量
            InetAddress address = null;
            int port = serverPort;
            DatagramSocket socket = null;
            try {
                address = InetAddress.getByName(serverIP);
                socket = new DatagramSocket();
            } catch (UnknownHostException | SocketException e) {
                e.printStackTrace();
            }

            //发送数据包
            for (int i = 1; i <= 12; i ++) {
                udpheader udp_header = new udpheader();

                //设置数据报文的各项值
                udp_header.setConnected(1);
                udp_header.setSeq_no(i);
                SimpleDateFormat formatter= new SimpleDateFormat("HH:mm:ss.SSS");
                sendTime[i] = System.currentTimeMillis();
                Date date = new Date(System.currentTimeMillis());
                udp_header.setSendTime(formatter.format(date));
                udp_header.setFrom("Windows");
                udp_header.setTo("Ubuntu");
                udp_header.setData("This is the request" + i);

                //发送数据包到指定端口，并改变数据包状态
                byte[] data = udp_header.toString().getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                try {
                    assert socket != null;
                    socket.send(packet);
                    status[i] = 1;
                    sendPacket += 1;
                    Thread.sleep(100);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
            socket.close();
        }
    }

    /*
    * receive线程用来接收从服务端发送的回应报文
    * 这里有个编程上的问题，服务端发送的reply的端口不能和客户端send线程的端口一样，需要另外指定，这里我设置成了一个固定的值
    */
    static class receive extends Thread {
        public String serverIP;
        public int serverPort;
        public receive(String serverIP, String serverPort) {
            this.serverIP = serverIP;
            this.serverPort = Integer.parseInt(serverPort);
        }
        @Override
        public void run() {
            //声明必要的参数变量
            InetAddress address;
            int port = serverPort;
            DatagramSocket socket = null;
            try {
                address = InetAddress.getByName(serverIP);

                //这里的socket直接定义端口和IP地址，即服务器端发送的报文会在这个端口处接收
                socket = new DatagramSocket(port);
            } catch (UnknownHostException | SocketException e) {
                e.printStackTrace();
            }

            //循环接收reply
            while (!isOver()) {
                byte[] data = new byte[128];
                DatagramPacket packet = new DatagramPacket(data, 0, data.length);
                try {
                    assert socket != null;
                    socket.receive(packet);
                } catch (IOException e) {
                    continue;
                }
                String info = new String(packet.getData(), 0, packet.getLength());
                udpheader udpheader = new udpheader().decode(info);

                //获取报文的信息，这里只获取了序列号
                int seqNo = udpheader.getSeq_no();
                receiveTime[seqNo] = System.currentTimeMillis();
                RTT[seqNo] = receiveTime[seqNo] - sendTime[seqNo];

                //如果当前的RTT≥设定值，则不显示这次的输出
                if (RTT[seqNo] >= TTL) {
                    continue;
                }

                //设置数据包状态
                status[seqNo] = 2;
                address = packet.getAddress();
                port = packet.getPort();
                System.out.println("sequence no " + seqNo + "， " + address.toString().substring(1) + ":" + port + "，RTT=" + RTT[seqNo] + "ms");
                receivePacket += 1;
            }
            assert socket != null;
            socket.close();
        }
    }

    /*
    * resend线程用来实现重发的操作
    * resend线程和send线程的主要代码没有什么区别，应该写成一个函数，主要是对状态的设定有些不同，和receive一样，也要用isOver函数控制是否结束
    */
    static class resend extends Thread {
        public String serverIP;
        public int serverPort;
        public resend(String serverIP, String serverPort) {
            this.serverIP = serverIP;
            this.serverPort = Integer.parseInt(serverPort);
        }
        @Override
        public void run() {

            //声明必要的参数变量
            InetAddress address = null;
            int port = serverPort;
            DatagramSocket socket = null;
            try {
                address = InetAddress.getByName(serverIP);
                socket = new DatagramSocket();
            } catch (UnknownHostException | SocketException e) {
                e.printStackTrace();
            }

            //循环判断是否结束
            while (!isOver()) {
                for (int i = 1; i <= 12; i ++){

                    //对于每个RTT进行判断，如果RTT≥设定值，则进行重传的操作
                    if (receiveTime[i] - sendTime[i] >= TTL) {

                        //当然，如果当前的数据包已经发了两次，则不需要再次重发，直接报超时即可
                        if (status[i] == 1 || status[i] == -1) {
                            udpheader udp_header = new udpheader();

                            //设置数据报文的各项值
                            udp_header.setConnected(1);
                            udp_header.setSeq_no(i);
                            SimpleDateFormat formatter= new SimpleDateFormat("HH:mm:ss.SSS");
                            Date date = new Date(System.currentTimeMillis());
                            udp_header.setSendTime(formatter.format(date));
                            sendTime[i] = System.currentTimeMillis();
                            udp_header.setFrom("Windows");
                            udp_header.setTo("Ubuntu");
                            udp_header.setData("This is the request" + i);

                            //发送数据包到指定端口，并改变数据包状态
                            byte[] data = udp_header.toString().getBytes();
                            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
                            try {
                                assert socket != null;
                                socket.send(packet);
                                status[i] -= (status[i] == 1 ? 2 : 1);
                                sendPacket += 1;
                                Thread.sleep(100);
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        } else if (status[i] == -2) {
                            status[i] --;
                            System.out.println("sequence no " + i + " , request time out");
                            RTT[i] = TTL;
                        }
                    }
                }
            }
        }
    }

    /*
    * isOver函数判断是否已经结束，要么reply得到回复（2），要么超时（-3）
    */
    public static boolean isOver() {
        for (int i = 1; i <= 12; i ++) {
            if (status[i] != 2 && status[i] != -3) {
                return false;
            }
        }
        return true;
    }

    /*
    * getConnected函数是用来模拟建立连接的过程，即在发送数据包之前，先“hello”一下
    */
    public static void getConnected(InetAddress address, int port) throws IOException, InterruptedException {

        //在应用层建立连接，发送“hello”
        byte[] data = "hello".getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        DatagramSocket socket = new DatagramSocket();

        //这个while循环模拟了建立连接，setSoTimeout函数表示如果当前发送的hello，服务器端响应的时间超出了这个数值（单位：ms），则不执行后面的语句，即解决了socket.receive卡住的问题
        while (true) {

            //一次握手
            System.out.println("向服务器端请求连接");
            socket.send(packet);
            Thread.sleep(1000);
            packet = new DatagramPacket(data, data.length, address, port);
            try {
                socket.setSoTimeout(2000);
                socket.receive(packet);
            } catch (Exception e) {
                continue;
            }

            //接受服务器端的响应，即同意连接，此时客户端再发送一个OK，然后发送数据包
            startResponseServer = System.currentTimeMillis();
            String s = new String(data, 0, packet.getLength());
            if (s.equals("hi")) {

                //三次握手
                System.out.println("服务器端已响应请求连接");
                data = "OK".getBytes();
                address = packet.getAddress();
                port = packet.getPort();
                packet = new DatagramPacket(data, data.length, address, port);
                socket.send(packet);
                break;
            }
        }
    }

    /*
    * disConnected函数是用来模拟断开连接的过程，即四次挥手
    */
    public static void disConnected(InetAddress address, int port) throws IOException, InterruptedException {

        //发送一个udpheader，设置isConnected为0，代表断开连接
        udpheader udpheader = new udpheader(0, 0, 2, "", "", "", "");
        byte[] data = udpheader.toString().getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        DatagramSocket socket = new DatagramSocket();

        while (true) {

            //一次挥手
            System.out.println("客户端向服务器端请求断开连接");
            socket.send(packet);
            Thread.sleep(100);
            packet = new DatagramPacket(data, data.length, address, port);
            try {
                socket.setSoTimeout(2000);
                socket.receive(packet);
            } catch (Exception e) {
                continue;
            }

            //接收服务器端响应的字符串
            String s = new String(data, 0, packet.getLength());
            if (s.equals("OK")) {

                //二次挥手
                System.out.println("服务器端已响应断开连接");
                break;
            }
        }

        while (true) {
            data = new byte[128];
            packet = new DatagramPacket(data, data.length, address, 8888);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                continue;
            }
            String info = new String(data, 0, packet.getLength());
            if (info.equals("Over")) {

                //四次挥手
                System.out.println("收到服务器端结束连接请求");
                endResponseServer = System.currentTimeMillis();
                data = "OK".getBytes();
                address = packet.getAddress();
                port = packet.getPort();
                packet = new DatagramPacket(data, data.length, address, port);
                socket.send(packet);
                break;
            }
        }
    }

    public static void print() {
        System.out.println("收到的packet数目：" + receivePacket);
        BigDecimal bDecimal = new BigDecimal(1 - 1.00 * receivePacket / sendPacket);
        double rate = bDecimal.setScale(3, RoundingMode.HALF_UP).doubleValue();
        System.out.println("丢包率：" + rate);
        long max = -1, min = 1000;
        int sum = 0;
        double summ = 0.00;
        for (int i = 1; i <= 12; i ++) {
            if (RTT[i] < TTL) {
                max = Math.max(max, RTT[i]);
                min = Math.min(min, RTT[i]);
                sum += RTT[i];
            }
        }
        double avg = 1.00 * sum / 12;
        bDecimal = new BigDecimal(avg);
        avg = bDecimal.setScale(3, RoundingMode.HALF_UP).doubleValue();
        System.out.println("最大RTT：" + max + "ms");
        System.out.println("最小RTT：" + min + "ms");
        System.out.println("平均RTT：" + avg + "ms");
        for (int i = 1; i <= 12; i ++) {
            if (RTT[i] < TTL) {
                summ += (RTT[i] - avg) * (RTT[i] - avg);
            }
        }
        bDecimal = BigDecimal.valueOf(Math.sqrt(summ / 12));
        double standardDeviation = bDecimal.setScale(3, RoundingMode.HALF_UP).doubleValue();
        System.out.println("RTT标准差：" + standardDeviation + "ms");
        System.out.println("Server整体响应时间：" + (endResponseServer - startResponseServer) + "ms");

        System.exit(0);
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        //输入服务器地址以及端口号，二者有一个不匹配都建立不上链接，服务器的端口号是固定的
        System.out.print("服务器地址和端口号：");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String input = br.readLine();
        String[] str = input.split(" ");

        //建立连接，三次握手
        InetAddress address = InetAddress.getByName(str[0]);
        int port = Integer.parseInt(str[1]);
        getConnected(address, port);

        //创建一个发送线程，使用有参构造函数
        Thread thread1 = new send(str[0], str[1]);
        //创建一个接收线程，使用有参构造函数
        Thread thread2 = new receive(str[0], str[1]);
        //创建一个重发线程，使用有参构造函数
        Thread thread3 = new resend(str[0], str[1]);

        //设置接收线程为守护线程，即主线程停止后该线程也会随之停止
        thread2.setDaemon(true);

        //线程开始
        thread1.start();
        thread2.start();
        thread3.start();

        thread1.join();
        thread3.join(3000);

        //断开连接，四次挥手
        disConnected(address, port);

        //输出结果
        print();
    }
}