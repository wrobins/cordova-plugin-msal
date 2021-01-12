# Cordova MSAL Plugin
So you want to integrate your mobile app with Microsoft's authentication service?
## Basic Assumptions and Requirements
This plugin implements [Microsoft's MSAL plugin](https://docs.microsoft.com/en-us/azure/active-directory/develop/msal-overview) for Android and iOS. I'm assuming you're here because you've already read their documentation and understand how to configure Azure AD authentication for your organization and are simply looking for an existing Cordova wrapper to implement it on the mobile side.
## How do I install it?
You can specify three variables during installation: the tenant ID and client ID of your Identity Platform, and, if you're building for Android, a base64 sha1 hash of your keystore file. The latter of which can be obtained like this:
<pre>
keytool -exportcert -alias yourkeystorealias -keystore path/to/your/keystore/file.keystore | openssl sha1 -binary | openssl base64
</pre>
If you aren't using AzureADMyOrg as one of your authorities, you can omit TENANT_ID, and if you're only building for iOS, you can omit KEY_HASH, but you really need to provide CLIENT_ID.
### If you're using a CLI:
<pre>
cordova plugin add cordova-plugin-msal --variable TENANT_ID=your-tenant-guid-here --variable CLIENT_ID=your-client-guid-here --variable KEY_HASH=S0m3K3yh4shH3re=
</pre>
### If you're using OutSystems
You should use my [forge component](https://www.outsystems.com/forge/Component_Overview.aspx?ProjectId=8038). But if you want to implement a wrapper yourself, or if you're here because you're using that component and you want additional documentation, continue reading:

The latest releases of this plugin use AndroidX, which requires MABS 6.3 or later. MABS 7 beta or later enables AndroidX by default, but if you're using MABs 6x, you'll need to follow [OutSystems' documentation](https://success.outsystems.com/Documentation/11/Delivering_Mobile_Apps/Mobile_Apps_Build_Service/Building_apps_with_AndroidX) to enable AndroidX in your project.

If you can't use a recent MABS version, be sure to grab a release tagged with OutSystems-*. I have a separate branch for OutSystems because MABS versions prior to 6.3 do not support AndroidX features, which are now a standard part of the Android MSAL library. I had to do some finagling with a custom build of the library with AndroidX features stripped out to avoid getting an error trying to build it with MABS.

A side effect of this is the authorizationUserAgent option is locked on WEBVIEW for those versions, since the other options rely on AndroidX.

Here's the JSON you'll need to configure your plugin. If you only have one environment and build, you can put it in your extensibility configuration in your wrapper application. But you probably have debug/release builds in multiple environments with multiple Azure clients/tenants, so LifeTime is probably the best place to manage your extensibility configuration JSON. Open your wrapper application implementing this plugin in LifeTime and click the Settings link near the application's title with the gear icon. Select your environment in the dropdown near the application's title, and scroll down to the Advanced section. Under Extensibility Configurations, tick the Custom > radial and paste your JSON with that environment's variables there:
<pre>
{
    "plugin": {
        "url": "https://github.com/wrobins/cordova-plugin-msal.git#v3.0.0-alpha.0",
        "variables": [
            {
                "name": "TENANT_ID",
                "value": "your-tenant-guid-here-optional"
            },
            {
                "name": "CLIENT_ID",
                "value": "your-client-guid-here-required"
            },
            {
                "name": "KEY_HASH",
                "value": "S0m3K3yh4shH3re="
            }
        ]
    }
}
</pre>
## How do I use it?
Invoke this plugin following the same logic as you would for any Cordova plugin. You probably want to make sure it has loaded correctly before using it:
```js
let isAvailable = typeof(cordova.plugins.msalPlugin) !== "undefined";
```
Then, call msalInit() to configure how you want your MSAL instance to work.
```js
window.cordova.plugins.msalPlugin.msalInit(function() {
    // Success logic goes here
},
function (err) {
    // err has your exception message
}, options);
```
The options parameter is an object that contains all of your MSAL configuration items, and is documented below. You can pass as much or as little of this object as you would like, or not even pass it at all. The full object, though, with all of the default options, looks like this:
```js
{
    authorities: [
        {
            type: 'AAD',
            audience: 'AzureADandPersonalMicrosoftAccount',
            authorityUrl: '',
            cloudInstance: 'MSALAzurePublicCloudInstance',
            default: true
        }
    ],
    authorizationUserAgent: 'DEFAULT',
    multipleCloudsSupported: false,
    brokerRedirectUri: false,
    accountMode: 'SINGLE',
    scopes: ['User.Read']
}
```
Like I said before, this readme assumes basic knowledge of MSAL and you should look at Microsoft's documentation for how most of these attributes work, as they are named very similar both in this library and in all native platforms. But here is a basic refresher of what each option does and how to use it in this plugin.

Once again, you can omit any of these attributes in your options object and the default ones will be supplied to our native code, which should work for most developers. You can even not pass in this third argument at all, and a complete default options object will be used.
#### authorities
This tells MSAL where it should look to find the client's account and redirect them to login. You can specify one or more. Default: one authority object (see below)
##### type
Can be either 'AAD' or 'B2C'. Default: 'AAD'
##### audience
What kind of accounts are you allowing (personal, work/school, both)? Can be one of the following: 'AzureADandPersonalMicrosoftAccount' or 'AzureADMyOrg' or 'AzureADMultipleOrgs' or 'PersonalMicrosoftAccount'. If you use 'AzureADMyOrg' make sure you specified the TENANT_ID variable when you installed this plugin. Default: 'AzureADandPersonalMicrosoftAccount'
##### authorityUrl
If this authority type is 'B2C' this is required - specify the literal URL you would redirect your user to sign in (ex. 'https://login.microsoftonline.com/tfp/contoso.onmicrosoft.com/B2C_1_SISOPolicy/'). If type is 'AAD' then leave blank. Default: ''
##### cloudInstance
This specifies the locale of your organization's Azure instance. Can be one of the following: 'MSALAzurePublicCloudInstance', 'MSALAzureChinaCloudInstance', 'MSALAzureGermanyCloudInstance', or 'MSALAzureUsGovernmentCloudInstance'. Default: 'MSALAzurePublicCloudInstance'
##### default
Boolean supplied if your authorities array contains multiple objects. Tells MSAL to try this authority first when signing in a user. Default: true
#### authorizationUserAgent
This is for Android - tells MSAL which webview to use when redirecting a user to sign in interactively (stay inside the native app's webview or use the device's external browser). Can be one of the following: 'DEFAULT' 'BROWSER' or 'WEBVIEW'. Default: 'DEFAULT'.
#### multipleCloudsSupported
If your organization supports multiple national clouds, set this to true. Otherwise, especially if you don't know what multiple national clouds means, leave this at false. Default: false
#### brokerRedirectUri
This is another Android option (boolean). Set to true if you want your application to have access to Microsoft accounts stored on the device's account broker, such as the built in broker or the Microsoft Authenticator app. This doesn't apply to iOS because by default MSAL already tries to use the account broker in Safari for this purpose. Default: false
#### accountMode
This is either 'SINGLE' or 'MULTIPLE' and controls the behavior of signing in and out. In single mode, MSAL only uses one account at a time, and signs in the user silently with that account if you call signInSilent(). In multiple account mode, you can support multiple accounts in your app, but you need to manage MSAL's list of those accounts (see below) and pass in which account you want to sign in and out of unless you're doing interactive sign-in. Default: 'SINGLE'
#### scopes
This is a text array and represents the information MSAL requests from the Graph API during its operation. In most cases you won't need to change this, and only do so if you really know what you're doing. Default: ['User.Read']

Ok, you have your plugin initialized with your organization's configuration. Here's how you sign users in and out:
### Single Client
Check to see if the user has an account cached with your app:
```js
window.cordova.plugins.msalPlugin.signInSilent(
    function(resp) {
        // resp is an object containing information about the signed in user, see below.
    }, 
    function(err) {
        // err probably says "No accounts found" but maybe other debugging info
        // Don't show this to the user; just use it for debugging.
        // Here's where you either call the next prompt or wait for the user
    }
);
```
If you don't have an account, then either prompt immediately to sign in interactively (see below) or, if your app has guest access, do nothing and show your user an interface with options for signing in or continuing as a guest, wait for them to pick one. (Or don't do either. This plugin won't do you any good but it's your app.) If they choose to sign in, then start that flow with:
```js
window.cordova.plugins.msalPlugin.signInInteractive(
    function(resp) {
        // resp is an object containing information about the signed in user, see below.
    }, 
    function(err) {
        // Usually if we get an error it just means the user cancelled the
        // signin process and closed the popup window. Handle this however
        // you want, depending again if you want guest access or not.
    }
);
```
Signing the user out is as simple as calling:
```js
window.cordova.plugins.msalPlugin.signOut(
    function(msg) {
        // account is an object containing information about the signed in user, see below.
    }, 
    function(err) {
        // An error here usually either means you accidentally tried to
        // sign out someone who wasn't signed in, or there was a problem
        // communicating with the server.
    }
);
```
### Response Object
The object returned by signInInteractive() and signInSilent() will look something like this:
```js
{
    token: 'eyJ0eXAiOiJKV1QiLCJub...',
    account: {
        id: 'abc-someguid-123',
        username: 'wrobins@myemailaddr.com',
        claims: [
            {key: "name", value: "Robins, Walter"},
            {key: "ver", value: "2.0"},
            { ... }
        ]
    }
}
```
### token
This is a JWT that you can use to make any call you need to the Microsoft Graph API. You can read more about it [here](https://docs.microsoft.com/en-us/graph/overview), but here's a simple request you can make with it:
<pre>
GET https://graph.microsoft.com/v1.0/me
'Authorization': 'Bearer resp.token'
</pre>
You'll get an object back something like:
```js
{
  "@odata.context": "https://graph.microsoft.com/v1.0/$metadata#users/$entity",
  "businessPhones": [
    "+12345678910"
  ],
  "displayName": "Robins, Walter",
  "givenName": "Walter",
  "jobTitle": "Developer",
  "mail": null,
  "mobilePhone": null,
  "officeLocation": null,
  "preferredLanguage": null,
  "surname": "Robins",
  "userPrincipalName": "wrobins@myemailaddr.com",
  "id": "myaccount-guid-1234"
}
```
### account
This object contains information about the account returned by Microsoft.
#### id
This is a unique GUID that Microsoft has assigned to this account.
#### username
This is usually the account's email address that they would use to sign in.
#### claims
This is an array of key/value pairs that, depending on the organization, tenant, account, can contain lots of different pieces of information about the account, such as the user's given name, audience and tenant information, as well as the the token's issue and expiration date. But you'll have to play around with this to see what it contains.

### Multiple Clients
Operation is very similar to single account mode, but with one extra method to get the accounts you need: getAccounts(). It returns an array of objects to represent accounts found in your app, defined exactly like the signin response objects outlined above. Use it to see if you need to sign in a user interactively or manually. Your signInSilent() and signOut() methods will take an account id (the GUID, not the username) as an argument to pick which account you're working with.

Again, your logic might look more separate if you want to have guest access to your app, but this code without guest access should give you an idea of how this plugin works:
```js
window.cordova.plugins.msalPlugin.getAccounts(
    function(accounts) {
        if (accounts.length === 0) {
            window.cordova.plugins.msalPlugin.signInInteractive(
                function(resp) {
                    doMySigninBusinessLogic(resp);
                },
                function(err) {
                     myAwesomeErrorHandler.handlerError(err);
                }
            );
        } else {
            myAwesomeInterface.showAccountsList(accounts).waitForUserInput().then(pickedAccount => {
                if (pickedAccount) {
                    window.cordova.plugins.msalPlugin.signInSilent(
                        function(resp) {
                            doMySigninBusinessLogic(resp);
                        },
                        function(err) {
                            myAwesomeErrorHandler.handlerError(err);
                        },
                        pickedAccount.account.id
                    );
                } else {
                    window.cordova.plugins.msalPlugin.signInInteractive(
                        function(resp) {
                            doMySigninBusinessLogic(resp);
                        },
                        function(err) {
                            myAwesomeErrorHandler.handlerError(err);
                        }
                    );
                }
            });
        }
    },
    function(err) {
        myAwesomeErrorHandler.handlerError(err);
    }
);
```
To sign out a user, same thing:
```js
window.cordova.plugins.msalPlugin.signOut(
    function() {
        myAfterSignoutBusinessLogic();
    }, 
    function(err) {
        myAwesomeErrorHandler.handlerError(err);
    },
    currentLoggedInAccount.account.id
);
```

## Advanced Login Configuration
Normally, you don't need to pass anything into signInInteractive() other than your callbacks; it just works. But there might be cases where you need some more control over signing someone in.
You can pass a configuration object to signInInteractive() with as few or as many of the following attributes (they're all optional):
### loginHint
If you want to bypass the username selection step, you can provide a username here (string) to pre-populate it and have the user only be asked for a password if need be.
### prompt
This string tells MSAL how you want the login experience to be in terms of authentication and consent when they are redirected to Microsoft to sign in. It can be one of four (4) values; by default it is 'WHEN_REQUIRED' which actually is like a null value which tells MSAL not to send a prompt parameter at all and accept all default behavior. Other possible values are:
#### 'SELECT_ACCOUNT'
Show a list of possible usernames from which the user can select to sign in
#### 'LOGIN'
Normally if a user has had a recent session with Microsoft, even if their local token is cleared from your app and they need to sign in interactively again, they may be allowed back in without entering their password again. Setting prompt to 'LOGIN' will always force a user to be prompted for their password when signing in interactively no matter what.
#### 'CONSENT'
Any consent prompts that the user accepted giving your app access to their account the first time they signed in to your app will be given, even if they accepted before.
### authorizationQueryStringParameters
This is an array of objects of type {param: string, value: string}, empty by default. There are lots of extra query string parameters that can be passed with MSAL signin requests, and if you know you need to add this you probably already know what you want to put here.
### otherScopesToAuthorize
This is an array of strings just like the scopes array you may have provided when you first called msalInit (or maybe you just left it at ['User.Read']). The different here is that the vanilla scopes array only tells MSAL, programmatically, which API scopes it's allowed to call when getting data. Users are not asked to consent to those scopes until something tries to use them, which can result in annoying prompts in the middle of using your app. If you provide these extra scopes here as well, the user will be asked to accept all of them at once when signing in and won't get bugged later, regardless of whether those scopes are actually used or not.
### webViewType (iOS only)
By default, MSAL picks a default web view type for sign in based on the version of iOS. For iOS 11 and up, this is an AuthenticationSession (ASWebAuthenticationSession or SFAuthenticationSession) which shows the "App wants to sign in with" permission dialog. If you do not require SSO and wish to avoid this permission dialog you can specify one of the other web view types to use - both of which avoid the permission dialog.
#### 'SAFARI_VIEW_CONTROLLER'
Use a Safari web view for the sign in.
#### 'WK_WEB_VIEW'
Use a plain WKWebView for sign in.

Here's an example usage:
```js
window.cordova.plugins.msalPlugin.signInInteractive(mycbfunction(msg), myerrorfunction(errmsg), {
        prompt: 'LOGIN',
        authorizationQueryStringParameters: [{param: 'domain_hint', value: 'my-tenant-guid'}];
    }
);
```

## Logging/Debugging
You can enable the MSAL logger in this plugin by simply calling:
```js
window.cordova.plugins.msalPlugin.startLogger(successcallback(entry), failcallback(error), false, 'VERBOSE');
```
The error in the fail callback is the exception string you know and love. The argument in the success callback ('entry' in this example) is an object with the following structure:
```js
{
    timestamp: string,
    threadId: number,
    correlationId: string,
    logLevel: string,
    containsPII: boolean,
    message: string
}
```
### timestamp
The date/time (to-the-minute resolution) of the log entry
### threadId
Identifies the OS-level thread that was running during this operation
### correlationId
Sometimes 'UNSET' if unavailable, but otherwise is a GUID-format string generated by the native code that uniquely identifies an MSAL operation that took place so that if you collect these logs, you can group entries by transaction.
### logLevel
See below
### containsPII
See below
### message
The log message with all related metadata stripped out and placed elsewhere in this structure.

Note here that the callbacks here will be called over and over again each time MSAL logs something, which will be many times per second with dozens of log entries. So the success callback in particular should be something that either spits out each entry it gets to the console or some other handler in your application.

The third and fourth arguments to startLogger() are optional, but can further control the logger.

The first is a boolean (false by default) that tells it whether to include log messages that contain PII (Personally-Identifiable information) such as account IDs and credentials. Be very careful with this and only use it if you absolutely need to for debugging, and NEVER use it in production or store these data unattended. You have been warned and I am not responsible for anything bad that happens as a result of leaving this enabled.

The second argument is a string that filters which log messages are returned from the native code. There are 4 levels that can be passed in (most verbiage shamelessly stolen from [Microsoft's documentation](https://docs.microsoft.com/en-us/azure/active-directory/develop/msal-logging):
### 'VERBOSE'
Default if you provide nothing here. MSAL logs the full details of library behavior.
### 'ERROR'
Indicates something has gone wrong and an error was generated. Use for debugging and identifying problems.
### 'WARNING'
There hasn't necessarily been an error or failure, but are intended for diagnostics and pinpointing problems.
### 'INFO'
MSAL will log events intended for informational purposes not necessarily intended for debugging.

Note that once the logger is started it can't be stopped for the duration of your app being open. This is a limitation from Android's implementation of MSAL; once the logger callback has been defined it can't be modified or an exception is thrown.

## Troubleshooting
This plugin uses androidx features. If you get an error complaining about conflicting dependencies, you might need to add a couple of plugins to provide androidx compatibility, but your results may vary depending on if you are building locally or with a cloud-based utility.
<pre>
cordova plugin add cordova-plugin-androidx
cordova plugin add cordova-plugin-androidx-adapter
</pre>
