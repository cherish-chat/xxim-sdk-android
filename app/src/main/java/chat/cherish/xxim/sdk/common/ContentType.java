package chat.cherish.xxim.sdk.common;

public class ContentType {
    public static int unknown = 0; // 未知类型
    public static int typing = 1; // 正在输入
    public static int read = 2; // 已读
    public static int revoke = 3; // 撤回

    public static int text = 11; // 文本
    public static int image = 12; // 图片
    public static int audio = 13; // 语音
    public static int video = 14; // 视频
    public static int file = 15; // 文件
    public static int location = 16; // 位置
    public static int card = 17; // 名片
    public static int merge = 18; // 合并
    public static int emoji = 19; // 表情
    public static int command = 20; // 命令
    public static int richTxt = 21; // 富文本
    public static int markdown = 22; // markdown

    public static int custom = 100; // 自定义消息
}
