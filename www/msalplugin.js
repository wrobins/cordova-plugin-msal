module.exports = {
    defaultOptions = {
      authorities: [
        {
          type: 'AAD', // Android, 'AAD' or 'B2C'
          audience: 'AzureADandPersonalMicrosoftAccount', // Android, 'AzureADandPersonalMicrosoftAccount' or 'AzureADMyOrg' or 'AzureADMultipleOrgs' or 'PersonalMicrosoftAccount'. If 'AzureADMyOrg' make sure you specified the TENANT_ID variable 
          authorityUrl: '', // Android, only needed if type = 'B2C'
          default: true // Android, only needed if multiple authorities
        }
      ],
      authorizationUserAgent: 'DEFAULT', // Android only, 'DEFAULT' 'BROWSER' or 'WEBVIEW' OutSystems is 'WEBVIEW' only
      multipleCloudsSupported: false, // Android, Set to true if you enabled this in your Azure AD client
      brokerRedirectUri: false, // Android, Set to true if you want to support the device's broker, such as the Authenticator app. Will be ignored if using only the 'PersonalMicrosoftAccount' AAD authority
      accountMode: 'SINGLE' // Android and iOS: either 'SINGLE' or 'MULTIPLE'. Specifies how many accounts can be used in your app at a time, and what interfaces you are allowed to use to authenticate.
    },
    signInSilent: function(successCallback, errorCallback, options) {
      cordova.exec(successCallback, errorCallback, 'MsalPlugin', 'signInSilent', []);
    },
    signInInteractive: function(successCallback, errorCallback, options) {
      cordova.exec(successCallback, errorCallback, 'MsalPlugin', 'signInInteractive', []);
    },
    signOut: function(successCallback, errorCallback, options) {
      cordova.exec(successCallback, errorCallback, 'MsalPlugin', 'signOut', []);
    }
};