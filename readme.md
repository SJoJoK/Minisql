# Minisql

### Update Info 更新信息

| Date       | User | Info                             |
| ---------- | ---- | -------------------------------- |
| 2019-05-18 | ycj  | 新建项目框架，约定命名、注释规范 |
| 2019-05-30 | ycj  | Buffer&BufferManager实现|
| 2019-06-01 | yrj  | Address&FieldType&TableRow&Condition类实现|
| 2019-06-01 | ycj  | BufferManager类增加了make_invalid接口，修改了read_block_from_disk函数 |
| 2019-06-03 | stl  | CatalogManager&Attribute&Index&Table实现&Main test函数&Condition修改 |
| 2019-06-04 | yrj  | Condition&TableRow类修改，RecordManager实现，TestRecord测试程序上传 |


------------------------------

### Coding Style 代码风格约定

```java
	/*
     * [代码风格约定]
     * 函数命名采用下划线法，示例：void build_red_black_tree();
     * 变量命名采用小驼峰，示例：indexValue
     * 每个类的类名必须大写，且需要提供相应的接口，并对接口进行注释
     * 每个类中的方法需要进行注释
     * 单行注释统一采用//
     * 函数类定义的左大括号不换行，示例： void build_red_black_tree() {
     * 变量赋值、运算符预留空格，示例：int a = b + 5;
     * [新增]必要时可以在函数内部自行处理异常抛出
     * [新增]静态常量（宏）定义大写，示例：public static final int SIZE = 1024;
     * */

	/*
     * [示例]
     * function: 主方法
     * param[in]: 命令行参数数组args(如果是void则省略该行)
     * param[out]: (如果是void则省略该行)
     * author: ycj (采用个人姓名缩写)
     * note: 这是一个示例(如果没有特殊说明则省略该行)
     * */
```

---

### Buffer Manager 模块接口说明

##### 约定：块大小为4KB

```java
//构造函数，为缓冲区中所有块申请内存空间
+ BufferManage();

//将块中有效的且脏的数据回写到磁盘
+ public void destruct_buffer_manager();

//将缓冲区中对应文件名为filename的块设置为无效
+ public void make_invalid(String filename);

//将文件中的第ofs块读入到缓冲区中
//如果第ofs块已经在内存中存在，则直接返回对该块的引用
//如果不存在空闲的块则返回null
//如果ofs溢出，则返回对默认块（所有字节都是0）的引用
//如果第ofs块在内存中不存在但是其他必要条件满足，则将该块从文件写入到缓冲区中并返回对该块的引用
+ public Block read_block_from_disk_quote(String filename, int ofs);

//将文件中第ofs块读入到缓冲区中，并返回对该块的下标，若出现错误则返回-1或默认块
+ public int read_block_from_disk(String filename, int ofs);

-----------------------------------------
//从磁盘的filename文件中的第ofs块读取数据到缓冲区的第bid块中
//若操作成功则返回true，否则返回false
+ private boolean read_block_from_disk(String filename, int ofs, int bid);

//把缓冲区的第bid块回写到磁盘中
//若操作成功则返回true，否则返回false
+ private boolean write_block_to_disk(int bid);

//得到缓冲区中空闲的块的编号，若返回-1则没有所有的块都被占用
+ private int get_free_block_id();
```

##### 此外对于缓冲区中的块，提供部分底层的操作接口：

```java
//返回块是否是脏的或设置块是否是脏的
+ public boolean dirty();
+ public void dirty(boolean flag);

//返回块是否是锁住的，或给块上锁/取消锁
+ public boolean lock();
+ public void lock(boolean flag);

//返回块是否是有效的，或设置块是否是有效的
+ public boolean valid();
+ public void valid(boolean flag);

//获得或设置块对应的文件名
+ public String get_filename();
+ public void set_filename(String fname);

//获得或设置块在文件中的偏移量
+ public int get_block_offset();
+ public void set_block_offset(int ofs);

//获得或设置块中存储的数据
+ public byte[] get_block_data();
+ public void set_block_data(byte[] data);

//清空块中所有数据
+ public void set_block_data();

//读入或写入一个整数到offset字节数处
+ public int read_integer(int offset);
+ public boolean write_integer(int offset, int val);

//读入或写入一个单精度浮点数到offset字节数处
+ public float read_float(int offset);
+ public boolean write_float(int offset, float val);

//读入或写入一个长度为length的字符串到offset字节数处
+ public String read_string(int offset, int length);
+ public boolean write_string(int offset, String str);
```

-----------------------
### Address类说明
```java
//成员变量定义
+ private String fileName; //文件名字
+ private int blockOffset; //块在文件内的偏移
+ private int byteOffset;  //字节在块内的偏移
```

### FieldType 类说明
```java
//成员变量定义
+ private String type; // 数值类型,"char","int","float" 三种
+ private int length;  // char类型对应的长度
```

### Condition 类说明
```java
//成员变量定义
+ private String name;  // 属性名字
+ private String value; // 属性的值
+ private String operator; //操作符，"=","<>",">","<",">=","<=" 六种
```

### TableRow 类说明
```java
//成员变量定义
+ private List<String> attributeValue; // 属性值列表

//成员方法
+ public void add_attribute_value(String attributeValue); // 添加一个属性值
+ public String get_attribute_value(int index)； // 得到对应下标的属性值
```

-----------------------

### Attribute 类说明

```java
+ public String attributeName;//字段名称
+ public FieldType type;//字段类型（引用FieldType类）
+ public boolean isUnique;//字段是否唯一（primaryKey）
```

### Index 类实现

```java
+ public String indexName;//索引名称
+ public String tableName;//表名
+ public String attributeName;//索引所在的字段名
+ public int rootNum;//不用关心
+ public int blockNum = 0;//不用关心
```

### Table 类实现

```java
+ public String tableName;//表名
+ public String primaryKey;//主键名
+ public Vector<Attribute> attributeVector;//以Vector形式存储的字段
+ public Vector<Index> indexVector;//以Vector形式存储的索引
+ public int indexNum;//索引数目
+ public int attributeNum;//字段数目
+ public int rowNum;//表中数据的行数（Catalog中不访问）
+ public int rowLength;//表中数据的总长度=字段长度之和
```

### Catalog Manager 模块接口说明

```java
+ public static void initial_catalog() throws IOException;//初始化Catalog
+ public static void store_catalog() throws IOException；//将内存中所有信息的写入文件
+ public static void show_catalog()；//打印内存中table和index的信息

 //通过表名获得Table类对象的信息 
+ public static Table get_table(String tableName)；
+ public static Index get_index(String indexName)；
+ public static String get_primary_key(String tableName)；
+ public static int get_row_length(String tableName)；
+ public static int get_attribute_num(String tableName)；
+ public static int get_row_num(String tableName)；

//字段的判断函数，不一定有用
+ public static boolean is_primary_key(String tableName, String attributeName)；
+ public static boolean is_unique(String tableName, String attributeName)；
+ public static boolean is_index_key(String tableName, String attributeName)；

//获得某个index或attribute的信息
+ public static String get_index_name(String tableName, String attributeName)；
+ public static String get_attribute_name(String tableName, int i)；
+ public static int get_attribute_index(String tableName, String attributeName)；
+ public static FieldType get_attribute_type(String tableName, String attributeName)；

//获得attribute的length和type
+ public static int get_length(String tableName, String attributeName)；
+ public static String get_type(String tableName, int i)；
+ public static int get_length(String tableName, int i)；

//tuple行数的修改
+ public static void add_row_num(String tableName)；
+ public static void delete_row_num(String tableName, int num)；

//更换index
+ public static boolean update_index_table(String indexName, Index tmpIndex)；

//检查attributeVector中是否有特定attribute
+ public static boolean is_attribute_exist(Vector<Attribute> attributeVector, String attributeName)；

//***常用接口*****

//建立表
+ public static boolean create_table(Table newTable)；

//删除表
+ public static boolean drop_table(String tableName)；

//建立索引
+ public static boolean create_index(Index newIndex)；

//删除索引
+ public static boolean drop_index(String indexName)；
```

-----------------------

### Record Manager 模块接口说明

```java
//创建给定表名的表（新建文件），
//若创建成功返回true，创建失败返回false
+ public static boolean create_table(String tableName)；

//删除给定表名的表 (删除文件），
//若删除成功返回true，删除失败返回false
+ public static boolean drop_table(String tableName)；

//select功能函数，给定表名和条件，返回符合该条件的全部记录
//返回记录的属性顺序和创建表时一致
+ public static Vector<TableRow> select(String tableName, Vector<Condition> conditions)；

//insert功能函数，给定表名和待插入记录，返回该记录所插入的地址
//插入记录中属性顺序必须和创建表时属性的顺序一致
+ public static Address insert(String tableName, TableRow data)；

//delete功能函数，给定表名和条件，删除表中符合条件的记录，返回删除记录的个数
+ public static int delete(String tableName, Vector<Condition> conditions)；

//select功能函数，给定一系列地址，返回地址对应的全部记录
//返回记录的属性顺序和创建表时一致
+ public static Vector<TableRow> select(Vector<Address> address)；

//delete功能函数，给定一系列地址，删除地址对应的记录，返回删除记录的个数
+ public static int delete(Vector<Address> address)；

//project功能函数，给定表名，查询结果和投影属性名称，返回投影后的记录结果
//投影属性名称顺序没有要求，查询结果的属性顺序必须和创建表时的属性一致
+ public static Vector<TableRow> project(String tableName, Vector<TableRow> result, Vector<String> projectName)；

//保存函数，将当前操作的记录保存到磁盘中
//程序结束前调用，其将内部缓冲区的块写入磁盘中
+ public static void store_record()；
```

-----------------------
