import org.apache.commons.codec.binary.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class ParseUtil {

    public static String decrypt(String context, String key, String vi) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException {
        SecretKeySpec k = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        byte[] encoded = k.getEncoded();
        SecretKeySpec aes = new SecretKeySpec(encoded, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec iv = new IvParameterSpec(vi.getBytes());
        cipher.init(Cipher.DECRYPT_MODE, aes, iv);
        byte[] bytes = cipher.doFinal(Base64.decodeBase64(context.getBytes(StandardCharsets.UTF_8)));
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * dealStrSub(val){
     * let numA = Math.ceil(val.length/2);
     * let str1 = val.substring(numA,val.length);
     * let str2 = val.substring(0,numA);
     * let str3 = str1.substring(1,3);
     * let str3_1 = str1.substring(1,2);
     * let str4 = str1.substring(4,6);
     * let str4_1 = str1.substring(4,5);
     * if(str3_1 === '0'){
     * str3 = str1.substring(2,3)
     * }
     * if(str4_1 === '0'){
     * str4 = str1.substring(5,6)
     * }
     * let str7 = str1.substring(6,str1.length)
     * let str5 = str2+str7;
     * let str6 = str5.substring(str4,str5.length-str3)
     * // console.log(str6)
     * return str6
     * }
     */

    public static String parseDataStr(String data) {
        int numA = (int) Math.ceil((double) data.length() / 2);
        String str1 = data.substring(numA, data.length());
        String str2 = data.substring(0, numA);
        String str3 = str1.substring(1, 3);
        String str3_1 = str1.substring(1, 2);
        String str4 = str1.substring(4, 6);
        String str4_1 = str1.substring(4, 5);
        if ("0" == str3_1) {
            str3 = str1.substring(2, 3);
        }
        if ("0" == str4_1) {
            str4 = str1.substring(5, 6);
        }
        String str7 = str1.substring(6, str1.length());
        String str5 = str2 + str7;
        String str6 = str5.substring(Integer.parseInt(str4), str5.length() - Integer.parseInt(str3));
        return str6;
    }
}
