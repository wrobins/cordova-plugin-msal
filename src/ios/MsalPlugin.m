#import "MsalPlugin.h"
#import <Cordova/CDVPlugin.h>
#import <MSAL/MSAL.h>

@implementation MsalPlugin

- (void)msalInit:(CDVInvokedUrlCommand *)command
{
    NSDictionary *settings = self.commandDelegate.settings;
    self.tenantId = [settings objectForKey:[@"tenantId" lowercaseString]];
    self.clientId = [settings objectForKey:[@"clientId" lowercaseString]];

    NSError *err = nil;
    NSError *msalError = nil;
    CDVPluginResult *result = nil;
    
    MSALAuthority *defaultAuthority;
    NSMutableArray<MSALAuthority *> *allAuthorities = [[NSMutableArray alloc] init];

    NSString *argument = [command.arguments objectAtIndex:0];
    NSData *json = [argument dataUsingEncoding:NSUTF8StringEncoding];
    id obj = [NSJSONSerialization JSONObjectWithData:json options:0 error:&err];
    if (err)
    {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[NSString stringWithFormat:@"Error parsing options object: %@", err]];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        return;
    }
    NSDictionary *options = (NSDictionary *)obj;
    NSArray *authorities = [options objectForKey:@"authorities"];
    for (NSDictionary *a in authorities)
    {
        MSALAuthority *authority;
        MSALAudienceType audience = MSALAzureADAndPersonalMicrosoftAccountAudience;
        if ([[a objectForKey:@"audience"] isEqualToString:@"AzureADMyOrg"])
        {
            audience = MSALAzureADMyOrgOnlyAudience;
        }
        else if ([[a objectForKey:@"audience"] isEqualToString:@"AzureADMultipleOrgs"])
        {
            audience = MSALAzureADMultipleOrgsAudience;
        }
        else if ([[a objectForKey:@"audience"] isEqualToString:@"PersonalMicrosoftAccount"])
        {
            audience = MSALPersonalMicrosoftAccountAudience;
        }
        MSALAzureCloudInstance cloudInstance = MSALAzurePublicCloudInstance;
        if (([[a objectForKey:@"cloudInstance"] isEqualToString:@"MSALAzureChinaCloudInstance"]))
        {
            cloudInstance = MSALAzureChinaCloudInstance;
        }
        if (([[a objectForKey:@"cloudInstance"] isEqualToString:@"MSALAzureGermanyCloudInstance"]))
        {
            cloudInstance = MSALAzureGermanyCloudInstance;
        }
        if (([[a objectForKey:@"cloudInstance"] isEqualToString:@"MSALAzureUsGovernmentCloudInstance"]))
        {
            cloudInstance = MSALAzureUsGovernmentCloudInstance;
        }
        if ([(NSString *)[a objectForKey:@"type"] isEqualToString:@"AAD"])
        {
            NSString *rawTenant = nil;
            if (audience == MSALAzureADMyOrgOnlyAudience)
            {
                rawTenant = [self tenantId];
            }
            authority = [[MSALAADAuthority alloc] initWithCloudInstance:cloudInstance audienceType:audience rawTenant:rawTenant error:&err];
        }
        else
        {
            NSURL *authorityUrl = [[NSURL alloc] initWithString:(NSString *)[a objectForKey:@"authorityUrl"]];
            authority = [[MSALB2CAuthority alloc] initWithURL:authorityUrl error:&err];
        }
        if (err)
        {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[err.userInfo objectForKey:@"MSALErrorDescriptionKey"]];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
            return;
        }
        [allAuthorities addObject:authority];
        if ([a objectForKey:@"default"] == [NSNumber numberWithBool:YES]) {
            defaultAuthority = authority;
        }
    }
    
    self.config = [[MSALPublicClientApplicationConfig alloc] initWithClientId:[self clientId] redirectUri:[NSString stringWithFormat:@"msauth.%@://auth", [[NSBundle mainBundle] bundleIdentifier]] authority:defaultAuthority];
    [self.config setKnownAuthorities:[[NSArray<MSALAuthority *> alloc] initWithArray:allAuthorities copyItems:YES]];
    [self.config setMultipleCloudsSupported:[options objectForKey:@"multipleCloudsSupported"] == [NSNumber numberWithBool:YES]];
    if ([options objectForKey:@"brokerRedirectUri"] == [NSNumber numberWithBool:NO])
    {
        MSALGlobalConfig.brokerAvailability = MSALBrokeredAvailabilityNone;
    }
    self.application = [[MSALPublicClientApplication alloc] initWithConfiguration:[self config] error:&msalError];
    self.scopes = [options objectForKey:@"scopes"];
    self.accountMode = [options objectForKey:@"accountMode"];
    if (msalError)
    {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[NSString stringWithFormat:@"Error creating MSAL configuration: %@", [msalError.userInfo objectForKey:@"MSALErrorDescriptionKey"]]];
    }
    else
    {
        result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    }
    self.isInit = YES;
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

- (void)getAccounts:(CDVInvokedUrlCommand *)command
{
    if (!self.isInit)
    {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"No configuration has been set yet. Call msalInit() before calling this."];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }
    else
    {
        NSMutableArray<NSDictionary *> *accounts = [[NSMutableArray<NSDictionary *> alloc] init];
        for (MSALAccount *account in [[self application] allAccounts:nil])
        {
            [accounts addObject:@{ @"id" : [account identifier], @"username" : [account username]}];
        }
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArray:accounts];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }
}

- (void)startLogger:(CDVInvokedUrlCommand *)command
{
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_NO_RESULT];
    [result setKeepCallbackAsBool:YES];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    
    if ([command.arguments objectAtIndex:0] == [NSNumber numberWithBool:NO]) {
        MSALGlobalConfig.loggerConfig.piiEnabled = NO;
    }
    else
    {
        MSALGlobalConfig.loggerConfig.piiEnabled = YES;
    }
    
    if ([[command.arguments objectAtIndex:1] isEqualToString:@"ERROR"])
    {
        MSALGlobalConfig.loggerConfig.logLevel = MSALLogLevelError;
    }
    else if ([[command.arguments objectAtIndex:1] isEqualToString:@"WARNING"])
    {
        MSALGlobalConfig.loggerConfig.logLevel = MSALLogLevelWarning;
    }
    else if ([[command.arguments objectAtIndex:1] isEqualToString:@"INFO"])
    {
        MSALGlobalConfig.loggerConfig.logLevel = MSALLogLevelInfo;
    }
    else
    {
        MSALGlobalConfig.loggerConfig.logLevel = MSALLogLevelVerbose;
    }
    
    [MSALGlobalConfig.loggerConfig setLogCallback:^(MSALLogLevel level, NSString * _Nullable message, BOOL containsPII) {
        NSMutableDictionary *logEntry = [[NSMutableDictionary alloc] initWithCapacity:6];
        NSCharacterSet *separators = [NSCharacterSet characterSetWithCharactersInString:@" []"];
        NSArray *messageData = [message componentsSeparatedByCharactersInSet:separators];
        @try
        {
            NSString *timestamp = [[[messageData objectAtIndex:7] stringByAppendingString:@" "] stringByAppendingString:[messageData objectAtIndex:8]];
            NSInteger threadId = [[[[messageData objectAtIndex:0] componentsSeparatedByString:@"="] objectAtIndex:1] intValue];
            NSString *correlationId;
            NSString *logLevel;
            if (level == MSALLogLevelInfo)
            {
                logLevel = @"INFO";
            }
            else if (level == MSALLogLevelWarning)
            {
                logLevel = @"WARNING";
            }
            else if (level == MSALLogLevelError)
            {
                logLevel = @"ERROR";
            }
            else
            {
                logLevel = @"VERBOSE";
            }
            NSString *messageText;
            
            // The MSAL Log Text can be in one of two formats which changes how we have to parse it out.
            if ([[messageData objectAtIndex:9] isEqualToString:@"-"])
            {
                correlationId = [messageData objectAtIndex:10];
                messageText = [[message componentsSeparatedByString:@"[MSAL] "] objectAtIndex:1];
            }
            else
            {
                correlationId = @"UNSET";
                messageText = [[message componentsSeparatedByString:@"] "] objectAtIndex:1];
            }
            
            [logEntry setValue:timestamp forKey:@"timestamp"];
            [logEntry setValue:[NSNumber numberWithInteger:threadId] forKey:@"threadId"];
            [logEntry setValue:correlationId forKey:@"correlationId"];
            [logEntry setValue:logLevel forKey:@"logLevel"];
            [logEntry setValue:[NSNumber numberWithBool:containsPII] forKey:@"containsPII"];
            [logEntry setValue:messageText forKey:@"message"];
            
            CDVPluginResult *logUpdate = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:logEntry];
            [logUpdate setKeepCallbackAsBool:YES];
            [self.commandDelegate sendPluginResult:logUpdate callbackId:command.callbackId];
        }
        @catch (NSException *ex)
        {
            // If the format of this log message is weird, just ignore it
        }
    }];
}

- (void)signInSilent:(CDVInvokedUrlCommand*)command
{
    if (!self.isInit)
    {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"No configuration has been set yet. Call msalInit() before calling this."];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }
    else
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
        MSALAccount *account = nil;
        if ([self.accountMode isEqualToString:@"SINGLE"])
        {
            account = accounts[0];
        }
        else
        {
            NSError *error = nil;
            NSString *accountId = [command.arguments objectAtIndex:0];
            account = [[self application] accountForIdentifier:accountId error:&error];
            if (error)
            {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error.userInfo objectForKey:@"MSALErrorDescriptionKey"]];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                return;
            }
        }
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
}

- (void)signInInteractive:(CDVInvokedUrlCommand*)command
{
    if (!self.isInit)
    {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"No configuration has been set yet. Call msalInit() before calling this."];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }
    else
    {
        MSALWebviewParameters *webParameters = [[MSALWebviewParameters alloc] initWithParentViewController:[self viewController]];
        
        NSError *err = nil;
        CDVPluginResult *result = nil;
        
        NSString *loginHint = (NSString *)[command.arguments objectAtIndex:0];
        NSString *prompt = (NSString *)[command.arguments objectAtIndex:1];
        NSString *webViewType = (NSString *)[command.arguments objectAtIndex:4];
        
        if (err)
        {
            result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[NSString stringWithFormat:@"Error parsing options object: %@", err]];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
            return;
        }
        
        if (![webViewType isEqual:[NSNull null]]) {
            if ([webViewType isEqualToString:@"WK_WEB_VIEW"])
            {
                webParameters.webviewType = MSALWebviewTypeWKWebView;
            }
            if ([webViewType isEqualToString:@"SAFARI_VIEW_CONTROLLER"])
            {
                webParameters.webviewType = MSALWebviewTypeSafariViewController;
            }
        }

        MSALInteractiveTokenParameters *interactiveParams = [[MSALInteractiveTokenParameters alloc] initWithScopes:[self scopes] webviewParameters:webParameters];
        
        if (![loginHint isEqual:[NSNull null]])
        {
            interactiveParams.loginHint = loginHint;
        }
        
        if (![prompt isEqual:[NSNull null]]) {
            if ([prompt isEqualToString:@"SELECT_ACCOUNT"])
            {
                interactiveParams.promptType = MSALPromptTypeSelectAccount;
            }
            if ([prompt isEqualToString:@"LOGIN"])
            {
                interactiveParams.promptType = MSALPromptTypeLogin;
            }
            if ([prompt isEqualToString:@"CONSENT"])
            {
                interactiveParams.promptType = MSALPromptTypeConsent;
            }
        }
        
        NSArray *queryStrings = [command.arguments objectAtIndex:2];
        NSMutableDictionary *extraQueryParameers = [[NSMutableDictionary alloc] init];
        for (NSDictionary *queryString in queryStrings)
        {
            [extraQueryParameers setObject:[queryString objectForKey:@"value"] forKey:[queryString objectForKey:@"param"]];
        }
        interactiveParams.extraQueryParameters = [[NSDictionary alloc] initWithDictionary:extraQueryParameers];
        interactiveParams.extraScopesToConsent = [command.arguments objectAtIndex:3];;
        
        [[self application] acquireTokenWithParameters:interactiveParams completionBlock:^(MSALResult *result, NSError *error) {
            if (!error)
            {
                CDVPluginResult * pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:result.accessToken];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            }
            else
            {
                CDVPluginResult * pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error.userInfo objectForKey:@"MSALErrorDescriptionKey"]];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            }
        }];
    }
}

- (void)signOut:(CDVInvokedUrlCommand*)command
{
    if (!self.isInit)
    {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"No configuration has been set yet. Call msalInit() before calling this."];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }
    else
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
        MSALAccount *account = nil;
        if ([self.accountMode isEqualToString:@"SINGLE"])
        {
            account = accounts[0];
        }
        else
        {
            NSError *error = nil;
            NSString *accountId = [command.arguments objectAtIndex:0];
            account = [[self application] accountForIdentifier:accountId error:&error];
            if (error)
            {
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error localizedDescription]];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                return;
            }
        }
        if ([[self application] removeAccount:account error:&error]) {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        }
        else
        {
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error localizedDescription]];
        }
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
}

@end
