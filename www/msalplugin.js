cordova.define("cordova-plugin-msal.msalPlugin", function(require, exports, module) {
    module.exports = {
        msalInit: function(successCallback, errorCallback, options) {
            var defaultOptions = {
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
            if (!options) {
                options = defaultOptions;
            } else {
                if (typeof(options.authorities) == 'undefined') {
                    options.authorities = defaultOptions.authorities;
                }
                if (typeof(options.authorizationUserAgent) == 'undefined') {
                    options.authorizationUserAgent = defaultOptions.authorizationUserAgent;
                }
                if (typeof(options.multipleCloudsSupported) == 'undefined') {
                    options.multipleCloudsSupported = defaultOptions.multipleCloudsSupported;
                }
                if (typeof(options.brokerRedirectUri) == 'undefined') {
                    options.brokerRedirectUri = defaultOptions.brokerRedirectUri;
                }
                if (typeof(options.accountMode) == 'undefined') {
                    options.accountMode = defaultOptions.accountMode;
                }
                if (typeof(options.scopes) == 'undefined') {
                    options.scopes = defaultOptions.scopes;
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
        signInInteractive: function(successCallback, errorCallback, signInOptions) {
            if (typeof(signInOptions) === 'undefined') {
                signInOptions = {};
            }
            var opts = [
                typeof(signInOptions.loginHint !== 'undefined') ? signInOptions.loginHint : '',
                typeof(signInOptions.prompt !== 'undefined') ? signInOptions.prompt : '',
                typeof(signInOptions.authorizationQueryStringParameters) !== 'undefined' ? signInOptions.authorizationQueryStringParameters : [],
                typeof(signInOptions.otherScopesToAuthorize) !== 'undefined' ? signInOptions.otherScopesToAuthorize : []
            ];
            cordova.exec(successCallback, errorCallback, 'MsalPlugin', 'signInInteractive', opts);
        },
        signOut: function(successCallback, errorCallback, account) {
            cordova.exec(successCallback, errorCallback, 'MsalPlugin', 'signOut', [account]);
        }
    };
      
    });
    