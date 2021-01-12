function msalInit(success, error, opts) {
    try {
        const msalConfig = {
            auth: {
                clientId: 'test'
            }
        };
        const msalInstance = new msal.PublicClientApplication(msalConfig);
        success(msalInstance);
    } catch (err) {
        error(err);
    }
}

module.exports = {
    msalInit: msalInit
};
require('cordova/exec/proxy').add('MsalPlugin', module.exports);