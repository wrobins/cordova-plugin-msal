#import <Cordova/CDVPlugin.h>
#import <MSAL/MSAL.h>

@interface MsalPlugin : CDVPlugin

@property MSALPublicClientApplicationConfig *config;
@property MSALPublicClientApplication *application;
@property NSArray<NSString *> *scopes;
@property MSALAccount *account;
@property NSString *clientId;
@property NSString *tenantId;

- (void) pluginInitialize;

- (void)signInSilent:(CDVInvokedUrlCommand*)command;
- (void)signInInteractive:(CDVInvokedUrlCommand*)command;
- (void)signOut:(CDVInvokedUrlCommand*)command;

@end
