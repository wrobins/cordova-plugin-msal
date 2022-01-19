const fs = require('fs');

const browser_platform='platforms/browser/browser.json';

// If our application has the browser platform installed, this is ideally where we want to configure it.
if (fs.existsSync(browser_platform)) {

    const config = JSON.parse(fs.readFileSync(browser_platform).toString());
    const CLIENT_ID = `'${config.installed_plugins["cordova-plugin-msal"].CLIENT_ID}'`;
    const TENANT_ID = `'${config.installed_plugins["cordova-plugin-msal"].TENANT_ID}'`;

    [
        'platforms/browser/www/plugins/cordova-plugin-msal/src/browser/MsalProxy.js',
        'platforms/browser/platform_www/plugins/cordova-plugin-msal/src/browser/MsalProxy.js'

    ].forEach( file => {
        if (fs.existsSync(file)) {
            const contents = fs.readFileSync(file).toString();
            fs.writeFileSync(file, contents.replace(/CLIENT_ID/g, CLIENT_ID).replace(/TENANT_ID/g, TENANT_ID));
        }
    });
}