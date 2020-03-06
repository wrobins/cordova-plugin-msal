# Cordova MSAL Plugin
So you want to integrate your mobile app with our Microsoft authentication service?
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
cordova plugin add path/to/msal-plugin --variable TENANT_ID=your-tenant-guid-here --variable CLIENT_ID=your-client-guid-here --variable KEY_HASH=S0m3K3yh4shH3re=
</pre>
### If you're using OutSystems
Be sure to grab a release tagged with OutSystems-*. I have a separate branch for OutSystems because as of this writing, MABS (MABS 6.x) does not support AndroidX features, which are now a standard part of the Android MSAL library. I had to do some finagling with a custom build of the library with AndroidX features stripped out to avoid getting an error trying to build it with MABS.

A side effect of this is the authorizationUserAgent option is locked on WEBVIEW, since the other options rely on AndroidX.

Zip up the msal-plugin folder and leave it named the same. Then, in your wrapper application, import the zip file as a resource and remember to set Deploy Action to Deploy to Target Directory. Then, under extensibility configuration, add the plugin like so:
<pre>
{
    "resource": "msal-plugin.zip",
    "plugin": {
        "resource": "msal-plugin",
        "variables": [
            {
                "name": "TENANT_ID",
                "value": "your-tenant-guid-here"
            },
            {
                "name": "CLIENT_ID",
                "value": "your-client-guid-here"
            },
            {
                "name": "KEY_HASH",
                "value": "S0m3K3yh4shH3re="
            }
        ]
    }
}
</pre>
If you need different values for different tenants/keyhashes for different environments, open your application in LifeTime and click the Settings link near the application's title with the gear icon. Select your environment in the dropdown near the application's title, and scroll down to the Advanced section. Under Extensibility Configurations, tick the Custom > radial and paste your JSON with that environment's variables there.
## How do I use it?
Invoke this plugin following the same logic as you would for any Cordova plugin. You probably want to make sure it has loaded correctly before using it:
```js
let isAvailable = typeof(cordova.plugins.msalPlugin) !== "undefined";
```

### Single Client
Check to see if the user has an account cached with your app:
```js
window.cordova.plugins.msalPlugin.signInSilent(
    function(msg){
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
    function(msg){
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
    function(msg){
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