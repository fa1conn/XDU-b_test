import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

public class login implements ActionListener {
    private JButton loginButton;
    private JButton registerButton;
    private JTextField textField;
    private JPasswordField pwdField;
    private JFrame jf;
    private Socket client_socket;
    private PrintStream client_out;
    private BufferedReader client_in;
    private String ip = "127.0.0.1";
    private int port = 5203;

    public login() {
        textField = null;
        pwdField = null;
        jf = new JFrame("登录");
        jf.setBounds(500, 250, 310, 210);
        jf.setResizable(false);  // 设置是否缩放

        JPanel jp1 = new JPanel();
        JLabel headJLabel = new JLabel("登录界面");
        headJLabel.setFont(new Font(null, 0, 35));  // 设置文本的字体类型、样式 和 大小
        jp1.add(headJLabel);

        JPanel jp2 = new JPanel();
        JLabel nameJLabel = new JLabel("用户名：");
        textField = new JTextField(20);
        JLabel pwdJLabel = new JLabel("密码：    ");
        pwdField = new JPasswordField(20);
        loginButton = new JButton("登录");
        registerButton = new JButton("注册");
        jp2.add(nameJLabel);
        jp2.add(textField);
        jp2.add(pwdJLabel);
        jp2.add(pwdField);
        jp2.add(loginButton);
        jp2.add(registerButton);
        System.out.println("this is ...");

        JPanel jp = new JPanel(new BorderLayout());  // BorderLayout布局
        jp.add(jp1, BorderLayout.NORTH);
        jp.add(jp2, BorderLayout.CENTER);

        loginButton.addActionListener(this);
        registerButton.addActionListener(this);//按钮添加监听

        jf.add(jp);
        jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  // 设置关闭图标作用
        jf.setVisible(true);  // 设置可见
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        JFrame loginJFrame = null;  // 登录窗口本身

        if(e.getSource() == loginButton){
            String un = textField.getText().trim();
            String pwd = pwdField.getText().trim();
            //System.out.println(un + "," + pwd);
            try {
                //初始化
                client_socket = new Socket(ip,port);
                client_out = new PrintStream(client_socket.getOutputStream(),true);
                client_in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
                client_out.println(un);
                client_out.println(pwd);
                client_out.println("@action=Login");
                //System.out.println(System.getProperty("user.dir"));
                String flag = client_in.readLine().trim();
                if(flag.equals("T")){
                    //System.out.println("the server send T and cilent receive it.");
                    JOptionPane.showMessageDialog(jf, "登陆成功！", "提示", JOptionPane.WARNING_MESSAGE);
                    jf.setVisible(false);//关闭登陆窗口
                    GroupFileView gf = new GroupFileView(client_in,client_out);
                }else{
                    //System.out.println("the server send F and cilent receive it.");
                    JOptionPane.showMessageDialog(jf, "账号或密码错误，请重新输入！", "提示", JOptionPane.WARNING_MESSAGE);
                }

            } catch (IOException b) {
                b.printStackTrace();
            }

        }else if(e.getSource() == registerButton){
            String un = textField.getText().trim();
            String pwd = pwdField.getText().trim();
            try {
                client_socket = new Socket(ip,port);
                client_out = new PrintStream(client_socket.getOutputStream(),true);
                client_in = new BufferedReader(new InputStreamReader(client_socket.getInputStream()));
                client_out.println(un);
                client_out.println(pwd);
                client_out.println("@action=Register");
                String flag = client_in.readLine().trim();
                if(flag.equals("T")){
                    JOptionPane.showMessageDialog(jf, "注册成功！", "提示", JOptionPane.WARNING_MESSAGE);
                }else{
                    JOptionPane.showMessageDialog(jf, "该用户名已注册！", "提示", JOptionPane.WARNING_MESSAGE);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
