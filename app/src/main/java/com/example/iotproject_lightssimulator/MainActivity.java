package com.example.iotproject_lightssimulator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    //Mqtt client and mosquitto for connection with broker
    private MqttAndroidClient client;

    //SERVER_URI holds the IP address of the Raspberry Pi as it has the broker on it.
    //The tcp is first the IPv4 address followed by the port used
    private static final String SERVER_URI = "tcp://192.168.0.83:1883";
    //TAG variable used for logging/debugging purposes. Not completely necessary for this project in particular but using such tags is a good habit to build
    private static final String TAG = "MainActivity";

    ImageView lightImage;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Getting the imageview and textview by its id
        lightImage = findViewById(R.id.lightImage);
        textView = findViewById(R.id.infoTextView);

        //Calling method connect below for connection with broker
        connect();

        //Callback. What to do when connected
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    System.out.println("Reconnected to: " + serverURI);
                    // Re-subscribe as we lost it due to new session
                    subscribe("lightModeTopicUpdate");
                } else {
                    System.out.println("Connected to: " + serverURI);
                    subscribe("lightModeTopicUpdate");
                }
            }
            @Override
            public void connectionLost(Throwable cause) {
                System.out.println("The Connection was lost.");
            }
            @Override
            public void messageArrived(String topic, MqttMessage message) throws
                    Exception {
                //Code for what should happen when a new message has arrived:

                String newMessage = new String(message.getPayload());
                System.out.println("Incoming message: " + newMessage);

                //To be able to consider time: (Couldn't use "LocalTime" as we need API min of 22 to work since thats the only android phone available to us)

                //To get current time, the hour more specifically
                Calendar calendar  = Calendar.getInstance();
                int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

                //Our time range for which to consider when controlling lights
                int startHourRange = 6;
                int endHourRange = 17;

                //Change the light depending on input:
                //Dim the lights if the command includes dim
                if (newMessage.contains("dim")) {
                    lightImage.setImageResource(R.drawable.lightdim);
                    textView.setText("Lights dimmed.");
                } //Turn off lights if command is off
                else if (newMessage.contains("off")) {
                    lightImage.setImageResource(R.drawable.lightoffcompletely);
                    textView.setText("Lights turned off.");
                } //If it's daytime turn lights on bright
                else if (newMessage.contains("on") && hourOfDay >= startHourRange && hourOfDay <= endHourRange) {
                    lightImage.setImageResource(R.drawable.lightonbright);
                    textView.setText("Lights turned on bright.");
                } //If it's late then turn them on as commanded but warmer
                else if (newMessage.contains("on")) {
                    lightImage.setImageResource(R.drawable.lightonwarm);
                    textView.setText("Lights set to 'On' but warmer since it's late.");
                }

            }
            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });
//End of callback section

    }

//Initializing and creating connection to the broker set in "SERVER_URI", in this case the IPv4 of the Raspberry pi with the broker and the port
    private void connect(){
        String clientId = MqttClient.generateClientId();
        client =
                new MqttAndroidClient(this.getApplicationContext(), SERVER_URI,
                        clientId);
        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //Connection successful:
                    Log.d(TAG, "onSuccess");
                    System.out.println(TAG + " Success. Connected to " + SERVER_URI);
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception)
                {
                    //If something went wrong when trying to connect:
                    Log.d(TAG, "onFailure");
                    System.out.println(TAG + " Oh no! Failed to connect to " + SERVER_URI);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    //End of connect method

    //Method for subscribing to topic 
    private void subscribe(String topicToSubscribe) {
        final String topic = topicToSubscribe;
        int qos = 1;
        try {
            IMqttToken subToken = client.subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    //Confirm if subscription was successful:
                    System.out.println("Subscription successful to topic: " + topic);
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    //If something goes wrong when trying to subscribe:
                    System.out.println("Failed to subscribe to topic: " + topic);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    //End of method subscribe

}