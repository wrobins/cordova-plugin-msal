//
//  AppDelegate+MsalCallback.h
//  KSUMobile
//
//  Created by wrobins on 12/6/19.
//

#import "AppDelegate.h"

@interface AppDelegate (MsalCallback)

- (BOOL)application:(UIApplication *)app
            openURL:(NSURL *)url
            options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options;

@end

/* AppDelegate_MsalCallback_h */
