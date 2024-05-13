import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class udpserver {

    public static void main(String[] args) throws IOException, InterruptedException {

        //定义服务器的端口号
        DatagramSocket socket = new DatagramSocket(8800);
        byte[] data = new byte[128];
        DatagramPacket packet = new DatagramPacket(data, data.length);
        System.out.println("服务器端已经启动");

        //接收客户端发来的请求连接字符串，模拟三次握手中第二次的握手过程
        InetAddress address;
        int port;
        while (true) {
            socket.receive(packet);
            String start = new String(data, 0, packet.getLength());
            if (start.equals("hello")) {

                //二次握手
                System.out.println("响应客户端请求连接");
                address = packet.getAddress();
                port = packet.getPort();
                byte[] da = ("hi").getBytes();
                DatagramPacket pa = new DatagramPacket(da, da.length, address, port);
                socket.send(pa);
            } else if (start.equals("OK")) {
                System.out.println("连接成功，开始传送数据包");
                break;
            }
        }

        //接收数据包
        while (true) {

            //这里设置一个sleep，主要是防止RTT为全0的情况
            Thread.sleep(5);

            //随机数决定是否丢包，0——丢包，1——不丢包
            Random random = new Random();
            int x = random.nextInt(2);
            if (x == 0) {
                Thread.sleep(50);
                continue;
            }

            //接收数据包
            socket.receive(packet);

            //将接收到的字符串报文转换成udp报文格式
            String info = new String(data, 0, packet.getLength());
            udpheader udp_header = new udpheader().decode(info);
            System.out.println(udp_header);
            address = packet.getAddress();
            port = packet.getPort();

            //如果报文首部的连接字段为0，则断开连接
            if (udp_header.getConnected() == 0) {
                System.out.println("已收到客户端发来的断开连接请求");
                byte[] da = ("OK").getBytes();
                DatagramPacket pa = new DatagramPacket(da, da.length, address, port);
                socket.send(pa);
                Thread.sleep(1000);
                break;
            }

            //设置报文内容
            SimpleDateFormat formatter= new SimpleDateFormat("HH:mm:ss.SSS");
            Date date = new Date(System.currentTimeMillis());
            udp_header.setSendTime(formatter.format(date));
            udp_header.setFrom("Ubuntu");
            udp_header.setTo("Windows");
            udp_header.setData("This is reply " + udp_header.getSeq_no());
            System.out.println(udp_header);

            byte[] da = udp_header.toString().getBytes();
            DatagramPacket pa = new DatagramPacket(da, da.length, address, 8800);
            socket.send(pa);
        }

        //模拟断开连接四次挥手，这里是第三次挥手过程
        byte[] dat = "Over".getBytes();
        packet = new DatagramPacket(dat, dat.length, address, port);
        socket = new DatagramSocket();

        while (true) {
            data = new byte[128];

            //第三次挥手
            System.out.println("服务器向客户端请求结束连接");
            socket.send(packet);
            Thread.sleep(100);
            packet = new DatagramPacket(data, data.length, address, port);
            try {
                socket.setSoTimeout(2000);
                socket.receive(packet);
            } catch (Exception e) {
                continue;
            }

            //接收最后一次客户端的回应，之后关闭连接
            String s = new String(data, 0, packet.getLength());
            if (s.equals("OK")) {
                System.out.println("服务器端已断开连接");
                break;
            }
        }
        socket.close();
    }
}