package chat.cherish.xxim.sdk.common;

public class MsgContentType {
    public static final int unknown = 0; // 未知类型
    public static final int typing = 1; // 正在输入
    public static final int tip = 2; // 提示

    public static final int text = 11; // 文本
    public static final int image = 12; // 图片
    public static final int audio = 13; // 语音
    public static final int video = 14; // 视频
    public static final int file = 15; // 文件
    public static final int location = 16; // 位置
    public static final int card = 17; // 名片
    public static final int merge = 18; // 合并
    public static final int emoji = 19; // 表情
    public static final int command = 20; // 命令
    public static final int richText = 21; // 富文本
    public static final int markdown = 22; // markdown

    public static final int custom = 100; // 自定义消息
}
