import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;

public class reversetcpclient {
    private static Socket socket;

    private static int Lmin;
    private static int Lmax;
    private static final ArrayList<String> block_msg = new ArrayList<>();
    private static final ArrayList<Integer> block_len = new ArrayList<>();
    private static final ArrayList<String> receive_msg = new ArrayList<>();

    public reversetcpclient() {}

    //输入相关信息，并判断是否能连接
    public static boolean init() throws IOException {
        System.out.print("服务器地址和端口号：");
        BufferedReader br1 = new BufferedReader(new InputStreamReader(System.in));
        String input1 = br1.readLine();
        String[] str1 = input1.split(" ");

        InetAddress address = InetAddress.getByName(str1[0]);
        int port = Integer.parseInt(str1[1]);
        try {
            socket = new Socket(address, port);
        } catch (Exception e) {
            System.out.println("无法连接至服务器！");
            return false;
        }

        System.out.print("设置发送报文长度范围：");
        BufferedReader br2 = new BufferedReader(new InputStreamReader(System.in));
        String input2 = br2.readLine();
        String[] str2 = input2.split(" ");

        Lmin = Integer.parseInt(str2[0]);
        Lmax = Integer.parseInt(str2[1]);

        if (Lmax > 200) {
            System.out.println("Lmax不得超过200！");
            return false;
        } else if (Lmin <= 0) {
            System.out.println("Lmin必须为正数！");
            return false;
        } else if (Lmin > Lmax) {
            System.out.println("Lmin不得大于Lmax！");
            return false;
        } else {
            return true;
        }
    }

    //读取txt文本文件函数
    public static boolean readFile(String filepath) {
        try {

            //建立文件对象，并指定路径
            File file = new File(filepath);
            if (file.isFile() && file.exists()) {

                //将文本文件的内容读出来到text字符串中
                InputStreamReader read = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(read);
                String text = "";
                String srcText = "";
                while ((text = br.readLine()) != null) {
                    srcText += text + " ";
                }
                read.close();

                //将文本文件分块
                while (true) {

                    //如果当前的字符数小于等于设置的最大值，则将其设置为一块，并退出循环
                    if (srcText.length() <= Lmax) {
                        block_msg.add(srcText);
                        block_len.add(srcText.length());
                        break;
                    }

                    //否则设置一个随机数，定义块的大小
                    Random random = new Random();
                    int length = random.nextInt(Lmax - Lmin + 1) + Lmin;
                    String msg = srcText.substring(0, length);
                    srcText = srcText.substring(length);
                    block_msg.add(msg);
                    block_len.add(length);
                }
                return true;
            } else {
                System.out.println("读取文件出错！");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

//    //建立连接，等待服务器端回应Accept
//    public static void getConnected() {
//        try {
//
//            //in用于接收服务器端发回的报文，out用于发送客户端的报文
//            DataInputStream in = new DataInputStream(socket.getInputStream());
//            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
//
//            while (true) {
//
//                //定义Initialization报文
//                System.out.println("发送Initialization报文");
//                tcpheader tcpheader = new tcpheader(1, block_msg.size(), 0, "Initialization");
//                out.writeUTF(tcpheader.toString());
//                Thread.sleep(2000);
//
//                tcpheader = new tcpheader().decode(in.readUTF());
//                if (tcpheader.getType() == 2) {
//                    System.out.println("收到Accept报文，开始传送数据");
//                    break;
//                }
//
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    //建立连接，等待服务器端回应Accept
    public static void getConnected2() {
        try {
            OutputStream os = socket.getOutputStream();
            PrintStream ps = new PrintStream(os);
            boolean isRunning = true;
            while (isRunning) {
                System.out.println("发送Initialization报文");
                tcpheader tcpheader = new tcpheader(1, block_msg.size(), 0, "Initialization");
                ps.println(tcpheader);
                ps.flush();
                Thread.sleep(1000);
                InputStream is = socket.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String msg;
                while ((msg = br.readLine()) != null) {
                    tcpheader = new tcpheader().decode(msg);
                    if (tcpheader.getType() == 2) {
                        System.out.println("收到Accept报文，开始传送数据");
                        isRunning = false;
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public static void SendReceive() throws IOException, InterruptedException {
//
//        //in用于接收服务器端发回的报文，out用于发送客户端的报文
//        DataInputStream in = new DataInputStream(socket.getInputStream());
//        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
//
//        for (int i = 0; i < block_msg.size(); i ++) {
//
//            //传送数据
//            tcpheader tcpheader = new tcpheader(3, i + 1, block_len.get(i), block_msg.get(i));
//            out.writeUTF(tcpheader.toString());
//            Thread.sleep(1000);
//
//            //接收数据
//            tcpheader = new tcpheader().decode(in.readUTF());
//            if (tcpheader.getType() == 4) {
//                System.out.println(tcpheader.getN() + ": " + tcpheader.getData());
//                receive_msg.add(tcpheader.getData());
//            }
//        }
//        socket.close();
//    }
    public static void SendReceive2() {
        try {
            OutputStream os = socket.getOutputStream();
            PrintStream ps = new PrintStream(os);

            for (int i = 0; i < block_msg.size(); i ++) {

                //传送数据
                tcpheader tcpheader = new tcpheader(3, i + 1, block_len.get(i), block_msg.get(i));
                ps.println(tcpheader);
                ps.flush();
                Thread.sleep(1000);

                //接收数据
                InputStream is = socket.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String msg;
                while ((msg = br.readLine()) != null) {
                    tcpheader = new tcpheader().decode(msg);
                    if (tcpheader.getType() == 4) {
                        System.out.println(tcpheader.getN() + ": " + tcpheader.getData());
                        receive_msg.add(tcpheader.getData());
                        break;
                    }
                }
            }
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeFile(String filepath) throws IOException {
        File file = new File(filepath);
        file.createNewFile();
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        for (int i = receive_msg.size() - 1; i >=0; i --) {
            bw.write(receive_msg.get(i) + "\n");
        }
        bw.flush();
        bw.close();
    }

    public static void main(String[] args) throws IOException, InterruptedException {

        while (true) {
            //初始化
            if (init()) {

                //读取文本文件，若读取文本文件失败，则退出循环
                if (!readFile("input.txt")) {
                    break;
                }

                //建立连接，即Initialization和Accept报文
                //getConnected();
                getConnected2();

                //发送与接收
                //SendReceive();
                SendReceive2();

                //写入文件
                writeFile("result.txt");

                break;
            }
        }
        socket.close();
        System.exit(0);
    }
}
