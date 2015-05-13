// Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.util.Log;

public class BackgroundPlugin extends CordovaPlugin {
    private static final String LOG_TAG = "BackgroundPlugin";

    // This reference lets us know whether or not the app is currently running.
    static BackgroundPlugin pluginInstance;
    private CallbackContext messageChannel;
    private boolean isMainInstance;
    private boolean resumeEventSeen;

    @Override
    public void pluginInitialize() {
        isMainInstance = pluginInstance == null;
        if (isMainInstance) {
            pluginInstance = this;
        }
    }

    @Override
    public void onReset() {
        messageChannel = null;
        if (isMainInstance) {
            releasePluginMessageChannels();
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        // Ignore the resume on start-up, but assume messageChannel means there was no onResume on start-up.
        // Cordova has flip-flopped on whether to send this event on start-up :S.
        if (!resumeEventSeen && messageChannel == null) {
            resumeEventSeen = true;
            return;
        }
        if (isMainInstance && BackgroundActivity.topInstance != null) {
            final BackgroundActivity topInstance = BackgroundActivity.topInstance;
            BackgroundActivity.topInstance = null;
            // Kill off the BackgroundActivity task stack. Leaving it around causes the next call
            // to BackgroundActivity.launchBackground() to have MainActivity in the wrong task stack.
            webView.getView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(LOG_TAG, "Finishing background activity now that foreground is alive");
                    topInstance.finish();
                }
            }, 50);
        }
        String switchType = BackgroundActivity.prevLaunchWasProgrammatic ? "programmatic" : "normal";
        sendEventMessage("foreground", switchType);

        if (isMainInstance) {
            webView.getView().post(new Runnable() {
                @Override
                public void run() {
                    BackgroundActivity.prevLaunchWasProgrammatic = false;
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        messageChannel = null;
        if (isMainInstance) {
            pluginInstance = null;
            releasePluginMessageChannels();
        }
    }

    @Override
    public boolean execute(String action, CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        if ("messageChannel".equals(action)) {
            messageChannel = callbackContext;
            sendEventMessage("startup", BackgroundActivity.topInstance != null);
            return true;
        } else if ("show".equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    show(callbackContext);
                }
            });
            return true;
        }
        return false;
    }

    private void sendEventMessage(String action, Object value) {
        if (messageChannel == null) {
            Log.w(LOG_TAG, "Message being dropped since channel not yet established: " + action);
            return;
        }
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", action);
            obj.put("value", value);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Failed to create background event", e);
        }
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, obj);
        pluginResult.setKeepCallback(true);
        messageChannel.sendPluginResult(pluginResult);
    }

    private void show(final CallbackContext callbackContext) {
        Activity activity = cordova.getActivity();

        if (!activity.hasWindowFocus()) {
            BackgroundActivity.launchForeground(activity, false);
        }
        callbackContext.success();
    }

    private static void releasePluginMessageChannels() {
        // Release the message channel for all plugins using our background event handler
        //  - The Cordova Plugin framework does not provide a direct way to handle the life cycle
        //    events for plugins (e.g. onReset, onDestroy)
        //  - To avoid extra boilerplate in any plugin using the event handler, will cleanup all
        //    the message channels for plugins here
        BackgroundEventHandler.releaseMessageChannels();
    }
}
