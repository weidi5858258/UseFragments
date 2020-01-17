package com.weidi.usefragments.business.contents;

import java.util.LinkedHashMap;
import java.util.Map;

/***
 Created by root on 19-7-20.
 */

public class Contents {

    public static Map<String, String> movieMap = new LinkedHashMap<>();
    private static String mPath;
//    private static final String PATH = "/storage/emulated/0/Download/";
//    private static final String PATH = "/storage/1532-48AD/Android/data/com.weidi.usefragments/files/Movies/";
    private static final String PATH = "/storage/2430-1702/BaiduNetdisk/video/";
//    private static final String PATH = "/storage/emulated/0/BaiduNetdisk/video/";

    static {
        // 本地视频
        movieMap.put(
                "Amazing_Picture_Quality_8K_HDR_60FPS_Demo",
                PATH + "Amazing_Picture_Quality_8K_HDR_60FPS_Demo.webm");
        movieMap.put(
                "痞子英雄2-黎明升起-local",
                PATH + "痞子英雄2-黎明升起.mp4");

        // 本地视频
        movieMap.put(
                "复仇者联盟4-终局之战-local",
                PATH + "复仇者联盟4-终局之战.mp4");
        movieMap.put(
                "三傻大闹宝莱坞-local",
                PATH + "三傻大闹宝莱坞.mp4");
        movieMap.put(
                "index.m3u8",
                PATH + "index.m3u8.mp4");
        movieMap.put(
                "videoplayback",
                PATH + "videoplayback.mp4");
        movieMap.put(
                "地狱男爵-血皇后崛起-local",
                PATH + "地狱男爵-血皇后崛起.mp4");
        movieMap.put(
                "流浪的地球",
                PATH + "流浪的地球.mp4");
        movieMap.put(
                "08_mm-MP4-H264_720x400_2997_AAC-LC_192_48",
                PATH + "08_mm-MP4-H264_720x400_2997_AAC-LC_192_48.mp4");
        movieMap.put(
                "shape_of_my_heart",
                PATH + "shape_of_my_heart.mp4");
        movieMap.put(
                "jlbhd5",
                PATH + "jlbhd5.mp4");
        movieMap.put(
                "test",
                PATH + "test.mp4");
        movieMap.put(
                "05",
                PATH + "05.mp4");

        movieMap.put(
                "第4节标准流输入输出",
                PATH + "/c_plus_plus_面向对象编程/第4节标准流输入输出.mp4");
        movieMap.put(
                "第5节标准库string类型",
                PATH + "/c_plus_plus_面向对象编程/第5节标准库string类型.mp4");
        movieMap.put(
                "第6节标准库vector类型",
                PATH + "/c_plus_plus_面向对象编程/第6节标准库vector类型.mp4");
        movieMap.put(
                "第7节再谈函数",
                PATH + "/c_plus_plus_面向对象编程/第7节再谈函数.mp4");
        movieMap.put(
                "第8节面向对象基础",
                PATH + "/c_plus_plus_面向对象编程/第8节面向对象基础.mp4");
        movieMap.put(
                "第9节定义类和对象",
                PATH + "/c_plus_plus_面向对象编程/第9节定义类和对象.mp4");
        movieMap.put(
                "第10节类和对象的使用",
                PATH + "/c_plus_plus_面向对象编程/第10节类和对象的使用.mp4");
        movieMap.put(
                "第11节构造函数和析构函数",
                PATH + "/c_plus_plus_面向对象编程/第11节构造函数和析构函数.mp4");
        movieMap.put(
                "第12节this指针和复制构造函数",
                PATH + "/c_plus_plus_面向对象编程/第12节this指针和复制构造函数.mp4");
        movieMap.put(
                "第13节类的静态成员",
                PATH + "/c_plus_plus_面向对象编程/第13节类的静态成员.mp4");
        movieMap.put(
                "第14节const对象和const成员",
                PATH + "/c_plus_plus_面向对象编程/第14节const对象和const成员.mp4");
        movieMap.put(
                "第15节友元",
                PATH + "/c_plus_plus_面向对象编程/第15节友元.mp4");
        movieMap.put(
                "第16节运算符重载基础",
                PATH + "/c_plus_plus_面向对象编程/第16节运算符重载基础.mp4");
        movieMap.put(
                "第17节运算符重载规则",
                PATH + "/c_plus_plus_面向对象编程/第17节运算符重载规则.mp4");
        movieMap.put(
                "第18节重载二元和一元运算符",
                PATH + "/c_plus_plus_面向对象编程/第18节重载二元和一元运算符.mp4");
        movieMap.put(
                "第19节流插入提取运算符和类型转换",
                PATH + "/c_plus_plus_面向对象编程/第19节流插入提取运算符和类型转换.mp4");
        movieMap.put(
                "第20节定义自己的String类1",
                PATH + "/c_plus_plus_面向对象编程/第20节定义自己的String类1.mp4");
        movieMap.put(
                "第21节定义自己的string类2",
                PATH + "/c_plus_plus_面向对象编程/第21节定义自己的string类2.mp4");
        movieMap.put(
                "第22节继承与派生基础",
                PATH + "/c_plus_plus_面向对象编程/第22节继承与派生基础.mp4");
        movieMap.put(
                "第23节派生类使用",
                PATH + "/c_plus_plus_面向对象编程/第23节派生类使用.mp4");
        movieMap.put(
                "第24节多态",
                PATH + "/c_plus_plus_面向对象编程/第24节多态.mp4");
        movieMap.put(
                "第25节文件操作",
                PATH + "/c_plus_plus_面向对象编程/第25节文件操作.mp4");
        movieMap.put(
                "第26节STL顺序容器",
                PATH + "/c_plus_plus_面向对象编程/第26节STL顺序容器.mp4");
        movieMap.put(
                "第27节STL关联容器和容器适配器",
                PATH + "/c_plus_plus_面向对象编程/第27节STL关联容器和容器适配器.mp4");
        movieMap.put(
                "第28节STL迭代器和算法",
                PATH + "/c_plus_plus_面向对象编程/第28节STL迭代器和算法.mp4");
        movieMap.put(
                "第29节STL实例",
                PATH + "/c_plus_plus_面向对象编程/第29节STL实例.mp4");

        // {csd-1=java.nio.HeapByteBuffer[pos=0 lim=8 cap=8], mime=video/avc, frame-rate=24, track-id=2, profile=2, width=1280, height=720, max-input-size=151238, durationUs=6227930041, csd-0=java.nio.HeapByteBuffer[pos=0 lim=41 cap=41], bitrate-mode=0, level=512}
        // csd-0: {0, 0, 0, 1, 103, 77, 64, 31, -24, -128, 40, 2, -35, -128, -87, 1, 1, 1, 64, 0, 0, -6, 64, 0, 46, -32, 56, 24, 0, 21, 92, -64, 1, 83, -38, 76, 48, 15, -116, 24, -119}
        // csd-1: {0, 0, 0, 1, 104, -21, -20, -78}
        movieMap.put(
                "军情五处-利益之争",
                "http://vip.zuiku8.com/1808/%E5%86%9B%E6%83%85%E4%BA%94%E5%A4%84%EF%BC%9A%E5%88%A9%E7%9B%8A%E4%B9%8B%E4%BA%89.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        // {csd-1=java.nio.HeapByteBuffer[pos=0 lim=9 cap=9], mime=video/avc, frame-rate=60, track-id=1, profile=8, width=1280, height=720, max-input-size=148263, durationUs=7247083333, csd-0=java.nio.HeapByteBuffer[pos=0 lim=33 cap=33], bitrate-mode=0, level=1024}
        // {0, 0, 0, 1, 103, 100, 0, 32, -84, -39, 64, 80, 5, -69, -1, 3, 27, 3, 26, 16, 0, 0, 3, 0, 16, 0, 0, 7, -128, -15, -125, 25, 96}
        // {0, 0, 0, 1, 104, -21, -20, -78, 44}
        // {crop-top=0, crop-right=1279, color-format=19, height=720, color-standard=1, crop-left=0, color-transfer=3, stride=1280, mime=video/raw, slice-height=720, width=1280, color-range=2, crop-bottom=719}
        movieMap.put(
                "地狱男爵-血皇后崛起",
                "http://anning.luanniao-zuida.com/1907/%E5%9C%B0%E7%8B%B1%E7%94%B7%E7%88%B5%EF%BC%9A%E8%A1%80%E7%9A%87%E5%90%8E%E5%B4%9B%E8%B5%B7.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "痞子英雄2-黎明升起",
                "http://download.xunleizuida.com/1905/%E7%97%9E%E5%AD%90%E8%8B%B1%E9%9B%842%EF%BC%9A%E9%BB%8E%E6%98%8E%E5%8D%87%E8%B5%B7.HD1280%E9%AB%98%E6%B8%85%E5%9B%BD%E8%AF%AD%E4%B8%AD%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "生死时速",
                "http://xunlei.jingpin88.com/20171028/6WQ5SFS2/mp4/6WQ5SFS2.mp4");
        movieMap.put(
                "三傻大闹宝莱坞",
                "http://xunleib.zuida360.com/1811/%E4%B8%89%E5%82%BB%E5%A4%A7%E9%97%B9%E5%AE%9D%E8%8E%B1%E5%9D%9E.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "焦点",
                "http://vip.zuiku8.com/1807/%E7%84%A6%E7%82%B92015.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "中国机长",
                "http://ok.renzuida.com/1912/ZG%E6%9C%BA%E9%95%BF.HD1280%E9%AB%98%E6%B8%85%E5%9B%BD%E8%AF%AD%E4%B8%AD%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "生死时速2-海上惊情",
                "http://xunlei.jingpin88.com/20171028/2JJsfDHh/mp4/2JJsfDHh.mp4");
        movieMap.put(
                "阿丽塔-战斗天使",
                "http://xunlei.xiazai-zuida.com/1906/AL%E5%A1%94%EF%BC%9A%E6%88%98%E6%96%97%E5%A4%A9%E4%BD%BF.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "海王",
                "http://xunlei.zuidaxunlei.com/1902/%E6%B5%B7W.HD1280%E9%AB%98%E6%B8%85%E5%9B%BD%E8%AF%AD%E4%B8%AD%E5%AD%97%E9%9F%A9%E7%89%88.mp4");
        movieMap.put(
                "黑豹",
                "http://xunleib.zuida360.com/1805/H%E8%B1%B9.BD1280%E9%AB%98%E6%B8%85%E5%9B%BD%E8%AF%AD%E7%89%B9%E6%95%88%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "生化危机5-惩罚",
                "http://xunlei.jingpin88.com/20171105/O3dRhCpO/mp4/O3dRhCpO.mp4");
        movieMap.put(
                "生化危机-终章",
                "http://zuidaziyuan.com/1704/%E7%94%9F%E5%8C%96%E5%8D%B1%E6%9C%BA6%EF%BC%9A%E7%BB%88%E7%AB%A0.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "疾速备战",
                "http://xunlei.xiazai-zuida.com/1907/J%E9%80%9F%E5%A4%87%E6%88%98.HD%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "黄飞鸿之五-龙城歼霸",
                "http://anning.luanniao-zuida.com/1907/%E9%BB%84%E9%A3%9E%E9%B8%BF%E4%B9%8B%E4%BA%94%EF%BC%9A%E9%BE%99%E5%9F%8E%E6%AD%BC%E9%9C%B8.DVD%E5%9B%BD%E8%AF%AD%E4%B8%AD%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "宝莱坞机器人2.0-重生归来",
                "http://xunlei.xiazai-zuida.com/1906/BL%E5%9D%9E%E6%9C%BA%E5%99%A8%E4%BA%BA2.0%EF%BC%9A%E9%87%8D%E7%94%9F%E5%BD%92%E6%9D%A5.HD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "少年当自强",
                "http://okxxxzy.xzokzyzy.com/20190718/27_08326ade/%E5%B0%91%E5%B9%B4%E5%BD%93%E8%87%AA%E5%BC%BA.mp4");
        movieMap.put(
                "极速复仇",
                "http://xz3-13.okzyxz.com/20190527/1282_5a7f1f18/%E6%9E%81%E9%80%9F%E5%A4%8D%E4%BB%87.mp4");
        movieMap.put(
                "死侍2",
                "http://xunleib.zuida360.com/1808/%E6%AD%BBshi2.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "谍影重重4",
                "http://xunlei.jingpin88.com/20171027/ZkYVjDJC/mp4/ZkYVjDJC.mp4");
        movieMap.put(
                "生死狙击",
                "http://xunlei.jingpin88.com/20171026/0L4MjcwA/mp4/0L4MjcwA.mp4");
        movieMap.put(
                "再生侠",
                "http://xunleib.zuida360.com/1805/%E5%86%8D%E7%94%9F%E4%BE%A0.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "追龙2",
                "http://xunlei.xiazai-zuida.com/1906/Zhuilong2%EF%BC%9A%E8%B4%BC%E7%8E%8B.HD1280%E9%AB%98%E6%B8%85%E5%9B%BD%E8%AF%AD%E4%B8%AD%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "黑衣人-全球追缉",
                "http://okxxzy.xzokzyzy.com/20190721/23773_0455e097/%E9%BB%91%E8%A1%A3%E4%BA%BA4%E9%9F%A9%E7%89%88%E4%B8%AD%E5%AD%97.mp4");
        movieMap.put(
                "秦明-生死语者",
                "http://anning.luanniao-zuida.com/1907/%E7%A7%A6%E6%98%8E%C2%B7%E7%94%9F%E6%AD%BB%E8%AF%AD%E8%80%85.HD1280%E9%AB%98%E6%B8%85%E5%9B%BD%E8%AF%AD%E4%B8%AD%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "复仇者联盟4-终局之战",
                "http://xunlei.zuidaxunlei.com/1907/FCZ%E8%81%94%E7%9B%9F4%EF%BC%9A%E7%BB%88%E5%B1%80%E4%B9%8B%E6%88%98.BD1280%E9%AB%98%E6%B8%85%E7%89%B9%E6%95%88%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
                //"http://okxxzy.xzokzyzy.com/20190728/23849_9b8076fd/AvengersEndgame.2019.1080p.mp4");
                //"http://xunlei.zuidaxunlei.com/1907/FCZ%E8%81%94%E7%9B%9F4%EF%BC%9A%E7%BB%88%E5%B1%80%E4%B9%8B%E6%88%98.HD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "忍者刺客",
                "http://download.xunleizuida.com/1904/%E5%BF%8D%E8%80%85%E5%88%BA%E5%AE%A2.HD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "怪兽之岛",
                "http://okxxzy.xzokzyzy.com/20190727/23842_f4bc2950/%E6%80%AA%E5%85%BD%E4%B9%8B%E5%B2%9B.mp4");
        movieMap.put(
                "魔精攻击",
                "http://anning.luanniao-zuida.com/1907/%E9%AD%94%E7%B2%BE%E6%94%BB%E5%87%BB.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "城市猎人2019",
                "http://xunlei.zuidaxunlei.com/1907/%E5%9F%8E%E5%B8%82%E7%8C%8E%E4%BA%BA2019.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "机器人病毒危机",
                "http://vip.zuiku8.com/1809/%E6%9C%BA%E5%99%A8%E4%BA%BA%E7%97%85%E6%AF%92%E5%8D%B1%E6%9C%BA.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "哥斯拉2-怪兽之王",
                "http://okxxzy.xzokzyzy.com/20190726/23836_8f001b98/%E5%93%A5%E6%96%AF%E6%8B%892%E9%9F%A9%E7%89%88.mp4");
        movieMap.put(
                "惊奇队长",
                "http://anning.luanniao-zuida.com/1907/%E6%83%8A%E5%A5%87%E9%98%9F%E9%95%BF.BD1280%E9%AB%98%E6%B8%85%E5%9B%BD%E8%AF%AD%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "魔童",
                "http://xunlei.zuidaxunlei.com/1908/%E9%AD%94%E7%AB%A5.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "马里布鲨鱼攻击",
                "http://xunlei.jingpin88.com/20171115/wuYhRcLi/mp4/wuYhRcLi.mp4");
        movieMap.put(
                "零号病人",
                "http://vip.zuiku8.com/1808/%E9%9B%B6%E5%8F%B7%E7%97%85%E4%BA%BA.HD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "决战异世界",
                "http://download.xunleizuida.com/1904/%E5%86%B3%E6%88%98%E5%BC%82%E4%B8%96%E7%95%8C.HD1280%E9%AB%98%E6%B8%85%E5%9B%BD%E8%AF%AD%E4%B8%AD%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "大轰炸",
                "http://xunleib.zuida360.com/1810/%E5%A4%A7%E8%BD%B0%E7%82%B8.HD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "特勤精英之逃出生天",
                "http://download.xunleizuida.com/1904/%E7%89%B9%E5%8B%A4%E7%B2%BE%E8%8B%B1%E4%B9%8B%E9%80%83%E5%87%BA%E7%94%9F%E5%A4%A9.HD1280%E9%AB%98%E6%B8%85%E5%9B%BD%E8%AF%AD%E4%B8%AD%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "特勤精英之生死救援",
                "http://download.xunleizuida.com/1904/%E7%89%B9%E5%8B%A4%E7%B2%BE%E8%8B%B1%E4%B9%8B%E7%94%9F%E6%AD%BB%E6%95%91%E6%8F%B4.HD1280%E9%AB%98%E6%B8%85%E5%9B%BD%E8%AF%AD%E4%B8%AD%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "扫毒2天地对决",
                "http://xunlei.xiazai-zuida.com/1908/%E6%89%AB%E6%AF%922.HD1280%E9%AB%98%E6%B8%85%E5%9B%BD%E8%AF%AD%E4%B8%AD%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "犬王",
                "http://xunlei.xiazai-zuida.com/1908/%E7%8A%AC%E7%8E%8B.HD1280%E9%AB%98%E6%B8%85%E5%9B%BD%E8%AF%AD%E7%89%88.mp4");
        movieMap.put(
                "X战警-黑凤凰",
                "http://okzy.xzokzyzy.com/20190830/14797_2afe866c/X%E6%88%98%E8%AD%A6%EF%BC%9A%E9%BB%91%E5%87%A4%E5%87%B0.Dark.Phoenix.2019.BD1080P.X264.AAC.English.CHS-ENG.mp4");
        movieMap.put(
                "蜘蛛侠-英雄远征",
                "http://okzy.xzokzyzy.com/20190912/14951_fdda0cb4/YXYZ.2019.1080P.%E5%9B%BD%E8%AF%AD%E4%B8%AD%E5%AD%97.mp4");
                //"http://anning.luanniao-zuida.com/1909/ZZ%E4%BE%A0%EF%BC%9A%E8%8B%B1%E9%9B%84%E8%BF%9C%E5%BE%81.HD1280%E9%AB%98%E6%B8%85%E5%9B%BD%E8%AF%AD%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "速度与激情-特别行动",
                "http://down.phpzuida.com/1909/%E9%80%9FD%E4%B8%8EJ%E6%83%85%EF%BC%9A%E7%89%B9%E5%88%AB%E8%A1%8C%E5%8A%A8.HD%E9%AB%98%E6%B8%85%E4%B8%AD%E5%AD%97%E9%9F%A9%E7%89%88.mp4");
        movieMap.put(
                "毁灭战士-灭绝",
                "http://caizi.meizuida.com/1909/%E6%AF%81%E7%81%AD%E6%88%98%E5%A3%AB%EF%BC%9A%E7%81%AD%E7%BB%9D.HD%E9%AB%98%E6%B8%85%E4%B8%AD%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "",
                "");
        movieMap.put(
                "",
                "");
        movieMap.put(
                "",
                "");
        movieMap.put(
                "",
                "");
        movieMap.put(
                "",
                "");
        movieMap.put(
                "",
                "");

        movieMap.put(
                "突袭1",
                "http://xunleib.zuida360.com/1806/%E7%AA%81%E8%A2%AD.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "突袭2",
                "http://xunleib.zuida360.com/1806/%E7%AA%81%E8%A2%AD2%EF%BC%9A%E6%9A%B4%E5%BE%92.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");

        movieMap.put(
                "伸冤人1",
                "http://xunleib.zuida360.com/1805/%E4%BC%B8%E5%86%A4%E4%BA%BA.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "伸冤人2",
                "http://vip.zuiku8.com/1810/%E4%BC%B8%E5%86%A4%E4%BA%BA2.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E5%AD%97%E7%89%88.mp4");

        movieMap.put(
                "碟中谍1",
                "http://xunleib.zuida360.com/1804/%E7%A2%9F%E4%B8%AD%E8%B0%8D1.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "碟中谍2",
                "http://xunleib.zuida360.com/1804/%E7%A2%9F%E4%B8%AD%E8%B0%8D2.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "碟中谍3",
                "http://xunleib.zuida360.com/1804/%E7%A2%9F%E4%B8%AD%E8%B0%8D3.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "碟中谍4",
                "http://xunleib.zuida360.com/1804/%E7%A2%9F%E4%B8%AD%E8%B0%8D4.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "碟中谍5-神秘国度",
                "http://xunlei.jingpin88.com/20171026/mVnlRJDm/mp4/mVnlRJDm.mp4");
        movieMap.put(
                "碟中谍6-全面瓦解",
                "http://vip.zuiku8.com/1809/D%E4%B8%AD%E8%B0%8D6%EF%BC%9A%E5%85%A8%E9%9D%A2%E7%93%A6%E8%A7%A3.HD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E9%9F%A9%E7%89%88.mp4");

        movieMap.put(
                "哈利波特1-神秘的魔法石",
                "http://ying.zuidaziyuan.com/1708/%E5%93%88%E5%88%A9%E6%B3%A2%E7%89%B91%EF%BC%9A%E7%A5%9E%E7%A7%98%E7%9A%84%E9%AD%94%E6%B3%95%E7%9F%B3.BD1024%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "哈利波特2-消失的密室",
                "http://ying.zuidaziyuan.com/1708/%E5%93%88%E5%88%A9%E6%B3%A2%E7%89%B92%EF%BC%9A%E6%B6%88%E5%A4%B1%E7%9A%84%E5%AF%86%E5%AE%A4.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97.mp4");
        movieMap.put(
                "哈利波特3-阿兹卡班的囚徒",
                "http://ying.zuidaziyuan.com/1708/%E5%93%88%E5%88%A9%E6%B3%A2%E7%89%B93%EF%BC%9A%E9%98%BF%E5%85%B9%E5%8D%A1%E7%8F%AD%E7%9A%84%E5%9B%9A%E5%BE%92.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97.mp4");
        movieMap.put(
                "哈利波特4-火焰杯",
                "http://ying.zuidaziyuan.com/1708/%E5%93%88%E5%88%A9%E6%B3%A2%E7%89%B94%EF%BC%9A%E7%81%AB%E7%84%B0%E6%9D%AF.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97.mp4");
        movieMap.put(
                "哈利波特5-凤凰令",
                "http://ying.zuidaziyuan.com/1708/%E5%93%88%E5%88%A9%E6%B3%A2%E7%89%B95%EF%BC%9A%E5%87%A4%E5%87%B0%E4%BB%A4.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97.mp4");
        movieMap.put(
                "哈利波特6-混血王子",
                "http://ying.zuidaziyuan.com/1708/%E5%93%88%E5%88%A9%E6%B3%A2%E7%89%B96%EF%BC%9A%E6%B7%B7%E8%A1%80%E7%8E%8B%E5%AD%90.BD1280%E9%AB%98%E6%B8%85%E5%9B%BD%E8%AF%AD%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "哈利波特7-死亡圣器(上)",
                "http://ying.zuidaziyuan.com/1708/%E5%93%88%E5%88%A9%E6%B3%A2%E7%89%B97%E6%AD%BB%E4%BA%A1%E5%9C%A3%E5%99%A8(%E4%B8%8A).BD1280%E9%AB%98%E6%B8%85%E5%9B%BD%E8%AF%AD%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "哈利波特7-死亡圣器(下)",
                "http://ying.zuidaziyuan.com/1708/%E5%93%88%E5%88%A9%E6%B3%A2%E7%89%B97%E6%AD%BB%E4%BA%A1%E5%9C%A3%E5%99%A8(%E4%B8%8B).BD1280%E9%AB%98%E6%B8%85%E5%9B%BD%E8%AF%AD%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");

        movieMap.put(
                "超凡蜘蛛侠1",
                "http://ying.zuidaziyuan.com/1707/CF%E8%9C%98%E8%9B%9B%E4%BE%A01.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "超凡蜘蛛侠2",
                "http://ying.zuidaziyuan.com/1707/CF%E8%9C%98%E8%9B%9B%E4%BE%A02.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");

        movieMap.put(
                "美国队长1(null)",
                "");
        movieMap.put(
                "美国队长2",
                "http://xunleib.zuida360.com/1804/%E7%BE%8E%E5%9B%BDd%E9%95%BF2%EF%BC%9A%E5%86%ACr%E6%88%98%E5%A3%AB.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "美国队长3-内战",
                "http://xunlei.jingpin88.com/20171026/cQ7hsCrN/mp4/cQ7hsCrN.mp4");

        movieMap.put(
                "变形金刚1",
                "http://ying.zuidaziyuan.com/1706/bx%E9%87%91%E5%88%9A1.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "变形金刚2",
                "http://ying.zuidaziyuan.com/1706/bx%E9%87%91%E5%88%9A2%EF%BC%9A%E5%A0%95%E8%90%BD%E8%80%85%E7%9A%84%E5%A4%8D%E4%BB%87.BD1280%E9%AB%98%E6%B8%85%E7%89%B9%E6%95%88%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "变形金刚3",
                "http://ying.zuidaziyuan.com/1706/bx%E9%87%91%E5%88%9A3%EF%BC%9A%E9%BB%91%E6%9C%88%E9%99%8D%E4%B8%B4.BD1280%E9%AB%98%E6%B8%85%E7%89%B9%E6%95%88%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "变形金刚4",
                "http://ying.zuidaziyuan.com/1706/bx%E9%87%91%E5%88%9A4%EF%BC%9A%E7%BB%9D%E8%BF%B9%E9%87%8D%E7%94%9F.BD1280%E9%AB%98%E6%B8%85%E4%B8%AD%E8%8B%B1%E5%8F%8C%E5%AD%97%E7%89%88.mp4");
        movieMap.put(
                "变形金刚5(null)",
                "");

        movieMap.put(
                "神盾局特工第六季01",
                "http://download.xunleizuida.com/1905/%E7%A5%9E%E7%9B%BE%E5%B1%80%E7%89%B9%E5%B7%A5%E7%AC%AC%E5%85%AD%E5%AD%A3-01.mp4");
        movieMap.put(
                "神盾局特工第六季02",
                "http://download.xunleizuida.com/1905/%E7%A5%9E%E7%9B%BE%E5%B1%80%E7%89%B9%E5%B7%A5%E7%AC%AC%E5%85%AD%E5%AD%A3-02.mp4");
        movieMap.put(
                "神盾局特工第六季03",
                "http://download.xunleizuida.com/1905/%E7%A5%9E%E7%9B%BE%E5%B1%80%E7%89%B9%E5%B7%A5%E7%AC%AC%E5%85%AD%E5%AD%A3-03.mp4");
        movieMap.put(
                "神盾局特工第六季04",
                "http://download.xunleizuida.com/1906/%E7%A5%9E%E7%9B%BE%E5%B1%80%E7%89%B9%E5%B7%A5%E7%AC%AC%E5%85%AD%E5%AD%A3-04.mp4");
        movieMap.put(
                "神盾局特工第六季05",
                "http://download.xunleizuida.com/1906/%E7%A5%9E%E7%9B%BE%E5%B1%80%E7%89%B9%E5%B7%A5%E7%AC%AC%E5%85%AD%E5%AD%A3-05.mp4");
        movieMap.put(
                "神盾局特工第六季06",
                "http://xunlei.xiazai-zuida.com/1906/%E7%A5%9E%E7%9B%BE%E5%B1%80%E7%89%B9%E5%B7%A5%E7%AC%AC%E5%85%AD%E5%AD%A3-06.mp4");
        movieMap.put(
                "神盾局特工第六季07",
                "http://xunlei.xiazai-zuida.com/1906/%E7%A5%9E%E7%9B%BE%E5%B1%80%E7%89%B9%E5%B7%A5%E7%AC%AC%E5%85%AD%E5%AD%A3-07.mp4");
        movieMap.put(
                "神盾局特工第六季08",
                "http://anning.luanniao-zuida.com/1907/%E7%A5%9E%E7%9B%BE%E5%B1%80%E7%89%B9%E5%B7%A5%E7%AC%AC%E5%85%AD%E5%AD%A3-08.mp4");
        movieMap.put(
                "神盾局特工第六季09",
                "http://xunlei.xiazai-zuida.com/1907/%E7%A5%9E%E7%9B%BE%E5%B1%80%E7%89%B9%E5%B7%A5%E7%AC%AC%E5%85%AD%E5%AD%A3-09.mp4");
        movieMap.put(
                "神盾局特工第六季10",
                "http://anning.luanniao-zuida.com/1907/%E7%A5%9E%E7%9B%BE%E5%B1%80%E7%89%B9%E5%B7%A5%E7%AC%AC%E5%85%AD%E5%AD%A3-10.mp4");
        movieMap.put(
                "神盾局特工第六季11",
                "http://xunlei.zuidaxunlei.com/1908/%E7%A5%9E%E7%9B%BE%E5%B1%80%E7%89%B9%E5%B7%A5%E7%AC%AC%E5%85%AD%E5%AD%A3-11.mp4");
        movieMap.put(
                "神盾局特工第六季12",
                "http://xunlei.zuidaxunlei.com/1908/%E7%A5%9E%E7%9B%BE%E5%B1%80%E7%89%B9%E5%B7%A5%E7%AC%AC%E5%85%AD%E5%AD%A3-12.mp4");
        movieMap.put(
                "神盾局特工第六季13",
                "http://xunlei.zuidaxunlei.com/1908/%E7%A5%9E%E7%9B%BE%E5%B1%80%E7%89%B9%E5%B7%A5%E7%AC%AC%E5%85%AD%E5%AD%A3-13.mp4");


        movieMap.put(
                "方谬神探01",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-01.mp4");
        movieMap.put(
                "方谬神探02",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-02.mp4");
        movieMap.put(
                "方谬神探03",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-03.mp4");
        movieMap.put(
                "方谬神探04",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-04.mp4");
        movieMap.put(
                "方谬神探05",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-05.mp4");
        movieMap.put(
                "方谬神探06",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-06.mp4");
        movieMap.put(
                "方谬神探07",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-07.mp4");
        movieMap.put(
                "方谬神探08",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-08.mp4");
        movieMap.put(
                "方谬神探09",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-09.mp4");
        movieMap.put(
                "方谬神探10",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-10.mp4");
        movieMap.put(
                "方谬神探11",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-11.mp4");
        movieMap.put(
                "方谬神探12",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-12.mp4");
        movieMap.put(
                "方谬神探13",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-13.mp4");
        movieMap.put(
                "方谬神探14",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-14.mp4");
        movieMap.put(
                "方谬神探15",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-15.mp4");
        movieMap.put(
                "方谬神探16",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-16.mp4");
        movieMap.put(
                "方谬神探17",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-17.mp4");
        movieMap.put(
                "方谬神探18",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-18.mp4");
        movieMap.put(
                "方谬神探19",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-19.mp4");
        movieMap.put(
                "方谬神探20",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-20.mp4");
        movieMap.put(
                "方谬神探21",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-21.mp4");
        movieMap.put(
                "方谬神探22",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-22.mp4");
        movieMap.put(
                "方谬神探23",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-23.mp4");
        movieMap.put(
                "方谬神探24",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-24.mp4");
        movieMap.put(
                "方谬神探25",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-25.mp4");
        movieMap.put(
                "方谬神探26",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-26.mp4");
        movieMap.put(
                "方谬神探27",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-27.mp4");
        movieMap.put(
                "方谬神探28",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-28.mp4");
        movieMap.put(
                "方谬神探29",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-29.mp4");
        movieMap.put(
                "方谬神探30",
                "http://xunlei.xiazai-zuida.com/1907/%E6%96%B9%E8%B0%AC%E7%A5%9E%E6%8E%A2-30.mp4");

        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树01",
                "http://download.xunleizuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-01.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树02",
                "http://download.xunleizuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-02.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树03",
                "http://download.xunleizuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-03.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树04",
                "http://download.xunleizuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-04.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树05",
                "http://download.xunleizuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-05.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树06",
                "http://download.xunleizuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-06.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树07",
                "http://download.xunleizuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-07.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树08",
                "http://download.xunleizuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-08.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树09",
                "http://download.xunleizuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-09.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树10",
                "http://download.xunleizuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-10.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树11",
                "http://download.xunleizuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-11.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树12",
                "http://download.xunleizuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-12.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树13",
                "http://download.xunleizuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-13.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树14",
                "http://download.xunleizuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-14.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树15",
                "http://download.xunleizuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-15.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树16",
                "http://download.xunleizuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-16.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树17",
                "http://download.xunleizuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-17.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树18",
                "http://download.xunleizuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-18.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树19",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-19.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树20",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-20.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树21",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-21.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树22",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-22.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树23",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-23.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树24",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-24.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树25",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-25.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树26",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-26.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树27",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-27.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树28",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-28.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树29",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-29.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树30",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-30.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树31",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-31.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树32",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-32.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树33",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-33.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树34",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-34.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树35",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-35.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树36",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-36.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树37",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-37.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树38",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-38.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树39",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-39.mp4");
        movieMap.put(
                "盗墓笔记之怒海潜沙-秦岭神树40",
                "http://xunlei.xiazai-zuida.com/1906/DM%E7%AC%94%E8%AE%B0%E4%B9%8B%E6%80%92%E6%B5%B7QS-40.mp4");



    }

    private static String mTitle = "";

    public static String getUri() {
        mPath = "http://192.168.0.112:8080/tomcat_video/game_of_thrones_5_01.mp4";
        mPath = "http://192.168.0.112:8080/tomcat_video/test.mp4";
        mPath = "/storage/37C8-3904/myfiles/video/moumoon_tomodachi-koibito_RV10_cook.rmvb";
        mPath = "/storage/37C8-3904/myfiles/video/kingsman.mp4";
        mPath = "/storage/37C8-3904/myfiles/video/fragment.flv";
        mPath = "/storage/37C8-3904/myfiles/video/aaaaa.rmvb";
        mPath = "/storage/37C8-3904/myfiles/video/[HDR]4K_HDR_Technology_English.mp4";// 播放有点问题
        mPath = "/storage/37C8-3904/myfiles/video/Surfing.mp4";
        // {sample-rate=48000, track-id=2, durationUs=30080000, mime=audio/ac4, channel-count=6, language=eng, max-input-size=2582}
        // {sample-rate=48000, pcm-encoding=2, mime=audio/raw, channel-count=6}
        mPath = "/storage/37C8-3904/myfiles/video/Silent_Movie_Short_321_AC4_h264_MP4_25fps.mp4";
        // {sample-rate=48000, track-id=2, durationUs=159120000, mime=audio/ac4, channel-count=5, language=eng, max-input-size=1393}
        // {sample-rate=48000, pcm-encoding=2, mime=audio/raw, channel-count=5}
        mPath = "/storage/37C8-3904/myfiles/video/Silent_Movie_321_AC4_H265_MP4_50fps.mp4";
        // {sample-rate=48000, track-id=2, durationUs=159120000, mime=audio/ac4, channel-count=5, language=eng, max-input-size=1393}
        // {sample-rate=48000, pcm-encoding=2, mime=audio/raw, channel-count=5}
        mPath = "/storage/37C8-3904/myfiles/video/Silent_Movie_321_AC4_H264_MP4_50fps.mp4";
        mPath = "/storage/37C8-3904/myfiles/video/Ref_997_200_48k_20dB_ddp.mp4";
        // {sample-rate=48000, track-id=2, durationUs=30130100, mime=audio/ac4, channel-count=6, language=eng, max-input-size=682}
        // {sample-rate=48000, pcm-encoding=2, mime=audio/raw, channel-count=6}
        mPath = "/storage/37C8-3904/myfiles/video/FrameRate_321_AC4_H265_MP4_29fps97.mp4";

        mPath = "/storage/37C8-3904/myfiles/video/01_APITest_MPEG1.mpg";
        mPath = "/storage/37C8-3904/myfiles/video/02_APITest_MPEG2PS.mpg";
        mPath = "/storage/37C8-3904/myfiles/video/03_APItest_MPEG2TS.ts";
        mPath = "/storage/37C8-3904/myfiles/video/04_APITest_MPEG4-AVC.mp4";
        mPath = "/storage/37C8-3904/myfiles/video/06_APITest_3GPP-MPEG4V.3g2";
        mPath = "/storage/37C8-3904/myfiles/video/07_APITest_AVI-xvid.avi";
        mPath = "/storage/37C8-3904/myfiles/video/08_APITest_ASF-VC1.wmv";
        mPath = "/storage/37C8-3904/myfiles/video/09_APITest_MOV-MJPEG.mov";
        mPath = "/storage/37C8-3904/myfiles/video/10_APITest_MKV-HEVC.mkv";
        mPath = "/storage/37C8-3904/myfiles/video/11_APITest_WebM-VP8.webm";
        mPath = "/storage/37C8-3904/myfiles/video/11_MPEG4_H265MP@L5.1_AAC-LC.mp4";
        mPath = "/storage/37C8-3904/myfiles/video/12_APITest_WebM-VP9.webm";
        mPath = "/storage/37C8-3904/myfiles/video/AC3Plus_mountainbike-cyberlink_1920_1080.mp4";
        mPath = "/storage/37C8-3904/myfiles/video/Escape.Plan.2.mp4";

        mPath = movieMap.get(mTitle);
        return mPath;
    }

    public static void setTitle(String title) {
        mTitle = title;
    }

    public static String getTitle(){
        return mTitle;
    }

    public static void setPath(String path) {
        mPath = path;
    }

    public static String getPath() {
        return mPath;
    }

    private void backup() {
        /*movieMap.put(
                "401",
                "/storage/emulated/0/Movies/权力的游戏第四季01.mp4");*/
        movieMap.put(
                "重案六组第四季01",
                "http://okzy.xzokzyzy.com/20190629/9876_8b3388bf/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC01%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季02",
                "http://okzy.xzokzyzy.com/20190629/9877_1ffc17e6/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC02%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季03",
                "http://okzy.xzokzyzy.com/20190629/9878_697082ae/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC03%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季04",
                "http://okzy.xzokzyzy.com/20190629/9879_fdde617d/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC04%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季05",
                "http://okzy.xzokzyzy.com/20190629/9880_0a3c392e/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC05%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季06",
                "http://okzy.xzokzyzy.com/20190629/9881_8af5387b/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC06%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季07",
                "http://okzy.xzokzyzy.com/20190629/9882_c01aa477/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC07%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季08",
                "http://okzy.xzokzyzy.com/20190629/9883_73d24a0f/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC08%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季09",
                "http://okzy.xzokzyzy.com/20190629/9884_096eceaa/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC09%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季10",
                "http://okzy.xzokzyzy.com/20190629/9885_6a5cd38a/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC10%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季11",
                "http://okzy.xzokzyzy.com/20190629/9886_1378f07c/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC11%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季12",
                "http://okzy.xzokzyzy.com/20190629/9887_595945cb/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC12%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季13",
                "http://okzy.xzokzyzy.com/20190629/9888_1fcd0d2b/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC13%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季14",
                "http://okzy.xzokzyzy.com/20190629/9889_1f60ddd9/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC14%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季15",
                "http://okzy.xzokzyzy.com/20190629/9890_76d30e9e/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC15%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季16",
                "http://okzy.xzokzyzy.com/20190629/9891_53058510/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC16%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季17",
                "http://okzy.xzokzyzy.com/20190629/9892_f0f11c7d/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC17%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季18",
                "http://okzy.xzokzyzy.com/20190629/9893_2aff0568/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC18%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季19",
                "http://okzy.xzokzyzy.com/20190629/9894_1af77e81/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC19%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季20",
                "http://okzy.xzokzyzy.com/20190629/9895_0e8e661d/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC20%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季21",
                "http://okzy.xzokzyzy.com/20190629/9896_053bbac4/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC21%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季22",
                "http://okzy.xzokzyzy.com/20190629/9897_6f2a7075/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC22%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季23",
                "http://okzy.xzokzyzy.com/20190629/9898_49e74704/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC23%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季24",
                "http://okzy.xzokzyzy.com/20190629/9899_171d0bcd/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC24%E9%9B%86.mp4  ");
        movieMap.put(
                "重案六组第四季25",
                "http://okzy.xzokzyzy.com/20190629/9900_622cf90b/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC25%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季26",
                "http://okzy.xzokzyzy.com/20190629/9901_b3b909e4/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC26%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季27",
                "http://okzy.xzokzyzy.com/20190629/9902_4a6304c2/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC27%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季28",
                "http://okzy.xzokzyzy.com/20190629/9903_c59f203f/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC28%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季29",
                "http://okzy.xzokzyzy.com/20190629/9904_11bb1b82/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC29%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季30",
                "http://okzy.xzokzyzy.com/20190629/9905_6848aea9/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC30%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季31",
                "http://okzy.xzokzyzy.com/20190629/9906_5ba02682/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC31%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季32",
                "http://okzy.xzokzyzy.com/20190629/9907_c044d536/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC32%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季33",
                "http://okzy.xzokzyzy.com/20190629/9908_b3688e6a/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC33%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季34",
                "http://okzy.xzokzyzy.com/20190629/9909_4df27a5d/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC34%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季35",
                "http://okzy.xzokzyzy.com/20190629/9910_044c4654/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC35%E9%9B%86.mp4");
        movieMap.put(
                "重案六组第四季36",
                "http://okzy.xzokzyzy.com/20190629/9911_779d9559/%E9%87%8D%E6%A1%88%E5%85%AD%E7%BB%844%E7%AC%AC36%E9%9B%86.mp4");

        movieMap.put(
                "勇探实录01",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-01.mp4");
        movieMap.put(
                "勇探实录02",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-02.mp4");
        movieMap.put(
                "勇探实录03",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-03.mp4");
        movieMap.put(
                "勇探实录04",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-04.mp4");
        movieMap.put(
                "勇探实录05",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-05.mp4");
        movieMap.put(
                "勇探实录06",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-06.mp4");
        movieMap.put(
                "勇探实录07",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-07.mp4");
        movieMap.put(
                "勇探实录08",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-08.mp4");
        movieMap.put(
                "勇探实录09",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-09.mp4");
        movieMap.put(
                "勇探实录10",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-10.mp4");
        movieMap.put(
                "勇探实录11",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-11.mp4");
        movieMap.put(
                "勇探实录12",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-12.mp4");
        movieMap.put(
                "勇探实录13",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-13.mp4");
        movieMap.put(
                "勇探实录14",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-14.mp4");
        movieMap.put(
                "勇探实录15",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-15.mp4");
        movieMap.put(
                "勇探实录16",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-16.mp4");
        movieMap.put(
                "勇探实录17",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-17.mp4");
        movieMap.put(
                "勇探实录18",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-18.mp4");
        movieMap.put(
                "勇探实录19",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-19.mp4");
        movieMap.put(
                "勇探实录20",
                "http://anning.luanniao-zuida.com/1908/%E5%8B%87%E6%8E%A2%E5%AE%9E%E5%BD%95%E5%9B%BD%E8%AF%AD-20.mp4");
    }

}
