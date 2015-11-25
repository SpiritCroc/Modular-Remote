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

package de.spiritcroc.modular_remote;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import de.spiritcroc.modular_remote.dialogs.SelectContainerDialog;
import de.spiritcroc.modular_remote.modules.Container;
import de.spiritcroc.modular_remote.modules.DisplayFragment;
import de.spiritcroc.modular_remote.modules.HorizontalContainerFragment;
import de.spiritcroc.modular_remote.modules.ModuleFragment;
import de.spiritcroc.modular_remote.modules.PageContainerFragment;
import de.spiritcroc.modular_remote.modules.ScrollContainerFragment;
import de.spiritcroc.modular_remote.modules.SpinnerFragment;
import de.spiritcroc.modular_remote.modules.ToggleFragment;
import de.spiritcroc.modular_remote.modules.WebViewFragment;
import de.spiritcroc.modular_remote.modules.WidgetContainerFragment;

public abstract class Util {
    private static final String LOG_TAG = Util.class.getSimpleName();
    private static final boolean DEBUG = false;

    public static final boolean SCREENSHOT = false;

    private static Context applicationContext;

    public static void init(Context context) {
        if (context != null) {
            applicationContext = context.getApplicationContext();
        }
    }

    // Recovery key constants:
    // Increment if recoveryKey scheme changed (for single modules or full recreationKey)
    public static final int RECREATION_KEY_VERSION = 1;
    public static final String RK_ATTRIBUTE_SEPARATOR = "䷀";
    // Alternative to above, used for unknown numbers:
    public static final String RK_ARRAY_ATTRIBUTE_SEPARATOR = "䷁";
    public static final String RK_CONTAINER_BRACKET = "䷄";
    public static final String RK_CUSTOMIZED_MENU_SEPARATOR = "䷂";
    public static final String RK_CUSTOMIZED_MENU_ATTRIBUTE_SEPARATOR = "䷃";
    public static final String RK_FRAGMENT_POS = "䷚";
    // Replace empty string in recovery keys with following string in order to be able to recreate
    // with String.split()
    public static final String RK_EMPTY_STRING_REPLACEMENT = "䷅";

    // Strings users should not use, as it would damage the recovery keys
    public static final String[] FORBIDDEN_SUBSTRINGS = {
            RK_ATTRIBUTE_SEPARATOR,
            RK_ARRAY_ATTRIBUTE_SEPARATOR,
            RK_CONTAINER_BRACKET,
            RK_CUSTOMIZED_MENU_SEPARATOR,
            RK_CUSTOMIZED_MENU_ATTRIBUTE_SEPARATOR,
            RK_EMPTY_STRING_REPLACEMENT,
            RK_FRAGMENT_POS,
            // Reserved for future needs:
            "䷆", "䷇", "䷈", "䷉", "䷊", "䷋", "䷌", "䷍", "䷎", "䷏",
            "䷐", "䷑", "䷒", "䷓", "䷔", "䷕", "䷖", "䷗", "䷘", "䷙", "䷛", "䷜", "䷝", "䷞", "䷟",
            "䷠", "䷡", "䷢", "䷣", "䷤", "䷥", "䷦", "䷧", "䷨", "䷩", "䷪", "䷫", "䷬", "䷭", "䷮", "䷯",
            "䷰", "䷱", "䷲", "䷳", "䷴", "䷵", "䷶", "䷷", "䷸", "䷹", "䷺", "䷻", "䷼", "䷽", "䷾", "䷿"
    };

    /**
     * @return
     * User input if input allowed. null if invalid
     */
    public static String getUserInput (TextView textView, boolean emptyForbidden) {
        String input = textView.getText().toString();
        if (input.equals("")) {
            if (emptyForbidden) {
                Context context = textView.getContext();
                textView.setError(context.getString(R.string.error_should_not_be_empty));
                return null;
            } else {
                return input;
            }
        }
        for (String forbidden: FORBIDDEN_SUBSTRINGS) {
            if (input.contains(forbidden)) {
                Context context = textView.getContext();
                String errorMsg = context.getString(R.string.error_should_not_contain_s, forbidden);
                textView.setError(errorMsg);
                return null;
            }
        }
        return input;
    }

    /**
     * @return
     * Size if valid, else -1
     */
    public static double getSizeInput (TextView textView) {
        String input = textView.getText().toString();
        if (input.equals("")) {
            Context context = textView.getContext();
            textView.setError(context.getString(R.string.error_should_not_be_empty));
            return -1;
        }
        try {
            double result = Double.parseDouble(input);
            if (result > 0) {
                return result;
            } else {
                Context context = textView.getContext();
                textView.setError(context.getString(R.string.error_should_be_higher_than_zero));
                return -1;
            }
        } catch(Exception e) {
            Context context = textView.getContext();
            textView.setError(context.getString(R.string.error_invalid));
            return -1;
        }
    }

    private static int bufferedX = -1;
    private static int getX(View view, boolean force) {
        if (!force && bufferedX != -1) {
            return bufferedX;
        }
        int newX = view.getMeasuredWidth();
        if (newX > 0) {
            bufferedX = newX;
        }
        if (DEBUG) Log.v(LOG_TAG, "getX" + bufferedX);
        return bufferedX;
    }
    public static int getX(View view) {
        return getX(view, false);
    }

    /**
     * Loads the current y value
     * @return
     * Whether the y value has changed since last checking it
     */
    public static boolean newY(View view) {
        int previous = bufferedY;
        return getY(view, true) != previous;
    }
    private static int bufferedY = -1;
    public static int getY(View view, boolean force) {
        if (!force && bufferedY != -1) {
            return bufferedY;
        }
        int newY = view.getMeasuredHeight();
        if (view instanceof ViewPager) {
            ViewPager viewPager = (ViewPager) view;
            for (int i = 0; i < viewPager.getChildCount(); i++) {
                View child = viewPager.getChildAt(i);
                if (child.getVisibility() != View.GONE &&
                        (child instanceof PagerTabStrip ||
                        child instanceof PagerTitleStrip)) {
                    // Some views use space within the viewPager, but are not part of the page
                    newY -= child.getMeasuredHeight();
                }
            }
        }
        if (newY > 0) {
            bufferedY = newY;
        }
        if (DEBUG) Log.v(LOG_TAG, "getY " + bufferedY);
        return bufferedY;
    }
    public static int getY(View view) {
        return getY(view, false);
    }

    public static void resizeLayoutWidth(View containerView, LinearLayout layout, double size) {
        resizeLayoutWidth(containerView, layout, size, LinearLayout.LayoutParams.MATCH_PARENT);
    }
    public static void resizeLayoutWidth(View containerView, LinearLayout layout, double size,
                                         int valueForNegativeSize) {
        if (size > 0) {
            layout.getLayoutParams().width = getWidthFromBlockUnits(containerView, size, false);
        } else {
            layout.getLayoutParams().width = valueForNegativeSize;
        }
        layout.requestLayout();
    }

    public static void resizeLayoutHeight(View containerView, LinearLayout layout, double size) {
        resizeLayoutHeight(containerView, layout, size, LinearLayout.LayoutParams.MATCH_PARENT);
    }
    public static void resizeLayoutHeight(View containerView, LinearLayout layout, double size,
                                          int valueForNegativeSize) {
        if (size > 0) {
            layout.getLayoutParams().height = getHeightFromBlockUnits(containerView, size, false);
        } else {
            layout.getLayoutParams().height = valueForNegativeSize;
        }
        layout.requestLayout();
    }
    public static int getWidthFromBlockUnits(View containerView, double size, boolean pos) {
        int screenWidth = getX(containerView);
        if (size <= 0) {
            return screenWidth;
        }

        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(containerView.getContext());
        int blockUnits = getPreferenceInt(preferences, Preferences.KEY_BLOCK_SIZE, 4);
        if (blockUnits <= 0) {
            blockUnits = 4;
        }

        if (pos) {
            return (int) Math.ceil(size * screenWidth / blockUnits);
        } else {
            return (int) Math.floor(size * screenWidth / blockUnits);
        }
    }

    public static int getHeightFromBlockUnits(View containerView, double size, boolean pos) {
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(containerView.getContext());
        if (!preferences.getBoolean(Preferences.KEY_USE_BLOCK_SIZE_HEIGHT, false)) {
            return getWidthFromBlockUnits(containerView, size, pos);
        }

        int screenHeight = getY(containerView);
        if (size <= 0) {
            return screenHeight;
        }

        int blockUnits = getPreferenceInt(preferences, Preferences.KEY_BLOCK_SIZE_HEIGHT, 6);
        if (blockUnits <= 0) {
            blockUnits = 6;
        }

        if (pos) {
            return (int) Math.ceil(size * screenHeight / blockUnits);
        } else {
            return (int) Math.floor(size*screenHeight/blockUnits);
        }
    }

    public static int blockRound(View containerView, double d, boolean yDir) {
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(containerView.getContext());
        int blockUnits, screenSize;
        if (yDir) {
            if (!preferences.getBoolean(Preferences.KEY_USE_BLOCK_SIZE_HEIGHT, false)) {
                return blockRound(containerView, d, false);
            }
            blockUnits = getPreferenceInt(preferences, Preferences.KEY_BLOCK_SIZE_HEIGHT, 6);
            screenSize = getY(containerView);
        } else {
            blockUnits = getPreferenceInt(preferences, Preferences.KEY_BLOCK_SIZE, 4);
            screenSize = getX(containerView);
        }
        return (int) Math.round(d/screenSize*blockUnits);
    }

    public static void restoreContentFromRecreationKey(Container container, String key,
                                                       boolean menuEnabled) {
        String[] args = split(key, getSeparator(container), 0);
        for (int i = 1; i < args.length; i++) {
            String[] subArgs = split(args[i], RK_ATTRIBUTE_SEPARATOR, 1);
            ModuleFragment fragment = null;
            if (ModuleFragment.WEB_VIEW_FRAGMENT.equals(subArgs[0])) {
                fragment = WebViewFragment.recoverFromRecreationKey(args[i]);
            } else if (ModuleFragment.SCROLL_CONTAINER_FRAGMENT.equals(subArgs[0])) {
                fragment = ScrollContainerFragment.recoverFromRecreationKey(args[i]);
            } else if (ModuleFragment.WIDGET_CONTAINER_FRAGMENT.equals(subArgs[0])) {
                fragment = WidgetContainerFragment.recoverFromRecreationKey(args[i]);
            } else if (ModuleFragment.HORIZONTAL_CONTAINER.equals(subArgs[0])) {
                fragment = HorizontalContainerFragment.recoverFromRecreationKey(args[i]);
            } else if (ModuleFragment.BUTTON_FRAGMENT.equals(subArgs[0])) {
                fragment = DisplayFragment.recoverFromRecreationKey(args[i], true);
            } else if (ModuleFragment.DISPLAY_FRAGMENT.equals(subArgs[0])) {
                fragment = DisplayFragment.recoverFromRecreationKey(args[i], false);
            } else if (ModuleFragment.SPINNER_FRAGMENT.equals(subArgs[0])) {
                fragment = SpinnerFragment.recoverFromRecreationKey(args[i]);
            } else if (ModuleFragment.TOGGLE_FRAGMENT.equals(subArgs[0])) {
                fragment = ToggleFragment.recoverFromRecreationKey(args[i]);
            } else {
                Log.w(LOG_TAG, "restoreContentFromRecreationKey: Could not find fragment with " +
                        "className " + subArgs[0]);
            }
            if (fragment != null) {
                fragment.setMenuEnabled(menuEnabled);
                container.addFragment(fragment);
            }
        }
    }
    public static String getSeparator(Container container) {
        return RK_CONTAINER_BRACKET + container.getDepth() + RK_CONTAINER_BRACKET;
    }

    /*
    Following code to obtain unique view id so my container concept works is from
    https://stackoverflow.com/questions/1714297/android-view-setidint-id-programmatically-how-to-avoid-id-conflicts
     */
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    public static int generateViewId() {
        for (;;) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    public static String createCommandChain(String bossCommand, String command) {
        if (bossCommand.contains("…")) {
            command = bossCommand.replaceFirst("…", command);
        } else {
            String copy = command;
            command = bossCommand;
            for (int i = 0; i < copy.length(); i++) {
                command = command.replaceFirst("·", "" + copy.charAt(i));
            }
        }
        return command;
    }

    public static void suggestPreviousIps(final Fragment fragment,
                                          final AutoCompleteTextView textView) {
        Activity activity = fragment.getActivity();
        final TcpConnectionManager tcpConnectionManager =
                TcpConnectionManager.getInstance(activity.getApplicationContext());
        textView.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1,
                tcpConnectionManager.getConnectionSuggestions()));

        if (fragment instanceof ReceiverIpSelectorUser) {
            textView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    TcpConnectionManager.TcpConnection connection =
                            tcpConnectionManager.getTcpConnection(textView.getText().toString());
                    ((ReceiverIpSelectorUser) fragment)
                            .setReceiverType(connection.getType());
                }
            });
        }
    }

    public static long getPageId(Context context) {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        long lastId = sharedPreferences.getLong(Preferences.KEY_LAST_PAGE_ID, -1);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(Preferences.KEY_LAST_PAGE_ID, ++lastId).apply();
        return lastId;
    }

    public static class StringReference {
        public String value;
    }

    public static View scrollView(View view) {
        ScrollView scrollView = new ScrollView(view.getContext());
        scrollView.addView(view);
        return scrollView;
    }

    /**
     * @return
     * Recreation key that can contain "" values and still be recovered with String.split()
     */
    public static String fixRecreationKey(String key, String separator) {
        return key.replaceAll(separator+separator, separator+RK_EMPTY_STRING_REPLACEMENT+separator);
    }

    /**
     * @param minArgCount
     * Number of expected arguments, if there are fewer: fill with empty strings
     * Use 0 if possible out-of-range exception already handled
     * @return
     * Args that were supposed to be saved with fixRecreationKey
     */
    public static String[] recoverRecreationArgs(String[] args, int minArgCount) {
        String[] result = new String[Math.max(args.length, minArgCount)];
        for (int i = 0; i < args.length; i++) {
            if (RK_EMPTY_STRING_REPLACEMENT.equals(args[i])) {
                result[i] = "";
            } else {
                result[i] = args[i];
            }
        }
        // Fill with empty args
        for (int i = args.length; i < result.length; i++) {
            result[i] = "";
        }
        return result;
    }

    public static String[] split(String s, String separator, int minArgCount) {
        return recoverRecreationArgs(s.split(separator), minArgCount);
    }

    public static void addFragmentToContainer(Activity activity,
                                              @NonNull ModuleFragment fragment,
                                              PageContainerFragment page,
                                              @Nullable Container container) {
        if (container == null) {
            new SelectContainerDialog().setValues(page, fragment)
                    .show(activity.getFragmentManager(), "SelectContainerDialog");
        } else {
            container.addFragment(fragment);
            if (activity instanceof MainActivity) {
                // Scroll to newly added fragment
                Container parent = fragment instanceof Container ?
                        (Container) fragment : fragment.getParent();
                while (parent != null && !(parent instanceof PageContainerFragment) &&
                        parent instanceof ModuleFragment) {
                    parent = ((ModuleFragment) parent).getParent();
                }
                if (parent instanceof PageContainerFragment) {
                    ((MainActivity) activity).scrollToPage((PageContainerFragment) parent);
                }
            }
        }
    }
    public static void addWidgetToContainer(Activity activity, double width, double height,
                                            PageContainerFragment page,
                                            @Nullable Container container,
                                            @Nullable DialogFragment dismissDialog) {
        if (container == null) {
            new SelectContainerDialog().addWidget(page, width, height)
                    .show(activity.getFragmentManager(), "SelectContainerDialog");
        } else if (activity instanceof MainActivity) {
            ((MainActivity) activity).setAddWidgetListener(dismissDialog);
            ((MainActivity) activity).addWidget(container, width, height);
        } else {
            Log.w(LOG_TAG, "addWidgetToContainer: !(activity instanceof MainActivity)");
        }
    }

    public static Spannable getContainerContentReadableName(Container container,
                                                            @Nullable String prefix) {
        if (prefix == null) {
            prefix = "";
        }
        if (container.isEmpty()) {
            return new SpannableString(prefix + container.getReadableName());
        } else {
            ModuleFragment[] fragments = container.getFragments();
            String name = prefix + container.getReadableName();
            int spanStartIndex = name.length();
            name += " [" + fragments[0].getReadableName();
            int contentColor = fragments[0].getActivity().getResources()
                    .getColor(R.color.text_container_content);
            for (int i = 1; i < fragments.length; i++) {
                name += "; " + fragments[i].getReadableName();
            }
            name += "]";
            Spannable nameSpan = new SpannableString(name);
            nameSpan.setSpan(new ForegroundColorSpan(contentColor), spanStartIndex, name.length(),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            return nameSpan;
        }
    }

    public static Container[] getAllContainers(Container container) {
        ArrayList<Container> list = new ArrayList<>();
        list.add(container);
        ModuleFragment[] fragments = container.getFragments();
        for (ModuleFragment fragment: fragments) {
            if (fragment instanceof Container) {
                Container[] addition = ((Container) fragment).getAllContainers();
                list.addAll(Arrays.asList(addition));
            }
        }
        return list.toArray(new Container[list.size()]);
    }

    public static boolean hasContainerOverlappingFragments(Container container) {
        ModuleFragment[] fragments = container.getFragments();
        ArrayList<Container> containedContainers = new ArrayList<>();
        for (int i = 0; i < fragments.length; i++) {
            if (fragments[i] instanceof Container) {
                containedContainers.add((Container) fragments[i]);
            }
            View currentView = fragments[i].getView();
            if (currentView != null) {
                Rect currentRect = new Rect(currentView.getLeft(), currentView.getTop(),
                        currentView.getRight(), currentView.getBottom());

                for (int j = i + 1; j < fragments.length; j++) {
                    View checkView = fragments[j].getView();
                    if (checkView != null &&
                            currentRect.intersects(checkView.getLeft(), checkView.getTop(),
                                    checkView.getRight(), checkView.getBottom())) {
                        Log.i(LOG_TAG, fragments[i].getReadableName() + " overlaps with " +
                                fragments[j].getReadableName());
                        return true;
                    }
                }
            }
        }

        for (int i = 0; i < containedContainers.size(); i++) {
            if (hasContainerOverlappingFragments(containedContainers.get(i))) {
                return true;
            }
        }

        return false;
    }

    public static int getPreferenceInt(SharedPreferences sharedPreferences, String key,
                                    int defaultValue) {
        try {
            return Integer.parseInt(sharedPreferences.getString(key, "" + defaultValue));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Use instead of context.getString() if context might be null
     * Requires init() to be called before at least once
     */
    public static String getACString(int resId) {
        if (applicationContext == null) {
            Log.e(LOG_TAG, "getADString: applicationContext == null");
            return "";
        } else {
            return applicationContext.getString(resId);
        }
    }

    /**
     * Use this to test which chars the device is able to display
     */
    public static String getDebugText() {
        final String[][] test  = {
                {"00", " "},
                //{"01", "⟳⤨"},
                //{"01", "⟳⤨"},//alternate
                //{"01", "⟳⤭"},//alternate
                //{"01", "⟳⤮"},//alternate
                //{"01", "⟳x⇉"},//alternate
                //{"01", "⟳&#10536;"},//alternate
                //{"01", "⟳\u2928"},//alternate
                {"01", "⟳x"},//alternate
                {"02", "⟳"},
                //{"03", "⤨"},
                {"03", "x"},//alternate
                //{"04", "⧎"},
                //{"04", "\u23F6\u23F7"},//alternate
                {"04", "▲▼"},//alternate
                {"05", "DOL"},
                {"06", "BY"},
                {"07", "I"},
                {"08", "II"},
                {"09", "◀"},
                {"0A", "▶"},
                {"0B", "♡"},
                {"0C", "."},
                {"0D", ".0"},
                {"0E", ".5"},
                {"0F", "Ω"},
                {"10", "0"},
                {"11", "1"},
                {"12", "2"},
                {"13", "3"},
                {"14", "4"},
                {"15", "5"},
                {"16", "6"},
                {"17", "7"},
                {"18", "8"},
                {"19", "9"},
                {"1A", "A"},
                {"1B", "B"},
                {"1C", "C"},
                {"1D", "F"},
                {"1E", "M"},
                {"1F", " ̄"},
                {"20", " "},
                {"21", "!"},
                {"22", "\""},
                {"23", "#"},
                {"24", "$"},
                {"25", "%"},
                {"26", "&"},
                {"27", "\'"},
                {"28", "("},
                {"29", ")"},
                {"2A", "*"},
                {"2B", "+"},
                {"2C", ","},
                {"2D", "-"},
                {"2E", "."},
                {"2F", "/"},
                {"30", "0"},
                {"31", "1"},
                {"32", "2"},
                {"33", "3"},
                {"34", "4"},
                {"35", "5"},
                {"36", "6"},
                {"37", "7"},
                {"38", "8"},
                {"39", "9"},
                {"3A", ":"},
                {"3B", ";"},
                {"3C", "<"},
                {"3D", "="},
                {"3E", ">"},
                {"3F", "?"},
                {"40", "@"},
                {"41", "A"},
                {"42", "B"},
                {"43", "C"},
                {"44", "D"},
                {"45", "E"},
                {"46", "F"},
                {"47", "G"},
                {"48", "H"},
                {"49", "I"},
                {"4A", "J"},
                {"4B", "K"},
                {"4C", "L"},
                {"4D", "M"},
                {"4E", "N"},
                {"4F", "O"},
                {"50", "P"},
                {"51", "Q"},
                {"52", "R"},
                {"53", "S"},
                {"54", "T"},
                {"55", "U"},
                {"56", "V"},
                {"57", "W"},
                {"58", "X"},
                {"59", "Y"},
                {"5A", "Z"},
                {"5B", "["},
                {"5C", "\\"},
                {"5D", "]"},
                {"5E", "^"},
                {"5F", "_"},
                {"60", "||"},
                {"61", "a"},
                {"62", "b"},
                {"63", "c"},
                {"64", "d"},
                {"65", "e"},
                {"66", "f"},
                {"67", "g"},
                {"68", "h"},
                {"69", "i"},
                {"6A", "j"},
                {"6B", "k"},
                {"6C", "l"},
                {"6D", "m"},
                {"6E", "n"},
                {"6F", "o"},
                {"70", "p"},
                {"71", "q"},
                {"72", "r"},
                {"73", "s"},
                {"74", "t"},
                {"75", "u"},
                {"76", "v"},
                {"77", "w"},
                {"78", "x"},
                {"79", "y"},
                {"7A", "z"},
                {"7B", "{"},
                {"7C", "|"},
                {"7D", "}"},
                {"7E", "~"},
                {"7F", "■"},
                {"80", "Œ"},
                {"81", "œ"},
                {"82", "IJ"},
                {"83", "ij"},
                {"84", "π"},
                {"85", "±"},
                {"86", " "},
                {"87", " "},
                {"88", " "},
                {"89", " "},
                {"8A", " "},
                {"8B", " "},
                {"8C", "←"},
                {"8D", "↑"},
                {"8E", "→"},
                {"8F", "↓"},
                {"90", "+"},
                {"91", "♪"},
                //{"92", "\uD83D\uDDC0"},
                {"92", "\uD83D\uDCC1"},//alternate
                //{"92", "\uD83D\uDCC2"},//alternate
                {"93", " "},
                {"94", " "},
                {"95", " "},
                {"96", " "},
                {"97", " "},
                {"98", " "},
                {"99", " "},
                {"9A", " "},
                {"9B", " "},
                {"9C", " "},
                {"9D", " "},
                {"9E", " "},
                {"9F", " "},
                {"A0", " "},
                {"A1", "¡"},
                {"A2", "¢"},
                {"A3", "£"},
                {"A4", "¤"},
                {"A5", "¥"},
                {"A6", "¦"},
                {"A7", "§"},
                {"A8", "̈"},
                {"A9", "©"},
                {"AA", "ª"},
                {"AB", "«"},
                {"AC", "¬"},
                {"AD", "-"},
                {"AE", "®"},
                {"AF", "̄"},
                {"B0", "°"},
                {"B1", "±"},
                {"B2", "²"},
                {"B3", "³"},
                {"B4", "́"},
                {"B5", "μ"},
                {"B6", "¶"},
                {"B7", "·"},
                {"B8", "̧"},
                {"B9", "¹"},
                {"BA", "º"},
                {"BB", "»"},
                {"BC", "¼"},
                {"BD", "½"},
                {"BE", "¾"},
                {"BF", "¿"},
                {"C0", "À"},
                {"C1", "Á"},
                {"C2", "Â"},
                {"C3", "Ã"},
                {"C4", "Ä"},
                {"C5", "Å"},
                {"C6", "Æ"},
                {"C7", "Ç"},
                {"C8", "È"},
                {"C9", "É"},
                {"CA", "Ê"},
                {"CB", "Ë"},
                {"CC", "Ì"},
                {"CD", "Í"},
                {"CE", "Î"},
                {"CF", "ï"},
                {"D0", "Ð"},
                {"D1", "Ñ"},
                {"D2", "Ò"},
                {"D3", "Ó"},
                {"D4", "Ô"},
                {"D5", "Õ"},
                {"D6", "Ö"},
                {"D7", "×"},
                {"D8", "Ø"},
                {"D9", "Ù"},
                {"DA", "Ú"},
                {"DB", "Û"},
                {"DC", "Ü"},
                {"DD", "Ý"},
                {"DE", "Þ"},
                {"DF", "ß"},
                {"E0", "à"},
                {"E1", "á"},
                {"E2", "â"},
                {"E3", "ã"},
                {"E4", "ä"},
                {"E5", "å"},
                {"E6", "æ"},
                {"E7", "ç"},
                {"E8", "è"},
                {"E9", "é"},
                {"EA", "ê"},
                {"EB", "ë"},
                {"EC", "ì"},
                {"ED", "í"},
                {"EE", "î"},
                {"EF", "ï"},
                {"F0", "ð"},
                {"F1", "ñ"},
                {"F2", "ò"},
                {"F3", "ó"},
                {"F4", "ô"},
                {"F5", "õ"},
                {"F6", "ö"},
                {"F7", "÷"},
                {"F8", "ø"},
                {"F9", "ù"},
                {"FA", "ú"},
                {"FB", "û"},
                {"FC", "ü"},
                {"FD", "ý"},
                {"FE", "þ"},
                {"FF", "ÿ"}
        };
        String s = "PIONEER:";
        for (int i = 0; i < test.length; i++) {
            s += "\n" + test[i][0] + ":" + test[i][1] + ":";
        }
        s += "\nOTHER:\n⏩\n⏪\n⏭\n⏮\n⏯\n";
        return s;
    }
}
