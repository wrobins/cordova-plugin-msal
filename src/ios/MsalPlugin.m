#import "MsalPlugin.h"
#import <Cordova/CDVPlugin.h>
#import <MSAL/MSAL.h>

@implementation MsalPlugin

- (void) pluginInitialize
{
    NSDictionary *settings = self.commandDelegate.settings;
    self.tenantId = [settings objectForKey:[@"tenantId" lowercaseString]];
    self.clientId = [settings objectForKey:[@"clientId" lowercaseString]];

    NSError *err = nil;
    NSError *msalError = nil;
    MSALAuthority *authority = [MSALAuthority authorityWithURL:[NSURL URLWithString:[NSString stringWithFormat:@"https://login.microsoftonline.com/%@", self.tenantId]] error:&err];
    self.config = [[MSALPublicClientApplicationConfig alloc] initWithClientId:[self clientId] redirectUri:[NSString stringWithFormat:@"msauth.%@://auth", [[NSBundle mainBundle] bundleIdentifier]] authority:authority];
    self.application = [[MSALPublicClientApplication alloc] initWithConfiguration:[self config] error:&msalError];
    self.scopes = @[@"User.Read"];
}

- (void)signInSilent:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult * pluginResult = nil;
    NSError *error = nil;
    NSArray *accounts = [[self application] allAccounts:nil];
    if ([accounts count] == 0)
    {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"No accounts found on device."];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    MSALAccount *account = accounts[0];
    if (!account)
    {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error localizedDescription]];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
        
    MSALSilentTokenParameters *silentParams = [[MSALSilentTokenParameters alloc] initWithScopes:[self scopes] account:account];
    [[self application] acquireTokenSilentWithParameters:silentParams completionBlock:^(MSALResult *result, NSError *error) {
        if (!error)
        {
            CDVPluginResult * pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:result.accessToken];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
        else
        {
            if ([error.domain isEqual:MSALErrorDomain] && error.code == MSALErrorInteractionRequired)
            {
                CDVPluginResult * pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"No account currently exists"];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            }
                
            // Other errors may require trying again later, or reporting authentication problems to the user
        }
    }];
}

- (void)signInInteractive:(CDVInvokedUrlCommand*)command
{
    MSALWebviewParameters *webParameters = [[MSALWebviewParameters alloc] initWithParentViewController:[self viewController]];

    MSALInteractiveTokenParameters *interactiveParams = [[MSALInteractiveTokenParameters alloc] initWithScopes:[self scopes] webviewParameters:webParameters];
    [[self application] acquireTokenWithParameters:interactiveParams completionBlock:^(MSALResult *result, NSError *error) {
        if (!error) 
        {
            CDVPluginResult * pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:result.accessToken];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
        else
        {
            CDVPluginResult * pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error localizedDescription]];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
    }];
}

- (void)signOut:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult = nil;
    NSError *error = nil;
    NSArray *accounts = [[self application] allAccounts:nil];
    if ([accounts count] == 0)
    {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    if ([[self application] removeAccount:accounts[0] error:&error]) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
    else
    {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error localizedDescription]];
    }
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

@end
