adb kill-server
adb start-server

netstat -ano | findstr "5037"
adb shell am start -D -n com.example.xman.easymobile/.MainActivity
adb connect 127.0.0.1:5554

获取系统版本：adb shell getprop ro.build.version.release
获取系统api版本：adb shell getprop ro.build.version.sdk


ida:结构体指针：`JNIEnv*`
[toc]
## 安卓可执行文件
Dalvik虚拟机作为运行平台，DEX作为可执行文件格式，smali/baksmali则是Dalvik VM可执行文件的汇编器/反汇编器。
![image](/picture/034.png)
### android程序生成步骤
APK是发布前打包的文件，实际上就是zip压缩包，解压后发现由一些图片资源和文件组成，还有一个`classes.dex`文件，这就是可执行文件
打包过程如下图：
![image](/picture/035.png)
#### 第一步
打包资源文件，生成R.java文件
aapt是打包资源的工具，位于android-sdk/platform目录下
过程：
1)检查AndroidManifest.xml合法性
2)makeFileResources()对res目录下的资源子目录进行处理
3)compileResourceFile()编译res和asserts目录下的资源并生成resources.arsc文件，该函数最后会调用parseAndAddEntry()生成R.java文件
4）compileXmlFile()对res目录下的xml分别进行编译，xml被简单`加密`
5)将所有资源与编译生成的resources.arsc文件和AndroidManifest.xml打包压缩成resources.ap_文件
#### 第二步
实用aidl处理aidl文件，生成相应的java文件，如果没有aidl文件可跳过这一步
>AIDL:Android Interface Definition Language,即Android接口定义语言。
Android系统中的进程之间不能共享内存，因此，需要提供一些机制在不同进程之间进行数据通信。
为了使其他的应用程序也可以访问本应用程序提供的服务，Android系统采用了远程过程调用（Remote Procedure Call，RPC）方式来实现。与很多其他的基于RPC的解决方案一样，Android使用一种接口定义语言（Interface Definition Language，IDL）来公开服务的接口。我们知道4个Android应用程序组件中的3个（Activity、BroadcastReceiver和ContentProvider）都可以进行跨进程访问，另外一个Android应用程序组件Service同样可以。因此，可以将这种可以跨进程访问的服务称为AIDL（Android Interface Definition Language）服务。
#### 第三步
使用javac编译工程源代码，生成class文件
实际开发中，可能使用android NDK编译native代码，可能的话，这一步还得使用android NDK编译C/C++代码，也可能在第一步第二步执行该操作

知识补充：
NDK即Native Development Kit，众所周知，Android程序运行在Dalvik虚拟机中，NDK允许用户使用类似C / C++之类的原生代码语言执行部分程序。
NDK是一系列工具的集合。它提供了一系列的工具，帮助开发者快速开发C（或C++）的动态库，并能自动将so和java应用一起打包成apk(AndroidPackage的缩写，Android安装包)。这些工具对开发者的帮助是巨大的。它集成了交叉编译器，并提供了相应的mk文件隔离CPU、平台、ABI等差异，开发人员只需要简单修改mk文件（指出“哪些文件需要编译”、“编译特性要求”等），就可以创建出so。它可以自动地将so和Java应用一起打包，极大地减轻了开发人员的打包工作

为什么使用NDK
(1)代码的保护。由于apk的java层代码很容易被反编译，而C/C++库反汇难度较大。
(2)可以方便地使用现存的开源库。大部分现存的开源库都是用C/C++代码编写的。
(3)提高程序的执行效率。将要求高性能的应用逻辑使用C开发，从而提高应用程序的执行效率。
(4)便于移植。用C/C++写得库可以方便在其他的嵌入式平台上再次使用。

JNI是Java Native Interface的缩写，中文为JAVA本地调用。从Java1.1开始，Java Native Interface(JNI)标准成为java平台的一部分。

JNI是java语言提供的Java和C/C++相互沟通的机制，Java可以通过JNI调用本地的C/C++代码，本地的C/C++的代码也可以调用java代码。JNI 是本地编程接口，Java和C/C++互相通过的接口。Java通过C/C++使用本地的代码的一个关键性原因在于C/C++代码的高效性。
![image](/picture/036.png)

#### 第四步
工具dx转换所有的class文件，生成classes.dex文件(dx的主要工作是将java字节码转换为Dalvik字节码，压缩常量池，消除冗余信息)

#### 第五步
打包生成APK文件，工具为apkbuilder
构建ApkBuilder类，以包含resources.arsc的文件为基础生成apk文件，文件一般以ap_结尾，然后调用addSourceFolder()添加资源，包括res目录与assets目录中的文件，之后用addResourceFromJar往apk中写入依赖库，然后调用addNativeLibraries()添加libc目录下 的Native库

#### apk签名
调试程序时的签名或者发布程序时签名
jarsigner或者signapk工具
#### 对齐处理
偏移为4字节的整倍数

### dex文件格式
#### 数据结构
sleb128	有符号 LEB128，可变长度
uleb128	无符号 LEB128，可变长度
uleb128p1	无符号 LEB128 加 1，可变长度
>LEB128
表示任意有符号或无符号整数的可变长度编码。在`.dex`文件中，LEB128 仅用于对 32 位数字进行编码。
下面的东西暂时没看懂
![image](/picture/037.png)
#### dex文件整体结构
由多个结构体组合而成
文件结构如下：
```
dex header
string_ids
type_ids
proto_ids
field_ids
method_ids
class_def
data
link_data
```
##### dex header
header是DEX文件头，包含magic字段、alder32校验值、SHA-1哈希值、string_ids的个数以及偏移地址等。
结构体如下，占用0x70字节
```c
struct DexHeader {
    u1  magic[8];           /* includes version number */  
    u4  checksum;           /* adler32 checksum */  
    u1  signature[kSHA1DigestLen]; /* SHA-1 hash */
    u4  fileSize;           /* length of entire file */
    u4  headerSize;         /* offset to start of next section */
    u4  endianTag;
    u4  linkSize;
    u4  linkOff;
    u4  mapOff;
    u4  stringIdsSize;
    u4  stringIdsOff;
    u4  typeIdsSize;
    u4  typeIdsOff;
    u4  protoIdsSize;
    u4  protoIdsOff;
    u4  fieldIdsSize;
    u4  fieldIdsOff;
    u4  methodIdsSize;
    u4  methodIdsOff;
    u4  classDefsSize;
    u4  classDefsOff;
    u4  dataSize;
    u4  dataOff;
};
```
`magic`:为固定值`64 65 78 0A 30 33 35 00`
`checksum`:文件校验码，使用alder32算法校验文件除去magic、checksum外余下的所有文件区域，用于检查文件错误
`signature`:使用 SHA-1算法hash除去magic，checksum和signature外余下的所有文件区域 ，用于唯一识别本文件
`fileSize`：DEX文件的长度。
`headerSize`：header大小，一般固定为0x70字节
`endianTag`：指定了DEX运行环境的cpu字节序，预设值ENDIAN_CONSTANT等于0x12345678，表示默认采用Little-Endian字节序。
`linkSize`和`linkOff`：指定链接段的大小与文件偏移，大多数情况下它们的值都为0。`link_size`：LinkSection大小,如果为0则表示该DEX文件`不是`静态链接。link_off用来表示LinkSection距离DEX头的`偏移地址`，如果LinkSize为0，此值也会为0。
`mapOff`：DexMapList结构的文件偏移。
`stringIdsSize`和`stringIdsOff`：`DexStringId结构`的数据段大小与文件偏移。
后面的含义都一样
##### DexMapList区段（大纲）
Dalvik虚拟机解析DEX文件的内容，最终将其映射成DexMapList数据结构，它实际上包含所有其他区段的结构大纲
结构体如下
```c
struct DexMapList {
    u4 size;               /* DexMapItem的个数 */
    DexMapItem list[1];    /* DexMapItem的结构 */
};

struct DexMapItem {   
    u2 type;      /* kDexType开头的类型 */
    u2 unused;  /* 未使用，用于字节对齐，一般为0*/
    u4 size;    /* type指定类型的个数，它们在dex文件中连续存放 */
    u4 offset;  /* 指定类型数据的文件偏移 */
};

/* type字段为一个枚举常量，通过类型名称很容易判断它的具体类型。 */
/* map item type codes */
enum {
    kDexTypeHeaderItem               = 0x0000,
    kDexTypeStringIdItem             = 0x0001,
    kDexTypeTypeIdItem               = 0x0002,
    kDexTypeProtoIdItem              = 0x0003,
    kDexTypeFieldIdItem              = 0x0004,
    kDexTypeMethodIdItem             = 0x0005,
    kDexTypeClassDefItem             = 0x0006,
    kDexTypeMapList                  = 0x1000,
    kDexTypeTypeList                 = 0x1001,
    kDexTypeAnnotationSetRefList     = 0x1002,
    kDexTypeAnnotationSetItem        = 0x1003,
    kDexTypeClassDataItem            = 0x2000,
    kDexTypeCodeItem                 = 0x2001,
    kDexTypeStringDataItem           = 0x2002,
    kDexTypeDebugInfoItem            = 0x2003,
    kDexTypeAnnotationItem           = 0x2004,
    kDexTypeEncodedArrayItem         = 0x2005,
    kDexTypeAnnotationsDirectoryItem = 0x2006,
};
```
##### DexStringId区段（字符串）
结构如下：
```c
struct DexStringId {
    u4 stringDataOff;   /* 字符串数据偏移 */
}
```
这个区段中包含了DEX文件中用到的所有字符串
##### DexTypeId区段（类名/类型名称字符串）
结构如下：
```c
struct DexTypeId {
    u4 descriptorIdx;    /* 指向 DexStringId列表的索引 */
};
```
descriptorIdx为指向DexStringId列表的索引，它对应的字符串代表了具体类的类型（DEX文件中用到的所有基本数据类型和类的名称）
##### DexProtoId区段（方法声明=返回类型 + 参数列表）
结构如下：
```c
struct DexProtoId {
    u4 shortyIdx;   /* 指向DexStringId列表的索引 */
    u4 returnTypeIdx;   /* 指向DexTypeId列表的索引 */
    u4 parametersOff;   /* 指向DexTypeList的偏移 */
}

struct DexTypeList {
    u4 size;             /* 接下来DexTypeItem的个数 */
    DexTypeItem list[1]; /* DexTypeItem结构 */
};

struct DexTypeItem {
    u2 typeIdx;    /* 指向DexTypeId列表的索引 */
};
```
`shortyIdx`：方法声明字符串，具体而言是由方法的返回类型与参数列表组成的一个字符串，并且返回类型位于参数列表的前面
`returnTypeIdx`：方法返回类型，指向DexTypeId列表
`parametersOff`：指向一个DexTypeList结构体，存放了方法的参数类型
`size`：DexTypeItem的个数，即参数的数量
`list`：指向size个DexTypeItem项，每一项代表方法的一个参数
`typeIdx`：指向DexTypeId列表，最终指向参数类型的字符串

## 静态分析
程序中使用到的Activity都需要在AndroidManifest.xml文件手动声明，声明Activity使用activity标签，`android:label`指定Activity标题，`android:name`指定具体的Activity类。`intent-filter`指定了Activity的启动意图，`android.intent.action.MAIN`表示这个Activity是程序的主活动，若没有指定，则不会出现图标。`android.intent.category.LAUNCHER`表示这个Activity可以通过LAUNCHER启动，若没有指定，则程序列表中是不可见的。
### 搜索字符串
程序用的字符串会存储在`String.xml`文件或硬编码到`程序代码`中。
前者字符串会以id形式访问，搜索字符串id即可
后者在反汇编中直接搜索字符串即可
### 特征函数法
调用Android SDK 中提供相关的API函数来完成
比如弹出提示信息就需要用Toast.MakeText().Show()，在反汇编中直接搜索Toast就应该很快能定位到调用代码，需要一定的开发知识
### 代码注入法
动态调试法，原理是手动修改apk文件的反汇编代码，加入Log输出，配合LogCat查看程序执行到特定点时的状态数据
### 栈跟踪法
输出运行时栈跟踪信息，查看栈上函数的调用序列理解方法的执行流程
### smali文件格式
`开头`:
.class <访问权限>[修饰关键字]<类名>
.super <父类名>
.source <源文件名>

`静态字段`：
`#static fields`
`.field <访问权限>static[修饰关键字]<字段名>:<字段类型>`    (访问权限可以是public,private,protected，修饰关键字描述了字段的其他属性，如synthetic)

`实例字段`：
`#instance fields`
`.field <访问权限>static[修饰关键字]<字段名>:<字段类型>`

`直接方法`:
```
# direct methods
.method <访问权限>[修饰关键字]<方法原型>
            .registers n   #用到寄存器n个
            ...
```
方法原型描述了方法的名称，参数和返回值
举个实际的例子：
```
.method private static getCount(II)V       
    .registers 2      
    .param p0, "x"    
    .param p1, "y"    
    .prologue       
    .line 28     
    return-void       
.end method
```
方法名是getCount，有两个参数，都是int类型的（I代表int），V代表无返回值
第三行和第四行为方法的参数，每有一个参数，就写一个参数，此处有两个参数
第五行为 方法的主体部分（.prologue）
第六行指定了该处指令在源代码中的行号，这里是从java源码中第28行开始

`类实现接口`:
如果一个类实现了接口，会在smali 文件中使用“.implements ”指令指出,相应的格式声明如下:
```
# interfaces
.implements <接口名>
```

`类使用注解`:
```
.annotation [ 注解属性] < 注解类名>  
    [ 注解字段 =  值]  
.end annotation  
```
注解的作用范围可以是类、方法或字段。如果注解的作用范围是类，“.annotation ”指令会直接定义在smali 文件中，如果是方法或字段，“.annotation ”指令则会包含在方法或字段定义中。
```
.field public sayWhat:Ljava/lang/String;           
    .annotation runtime Lcom/droider/anno/MyAnnoField;  
        info = ”Hello my friend”  
    .end annotation  
.end field
```
String 类型 它使用了 com.droider.anno.MyAnnoField 注解，注解字段info值为“Hello my friend”  
转换成java代码为：
```java
@com.droider.anno.MyAnnoField(info = ”Hello my friend”)
public String sayWhat;
```
>注解(Annotation)相当于一种标记，在程序中加入注解就等于为程序打上某种标记，没有加，则等于没有任何标记，以后，javac编译器、开发工具和其他程序可以通过反射来了解你的类及各种元素上有无何种标记，看你的程序有什么标记，就去干相应的事，标记可以加在包、类，属性、方法，方法的参数以及局部变量上。