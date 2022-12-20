const clientID = CLIENT_ID;
const tenantId = TENANT_ID;
const msal = window.msal;

let msalApp = null;
const msalConfig = {
    auth: {},
    cache: {
        cacheLocation: 'localStorage'
    }
};
const loginRequest = {};
const msalAccounts = [];
let accountMode = 'SINGLE';

function msalInit(success, error, opts) {
    try {
        const providedConfig = JSON.parse(opts[0]);
        msalConfig.auth.clientId = providedConfig.clientId || clientID;
        msalConfig.auth.authority = `https://login.microsoftonline.com/${providedConfig.tenantId || tenantId}`;
        msalConfig.auth.knownAuthorities = providedConfig.authorities.filter(a => a.authorityUrl !== '').map(a => a.authorityUrl);
        accountMode = providedConfig.accountMode;
        loginRequest.scopes = providedConfig.scopes;
        loginRequest.authority = msalConfig.auth.authority;
        msalApp = new msal.PublicClientApplication(msalConfig);
        success('OK');
    } catch(e) {
        error(e);
    }
}

function signInSilent(success, error, opts) {
    try {
        let account = null;
        if (accountMode === 'SINGLE') {
            account = msalApp.getAllAccounts()[0];
        } else {
            account = msalApp.getAccountById(opts[0]);
        }
        if (account) {
            loginRequest.account = account;
            msalApp.acquireTokenSilent(loginRequest)
                .then(resp => {
                    const account = {
                        token: resp.accessToken,
                        account: {
                            id: resp.uniqueId,
                            username: resp.account.username,
                            claims: []
                        }
                    };
                    for (const property in resp.account.idTokenClaims) {
                        account.account.claims.push({
                            key: property,
                            value: resp.account.idTokenClaims[property]
                        });
                    }
                    msalApp.setActiveAccount(resp.account);
                    success(account);
                })
                .catch(err => error(err));
        } else {
            error ('No account currently exists');
        }
    } catch (e) {
        error(e);
    }
}

function signInInteractive(success, error, opts) {
    // Parse out all optional parameters.
    let loginHint = '';
    let prompt = 'select_account';
    let authorizationQueryStringParameters = [];
    let otherScopesToAuthorize = [];
    if (opts.length >= 1 && opts[0] !== '' && typeof(opts[0]) !== "undefined") {
        const acct = msalApp.getAccountById(opts[0]);
        loginHint = acct.username;
    }
    if (opts.length >= 2 && opts[1] !== '' && typeof(opts[1]) !== "undefined" && opts[1] !== 'WHEN_REQUIRED') {
        prompt = opts[1].toLowerCase();
    }
    if (opts.length >= 3 && typeof(opts[2].length) !== 'undefined' && opts[2].length > 0) {
        authorizationQueryStringParameters = opts[2];
    }
    if (opts.length >= 4 && typeof(opts[3].length) !== 'undefined' && opts[3].length > 0) {
        otherScopesToAuthorize = opts[3];
    }
    loginRequest.prompt = prompt;
    loginRequest.extraQueryParameters = {};
    authorizationQueryStringParameters.forEach(p => {
        loginRequest[p.param] = p.value;
    });
    loginRequest.extraScopesToConsent = otherScopesToAuthorize;
    try {
        if (accountMode !== 'SINGLE') {
            loginRequest.loginHint = loginHint;
        }
        msalApp.loginPopup(loginRequest)
            .then(resp => {
                const account = {
                    token: resp.accessToken,
                    account: {
                        id: resp.uniqueId,
                        username: resp.account.username,
                        claims: []
                    }
                };
                for (const property in resp.account.idTokenClaims) {
                    account.account.claims.push({
                        key: property,
                        value: resp.account.idTokenClaims[property]
                    });
                }
                msalApp.setActiveAccount(resp.account);
                success(account);
            })
            .catch(err => error(err));
    } catch (e) {
        error(e);
    }
}

function getAccounts(success, error, opts) {
    try {
        const msalAccounts = msalApp.getAllAccounts();
        const accounts = [];
        msalAccounts.forEach(account => {
            const claims = [];
            for (const property in account.idTokenClaims) {
                claims.push({
                    key: property,
                    value: account.idTokenClaims[property]
                });
            }
            accounts.push({
                token: '',
                account: {
                    id: account.localAccountId,
                    username: account.username,
                    claims: claims
                }
            });
        });
        success(accounts);
    } catch(e) {
        error(e);
    }
}

function startLogger(success, error, opts) {
    try {
        let logLevel = 0;
        switch (opts[1]) {
            case 'INFO':
                logLevel = 2;
                break;
            case 'WARNING':
                logLevel = 1;
                break;
            case 'ERROR':
                logLevel = 0;
                break;
            default:
                logLevel = 3;
        }
        msalApp.setLogger(new msal.Logger({
            logLevel: logLevel,
            piiLoggingEnabled: opts[0],
            loggerCallback: function(level, message, containsPii) {
                success({
                    timestamp: new Date(),
                    threadId: 0,
                    correlationId: '',
                    logLevel: level,
                    containsPII: containsPii,
                    message: message
                },{keepCallback:true});
            }
        }));
    } catch (ex) {
        error(ex);
    }
}

function signOut(success, error, opts) {
    let account = null;
    if (accountMode === 'SINGLE'){
        account = msalApp.getActiveAccount();
    } else {
        account = msalApp.getAccountById(opts[0]);
    }
    const logoutRequest = {
        account: account
    };
    msalApp.logout(logoutRequest);
}

module.exports = {
    msalInit: msalInit,
    signInInteractive: signInInteractive,
    signInSilent: signInSilent,
    getAccounts: getAccounts,
    startLogger: startLogger,
    signOut: signOut
};

require('cordova/exec/proxy').add('MsalPlugin', module.exports);