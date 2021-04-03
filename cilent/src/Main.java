import javax.swing.*;
import java.awt.*;


public class Main extends Thread{

    private static int totalTime = 40;
    private static float totalDiff = 0.0f;
    private TextField tf1, tf2, tf3, tf4, tfStart;
    private TextArea taLog;         //日志输出区域 不可以设置颜色相关
    private JButton jbtStart;       //开始按钮


    public static void main(String[] args) {
        //Main cilent = new Main();
        //cilent.run();
        //GroupFileView cilent = new GroupFileView();
        login lg = new login();
    }

}
