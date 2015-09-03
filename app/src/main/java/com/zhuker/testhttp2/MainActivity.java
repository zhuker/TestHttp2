package com.zhuker.testhttp2;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.ConnectionPool;
import com.squareup.okhttp.Dispatcher;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends ActionBarActivity {
    private final static String TAG = "TestHttp2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final EditText editText = (EditText) findViewById(R.id.editText);
        editText.setText("https://google.com");
        final TextView text = (TextView) findViewById(R.id.textView);
        Button button = (Button) findViewById(R.id.button);
        final OkHttpClient c = likeAndroid();
        c.setProtocols(Arrays.asList(Protocol.HTTP_2, Protocol.SPDY_3, Protocol.HTTP_1_1));
        c.networkInterceptors().add(new Interceptor() {

            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                Log.d(TAG, "connection: " + chain.connection());
                Response response = chain.proceed(request);
                return response;
            }
        });
        c.interceptors().add(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                Log.d(TAG, "connection: " + chain.connection());
                Response response = chain.proceed(request);
                Log.d(TAG, "connection: " + chain.connection());
                return response;
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                text.setText("requesting...");
                c.newCall(new Request.Builder().url(editText.getText().toString()).build()).enqueue(new Callback() {
                    @Override
                    public void onFailure(Request request, final IOException e) {
                        Log.e(TAG, "onFailure", e);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                text.setText(String.valueOf(e));
                            }
                        });
                    }

                    @Override
                    public void onResponse(final Response response) throws IOException {
                        Log.d(TAG, "onResponse " + response);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                text.setText(String.valueOf(response));
                            }
                        });
                    }
                });

            }
        });
    }

    public static OkHttpClient getUnsafeOkHttpClient() {
        try {
            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                        throws CertificateException {
                    Log.d(TAG, "checkClientTrusted");
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                        throws CertificateException {
                    Log.d(TAG, "checkServerTrusted");
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            }};

            // Install the all-trusting trust manager
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.setSslSocketFactory(sslSocketFactory);
            okHttpClient.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    Log.d(TAG, "hostname " + hostname);
                    return true;
                }
            });

            return okHttpClient;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static OkHttpClient likeAndroid() {
        OkHttpClient c = getUnsafeOkHttpClient();
        c.interceptors().add(new Interceptor() {

            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                System.out.println(chain);
                Response response = chain.proceed(request);
                return response;
            }
        });
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(2);
        dispatcher.setMaxRequestsPerHost(2);
        c.setDispatcher(dispatcher);
        c.setConnectionPool(ConnectionPool.getDefault());
        c.setReadTimeout(2, TimeUnit.SECONDS);
        c.setWriteTimeout(2, TimeUnit.SECONDS);
        return c;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
