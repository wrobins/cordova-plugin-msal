const fs = require('fs');

const config = JSON.parse(fs.readFileSync('platforms/browser/browser.json').toString());
const CLIENT_ID = `'${config.installed_plugins["cordova-plugin-msal"].CLIENT_ID}'`;
const TENANT_ID = `'${config.installed_plugins["cordova-plugin-msal"].TENANT_ID}'`;

// If our application has the browser platform installed, this is ideally where we want to configure it.
if (fs.existsSync('platforms/browser/browser.json')) {
    const proxyFile = 'platforms/browser/www/plugins/cordova-plugin-msal/src/browser/MsalProxy.js';
    const contents = fs.readFileSync(proxyFile).toString();
    fs.writeFileSync(proxyFile, contents.replace(/CLIENT_ID/g, CLIENT_ID).replace(/TENANT_ID/g, TENANT_ID));
}