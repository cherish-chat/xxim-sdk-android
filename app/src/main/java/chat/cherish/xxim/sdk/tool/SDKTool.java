package chat.cherish.xxim.sdk.tool;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SDKTool {
    private static final String separator = "-";
    private static final String singlePrefix = "single:";
    private static final String groupPrefix = "group:";

    public static String getUUId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String singleConvId(String id1, String id2) {
        if (id1.compareTo(id2) < 0) {
            return singlePrefix + id1 + separator + id2;
        }
        return singlePrefix + id2 + separator + id1;
    }

    public static String groupConvId(String id) {
        return groupPrefix + id;
    }

    public static List<String> generateSeqList(int minSeq, int maxSeq) {
        List<String> list = new ArrayList<>();
        for (int i = minSeq + 1; i <= maxSeq; i++) {
            list.add(i + "");
        }
        return list;
    }

    public static byte[] aesEncode(String key, String iv, String value) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "AES"
            );
            IvParameterSpec ivSpec = new IvParameterSpec(
                    iv.getBytes(StandardCharsets.UTF_8)
            );
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return value.getBytes(StandardCharsets.UTF_8);
        }
    }

    public static String aesDecode(String key, String iv, byte[] bytes) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "AES"
            );
            IvParameterSpec ivSpec = new IvParameterSpec(
                    iv.getBytes(StandardCharsets.UTF_8)
            );
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return new String(cipher.doFinal(bytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

}
