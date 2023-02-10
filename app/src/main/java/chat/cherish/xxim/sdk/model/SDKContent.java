package chat.cherish.xxim.sdk.model;

import com.alibaba.fastjson.JSON;

import java.util.List;
import java.util.Map;

public class SDKContent {

    // 正在输入
    public static class TypingContent {
        public boolean focus;

        public TypingContent() {
        }

        public TypingContent(boolean focus) {
            this.focus = focus;
        }

        public static TypingContent fromJson(String content) {
            return JSON.parseObject(content, TypingContent.class);
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }
    }

    // 提示消息
    public static class TipContent {
        public String tip;

        public TipContent() {
        }

        public TipContent(String tip) {
            this.tip = tip;
        }

        public static TipContent fromJson(String content) {
            return JSON.parseObject(content, TipContent.class);
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }
    }

    // 图片消息
    public static class ImageContent {
        public String imageName;
        public String imagePath;
        public String imageUrl;
        public int width;
        public int height;
        public int size;

        public ImageContent() {
        }

        public ImageContent(String imageName, String imagePath, String imageUrl, int width, int height,
                            int size
        ) {
            this.imageName = imageName;
            this.imagePath = imagePath;
            this.imageUrl = imageUrl;
            this.width = width;
            this.height = height;
            this.size = size;
        }

        public static ImageContent fromJson(String content) {
            return JSON.parseObject(content, ImageContent.class);
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }
    }

    // 语音消息
    public static class AudioContent {
        public String audioName;
        public String audioPath;
        public String audioUrl;
        public List<Integer> decibels;
        public int duration;
        public int size;

        public AudioContent() {
        }

        public AudioContent(String audioName, String audioPath, String audioUrl, List<Integer> decibels, int duration,
                            int size
        ) {
            this.audioName = audioName;
            this.audioPath = audioPath;
            this.audioUrl = audioUrl;
            this.decibels = decibels;
            this.duration = duration;
            this.size = size;
        }

        public static AudioContent fromJson(String content) {
            return JSON.parseObject(content, AudioContent.class);
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }
    }

    // 视频消息
    public static class VideoContent {
        public String coverName;
        public String coverPath;
        public String coverUrl;
        public String videoName;
        public String videoPath;
        public String videoUrl;
        public int duration;
        public int width;
        public int height;
        public int size;

        public VideoContent() {
        }

        public VideoContent(String coverName, String coverPath, String coverUrl, String videoName, String videoPath,
                            String videoUrl, int duration, int width, int height, int size
        ) {
            this.coverName = coverName;
            this.coverPath = coverPath;
            this.coverUrl = coverUrl;
            this.videoName = videoName;
            this.videoPath = videoPath;
            this.videoUrl = videoUrl;
            this.duration = duration;
            this.width = width;
            this.height = height;
            this.size = size;
        }

        public static VideoContent fromJson(String content) {
            return JSON.parseObject(content, VideoContent.class);
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }
    }

    // 文件消息
    public static class FileContent {
        public String fileName;
        public String filePath;
        public String fileUrl;
        public String type;
        public int size;

        public FileContent() {
        }

        public FileContent(String fileName, String filePath, String fileUrl, String type, int size) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.fileUrl = fileUrl;
            this.type = type;
            this.size = size;
        }

        public static FileContent fromJson(String content) {
            return JSON.parseObject(content, FileContent.class);
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }
    }

    // 位置消息
    public static class LocationContent {
        public double latitude;
        public double longitude;
        public String address;

        public LocationContent() {
        }

        public LocationContent(double latitude, double longitude, String address) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.address = address;
        }

        public static LocationContent fromJson(String content) {
            return JSON.parseObject(content, LocationContent.class);
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }
    }

    // 名片消息
    public static class CardContent {
        public String userId;
        public String nickname;
        public String avatar;

        public CardContent() {
        }

        public CardContent(String userId, String nickname, String avatar) {
            this.userId = userId;
            this.nickname = nickname;
            this.avatar = avatar;
        }

        public static CardContent fromJson(String content) {
            return JSON.parseObject(content, CardContent.class);
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }
    }

    // 合并消息
    public static class MergeContent {
        public String mergeName;
        public Map<Integer, String> mergeMap;

        public MergeContent() {
        }

        public MergeContent(String mergeName, Map<Integer, String> mergeMap) {
            this.mergeName = mergeName;
            this.mergeMap = mergeMap;
        }

        public static MergeContent fromJson(String content) {
            return JSON.parseObject(content, MergeContent.class);
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }
    }

    // 表情消息
    public static class EmojiContent {
        public String coverUrl;
        public String emojiUrl;

        public EmojiContent() {
        }

        public EmojiContent(String coverUrl, String emojiUrl) {
            this.coverUrl = coverUrl;
            this.emojiUrl = emojiUrl;
        }

        public static EmojiContent fromJson(String content) {
            return JSON.parseObject(content, EmojiContent.class);
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }
    }

    // 命令消息
    public static class CommandContent {
        public String command;

        public CommandContent() {
        }

        public CommandContent(String command) {
            this.command = command;
        }

        public static CommandContent fromJson(String content) {
            return JSON.parseObject(content, CommandContent.class);
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }
    }

    // 富文本消息
    public static class RichTextContent {
        public List<Map<String, Object>> list;

        public RichTextContent() {
        }

        public RichTextContent(List<Map<String, Object>> list) {
            this.list = list;
        }

        public static RichTextContent fromJson(String content) {
            return JSON.parseObject(content, RichTextContent.class);
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }
    }

    // 标记消息
    public static class MarkdownContent {
        public String title;
        public String content;
        public String actions;

        public MarkdownContent() {
        }

        public MarkdownContent(String title, String content, String actions) {
            this.title = title;
            this.content = content;
            this.actions = actions;
        }

        public static MarkdownContent fromJson(String content) {
            return JSON.parseObject(content, MarkdownContent.class);
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }
    }

    // 自定义消息
    public static class CustomContent {
        public String data;
        public String ext;

        public CustomContent() {
        }

        public CustomContent(String data, String ext) {
            this.data = data;
            this.ext = ext;
        }

        public static CustomContent fromJson(String content) {
            return JSON.parseObject(content, CustomContent.class);
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }
    }

    // 已读消息
    public static class ReadContent {
        public String senderId;
        public String convId;
        public long seq;

        public ReadContent() {
        }

        public ReadContent(String convId, long seq) {
            this.convId = convId;
            this.seq = seq;
        }

        public static ReadContent fromJson(String content) {
            return JSON.parseObject(content, ReadContent.class);
        }

        public String toJson() {
            return JSON.toJSONString(this);
        }
    }
}
