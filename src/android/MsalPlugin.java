package com.wrobins.cordova.plugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.URLEncoder;
import java.util.ArrayList;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.MultipleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;


public class MsalPlugin extends CordovaPlugin {
    private Activity activity;
    private Context context;
    private CallbackContext callbackContext;
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
        this.callbackContext = callbackContext;

        try {
            if (MSAL_INIT.equals(action)) {
                this.msalInit(new JSONObject(args.getString(0)));
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
                this.signinUserInteractive();
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

    private void getAccounts() throws JSONException {
        this.checkConfigInit();
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                JSONArray accounts = new JSONArray();
                try {
                    if (SINGLE_ACCOUNT.equals(accountMode)) {
                        if (MsalPlugin.this.appSingleClient.getCurrentAccount().getCurrentAccount() == null) {
                            MsalPlugin.this.callbackContext.error("No account currently exists");
                        } else {
                            accounts.put(MsalPlugin.this.appSingleClient.getCurrentAccount().getCurrentAccount().getId());
                        }
                    } else {
                        for (IAccount account: MsalPlugin.this.appMultipleClient.getAccounts()) {
                            JSONObject accountObj = new JSONObject();
                            accountObj.put("id", account.getId());
                            accountObj.put("username", account.getUsername());
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

    private void signinUserSilent(final String account) {
        this.checkConfigInit();
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
                        for (IAccount search: MsalPlugin.this.appMultipleClient.getAccounts()) {
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

    private void signinUserInteractive() {
        this.checkConfigInit();
        if (this.SINGLE_ACCOUNT.equals(this.accountMode)) {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    MsalPlugin.this.appSingleClient.signIn(MsalPlugin.this.activity, "", MsalPlugin.this.scopes, new AuthenticationCallback() {
                        @Override
                        public void onCancel() {
                            MsalPlugin.this.callbackContext.error("Login cancelled.");
                        }

                        @Override
                        public void onSuccess(IAuthenticationResult authenticationResult) {
                            MsalPlugin.this.callbackContext.success(authenticationResult.getAccessToken());
                        }

                        @Override
                        public void onError(MsalException e) {
                            MsalPlugin.this.callbackContext.error(e.getMessage());
                        }
                    });
                }
            });
        } else {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    MsalPlugin.this.appMultipleClient.acquireToken(MsalPlugin.this.activity, MsalPlugin.this.scopes, new AuthenticationCallback() {
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
                }
            });
        }
    }

    private void signOut(final String account) {
        this.checkConfigInit();
        if (this.SINGLE_ACCOUNT.equals(this.accountMode)) {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(MsalPlugin.this.appSingleClient.getCurrentAccount().getCurrentAccount() != null) {
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

    private void checkConfigInit() {
        if (!this.isInit) {
            this.callbackContext.error("No configuration has been set yet. Call msalInit() before calling this.");
        }
    }
}