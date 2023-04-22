import com.xiaoleilu.hutool.json.JSONArray;
import com.xiaoleilu.hutool.json.JSONObject;
import com.xiaoleilu.hutool.json.JSONUtil;
import com.xiaoleilu.hutool.util.StrUtil;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;


public class GiantMain {

    private static final Logger logger = LoggerFactory.getLogger(GiantMain.class);

    private static Properties PROPERTIES = new Properties();

    private static final HttpClient httpClient = new HttpClient();

    private static final String OUTPUT_FORMAT = "this store {}, has {} stock\n";

    static {
        try (InputStream resourceAsStream = GiantMain.class.getResourceAsStream("./config.properties");) {
            Properties properties = new Properties();
            PROPERTIES = properties;
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        try {
            //1.get shop list
            Map<String, String> shopKeyValues = foreachGetStoreList();

            while (true) {
                logger.info("{} start search", LocalDateTime.now());
                StringBuilder stringBuilder = new StringBuilder();
                for (Map.Entry<String, String> entry : shopKeyValues.entrySet()) {
                    if (doStock(entry.getValue())) {
                        Integer stock = getStockList(entry.getValue());
                        if (stock > 0) {
                            stringBuilder.append(StrUtil.format(OUTPUT_FORMAT, entry.getKey(), stock));
                        }
                    }
                    logger.info("search stock {} over", entry.getKey());
                    Thread.sleep(1000);
                }
                //汇总
                if (!StrUtil.isEmpty(stringBuilder.toString())) {
                    //发送邮件
                    //sendMail("Giant has stock!!!!", stringBuilder.toString());
                    logger.info(stringBuilder.toString());
                } else {
                    //sendMail("Giant has no stock,555", "sad");
                }
                Thread.sleep(1000 * 30);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * for each 10 times to get area giant shop list
     *
     * @return
     * @throws IOException
     */
    private static Map<String, String> foreachGetStoreList() throws IOException {
        Map<String, String> result = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            result.putAll(getStoreList(i));
        }
        return result;
    }

    private static final String USER_LONG = "long_at";

    private static final String USER_LAT = "lat_at";

    private static final String PROVINCE = "province";

    /**
     * search shop list by config properties
     *
     * @param page
     * @return
     * @throws IOException
     */
    private static Map<String, String> getStoreList(Integer page) throws IOException {
        PostMethod request = new PostMethod("https://e-gw.giant.com.cn/index.php/api/store_list");
        request.setParameter("per_page", "10");
        request.setParameter("page", page + "");
        request.setParameter("user_long", PROPERTIES.getProperty(USER_LONG));
        request.setParameter("user_lat", PROPERTIES.getProperty(USER_LAT));
        request.setParameter("province", PROPERTIES.getProperty(PROVINCE));
        request.setParameter("city", "");
        request.setParameter("area", "");
        addHeader(request);
        httpClient.executeMethod(request);
        String responseBodyAsString = handlerResponse(request.getResponseBodyAsStream());
        JSONObject jsonObject = JSONUtil.parseObj(responseBodyAsString);
        JSONArray datas = JSONUtil.parseArray(jsonObject.getStr("data"));
        HashMap<String, String> result = new HashMap<>();
        for (Object data : datas) {
            JSONObject object = (JSONObject) data;
            result.put((String) object.get("name"), (String) object.get("code"));
        }
        return result;
    }

    private static final String USER_ID = "user_id";

    /**
     * set stock before search stock
     *
     * @param value
     * @return
     * @throws IOException
     */
    private static boolean doStock(String value) throws IOException {
        PostMethod request = new PostMethod("https://e-gw.giant.com.cn/index.php/api/do_store");
        request.setParameter("code", value);
        request.setParameter("user_id", PROPERTIES.getProperty(USER_ID));
        addHeader(request);
        httpClient.executeMethod(request);
        String responseBodyAsString = handlerResponse(request.getResponseBodyAsStream());
        JSONObject jsonObject = JSONUtil.parseObj(responseBodyAsString);
        if (jsonObject.get("msg").equals("ok")) {
            return true;
        }
        return false;
    }

    private static final String STOCK_ID = "stock_id";

    private static final String CODE = "code";

    /**
     * search stock
     *
     * @param code
     * @return
     * @throws IOException
     */
    private static Integer getStockList(String code) throws IOException {
        PostMethod request = new PostMethod("https://e-gw.giant.com.cn/index.php/api/sku_stock");
        request.setParameter("sku", PROPERTIES.getProperty(STOCK_ID));
        request.setParameter("shopno", code);
        request.setParameter("user_id", PROPERTIES.getProperty(USER_ID));
        addHeader(request);
        httpClient.executeMethod(request);
        String responseBodyAsString = handlerResponse(request.getResponseBodyAsStream());
        JSONObject jsonObject = JSONUtil.parseObj(responseBodyAsString);
        if (jsonObject.getStr("data") != null) {
            String value = ParseUtil.parseDataStr(jsonObject.getStr("data"));
            try {
                String decryptStr = ParseUtil.decrypt(value, PROPERTIES.getProperty(CODE).substring(16), PROPERTIES.getProperty(CODE).substring(0, 16));
                JSONObject object = JSONUtil.parseObj(decryptStr);
                Integer stock = object.getInt("stock");
                if (stock != null && stock > 0) {
                    return stock;
                }
            } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException e) {
                logger.error("sorry,we decode error");
            }
        } else {
            return 0;
        }
        return 0;
    }

    private static void addHeader(PostMethod request) {
        request.setRequestHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/111.0");
        request.setRequestHeader("Accept-Encoding", "gzip, deflate, br");
        request.setRequestHeader("Referer", "https://e.giant.com.cn/");
        request.setRequestHeader("Sec-Fetch-Dest", "empty");
        request.setRequestHeader("Sec-Fetch-Mode", "cors");
        request.setRequestHeader("Sec-Fetch-Site", "same-site");
    }

    private static String handlerResponse(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(inputStream), StandardCharsets.UTF_8));
        StringBuilder htmlStr = new StringBuilder();
        String str = null;
        while ((str = bufferedReader.readLine()) != null) {
            htmlStr.append(str);
        }
        return htmlStr.toString();
    }
}
