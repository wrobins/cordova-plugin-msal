//
//  AppDelegate+MsalCallback.m
//  KSUMobile
//
//  Created by wrobins on 12/6/19.
//

#import "AppDelegate+MsalCallback.h"
#import <MSAL/MSAL.h>

@implementation AppDelegate (MsalCallback)

- (BOOL)application:(UIApplication *)app
            openURL:(NSURL *)url
            options:(NSDictionary<UIApplicationOpenURLOptionsKey,id> *)options
{
    return [MSALPublicClientApplication handleMSALResponse:url
                                         sourceApplication:options[UIApplicationOpenURLOptionsSourceApplicationKey]];
}

@end
