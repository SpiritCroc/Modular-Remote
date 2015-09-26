/*
 * Copyright (C) 2015 SpiritCroc
 * Email: spiritcroc@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.spiritcroc.modular_remote.modules;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import de.spiritcroc.modular_remote.MainActivity;
import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.Util;

public class WebViewFragment extends ModuleFragment {
    private static final String ARG_ADDRESS = "ip";
    private static final String ARG_WIDTH = "width";
    private static final String ARG_HEIGHT = "height";
    private static final String ARG_JAVA_SCRIPT_ENABLED = "java_script_enabled";
    private static final String ARG_ALLOW_EXTERNAL_LINKS = "allow_external_links";
    private static final String LOG_TAG = WebViewFragment.class.getSimpleName();

    private Container parent;
    private String address;
    private double width, height;
    private boolean javaScriptEnabled, allowExternalLinks, connected = true;
    private WebView webView;
    private MenuItem menuReloadItem;
    private LinearLayout baseLayout;
    private boolean menuEnabled = false;

    public static WebViewFragment newInstance(String address, double width, double height,
                                              boolean javaScriptEnabled,
                                              boolean allowExternalLinks) {
        if (!address.contains("//")) {
            address = "http://" + address;
        }
        WebViewFragment fragment = new WebViewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ADDRESS, address);
        args.putDouble(ARG_WIDTH, width);
        args.putDouble(ARG_HEIGHT, height);
        args.putBoolean(ARG_JAVA_SCRIPT_ENABLED, javaScriptEnabled);
        args.putBoolean(ARG_ALLOW_EXTERNAL_LINKS, allowExternalLinks);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Bundle arguments = getArguments();
        if (arguments != null) {
            address = arguments.getString(ARG_ADDRESS);
            width = getArguments().getDouble(ARG_WIDTH);
            height = getArguments().getDouble(ARG_HEIGHT);;
            javaScriptEnabled = arguments.getBoolean(ARG_JAVA_SCRIPT_ENABLED);
            allowExternalLinks = arguments.getBoolean(ARG_ALLOW_EXTERNAL_LINKS);
        } else {
            Log.e(LOG_TAG, "onCreate: getArguments()==null");
            address = "";
            width = height = 1;
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_web_view, container, false);

        webView = (WebView) view.findViewById(R.id.web_view);
        baseLayout = (LinearLayout) view.findViewById(R.id.base_layout);

        // Set to our own WebViewClient so we can open links within the WebView
        webView.setWebViewClient(new CustomWebViewClient());
        setValues(address, width, height, javaScriptEnabled, allowExternalLinks);

        return view;
    }
    @Override
    public void onPause() {
        super.onPause();
        webView.clearCache(true);
    }
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (menuEnabled) {
            String shortenSearchingFor = "//", menuReloadEntry;
            if (address.contains(shortenSearchingFor)){
                menuReloadEntry = getString(R.string.action_reload_web_view) + " " +
                        address.substring(address.indexOf(shortenSearchingFor) +
                                shortenSearchingFor.length());
            } else {
                menuReloadEntry = getString(R.string.action_reload_web_view) + " " + address;
            }
            boolean first = true;
            for (int i = 0; i < menu.size(); i++) {
                if (menu.getItem(i).getTitle().equals(menuReloadEntry)) {
                    first = false;
                }
            }
            if (first) {
                menuReloadItem = menu.add(Menu.NONE, Menu.NONE, MENU_ORDER, menuReloadEntry);
            }
        }
        super.onPrepareOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (menuEnabled && item.getTitle().equals(menuReloadItem.getTitle())) {
            webView.reload();
            // So also other WebViewFragments can reload content if they have the same address
            return super.onOptionsItemSelected(item);
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
    @Override
    public void setMenuEnabled(boolean menuEnabled) {
        this.menuEnabled = menuEnabled;
    }

    @Override
    public String getReadableName() {
        return Util.getACString(R.string.fragment_web_view) + " " + address;
    }
    @Override
    public Container getParent() {
        return parent;
    }
    @Override
    public void setParent(Container parent) {
        this.parent = parent;
    }
    @Override
    public String getRecreationKey() {
        return fixRecreationKey(WEB_VIEW_FRAGMENT + SEP + address + SEP + width + SEP +
                height + SEP + javaScriptEnabled + SEP + allowExternalLinks + SEP);
    }
    public static WebViewFragment recoverFromRecreationKey(String key) {
        try {
            String[] args = Util.split(key, SEP, 0);
            String address = args[1];
            double width = Double.parseDouble(args[2]);
            double height = Double.parseDouble(args[3]);
            boolean javaScriptEnabled = Boolean.parseBoolean(args[4]);
            boolean allowExternalLinks = Boolean.parseBoolean(args[5]);
            return newInstance(address, width, height, javaScriptEnabled, allowExternalLinks);
        } catch (Exception e) {
            Log.e(LOG_TAG, "recoverFromRecreationKey: illegal key: " + key);
            Log.e(LOG_TAG, "Got exception: " + e);
            return null;
        }
    }
    @Override
    public ModuleFragment copy() {
        return newInstance(address, width, height, javaScriptEnabled, allowExternalLinks);
    }

    private class CustomWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            connected = true;
        }
        @Override
        public boolean shouldOverrideUrlLoading (WebView view, String url) {
            if (allowExternalLinks || Uri.parse(url).getHost().equals(Uri.parse(address).getHost())) {
                return false;// Open link in WebView
            }
            // Else: open default browser
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            return true;
        }
        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                                    String failingUrl) {
            Log.i(LOG_TAG, address + " received Error " + errorCode + ": " + description +
                    "; failingUrl: " + failingUrl);
            connected = false;
        }
    }

    public String getAddress() {
        return address;
    }
    public double getArgWidth() {
        return width;
    }
    public double getArgHeight() {
        return height;
    }
    public boolean getJavaScriptEnabled() {
        return javaScriptEnabled;
    }
    public boolean getAllowExternalLinks() {
        return allowExternalLinks;
    }
    public void setValues(String address, double width, double height, boolean javaScriptEnabled,
                          boolean allowExternalLinks) {
        if (width <= 0) {
            width = 1;
        }
        if (height != -1 && height <= 0) {// height == -1 → match_parent
            height = 1;
        }
        this.width = width;
        this.height = height;
        resize();
        this.javaScriptEnabled = javaScriptEnabled;
        this.allowExternalLinks = allowExternalLinks;
        webView.getSettings().setJavaScriptEnabled(javaScriptEnabled);
        webView.loadUrl(address);
    }
    @Override
    public void resize() {
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            View containerView = ((MainActivity) activity).getViewContainer();
            Util.resizeLayoutWidth(containerView, baseLayout, width);
            Util.resizeLayoutHeight(containerView, baseLayout, height);
        } else {
            Log.w(LOG_TAG, "Can't resize: !(activity instanceof MainActivity)");
        }
    }

    @Override
    public boolean isConnected() {
        return connected;
    }
}
