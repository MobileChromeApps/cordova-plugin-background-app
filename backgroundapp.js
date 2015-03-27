// Copyright (c) 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var exec = require('cordova/exec');
var channel = require('cordova/channel');

// Refer to README for possible values.
exports.resumeType = '';

exports.show = function() {
  exec(null, null, 'BackgroundPlugin', 'show', []);
};

channel.createSticky('onBackgroundAppReady');
channel.waitForInitialization('onBackgroundAppReady');
channel.onCordovaReady.subscribe(function() {
  exec(function(msg) {
    if (msg.type == 'startup') {
      if (!msg.value) {
        exports.resumeType = 'launch';
      }
      channel.initializationComplete('onBackgroundAppReady');
    } else {
      var firstResume = !exports.resumeType;
      exports.resumeType = msg.value; // 'normal' or 'programmatic'.
      if (firstResume) {
          exports.resumeType += '-launch';
      }
    }
  }, undefined, 'BackgroundPlugin', 'messageChannel', []);
});

