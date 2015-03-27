// Copyright (c) 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

public class BackgroundActivity extends Activity
{
    private static final String LOG_TAG = "BackgroundActivity";
    static boolean prevLaunchWasProgrammatic; // Used to set cordova.resumeType
    static BackgroundActivity topInstance; // Used for finish()ing the activity after re-parenting


    @Override
    public void onCreate(Bundle savedInstanceState) {
        // This is called only when launchBackground() is first called, and this is the activity
        // at the top of the MainActivity sandwich.
        super.onCreate(savedInstanceState);
        topInstance = this;

        final Application app = (Application)getApplicationContext();
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                app.unregisterActivityLifecycleCallbacks(this);
                activity.moveTaskToBack(true);
            }
            @Override
            public void onActivityStarted(Activity activity) {}
            @Override
            public void onActivityResumed(Activity activity) {}
            @Override
            public void onActivityPaused(Activity activity) {}
            @Override
            public void onActivityStopped(Activity activity) {}
            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override
            public void onActivityDestroyed(Activity activity) {}
        });

        Intent i = makeMainActivityIntent(this, false, Intent.FLAG_FROM_BACKGROUND | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(i);
    }

    private static Intent makeMainActivityIntent(Context context, boolean fromLauncher, int flags) {
        ComponentName foregroundActivityComponent = findMainActivityComponentName(context);
        Intent ret = new Intent();
        ret.setComponent(foregroundActivityComponent);
        ret.setFlags(flags);
        if (fromLauncher) {
            ret.setAction(Intent.ACTION_MAIN);
            ret.addCategory(Intent.CATEGORY_LAUNCHER);
        } else {
            ret.addCategory(Intent.CATEGORY_DEFAULT);
        }
        return ret;
    }

    private static ComponentName findMainActivityComponentName(Context context) {
        PackageManager pm = context.getPackageManager();
        PackageInfo packageInfo = null;
        try {
            packageInfo = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("No package info for " + context.getPackageName(), e);
        }

        for (ActivityInfo activityInfo : packageInfo.activities) {
            if ((activityInfo.flags & ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS) == 0) {
                return new ComponentName(packageInfo.packageName, activityInfo.name);
            }
        }
        throw new RuntimeException("Could not find main activity");
    }

    public static void launchBackground(Context context) {
        // To verify the state of the task stacks, use:
        //     adb shell dumpsys activity activities
        Intent activityIntent = new Intent(context, BackgroundActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_FROM_BACKGROUND | Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(activityIntent);
    }

    public static void launchForeground(Context context, boolean fromLauncher) {
        boolean isAlreadyRunning = BackgroundPlugin.pluginInstance != null;

        // When transitioning from background to foreground, the RESET_TASK_IF_NEEDED is what causes
        // the MainActivity to be "re-parented" to its own task stack rather than a new activity
        // being created on it.
        // If the launcher would contain this flag, then we wouldn't need BackgroundLauncherActivity.
        // However, it seems that on Lollipop, the flag isn't present, while on JellyBean it is.
        // It's launcher-dependent though.
        Intent intent = makeMainActivityIntent(context, fromLauncher, Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        prevLaunchWasProgrammatic = !fromLauncher;

        if (!isAlreadyRunning) {
            Log.i(LOG_TAG, "Starting foreground for first time. fromLauncher=" + fromLauncher);
        } else if (topInstance != null) {
            Log.i(LOG_TAG, "Reparenting background->foreground. fromLauncher=" + fromLauncher);
        } else {
            Log.i(LOG_TAG, "Resuming foreground activity. fromLauncher=" + fromLauncher);
        }
        // Need to use application context on older androids for intents not to be ignored :S
        context.getApplicationContext().startActivity(intent);
    }
}
