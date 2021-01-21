package com.github.wzf.uiuxfeedback;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

public class HttpUtil {

    private static final String TAG = "HttpUtil";


    private static final String BOUNDARY = "----WebKitFormBoundaryBashijyubijyu";

    /**
     * @param params       传递的普通参数
     * @param fileFormName 需要上传文件表单中的名字
     * @param uploadFile   需要上传的文件名
     * @param newFileName  上传的文件名称，不填写将为uploadFile的名称
     * @param urlStr       上传的服务器的路径
     * @param contentType  contentType
     * @throws IOException
     */
    public static int uploadForm(Map<String, String> params, String fileFormName, File uploadFile, String newFileName, String urlStr, String contentType)
            throws IOException {
        if (newFileName == null || newFileName.trim().equals("")) {
            newFileName = uploadFile.getName();
        }

        StringBuilder sb = new StringBuilder();
        /**
         * 普通的表单数据
         */
        if (params != null) {
            for (String key : params.keySet()) {
                sb.append("--" + BOUNDARY + "\r\n");
                sb.append("Content-Disposition: form-data; name=\"" + key + "\""
                        + "\r\n");
                sb.append("\r\n");
                sb.append(params.get(key) + "\r\n");
            }
        }
        /**
         * 上传文件的头
         */
        sb.append("--" + BOUNDARY + "\r\n");
        sb.append("Content-Disposition: form-data; name=\"" + fileFormName
                + "\"; filename=\"" + newFileName + "\"" + "\r\n");
        sb.append("Content-Type: "+contentType + "\r\n");// 如果服务器端有文件类型的校验，必须明确指定ContentType
        sb.append("\r\n");

        byte[] headerInfo = sb.toString().getBytes("UTF-8");
        byte[] endInfo = ("\r\n--" + BOUNDARY + "--\r\n").getBytes("UTF-8");
        Log.i(TAG, sb.toString());
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type",
                "multipart/form-data; boundary=" + BOUNDARY);
        conn.setRequestProperty("Content-Length", String
                .valueOf(headerInfo.length + uploadFile.length()
                        + endInfo.length));
        conn.setDoOutput(true);

        OutputStream out = conn.getOutputStream();
        InputStream in = new FileInputStream(uploadFile);
        out.write(headerInfo);

        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) != -1)
            out.write(buf, 0, len);

        out.write(endInfo);
        in.close();
        out.close();
        if (conn.getResponseCode() == 200) {
            InputStream is = (InputStream) conn.getContent();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

            int buffer = 1024;
            byte[] b = new byte[buffer];
            int n = 0;
            while ((n = is.read(b, 0, buffer)) > 0) {
                baos.write(b, 0, n);
            }
            String s = new String(baos.toByteArray(), "UTF-8");
            is.close();
            baos.close();
            Log.i(TAG, s);
        }
        return conn.getResponseCode();

    }
}
