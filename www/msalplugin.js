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
            scopes: ['User.Read'],
            webViewZoomControlsEnabled: false,
            webViewZoomEnabled: false,
            powerOptCheckForNetworkReqEnabled: true
        }
        if (!options) {
            options = defaultOptions;
        } else {
            if (typeof(options.authorities) == 'undefined') {
                options.authorities = defaultOptions.authorities;
            }
            else {
                for (var i = 0; i < options.authorities.length; i++) {
                    var authority = options.authorities[i];
                    if (typeof(authority.type) == 'undefined') {
                        authority.type = defaultOptions.authorities[0].type;
                    }
                    if (typeof(authority.audience) == 'undefined') {
                        authority.audience = defaultOptions.authorities[0].audience;
                    }
                    if (typeof(authority.authorityUrl) == 'undefined') {
                        authority.authorityUrl = defaultOptions.authorities[0].authorityUrl;
                    }
                    if (typeof(authority.cloudInstance) == 'undefined') {
                        authority.cloudInstance = defaultOptions.authorities[0].cloudInstance;
                    }
                    if (typeof(authority.default) == 'undefined') {
                        authority.default = defaultOptions.authorities[0].default;
                    }
                }
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
    startLogger: function(updateCallback, errorCallback, containsPII = false, logLevel = 'VERBOSE') {
        cordova.exec(updateCallback, errorCallback, 'MsalPlugin', 'startLogger', [containsPII, logLevel]);
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
            typeof(signInOptions.otherScopesToAuthorize) !== 'undefined' ? signInOptions.otherScopesToAuthorize : [],
            typeof(signInOptions.webViewType) !== 'undefined' ? signInOptions.webViewType : ''
        ];
        cordova.exec(successCallback, errorCallback, 'MsalPlugin', 'signInInteractive', opts);
    },
    signOut: function(successCallback, errorCallback, account) {
        cordova.exec(successCallback, errorCallback, 'MsalPlugin', 'signOut', [account]);
    }
};