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

Be sure to grab a release tagged with OutSystems-*. I have a separate branch for OutSystems because as of this writing, MABS (MABS 6.x) does not support AndroidX features, which are now a standard part of the Android MSAL library. I had to do some finagling with a custom build of the library with AndroidX features stripped out to avoid getting an error trying to build it with MABS.

A side effect of this is the authorizationUserAgent option is locked on WEBVIEW, since the other options rely on AndroidX.

Here's the JSON you'll need to configure your plugin. If you only have one environment and build, you can put it in your extensibility configuration in your wrapper application. But you probably have debug/release builds in multiple envrionments with multiple Azure clients/tenants, so LifeTime is probably the best place to manage your extensibility configuration JSON. Open your wrapper application implementing this plugin in LifeTime and click the Settings link near the application's title with the gear icon. Select your environment in the dropdown near the application's title, and scroll down to the Advanced section. Under Extensibility Configurations, tick the Custom > radial and paste your JSON with that environment's variables there:
<pre>
{
    "plugin": {
        "url": "https://github.com/wrobins/cordova-plugin-msal.git#OutSystems-v1.0.0",
        "variables": [
            {
                "name": "TENANT_ID",
                "value": "your-tenant-guid-here-optional"
            },
            {
                "name": "CLIENT_ID",
                "value": "your-client-guid-here-reuired"
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
This is for Android - tells MSAL which webview to use when redirecting a user to sign in interactively (stay inside the native app's webview or use the device's external browser). Can be one of the following: 'DEFAULT' 'BROWSER' or 'WEBVIEW'. Default: 'DEFAULT'. Note: in OutSystems, this option is locked on 'WEBVIEW' regardless of what you try to set here because anything else requires androidX features which MABS does not currently allow.
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
    function(msg) {
        // msg is your JWT for the account we found.
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
    function(msg) {
        // msg is your JWT for the account the user just signed into
        // Also never use a preposition to end a sentence with
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
        // msg is just the "OK" plugin result
    }, 
    function(err) {
        // An error here usually either means you accidentally tried to
        // sign out someone who wasn't signed in, or there was a problem
        // communicating with the server.
    }
);
```
### Multiple Clients
Operation is very similar to single account mode, but with one extra method to get the accounts you need: getAccounts(). It returns an array of objects {id: string, username: string} to represent accounts found in your app. Use it to see if you need to sign in a user interactively or manually. Your signInSilent() and signOut() methods will take an account id as an argument to pick which account you're working with.

Again, your logic might look more separate if you want to have guest access to your app, but this code without guest access should give you an idea of how this plugin works:
```js
window.cordova.plugins.msalPlugin.getAccounts(
    function(accounts) {
        if (accounts.length === 0) {
            window.cordova.plugins.msalPlugin.signInInteractive(
                function(jwt) {
                    validateMyJWT(jwt);
                    doMySigninBusinessLogic();
                },
                function(err) {
                     myAwesomeErrorHandler.handlerError(err);
                }
            );
        } else {
            myAwesomeInterface.showAccountsList(accounts).waitForUserInput().then(pickedAccount => {
                if (pickedAccount) {
                    window.cordova.plugins.msalPlugin.signInSilent(
                        function(jwt) {
                            validateMyJWT(jwt);
                            doMySigninBusinessLogic();
                        },
                        function(err) {
                            myAwesomeErrorHandler.handlerError(err);
                        },
                        pickedAccount.id
                    );
                } else {
                    window.cordova.plugins.msalPlugin.signInInteractive(
                        function(jwt) {
                            validateMyJWT(jwt);
                            doMySigninBusinessLogic();
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
    accountId
);
```
## Validating the JWT
Once you get a successful sign-in, the plugin just returns the JWT for that account. You can validate it against Graph:
<pre>
GET https://graph.microsoft.com/v1.0/me
'Authorization': 'Bearer myaccountJWT'
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
You can use that in your business logic to manage your accounts locally.