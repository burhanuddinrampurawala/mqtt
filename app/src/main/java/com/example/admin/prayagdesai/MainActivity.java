package com.example.admin.prayagdesai;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private MqttAndroidClient client;
    TextToSpeech t1;
    final static  String  TAG = "mainactivity";
    private String ip;
    private String port;
    private String topic;
    private SharedPreferences prayag;
    AlertDialog dialog = null;
    private final static String clientId = MqttClient.generateClientId();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        speechInitialisation();
        prayag = getApplicationContext().getSharedPreferences("prayag", Context.MODE_PRIVATE);
        ip = prayag.getString("ip",null);
        port = prayag.getString("port",null);
        topic = prayag.getString("topic",null);
        dataDialog();
    }
    public void connection(){
        String path = "tcp://" + ip + ":" + port;
        Log.w(TAG,path);
        client = new MqttAndroidClient(MainActivity.this, path,
                clientId);
        send();
    }
    public void send(){
        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // We are connected
                    Log.d(TAG, "onSuccess");
                    t1.speak("Connected",TextToSpeech.QUEUE_ADD,null,"volume");
                    String topic = "client";
                    String payload = "hello from app";
                    byte[] encodedPayload = new byte[0];
                    try {
                        encodedPayload = payload.getBytes("UTF-8");
                        MqttMessage message = new MqttMessage(encodedPayload);
                        client.publish(topic, message);
                        Log.i(TAG,"Message sent from " + topic);
                        subscribe();
                    } catch (UnsupportedEncodingException | MqttException e) {
                        Log.e(TAG,e.getMessage());
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    // Something went wrong e.g. connection timeout or firewall problems
                    Log.e(TAG, "onFailure");
                    Log.e(TAG, exception.getMessage());
                    t1.speak("Connection timed out",TextToSpeech.QUEUE_ADD,null,"volume");


                }
            });
        } catch (MqttException e) {
            Log.e(TAG,e.getMessage());
        }
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                byte  [] bytes = message.getPayload();
                String str = null;
                try {
                    str = new String(bytes, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Log.i(TAG,str);
                t1.speak(str,TextToSpeech.QUEUE_ADD,null,"volume");
                Toast.makeText(getApplicationContext(),str,Toast.LENGTH_LONG).show();
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }
    public void subscribe()  {
        Log.i(TAG,"ip: " + ip + "\nport: " + port + "\ntopic: " + topic + "\nclient: " + client);
        int qos = 0;
        if(topic != null){
            try {
                client.subscribe(topic, qos, getApplicationContext(), new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.i(TAG,"subscribed to topic : " + topic);
                        Toast.makeText(getApplicationContext(),"subscribed to topic : " + topic,Toast.LENGTH_LONG).show();
                        t1.speak("subscribed to topic " + topic,TextToSpeech.QUEUE_ADD,null,"volume");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        // The subscription could not be performed, maybe the user was not
                        // authorized to subscribe on the specified topic e.g. using wildcards
                        Log.e(TAG,exception.getMessage());
                    }
                });

            } catch (MqttException e) {
                Log.e(TAG,e.getMessage());
            }
//            catch (NullPointerException e){
//                Log.e(TAG,e.getMessage());
//            }
        }

    }
    public void unSubscribe(){
        if(topic !=null){
            try {
                IMqttToken unsubToken = client.unsubscribe(topic);
                unsubToken.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        // The subscription could successfully be removed from the client
                        Log.w(TAG,"unsubscribed to topic : " + topic);
                        disconnect();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken,
                                          Throwable exception) {
                        // some error occurred, this is very unlikely as even if the client
                        // did not had a subscription to the topic the unsubscribe action
                        // will be successfully
                        Log.e(TAG,exception.getMessage());
                        disconnect();
                    }
                });

            } catch (MqttException e) {
                Log.e(TAG,e.getMessage());
            }
            catch (NullPointerException e){
                Log.e(TAG,e.getMessage());
            }
        }
    }
    public void speechInitialisation(){

        t1 = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status!=TextToSpeech.ERROR){
                    t1.setLanguage(Locale.getDefault());
                }
            }
        });

    }

    private void disconnect() {

        try {
            client.disconnect().setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    Log.w(TAG,"disconnected");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // some error occurred, this is very unlikely as even if the client
                    // did not had a subscription to the topic the unsubscribe action
                    // will be successfully
                    Log.e(TAG,exception.getMessage());
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onRestart() {
        connection();
        super.onRestart();
    }

    @Override
    protected void onStop() {
        unSubscribe();
        super.onStop();
    }

    private void dataDialog(){
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.adddata,
                (ViewGroup) findViewById(R.id.dialogLayout));
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        builder.setTitle("Enter");
        final EditText ipText =  layout.findViewById(R.id.ip);
        final EditText portText = layout.findViewById(R.id.port);
        final EditText topicText = layout.findViewById(R.id.topic);
        ip = prayag.getString("ip",null);
        port = prayag.getString("port",null);
        topic = prayag.getString("topic",null);
        if(ip != null)
            ipText.setHint(ip);
        if(port != null)
            portText.setHint(port);
        if(topic != null)
            topicText.setHint(topic);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                if(ipText.length() != 0 && portText.length() != 0 && topicText.length() != 0){
                    SharedPreferences.Editor editor = prayag.edit();
                    ip = ipText.getText().toString();
                    port  = portText.getText().toString();
                    topic = topicText.getText().toString();
                    //saving into database
                    editor.putString("ip",ip);
                    editor.putString("port",port);
                    editor.putString("topic",topic);
                    editor.commit();
                    connection();
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialog.cancel();
            }
        });
        dialog = builder.create();
        dialog.show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.settings,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(R.id.settings == item.getItemId()){
            dataDialog();
        }
        return super.onOptionsItemSelected(item);
    }
}
