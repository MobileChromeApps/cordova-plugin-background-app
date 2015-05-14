// Copyright (c) 2015 The Chromium Authors. All rights reserved.

#import <Cordova/CDVPlugin.h>
#import <Cordova/CDVViewController.h>

#import <objc/runtime.h>

static void swizzleMethod(Class class, SEL destinationSelector, SEL sourceSelector);

@interface CDVViewController (MCABackgroundApp)
-(void)background_onAppWillEnterForeground:(NSNotification*)notification;
@end

@interface MCABackgroundApp : CDVPlugin {
  @public
    NSString* _callbackId;
    BOOL _hasEverLaunched;
}
@end

@implementation MCABackgroundApp

+ (void)load
{
    // Ensure our method is called before the "resume" event.
    swizzleMethod([CDVViewController class], NSSelectorFromString(@"onAppWillEnterForeground:"), @selector(background_onAppWillEnterForeground:));
}

- (void)onReset {
    _hasEverLaunched = NO;
    _callbackId = nil;
}

- (void)messageChannel:(CDVInvokedUrlCommand*)command
{
    _hasEverLaunched = ([[UIApplication sharedApplication] applicationState] != UIApplicationStateBackground);
    _callbackId = command.callbackId;
    [self sendResult:@{
        @"type": @"startup",
        @"value": _hasEverLaunched ? @NO : @YES
    }];
}

- (void)sendResult:(NSDictionary*)payload {
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:payload];
    [pluginResult setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:_callbackId];
}

@end

@implementation CDVViewController (MCABackgroundApp)

- (void)background_onAppWillEnterForeground:(NSNotification*)notification {
    MCABackgroundApp* plugin = [self getCommandInstance:@"backgroundplugin"];
    [plugin sendResult:@{
                         @"type": @"foreground",
                         @"value": plugin->_hasEverLaunched ? @"normal" : @"normal-launch"
                         }];
    plugin->_hasEverLaunched = YES;
    [self background_onAppWillEnterForeground:notification];
}

@end

static void swizzleMethod(Class class, SEL destinationSelector, SEL sourceSelector)
{
    Method destinationMethod = class_getInstanceMethod(class, destinationSelector);
    Method sourceMethod = class_getInstanceMethod(class, sourceSelector);

    // If the method doesn't exist, add it.  If it does exist, replace it with the given implementation.
    if (class_addMethod(class, destinationSelector, method_getImplementation(sourceMethod), method_getTypeEncoding(sourceMethod))) {
        class_replaceMethod(class, destinationSelector, method_getImplementation(destinationMethod), method_getTypeEncoding(destinationMethod));
    } else {
        method_exchangeImplementations(destinationMethod, sourceMethod);
    }
}

