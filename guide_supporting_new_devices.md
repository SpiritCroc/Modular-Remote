# Guide to supporting new TCP devices

## 1. Find a reference sheet
In order to support new devices, you have to know the specific TCP commands.
Some companies provide a reference sheet so you can look up all possible commands.
All known reference sheets can be found [here](https://github.com/SpiritCroc/Modular-Remote/blob/master/known_reference_sheets.md).
If your device is not present there, just use the [search engine of your choice](http://www.duckduckgo.com) and try to find a reference sheet, or you won't be able to operate your device using this app.

## 2. Test your commands
If your device is not officially supported, but supports TCP commands, it should already be able to receive commands from this app nevertheless.
Just add a button for an "unspecified" device, enter the IP of the device you want to control, and enter your commands in raw form.
Commands that don't work like this won't work after following this guide either.

## 3. Create an XML-File
Create a file called "strings_tcp_commands_\[device\].xml", replacing \[device\] with a meaningful identifier for the supported devices of your command sheet (e.g. "pioneer" for Pioneer AV receivers).
To edit your XML, I recommend using an editor with syntax highlighting, for example Kate/KWrite for Linux or Notepad++ for Windows.
Use the following body for your XML:

```xml
<?xml version="1.0" encoding="utf-8"?>
<!--
    Copyright (C) 2015 SpiritCroc

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
-->

<resources>
    <!--Add your content here-->
</resources>
```


## 4. Create string-arrays
To list your commands, you need at least those three arrays in your created XML:

```xml
string-array [device]_tcp_commands
```

Contains the readable command names.

```xml
string-array [device]_tcp_commands_values
```

Contains the raw strings sent to the TCP device (leaving out the carriage-return in the end, as the app adds those automatically).

```xml
integer-array [device]_tcp_commands_has_submenu
```

Contains information required for linking submenus (as described later). If you don't need a submenu, just add an item "0" (that's a zero).

You can add arrays to your XML like this:

```xml
    <?xml version="1.0" encoding="utf-8"?>
    <resources>
        <string-array name="[device]_tcp_commands" translateable="false">
            <!--add your items here later-->
        </string-array>
        <string-array name="[device]_tcp_commands_values" translateable="false">
            <!--add your items here later-->
        </string-array>
        <integer-array name="[device]_tcp_commands_has_submenu" translateable="false">
            <!--This is the syntax of a comment btw-->
        </integer-array>
    </resources>
```

Of course, you should always replace \[device\] with your chosen device identifier.

## 5. Adding items
Each command consists of one item of each of those arrays, all being at the same position.

Adapted example from strings_tcp_commands_pionner.xml:

```xml
    <string-array name="pioneer_tcp_commands" translateable="false">
        <item>Cursor up</item>
        <item>Cursor down</item>
        <item>Cursor right</item>
        <item>Cursor left</item>
    </string-array>
    <string-array name="pioneer_tcp_commands_values" translateable="false">
        <item>CUP</item>
        <item>CDN</item>
        <item>CRI</item>
        <item>CLE</item>
    </string-array>
    <integer-array name="pioneer_tcp_commands_has_submenu" translateable="false">
        <item>0</item>
        <item>0</item>
        <item>0</item>
        <item>0</item>
    </integer-array>
```

This means that the first command has the name "Cursor up" and sends "CUP" to the TCP device (We don't have any submenus, so the has_submenu value is 0) and so on.
Following this scheme, all three arrays should have the same amount of items.

## 6. Providing translateable strings
In the previously described arrays, we forbid to translate the strings by using translateable="false".
If you want to be able to provide translations, translateable strings can be found in [strings_tcp_commands_general.xml](https://github.com/SpiritCroc/Modular-Remote/blob/master/app/src/main/res/values/strings_tcp_commands_general.xml).
You can reuse present strings or add your own. You can then reference them like this:

```xml
    <string-array name="pioneer_tcp_commands" translateable="false">
        <item>@string/command_cursor_up</item>
        <item>@string/command_cursor_down</item>
        <item>@string/command_cursor_right</item>
        <item>@string/command_cursor_left</item>
    </string-array>
```

## 7. Providing submenus
You might want to add commands that can take a variable parameter (like for example "Input select" for Pioneer AV receivers, consisting of the input code (two integers) and then "FN").
For this reason, you can chain commands using submenus.

At first, your command value should define what should be replaced with your selection from a submenu. This can be done in two ways:
- Using "…": This means that "…" will later be replaced by an undefined amount of characters
- Using "·": This means that each "·" will later be replaced by one character

Be sure to use the exact replacement characters specified above (really use "…", not "...").

Then, you can create your submenus in the same way you created the strings-array before, just use different names now:

```xml
[device]_tcp_commands_submenu_[number of your submenu]
```

```xml
[device]_tcp_commands_submenu_[number of your submenu]_values
```

```xml
[device]_tcp_commands_submenu_[number of your submenu]_has_submenu
```

Replace \[number of your submenu\] with an integer, beginning with "1".
As an example, here is the first Pioneer submenu:

```xml
    <string-array name="pioneer_tcp_commands_submenu_1" translateable="false">
        <item>@string/command_on</item>
        <item>@string/command_off</item>
    </string-array>
    <string-array name="pioneer_tcp_commands_submenu_1_values" translateable="false">
        <item>O</item>
        <item>F</item>
    </string-array>
    <integer-array name="pioneer_tcp_commands_submenu_1_has_submenu" translateable="false">
        <item>0</item>
        <item>0</item>
    </integer-array>
```
Finally, you have to link your previous command with the new submenu. Just use the number of your submenu as item for your command in the _has_submenu array.
Here is an example of a Pioneer command using the previously shown submenu:

```xml
    <string-array name="pioneer_tcp_commands" translateable="false">
        <item>@string/command_power</item>
    </string-array>
    <string-array name="pioneer_tcp_commands_values" translateable="false">
        <item>P·</item>
    </string-array>
    <integer-array name="pioneer_tcp_commands_has_submenu" translateable="false">
        <item>1</item>
    </integer-array>
```

Alternatively, if a submenu makes less sense then letting users enter the missing part of the command themselves (e.g. when renaming an input of an AV receiver), you can use "-1" as _has_submenu value (this will open an editable text field in the app).

## 8. Further arrays
The arrays described above are used for simple commands sent to the TCP device, e.g. when clicking a button. But if you specify some more arrays, you can unlock more features:

### 8.1 Responses
Consisting of the three arrays \[device\]_tcp_responses, \[device\]_tcp_responses_values, and \[device\]_tcp_responses_has_submenu.

Those use the same submenu system as commands (so you can and should reuse command submenu arrays), and are used to show some received information from the TCP device, e.g. as used on Displays. A response should always have a submenu (so don't use has_submenu 0). If the response is to complicated that it requires some code, you can use the submenu -1 and write the required code into TCPConnectionManager.enhancedResponse().

### 8.2 Start requests
Consisting of the two arrays \[device\]_tcp_start_requests and \[device\]_tcp_start_requests_submenus.

If you want some information when starting the app (probably in order to let the displays show some information as specified in 8.1, e.g. the TCP devices on/off state), then you can use those arrays to request it. If you specify a submenu, all possible commands are sent (useful e.g. when requesting the input names of an AV receiver).

### 8.3 Init requests
Consisting of the three arrays \[device\]_tcp_should_init_responses, \[device\]_tcp_init_requests and \[device\]_tcp_init_requests_submenus.

It basically works like TCP start requests, but the requests are not sent when starting the app, but when receiving a response included in \[device\]_tcp_should_init_responses.
A possible usage is an AV receiver telling the app that it switched on, and then the app requesting more information, like e.g. the volume level, mute status etc..

### 8.4 Should clear display responses
Consisting of the three arrays \[device\]_tcp_should_clear_display_responses, \[device\]_tcp_should_stop_clear_display_responses and \[device\]_tcp_clearable_displays.

This is useful when the TCP device sends the information that it switched off and thus some information on some displays is not fitting anymore (like e.g. the volume level of an AV receiver). \[device\]_tcp_should_clear_display_responses specifies the responses that should trigger the displays to clear, and \[device\]_tcp_clearable_displays has the response values specified in  \[device\]_tcp_responses_values that displays that should be cleared could be listening to. As information gets saved in order to give all interested modules the information again without having to request it, they will get an empty display if a response contained in \[device\]_tcp_should_clear_display_responses was previously received, so you should put responses that cancel this behaviour (probably the power on response) in \[device\]_tcp_should_stop_clear_display_responses.

### 8.5 Dropdown menus
Consisting of the four arrays \[device\]_tcp_dropdown_menu_names, \[device\]_tcp_dropdown_menu_command_values, \[device\]_tcp_dropdown_menu_response_values and \[device\]_tcp_dropdown_menu_submenus.

You should list commands here that have a submenu and a response that is returned after the command is sent with the same submenu. This will be used for spinners (= dropdown menus) that show the current selection of your TCP device, and let you select another one. A possible usage is the input selection of an AV receiver.

### 8.6 Customizable menus
Consisting of the three arrays \[device\]_tcp_customizable_submenu_names, \[device\]_tcp_customizable_submenu_numbers and \[device\]_tcp_customizable_submenu_values.

Some menus can get customized by code and/or by the users.
\[device\]_tcp_customizable_submenu_names contains the user friendly names of the submenus, \[device\]_tcp_customizable_submenu_numbers the numbers of the customizable submenus, and \[device\]_tcp_customizable_submenu_values the kind of customization (containing either @integer/customizable_submenu_hide_items, @integer/customizable_submenu_receive_names, or @integer/customizable_submenu_hide_items_receive_names). If an arrays item names can be changed, you will have to do that from code.

## 9. Compare your XML with already included XMLs
If you feel unsure about how your XML should look, just take a look on [already included files](https://github.com/SpiritCroc/Modular-Remote/blob/master/app/src/main/res/values/strings_tcp_commands_pioneer.xml)!

## 10. Share your work
You don't need to have finished with your work when showing it. You can send an email at spiritcroc@gmail.com in order to receive feedback from me on your work (or maybe even an .apk with your work included, so you can test it).
This way, I can help you when you have questions and can try to prevent unnecessary work.
Including new commands takes a little coding effort, but I am willing to do any coding needed to include your commands if you send a correct XML to the email-address given above.
If you are a developer and familiar with GitHub, I probably don't have to explain you how you can push your changes to the project without depending on me too much.
