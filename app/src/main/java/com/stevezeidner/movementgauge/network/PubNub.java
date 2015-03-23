package com.stevezeidner.movementgauge.network;

import android.util.Log;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Service to abstract some of PubNub's functionality
 */
public class PubNub {
    private Pubnub pubnub;
    private String channel;
    private static final String LOG_TAG = PubNub.class.getSimpleName();

    public PubNub(String pub, String sub, String sec, boolean ssl, String channel) {
        pubnub = new Pubnub(
                pub,
                sub,
                sec,
                "",
                ssl
        );

        this.channel = channel;
    }

    public void Subscribe() {
        try {
            pubnub.subscribe(channel, new Callback() {
                @Override
                public void connectCallback(String channel, Object message) {
                    Log.d(LOG_TAG, "SUBSCRIBE : CONNECT on channel:" + channel
                            + " : " + message.getClass() + " : "
                            + message.toString());
                }

                @Override
                public void disconnectCallback(String channel, Object message) {
                    Log.d(LOG_TAG, "SUBSCRIBE : DISCONNECT on channel:" + channel
                            + " : " + message.getClass() + " : "
                            + message.toString());
                }

                public void reconnectCallback(String channel, Object message) {
                    Log.d(LOG_TAG, "SUBSCRIBE : RECONNECT on channel:" + channel
                            + " : " + message.getClass() + " : "
                            + message.toString());
                }

                @Override
                public void successCallback(String channel, Object message) {
                    Log.d(LOG_TAG, "SUBSCRIBE : " + channel + " : "
                            + message.getClass() + " : " + message.toString());
                }

                @Override
                public void errorCallback(String channel, PubnubError error) {
                    Log.e(LOG_TAG, "SUBSCRIBE : ERROR on channel " + channel
                            + " : " + error.toString());
                }
            });
        } catch (PubnubException e) {
            if (e.getPubnubError() != null) {
                Log.e(LOG_TAG, "" + e.getPubnubError());
            }
        }
    }

    public void Unsubscribe() {
        pubnub.unsubscribe(channel);
    }

    public void Publish(JSONObject jsonObject) {
        pubnub.publish(channel, jsonObject, new Callback() {

            @Override
            public void successCallback(String channel, Object response) {
                Log.d(LOG_TAG, response.toString());
            }

            @Override
            public void errorCallback(String channel, PubnubError error) {
                Log.e(LOG_TAG, error.toString());
            }

        });
    }

    public void Publish(JSONArray jsonArray) {
        pubnub.publish(channel, jsonArray, new Callback() {

            @Override
            public void successCallback(String channel, Object response) {
                Log.d(LOG_TAG, response.toString());
            }

            @Override
            public void errorCallback(String channel, PubnubError error) {
                Log.e(LOG_TAG, error.toString());
            }

        });
    }
}
