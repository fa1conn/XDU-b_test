import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Date;
//https://blog.csdn.net/weixin_41347419/article/details/88750088
public class Server {
    private final int port = 5203;
    private ServerSocket server_socket;   //普通消息传输服务器套接字
    private ServerSocket fileServerSocket;  // 文件传输服务器套接字
    private File log = new File("log.txt");
    FileOutputStream logOutput = new FileOutputStream(log, true);
    private String path_ = "files";

    public Server() throws FileNotFoundException {
        try {
            //1-初始化
            server_socket = new ServerSocket(this.port);   // 创建消息传输服务器
            fileServerSocket = new ServerSocket(8888);  //创建文件传输服务器


            //2-每次接收一个客户端请求连接时都启用一个线程处理
            while(true) {
                Socket client_socket = server_socket.accept();
                new ServerThread(client_socket).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ServerThread extends Thread{
        private Socket client_socket;
        private BufferedReader server_in;
        private PrintWriter server_out;

        public ServerThread(Socket client_socket) {
            try {
                //初始化
                this.client_socket = client_socket;
                server_in = new BufferedReader(new InputStreamReader(this.client_socket.getInputStream()));
                server_out = new PrintWriter(this.client_socket.getOutputStream(),true);


            } catch (IOException e) {
            }
        }

        public void run() {
            try {
                String un;
                String pwd;
                String func;
                int flag_ = 0;
                int flag2 = 0;
                un = server_in.readLine().trim();
                pwd = server_in.readLine().trim();
                func = server_in.readLine();
                //System.out.println(un + ',' + pwd);
                File config = new File("config.txt");
                if(func.startsWith("@action=Register")){
                    FileOutputStream fileOutputStream = null;
                    try {
                        String tmpString;
                        int flag3 = 0;
                        BufferedReader reader = new BufferedReader(new FileReader(config));
                        while((tmpString = reader.readLine()) != null){
                            String[] split = tmpString.split(",");//检查用户名是否存在
                            if(split[0].substring(3).equals(un)){
                                server_out.println("F");
                                flag3 = 1;
                            }
                        }
                        if(flag3 == 0){
                            fileOutputStream = new FileOutputStream(config, true);
                            String imformation = "un:" + un + "," + "pwd:" + pwd + '\n';//保存用户名和密码
                            fileOutputStream.write(imformation.getBytes());
                            String path = path_ + File.separator + un;
                            File catalog=new File(path);
                            server_out.println("T");
                            if(!catalog.exists()){
                                String str = "欢迎使用云存储服务试用版\n";
                                catalog.mkdirs();//创建存储文件夹
                                String strPath = catalog.getAbsolutePath() + File.separator +"readme.md";
                                File file = new File(strPath);
                                file.createNewFile();
                                FileOutputStream fileOutput = new FileOutputStream(file, true);
                                //file = new File(strPath);
                                file.delete();
                                Date date = new Date();
                                DateFormat df4 = DateFormat.getDateTimeInstance(DateFormat.FULL,DateFormat.FULL);
                                fileOutput.write((df4.format(date) + " 创建").getBytes());
                                fileOutput.close();
                            }

                            logWrite("注册用户 " + un);
                            flag2 = 1;
                        }
                    } catch (FileNotFoundException e) {
                        server_out.println("F");
                        flag2 = 1;
                        e.printStackTrace();
                    }

                }
                if(func.startsWith("@action=Login")){
                    BufferedReader reader = new BufferedReader(new FileReader(config));
                    String tmpString;
                    while((tmpString = reader.readLine()) != null){
                        String[] split = tmpString.split(",");
                        if(un.equals(split[0].substring(3)) && pwd.equals(split[1].trim().substring(4))){
                            flag_ = 1;
                        }
                    }
                }


                if(flag_ == 1 && flag2 == 0){
                    //登陆
                    logWrite("用户" +  un + "登陆");
                    server_out.println("T");
                    String path = path_ + "\\" + un;
                    File catalog=new File(path);
                    /*
                    if(!catalog.exists()){
                        catalog.mkdir();//创建存储文件夹
                    }
                     */

                    String uploadFileName = null;
                    String uploadFileSize = null;
                    String fromClientData ;
                    while((fromClientData = server_in.readLine()) != null){
                        System.out.println(fromClientData);
                        System.out.println(path);
                        //把服务器文件列表返回
                        if(fromClientData.startsWith("@action=loadFileList")){
                            File dir = new File(path);
                            if (dir.isDirectory()){
                                String[] list = dir.list();
                                //System.out.println(list[0]);
                                String filelist = "@action=GroupFileList[";
                                for (int i = 0; i < list.length; i++) {
                                    if (i == list.length-1){
                                        filelist  = filelist + list[i]+"]";
                                    }else {
                                        filelist  = filelist + list[i]+":";
                                    }
                                }
                                //System.out.println(filelist);
                                server_out.println(filelist);
                            }
                        }

                        //请求上传文件
                        if (fromClientData.startsWith("@action=Upload")){
                            uploadFileName = ParseDataUtil.getUploadFileName(fromClientData);
                            uploadFileSize = ParseDataUtil.getUploadFileSize(fromClientData);
                            File file = new File(path,uploadFileName);
                            //文件是否已存在
                            if (file.exists()){
                                //文件已经存在无需上传
                                server_out.println("@action=Upload[null:null:NO]");
                            }else {
                                //通知客户端开可以上传文件
                                server_out.println("@action=Upload["+uploadFileName+":"+uploadFileSize+":YES]");
                                //开启新线程上传文件
                                new HandleFileThread(1,uploadFileName,uploadFileSize,path).start();
                                logWrite("文件" +  uploadFileName + "已上传到服务器");
                            }
                        }

                        //请求下载文件
                        if(fromClientData.startsWith("@action=Download")){
                            String fileName = ParseDataUtil.getDownFileName(fromClientData);
                            File file = new File(path,fileName);
                            if(!file.exists()){
                                server_out.println("@action=Download[null:null:文件不存在]");
                            }else {
                                //通知客户端开可以下载文件
                                server_out.println("@action=Download["+file.getName()+":"+file.length()+":OK]");
                                //开启新线程处理下载文件
                                new HandleFileThread(0,file.getName(),file.length()+"",path).start();
                                logWrite("用户 " + un + "已下载" + "文件" +  file.getName());
                            }
                        }
                    }



                }else if(flag_ == 0 && flag2 == 0){
                    //登陆验证错误
                    server_out.println("F");
                }else if(flag_ == 0 && flag2 == 1){
                    //注册成功，什么都不干
                }



            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         *     文件传输线程
         */
        class HandleFileThread extends Thread{
            private String filename;
            private String filesize;
            private int mode;  //文件传输模式
            private String path;
            public HandleFileThread(int mode,String name,String size,String path){

                filename = name;
                filesize = size;
                this.mode = mode;
                this.path = path;
            }

            public void run() {
                try {
                    Socket socket = fileServerSocket.accept();
                    //上传文件模式
                    if(mode == 1){
                        //开始接收上传
                        BufferedInputStream file_in = new BufferedInputStream(socket.getInputStream());
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(path,filename)));

                        int len;
                        byte[] arr = new byte[8192];

                        while ((len = file_in.read(arr)) != -1){
                            bos.write(arr,0,len);
                            bos.flush();
                        }
                        server_out.println("@action=Upload[null:null:上传完成]");
                        server_out.println("\n");
                        bos.close();
                    }

                    //下载文件模式
                    if(mode == 0){
                        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(path,filename)));
                        BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());

                        System.out.println("服务器：开始下载");
                        int len;
                        byte[] arr =new byte[8192];
                        while((len = bis.read(arr)) != -1){
                            bos.write(arr,0,len);
                            bos.flush();
                        }

                        socket.shutdownOutput();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    public void logWrite(String log_) throws IOException {
        Date date = new Date();
        DateFormat df4 = DateFormat.getDateTimeInstance(DateFormat.FULL,DateFormat.FULL);

        logOutput.write((df4.format(date)+ ": " + log_ + '\n').getBytes());
        System.out.println(df4.format(date)+ ": " +log_);
    }

    //启动程序
    public static void main(String[] args) throws FileNotFoundException {
        new Server();
    }
}
