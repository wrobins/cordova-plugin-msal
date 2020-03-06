cordova.define("cordova-plugin-msal.msalPlugin", function(require, exports, module) {
  module.exports = {
      msalInit: function(successCallback, errorCallback, options) {
          const defaultOptions = {
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
          }
          if (!options) {
              options = defaultOptions;
          } else {
              if (typeof(options.authorities) == "undefined") {
                  options.authorities = defaultOptions.authorities;
              }
              if (typeof(options.authorizationUserAgent) == "undefined") {
                  options.authorizationUserAgent = defaultOptions.authorizationUserAgent;
              }
              if (typeof(options.multipleCloudsSupported) == "undefined") {
                  options.multipleCloudsSupported = defaultOptions.multipleCloudsSupported;
              }
              if (typeof(options.brokerRedirectUri) == "undefined") {
                  options.brokerRedirectUri = defaultOptions.brokerRedirectUri;
              }
              if (typeof(options.accountMode) == "undefined") {
                  options.accountMode = defaultOptions.accountMode;
              }
          }
          cordova.exec(successCallback, errorCallback, 'MsalPlugin', 'msalInit', [JSON.stringify(options)]);
      },
      getAccounts: function(successCallback, errorCallback) {
          cordova.exec(successCallback, errorCallback, 'MsalPlugin', 'getAccounts', []);
      },
      signInSilent: function(successCallback, errorCallback, account) {
          cordova.exec(successCallback, errorCallback, 'MsalPlugin', 'signInSilent', [account]);
      },
      signInInteractive: function(successCallback, errorCallback, account) {
          cordova.exec(successCallback, errorCallback, 'MsalPlugin', 'signInInteractive', [account]);
      },
      signOut: function(successCallback, errorCallback, account) {
          cordova.exec(successCallback, errorCallback, 'MsalPlugin', 'signOut', [account]);
      }
      };
  });
  