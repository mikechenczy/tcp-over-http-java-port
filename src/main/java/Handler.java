
import java.io.*;
import java.net.*;
import java.util.Base64;

public class Handler extends Thread {
    public Socket clientSocket;
    public String targetUrl;
    public Proxy proxy;
    public String uid;

    public boolean downloading = false;
    public Handler(Socket clientSocket, String targetUrl) {
        String proxyHost = "127.0.0.1";
        int proxyPort = 1080;
        //proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        this.clientSocket = clientSocket;
        this.targetUrl = targetUrl;
    }

    @Override
    public void run() {
        try {
            System.out.println("INIT");
            uid = postInit(targetUrl+"open");
            System.out.println("INIT SUCCESS:"+uid);
            InputStream inputStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream();


            new Thread(() -> {
                while (!isInterrupted()) {
                    // 将目标服务器的响应发送回客户端
                    try {
                        Thread.sleep(0);
                    } catch (InterruptedException e) {
                        break;
                    }
                    if(!downloading) {
                        //System.out.println("DOWNLOADING");
                        downloading = true;
                        new Thread(() -> {
                            try {
                                outputStream.write(downloadData(targetUrl));
                                outputStream.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                                closeHandler();
                            }
                            downloading = false;
                        }).start();
                    }
                }
            }).start();
            new Thread(() -> {
                while (!isInterrupted()) {
                    try {
                        Thread.sleep(0);
                    } catch (InterruptedException e) {
                        break;
                    }
                    try {
                        // 从客户端读取数据
                        ByteArrayOutputStream requestBuffer = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            requestBuffer.write(buffer, 0, bytesRead);
                            if (inputStream.available() == 0) {
                                break;
                            }
                        }
                        byte[] requestData = requestBuffer.toByteArray();

                        // 上传数据到目标服务器
                        //System.out.println("UPLOADING");

                        uploadData(targetUrl, requestData);
                    } catch (IOException e) {
                        e.printStackTrace();
                        closeHandler();
                        break;
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
            closeHandler();
        }
    }

    private void closeHandler() {
        System.out.println("CLOSING!!");
        if(clientSocket!=null) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        interrupt();
    }

    private byte[] uploadData(String targetUrl, byte[] data) throws IOException {
        return postData(targetUrl+"u/"+uid, data);
    }

    private byte[] downloadData(String targetUrl) throws IOException {
        return Base64.getDecoder().decode(postData(targetUrl+"s/"+uid, null));
    }

    public String postInit(String urlString) throws IOException {
        // 设置请求URL
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) (proxy==null?url.openConnection():url.openConnection(proxy));

        // 设置请求方法为POST
        conn.setRequestMethod("POST");

        // 获取响应状态码
        //int responseCode = conn.getResponseCode();

        // 读取响应数据
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // 打印响应结果
        //System.out.println("Response Code: " + responseCode);
        //System.out.println("Response Body: " + response.toString());

        // 关闭连接
        conn.disconnect();
        return response.toString();
    }

    public byte[] postData(String urlString, byte[] data) throws IOException {
        // 设置请求URL
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) (proxy==null?url.openConnection():url.openConnection(proxy));

        // 设置请求方法为POST
        conn.setRequestMethod("POST");

        if(data!=null) {
            // 允许输出
            conn.setDoOutput(true);

            // 设置请求体数据（如果有）
            //String requestBody = "param1=value1&param2=value2";
            //byte[] requestBodyBytes = requestBody.getBytes("UTF-8");

            // 设置请求体长度
            //conn.setRequestProperty("Content-Length", String.valueOf(data.length));

            // 设置请求体类型
            //conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            // 发送请求体数据
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(Base64.getEncoder().encode(data));
            outputStream.close();
        }

        // 获取响应状态码
        //int responseCode = conn.getResponseCode();

        // 读取响应数据
        InputStream inputStream = conn.getInputStream();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        byte[] responseBytes = byteArrayOutputStream.toByteArray();

        // 关闭连接
        conn.disconnect();
        return responseBytes;
    }
}
