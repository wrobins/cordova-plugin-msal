#import <Cordova/CDVPlugin.h>
#import <MSAL/MSAL.h>

@interface MsalPlugin : CDVPlugin

@property MSALPublicClientApplicationConfig *config;
@property MSALPublicClientApplication *application;
@property NSArray<NSString *> *scopes;
@property NSString *clientId;
@property NSString *tenantId;
@property NSString *accountMode;

- (void)msalInit:(CDVInvokedUrlCommand*)command;
- (void)getAccounts:(CDVInvokedUrlCommand*)command;
- (void)signInSilent:(CDVInvokedUrlCommand*)command;
- (void)signInInteractive:(CDVInvokedUrlCommand*)command;
- (void)signOut:(CDVInvokedUrlCommand*)command;

@end
