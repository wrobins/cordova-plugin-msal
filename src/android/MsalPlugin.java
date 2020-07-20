package com.wrobins.cordova.plugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;

import org.apache.cordova.PluginResult;
import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.util.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.microsoft.identity.client.AcquireTokenParameters;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.ILoggerCallback;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.Logger;
import com.microsoft.identity.client.Prompt;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.MultipleAccountPublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;


public class MsalPlugin extends CordovaPlugin {
    private Activity activity;
    private Context context;
    private CallbackContext callbackContext;
    private CallbackContext loggerCallbackContext;
    private PluginResult loggerPluginResult;
    private ISingleAccountPublicClientApplication appSingleClient;
    private IMultipleAccountPublicClientApplication appMultipleClient;
    private boolean isInit = false;

    private String clientId;
    private String tenantId;
    private String keyHash;
    private String accountMode;
    private String[] scopes;

    private static final String SIGN_IN_SILENT = "signInSilent";
    private static final String SIGN_IN_INTERACTIVE = "signInInteractive";
    private static final String SIGN_OUT = "signOut";
    private static final String MSAL_INIT = "msalInit";
    private static final String GET_ACCOUNTS = "getAccounts";
    private static final String START_LOGGER = "startLogger";

    private static final String SINGLE_ACCOUNT = "SINGLE";
    private static final String MULTIPLE_ACCOUNTS = "MULTIPLE";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        activity = cordova.getActivity();
        context = webView.getContext();

        clientId = this.preferences.getString("clientId","");
        tenantId = this.preferences.getString("tenantId","common");
        keyHash = this.preferences.getString("keyHash","");


    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        // We need a special callback context for the logger to keep continuous updates
        // without interfering with other plugin operations.
        if (!START_LOGGER.equals(action)) {
            this.callbackContext = callbackContext;
        }

        try {
            if (MSAL_INIT.equals(action)) {
                this.msalInit(new JSONObject(args.getString(0)));
            }
            if (START_LOGGER.equals(action)) {
                this.loggerCallbackContext = callbackContext;
                Logger.LogLevel logLevel;
                switch (args.getString(1)) {
                    case "ERROR":
                        logLevel = Logger.LogLevel.ERROR;
                        break;
                    case "WARNING":
                        logLevel = Logger.LogLevel.WARNING;
                        break;
                    case "INFO":
                        logLevel = Logger.LogLevel.INFO;
                        break;
                    default:
                        logLevel = Logger.LogLevel.VERBOSE;
                }
                this.startLogger(args.getBoolean(0), logLevel);
            }
            if (GET_ACCOUNTS.equals(action)) {
                this.getAccounts();
            }
            if (SIGN_IN_SILENT.equals(action)) {
                this.signinUserSilent(args.length() > 0 ? args.getString(0) : "");
            }
            if (SIGN_OUT.equals(action)) {
                this.signOut(args.length() > 0 ? args.getString(0) : "");
            }
            if (SIGN_IN_INTERACTIVE.equals(action)) {
                String loginHint = args.length() > 0 ? args.getString(0) : "";
                if (loginHint.equals("null")) {
                    loginHint = "";
                }
                Prompt prompt = Prompt.WHEN_REQUIRED;
                if (args.length() > 1) {
                    switch (args.getString(1)) {
                        case "SELECT_ACCOUNT":
                            prompt = Prompt.SELECT_ACCOUNT;
                            break;
                        case "LOGIN":
                            prompt = Prompt.LOGIN;
                            break;
                        case "CONSENT":
                            prompt = Prompt.CONSENT;
                            break;
                        default:
                            prompt = Prompt.WHEN_REQUIRED;
                    }
                }
                List<Pair<String, String>> authorizationQueryStringParameters = new ArrayList<Pair<String, String>>();
                if (args.length() > 2) {
                    JSONArray queryParams = args.getJSONArray(2);
                    for (int i = 0; i < queryParams.length(); ++i) {
                        JSONObject queryParam = queryParams.getJSONObject(i);
                        authorizationQueryStringParameters.add(new Pair<String, String>(
                                queryParam.getString("param"),
                                queryParam.getString("value")
                        ));
                    }
                }
                ArrayList<String> scopes = new ArrayList<String>();
                String[] otherScopesToAuthorize = new String[] {};
                if (args.length() > 3) {
                    for (int i = 0; i < args.getJSONArray(3).length(); ++i) {
                        scopes.add(args.getJSONArray(3).getString(i));
                    }
                    otherScopesToAuthorize = scopes.toArray(new String[scopes.size()]);
                }
                this.signinUserInteractive(loginHint, authorizationQueryStringParameters, prompt, otherScopesToAuthorize);
            }
        } catch (Exception e) {
            e.printStackTrace();
            callbackContext.error(e.getMessage());
            return false;
        }

        return true;
    }

    private void msalInit(final JSONObject options) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String keyHashUrlFriendly = "";
                    try {
                        keyHashUrlFriendly = URLEncoder.encode(MsalPlugin.this.keyHash, "UTF-8");
                    } catch(UnsupportedEncodingException e) {
                        MsalPlugin.this.callbackContext.error(e.getMessage());
                    }
                    StringBuilder authorities = new StringBuilder("    \"authorities\": [\n");
                    String data;
                    try {
                        JSONArray authoritiesList = options.getJSONArray("authorities");
                        for (int i = 0; i < authoritiesList.length(); ++i) {
                            JSONObject authority = authoritiesList.getJSONObject(i);
                            authorities.append("      {\n");
                            authorities.append("        \"type\": \"" + authority.getString("type") + "\",\n");
                            authorities.append("        \"audience\": {\n");
                            authorities.append("          \"type\": \"" + authority.getString("audience") + "\",\n");
                            authorities.append("          \"tenant_id\": \"" + MsalPlugin.this.tenantId + "\"\n");
                            authorities.append("        },\n");
                            if (authority.has("authorityUrl") && !authority.getString("authorityUrl").equals("")) {
                                authorities.append("          \"authority_url\": \"" + authority.getString("authorityUrl") + "\",\n");
                            }
                            if (authority.has("default")) {
                                authorities.append("          \"default\": " + authority.getBoolean("default") + "\n");
                            }
                            authorities.append("      }\n");
                        }
                        authorities.append("    ]\n");
                        data = "{\n" +
                                "    \"client_id\" : \"" + MsalPlugin.this.clientId + "\",\n" +
                                "    \"account_mode\": \"" + options.getString("accountMode") + "\",\n" +
                                "    \"authorization_user_agent\" : \"" + options.getString("authorizationUserAgent") + "\",\n" +
                                "    \"redirect_uri\" : \"msauth://" + MsalPlugin.this.activity.getApplicationContext().getPackageName() + "/" + keyHashUrlFriendly + "\",\n" +
                                "    \"multiple_clouds_supported\": " + options.getBoolean("multipleCloudsSupported") + ",\n" +
                                "    \"broker_redirect_uri_registered\": " + options.getBoolean("brokerRedirectUri") + ",\n" +
                                authorities.toString() +
                                "  }";
                        File config = createConfigFile(data);
                        if (options.getString("accountMode").equals(SINGLE_ACCOUNT)) {
                            MsalPlugin.this.appSingleClient = PublicClientApplication.createSingleAccountPublicClientApplication(context, config);
                            MsalPlugin.this.accountMode = SINGLE_ACCOUNT;
                        } else {
                            MsalPlugin.this.appMultipleClient = MultipleAccountPublicClientApplication.createMultipleAccountPublicClientApplication(context, config);
                            MsalPlugin.this.accountMode = MULTIPLE_ACCOUNTS;
                        }
                        config.delete();
                        ArrayList<String> scopes = new ArrayList<String>();
                        for (int i = 0; i < options.getJSONArray("scopes").length(); ++i) {
                            scopes.add(options.getJSONArray("scopes").getString(i));
                        }
                        MsalPlugin.this.scopes = scopes.toArray(new String[scopes.size()]);
                        MsalPlugin.this.isInit = true;
                        MsalPlugin.this.callbackContext.success();
                    } catch (JSONException ignored) {}
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (MsalException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void startLogger(boolean showPII, Logger.LogLevel logLevel) {
        // Set up a dedicated callback context to handler multiple log entries until we way to stop
        this.loggerPluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        this.loggerPluginResult.setKeepCallback(true);
        this.loggerCallbackContext.sendPluginResult(this.loggerPluginResult);
        try {
            // Set up the logger with the options we want
            Logger.getInstance().setEnablePII(showPII);
            Logger.getInstance().setLogLevel(logLevel);
            Logger.getInstance().setExternalLogger(new ILoggerCallback() {
                @Override
                public void log(String tag, Logger.LogLevel logLevel, String message, boolean containsPII) {
                    try {
                        JSONObject logEntry = new JSONObject();

                        // Parse out the message object to make it cleaner.
                        String meta = message.substring(message.indexOf("["), message.indexOf("]"));
                        logEntry.put("timestamp", meta.substring(1, meta.indexOf(" -")));
                        JSONObject data = new JSONObject(meta.substring(meta.indexOf("{"), meta.indexOf("}") + 1));
                        logEntry.put("threadId", Integer.parseInt(data.getString("thread_id")));
                        logEntry.put("correlationId", data.getString("correlation_id"));

                        logEntry.put("logLevel", logLevel.toString());
                        logEntry.put("containsPII", containsPII);
                        logEntry.put("message", message.substring(message.indexOf("]") + 2));


                        MsalPlugin.this.loggerPluginResult = new PluginResult(PluginResult.Status.OK, logEntry);
                        MsalPlugin.this.loggerPluginResult.setKeepCallback(true);
                        MsalPlugin.this.loggerCallbackContext.sendPluginResult(MsalPlugin.this.loggerPluginResult);
                    } catch (JSONException e) {
                        MsalPlugin.this.loggerCallbackContext.error(e.getMessage());
                    }
                }
            });
        } catch (IllegalStateException e) {
            MsalPlugin.this.loggerCallbackContext.error(e.getMessage());
        }

    }

    private void getAccounts() throws JSONException {
        if (this.checkConfigInit()) {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    JSONArray accounts = new JSONArray();
                    try {
                        if (SINGLE_ACCOUNT.equals(accountMode)) {
                            IAccount account = MsalPlugin.this.appSingleClient.getCurrentAccount().getCurrentAccount();
                            if (account != null) {
                                JSONObject accountObj = new JSONObject();
                                accountObj.put("id", account.getId());
                                accountObj.put("username", account.getUsername());
                                accountObj.put("claims", new JSONObject(account.getClaims()));
                                accounts.put(accountObj);
                            }
                        } else {
                            for (IAccount account : MsalPlugin.this.appMultipleClient.getAccounts()) {
                                JSONObject accountObj = new JSONObject();
                                accountObj.put("id", account.getId());
                                accountObj.put("username", account.getUsername());
                                accountObj.put("claims", new JSONObject(account.getClaims()));
                                accounts.put(accountObj);
                            }
                        }
                        MsalPlugin.this.callbackContext.success(accounts);
                    } catch (InterruptedException e) {
                        MsalPlugin.this.callbackContext.error(e.getMessage());
                    } catch (MsalException e) {
                        MsalPlugin.this.callbackContext.error(e.getMessage());
                    } catch (JSONException e) {
                        MsalPlugin.this.callbackContext.error(e.getMessage());
                    }
                }
            });
        }
    }

    private void signinUserSilent(final String account) {
        if (this.checkConfigInit()) {
            if (SINGLE_ACCOUNT.equals(accountMode)) {
                cordova.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String authority = MsalPlugin.this.appSingleClient.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
                            if (MsalPlugin.this.appSingleClient.getCurrentAccount().getCurrentAccount() == null) {
                                MsalPlugin.this.callbackContext.error("No account currently exists");
                            } else {
                                IAuthenticationResult silentAuthResult = MsalPlugin.this.appSingleClient.acquireTokenSilent(MsalPlugin.this.scopes, authority);
                                MsalPlugin.this.callbackContext.success(silentAuthResult.getAccessToken());
                            }
                        } catch (InterruptedException e) {
                            MsalPlugin.this.callbackContext.error(e.getMessage());
                        } catch (MsalException e) {
                            MsalPlugin.this.callbackContext.error(e.getMessage());
                        }
                    }
                });
            } else {
                cordova.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // Look for account first so we don't error out for one that doesn't exist
                            boolean found = false;
                            for (IAccount search : MsalPlugin.this.appMultipleClient.getAccounts()) {
                                if (search.getId().equals(account)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                MsalPlugin.this.callbackContext.error("Account not found");
                                return;
                            }
                            String authority = MsalPlugin.this.appMultipleClient.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
                            IAuthenticationResult result = MsalPlugin.this.appMultipleClient.acquireTokenSilent(
                                    MsalPlugin.this.scopes,
                                    MsalPlugin.this.appMultipleClient.getAccount(account),
                                    authority
                            );
                            MsalPlugin.this.callbackContext.success(result.getAccessToken());
                        } catch (InterruptedException e) {
                            MsalPlugin.this.callbackContext.error(e.getMessage());
                        } catch (MsalException e) {
                            MsalPlugin.this.callbackContext.error(e.getMessage());
                        }
                    }
                });
            }
        }
    }

    private void signinUserInteractive(final String loginHint, final List<Pair<String, String>> authorizationQueryStringParameters, final Prompt prompt, final String[] otherScopesToAuthorize) {
        if (this.checkConfigInit()) {
            if (this.SINGLE_ACCOUNT.equals(this.accountMode)) {
                cordova.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        AcquireTokenParameters.Builder params = new AcquireTokenParameters.Builder()
                                .startAuthorizationFromActivity(MsalPlugin.this.activity)
                                .withScopes(Arrays.asList(MsalPlugin.this.scopes))
                                .withOtherScopesToAuthorize(Arrays.asList(otherScopesToAuthorize))
                                .withPrompt(prompt)
                                .withCallback(new AuthenticationCallback() {
                                    @Override
                                    public void onCancel() {
                                        MsalPlugin.this.callbackContext.error("Login cancelled.");
                                    }

                                    @Override
                                    public void onSuccess(IAuthenticationResult iAuthenticationResult) {
                                        MsalPlugin.this.callbackContext.success(iAuthenticationResult.getAccessToken());
                                    }

                                    @Override
                                    public void onError(MsalException e) {
                                        MsalPlugin.this.callbackContext.error(e.getMessage());
                                    }
                                });
                        if (!loginHint.equals("")) {
                            params = params.withLoginHint(loginHint);
                        }
                        if (!authorizationQueryStringParameters.isEmpty()) {
                            params = params.withAuthorizationQueryStringParameters(authorizationQueryStringParameters);
                        }
                        MsalPlugin.this.appSingleClient.acquireToken(params.build());
                    }
                });
            } else {
                cordova.getThreadPool().execute(new Runnable() {
                    @Override
                    public void run() {
                        AcquireTokenParameters.Builder params = new AcquireTokenParameters.Builder()
                                .startAuthorizationFromActivity(MsalPlugin.this.activity)
                                .withScopes(Arrays.asList(MsalPlugin.this.scopes))
                                .withOtherScopesToAuthorize(Arrays.asList(otherScopesToAuthorize))
                                .withPrompt(prompt)
                                .withCallback(new AuthenticationCallback() {
                                    @Override
                                    public void onCancel() {
                                        MsalPlugin.this.callbackContext.error("Login cancelled.");
                                    }

                                    @Override
                                    public void onSuccess(IAuthenticationResult iAuthenticationResult) {
                                        MsalPlugin.this.callbackContext.success(iAuthenticationResult.getAccessToken());
                                    }

                                    @Override
                                    public void onError(MsalException e) {
                                        MsalPlugin.this.callbackContext.error(e.getMessage());
                                    }
                                });
                        if (!loginHint.equals("")) {
                            params = params.withLoginHint(loginHint);
                        }
                        if (!authorizationQueryStringParameters.isEmpty()) {
                            params = params.withAuthorizationQueryStringParameters(authorizationQueryStringParameters);
                        }
                        MsalPlugin.this.appMultipleClient.acquireToken(params.build());
                    }
                });
            }
        }
    }

    private void signOut(final String account) {
        this.checkConfigInit();
        if (this.SINGLE_ACCOUNT.equals(this.accountMode)) {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Look for account first so we don't error out for one that doesn't exist
                        if(MsalPlugin.this.appMultipleClient != null) {
                            boolean found = false;
                            for (IAccount search : MsalPlugin.this.appMultipleClient.getAccounts()) {
                                if (search.getId().equals(account)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                MsalPlugin.this.callbackContext.error("Account not found");
                                return;
                            }
                        }
                        if (MsalPlugin.this.appSingleClient.getCurrentAccount().getCurrentAccount() != null) {
                            MsalPlugin.this.appSingleClient.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
                                @Override
                                public void onSignOut() {
                                    MsalPlugin.this.callbackContext.success();
                                }

                                @Override
                                public void onError(MsalException e) {
                                    MsalPlugin.this.callbackContext.error(e.getMessage());
                                }
                            });
                        } else {
                            MsalPlugin.this.callbackContext.success();
                        }
                    } catch (InterruptedException e) {
                        MsalPlugin.this.callbackContext.error(e.getMessage());
                    } catch (MsalException e) {
                        MsalPlugin.this.callbackContext.error(e.getMessage());
                    }
                }
            });
        } else {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        MsalPlugin.this.appMultipleClient.removeAccount(MsalPlugin.this.appMultipleClient.getAccount(account),
                                new IMultipleAccountPublicClientApplication.RemoveAccountCallback() {
                                    @Override
                                    public void onRemoved() {
                                        MsalPlugin.this.callbackContext.success();
                                    }

                                    @Override
                                    public void onError(MsalException e) {
                                        MsalPlugin.this.callbackContext.error(e.getMessage());
                                    }
                                });
                    } catch (InterruptedException e) {
                        MsalPlugin.this.callbackContext.error(e.getMessage());
                    } catch (MsalException e) {
                        MsalPlugin.this.callbackContext.error(e.getMessage());
                    }
                }
            });
        }
    }
    

    private File createConfigFile(String data) {
        File config = new File(this.context.getFilesDir() + "auth_config.json");
        try {
            FileWriter writer = new FileWriter(config, false);
            writer.write(data);
            writer.flush();
            writer.close();
            return config;
        } catch (IOException e) {
            MsalPlugin.this.callbackContext.error(e.getMessage());
        }
        return config;
    }

    private boolean checkConfigInit() {
        if (!this.isInit) {
            this.callbackContext.error("No configuration has been set yet. Call msalInit() before calling this.");
            return false;
        }
        return true;
    }
}