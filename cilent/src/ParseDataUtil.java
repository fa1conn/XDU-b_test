public class ParseDataUtil {
    /**
 *
 * 下载文件消息格式： @action=Download["fileName":"fileSize":"result"]
 * 参数说明：
 *        fileName   要下载的文件的名字
 *        fileSize   要下载的文件的大小
 result     下载许可
 */
    public static String getDownFileName(String data){
        return data.substring(data.indexOf("[")+1,data.indexOf(":"));
    }
    public static String getDownFileSize(String data){
        return data.substring(data.indexOf(":")+1,data.lastIndexOf(":"));
    }
    public static String getDownResult(String data){
        return data.substring(data.lastIndexOf(":")+1,data.length()-1);
    }



    /**
     *
     *    上传文件消息格式： @action=Upload["fileName":"fileSize":result]
     参数说明：
     *          fileName   要上传的文件的名字
     *          fileSize   要上传的文件的大小
     result     上传许可
     */
    public static String getUploadFileName(String data){
        return data.substring(data.indexOf("[")+1,data.indexOf(":"));
    }
    public static String getUploadFileSize(String data){
        return data.substring(data.indexOf(":")+1,data.lastIndexOf(":"));
    }
    public static String getUploadResult(String data){
        return data.substring(data.lastIndexOf(":")+1,data.length()-1);
    }

    /**  返回文件列表格式
     @action=GroupFileList["fileName":"fileName":"fileName"...]
     */
    public static String[] getFileList(String data){
        String list = data.substring(data.indexOf("[")+1,data.length()-1);
        return  list.split(":");
    }
}
