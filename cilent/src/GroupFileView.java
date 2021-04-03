import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;

public class GroupFileView extends JFrame {
    private int width = 400;
    private int height = 600;
    private JLabel groupLabel;
    private JButton uploadButton;
    private JLabel flushLabel;

    private JScrollPane jScrollPane;
    private JPanel staffPanel;      //在JSpanel上的panel

    private Socket client_socket;
    private PrintStream client_out;
    private BufferedReader client_in;
    private String ip = "127.0.0.1";
    private int port = 5203;

    private File currentUpploadFile;
    private String downloadSavePath;
    private int Y = 0;

    public GroupFileView(BufferedReader client_in,PrintStream client_out) {
        //login login_ = new login();
        this.client_in = client_in;
        this.client_out = client_out;
        //1-初始化
        initVariable();
        //2-连接服务器
        readlist(client_in,client_out);
        //3-注册监听
        registerListener();
        //4-初始化窗口设置

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(width,height);
        this.setTitle("文件");
        this.setLocationRelativeTo(null);//窗口居中显示
        this.setResizable(false);
        this.setVisible(true);

    }

    private void readlist(BufferedReader client_in, PrintStream client_out) {
        try {
            client_out.println("@action=loadFileList");
            new ClientThread().start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *    监听服务器消息
     */
    class ClientThread extends Thread{
        public void run() {
            try {
                String fromServer_data;
                int flag = 0;

                while((fromServer_data=client_in.readLine()) != null){
                    //读取群文件列表
                    if(flag++ == 0){
                        if (fromServer_data.startsWith("@action=GroupFileList")){
                            String[] fileList = ParseDataUtil.getFileList(fromServer_data);
                            for (String filename : fileList) {
                                addToFileList(filename);
                            }
                        }
                        continue;
                    }
                    if(fromServer_data.startsWith("@action=GroupFileList")){
                        //重新渲染顶部面板
                        renderTop();

                        //注册监听
                        registerListener();

                        //渲染文件面板
                        String[] fileList = ParseDataUtil.getFileList(fromServer_data);
                        for (String filename : fileList) {
                            addToFileList(filename);
                        }
                    }

                    //文件上传
                    if (fromServer_data.startsWith("@action=Upload")){
                        String res = ParseDataUtil.getUploadResult(fromServer_data);
                        if("NO".equals(res)){
                            JOptionPane.showMessageDialog(null,"文件已存在!");
                        }else if ("YES".equals(res)){
                            //开始上传
                            if(currentUpploadFile != null){
                                //开启新线程传输文件
                                new HandelFileThread(1).start();
                            }

                        }else if ("上传完成".equals(res)){
                            JOptionPane.showMessageDialog(null,res);
                            loadGroupFile();
                        }
                    }

                    //文件下载
                    if(fromServer_data.startsWith("@action=Download")){
                        String res = ParseDataUtil.getDownResult(fromServer_data);
                        if(res.equals("文件不存在")){
                            JOptionPane.showMessageDialog(null,"该文件不存在404");
                        }else {
                            String downFileName = ParseDataUtil.getDownFileName(fromServer_data);
                            String downFileSize = ParseDataUtil.getDownFileSize(fromServer_data);
                            //开启新线程传输文件
                            new HandelFileThread(0,downFileName,downFileSize).start();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        /**----------------------------------------------------------------------------------
         *     文件传输线程
         */
        class HandelFileThread extends Thread{
            private int mode;  //文件传输模式  1-上传  2-下载
            private String filename;
            private Long fileSize;

            public HandelFileThread(int mode) {
                this.mode = mode;
            }
            public HandelFileThread(int mode,String filename,String fileSize){
                this.mode = mode;
                this.filename = filename;
                this.fileSize = Long.parseLong(fileSize);
            }

            public void run() {
                try {
                    //上传文件模式
                    if(this.mode == 1){
                        Socket socket = new Socket(ip,8888);
                        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(currentUpploadFile));
                        BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());

                        int len;
                        int i = 0;
                        double sum = 0;
                        byte[] arr = new byte[8192];
                        String schedule;

                        System.out.println("开始上传--文件大小为："+currentUpploadFile.length());

                        while((len = bis.read(arr)) != -1){
                            bos.write(arr,0,len);
                            bos.flush();
                            sum += len;
                            if (i++ %100 == 0){
                                schedule = "上传进度:"+100*sum/currentUpploadFile.length()+"%";
                                System.out.println(schedule);
                            }
                        }
                        //上传完成
                        socket.shutdownOutput();
                        System.out.println("上传进度:100%");
                    }

                    //下载文件模式
                    if(this.mode == 0){
                        Socket socket = new Socket(ip,8888);
                        BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(downloadSavePath+"/"+filename));

                        int len;
                        byte[] arr =new byte[8192];
                        double sumDown = 0;
                        int i = 0;

                        System.out.println("客户端开始下载 ");
                        while ((len = bis.read(arr)) != -1){
                            sumDown += len;
                            if(i++%100 == 0)
                                System.out.println("下载进度为："+100*sumDown/fileSize+"%");

                            bos.write(arr,0,len);
                            bos.flush();
                        }

                        bos.close();
                        bis.close();
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     渲染文件列表
     */
    public  void addToFileList(String filename){
        JLabel fileicon = new JLabel(scaleImage(new ImageIcon("file.png"),30,30));
        JButton downloadBtn = new JButton("下载");
        JLabel fileNameLab = new JLabel(filename);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1,3,0,0));
        panel.add(fileicon);
        panel.add(fileNameLab);
        panel.add(downloadBtn);
        //panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        panel.setBounds(2,Y,width,30);
        this.staffPanel.add(panel);
        Y+=30;

        panel.addMouseListener(new MouseAdapter() {
            //鼠标移入时
            public void mouseEntered(MouseEvent e) { // 鼠标移动到这里的事件
                panel.setBackground(Color.orange);
                panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // 让鼠标移动到
            }
            public void mouseExited(MouseEvent e) { // 鼠标离开的事件
                panel.setBackground(Color.white);
            }

        });

        //文件下载
        downloadBtn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                //1-选择下载保存的位置
                JFileChooser f = new JFileChooser(); // 查找文件
                f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                f.showOpenDialog(null);
                File file = f.getSelectedFile();

                if(file != null){
                    downloadSavePath  = file.getPath();
                    //向服务器请求下载
                    client_out.println("@action=Download["+filename+":null:null]");
                }
            }
        });
    }

    private void registerListener() {
        //上传文件    消息格式: @action=Upload["fileName":"fileSize":result]
        this.uploadButton.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                JFileChooser f = new JFileChooser(); // 查找文件
                f.showOpenDialog(null);
                currentUpploadFile = f.getSelectedFile();
                if(currentUpploadFile != null)
                    client_out.println("@action=Upload["+currentUpploadFile.getName()+":"+currentUpploadFile.length()+":null]");

            }
        });

        //刷新文件列表按钮
        flushLabel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                loadGroupFile();
            }
            //鼠标移入时
            public void mouseEntered(MouseEvent e) { // 鼠标移动到这里的事件
                flushLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); // 让鼠标移动到
            }
        });
    }
    private void loadGroupFile() {
        client_out.println("@action=loadFileList");
    }

    private void initVariable() {
        jScrollPane = new JScrollPane();
        this.getContentPane().add(jScrollPane);

        staffPanel = new JPanel();
        ///staffPanel.setLayout(new BoxLayout(staffPanel,BoxLayout.Y_AXIS));
        staffPanel.setLayout(null);
        staffPanel.setOpaque(false);
        staffPanel.setPreferredSize(new Dimension(width,height));

        jScrollPane.setViewportView(staffPanel);
        jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);//设置水平滚动条隐藏
        jScrollPane.getViewport().setOpaque(false);  //设置透明
        jScrollPane.setOpaque(false);  //设置透明

        renderTop();
    }

    private void renderTop(){
        staffPanel.removeAll();
        Y = 0;
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1,3,3,10));
        this.groupLabel = new JLabel("\t\t\t\t\t文件列表 ");
        this.uploadButton = new JButton("上传文件 ");
        flushLabel = new JLabel(scaleImage(new ImageIcon("flush.jpg"),30,30));
        panel.add(groupLabel);
        panel.add(uploadButton);
        panel.add(flushLabel);

        panel.setBounds(2,Y,width,30);
        this.staffPanel.add(panel);
        Y += 30;
    }

    public ImageIcon scaleImage(ImageIcon icon, int w, int h)
    {
        int nw = icon.getIconWidth();
        int nh = icon.getIconHeight();

        if(icon.getIconWidth() > w)
        {
            nw = w;
            nh = (nw * icon.getIconHeight())/icon.getIconWidth();
        }
        if(nh > h)
        {
            nh = h;
            nw = (icon.getIconWidth() * nh)/icon.getIconHeight();
        }
        return new ImageIcon(icon.getImage().getScaledInstance(nw, nh, Image.SCALE_DEFAULT));
    }
}
