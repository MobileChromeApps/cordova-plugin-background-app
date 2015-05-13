# Start an App Without Showing the Activity

The purpose of this plugin is to enable notifications, alarms, etc to
re-start your app and fire callbacks without the app causing any visual
cues to the user. In essense, it allows starting an app as an Android
service, but does so without the use of an actual service.

## Status

Works on iOS and Android.

iOS values for resumeType limited to: `''`, `'launch'`, `'normal'`, `'normal-launch'`

## Usage

To see it in action, try the [example app](https://github.com/MobileChromeApps/cordova-plugin-background-app/tree/master/example-app).

For how to create your own plugin that uses it, look at
[cordova-plugin-chrome-apps-notifications](https://github.com/MobileChromeApps/cordova-plugin-chrome-apps-notifications)
or
[cordova-plugin-chrome-apps-alarms](https://github.com/MobileChromeApps/cordova-plugin-chrome-apps-alarms)

### `cordova.backgroundapp.resumeType` (string)

After the `deviceready` event, this will either be:
* `''` - When started as a service.
* `'launch'` - When started normally.

After a `resume` event, this will be one of:
* `'normal'` - Triggered by an external intent (launcher / task switcher). App was running normally, but had been backgrounded.
* `'normal-launch'` - Triggered by an external intent (launcher / task switcher). App was running as a service.
* `'programmatic'` - Triggered by a call to `BackgroundActivity.launchForeground()`. App was running normally, but had been backgrounded.
* `'programmatic-launch'` - Triggered by a call to `BackgroundActivity. App was running as a service.

The normal way to use this plugin:

    document.addEventListener("deviceready", function() {
        if (cordova.backgroundapp.resumeType == 'launch') {
            renderUi();
        }
    }, false);
    document.addEventListener("resume", function() {
        if (cordova.backgroundapp.resumeType == 'normal-launch') {
            renderUi();
        } else if (cordova.backgroundapp.resumeType == 'programmatic-launch') {
            // You launched programatically (through cordova.backgroundapp.show() perhaps)
            // so you should have already called renderUi() from where you called .show().
        }
    }, false);

### `cordova.backgroundapp.show()`

Brings the app to the foreground. E.g. Call this in response to a user clicking on a notification.

This symbol will be null on iOS, since iOS has not yet implemented this functionality.

## Implementation Details

The goal is ultimately to be able to run the app in an Android service, but
because many plugins utilize CordovaInterface.getActivity(), a background Activity
rather than a Service is more viable.

* Uses gradle build rule to remove your default `<intent-filter>`
* Uses a `BackgroundLauncherActivity` to recieve launch intent and start your main activity.
* If you are an already shipping app, you should use an `<activity-alias>` to make your
  previous main activity point to `BackgroundLauncherActivity`, and then rename your
  main activity to not conflict with the alias. This is so that
  [launcher shortcuts](http://android-developers.blogspot.ca/2011/06/things-that-cannot-change.html)
  continue to function correctly.

## Known Issues

- When the app goes from not running -> running in background, if you are currently in the
  task switcher, the task switcher will close.
- On Lollipop, backgrounded app shows in recents list when no other recents exists ([fixed in MR1](https://code.google.com/p/android/issues/detail?id=78862))

# Release Notes

## 2.0.1 (May 13, 2015)
* Fix `cordova.backgroundapp.resumeType` being `'launch'` when running in background on Android
* Added sample app to git repo

## 2.0.0 (April 29, 2015)
* Introduced `cordova.backgroundapp.resumeType` and `cordova.backgroundapp.show()`
* Rewrote Android implementation to not require translucent theme
* Minimal iOS support added (basic `resumeType` values only)

# 1.0.0 (March 2015)
* Initial release
