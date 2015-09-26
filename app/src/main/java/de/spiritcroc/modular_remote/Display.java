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

public interface Display {
    enum ViewMode {
        TCP_DISPLAY,
        STATIC_TEXT,
        CLOCK
    }

    abstract class ModeSettings {
        public ViewMode mode;

        public abstract String getRecreationKey();

        public abstract ModeSettings copy();

        public static ModeSettings recoverFromRecreationKey(String key) throws Exception{
            String[] args = Util.split(key, Util.RK_ARRAY_ATTRIBUTE_SEPARATOR, 0);
            switch (ViewMode.valueOf(args[0])) {
                case TCP_DISPLAY:
                    return TcpDisplaySettings.recoverFromRecreationKey(args);
                case STATIC_TEXT:
                    return StaticTextSettings.recoverFromRecreationKey(args);
                case CLOCK:
                    return ClockSettings.recoverFromRecreationKey(args);
                default:
                    throw new Exception("Unknown viewMode " + args[0]);
            }
        }
    }
    class TcpDisplaySettings extends ModeSettings {
        public String ip;
        public TcpConnectionManager.ReceiverType receiverType;
        public String informationType;
        public TcpDisplaySettings(String ip, TcpConnectionManager.ReceiverType receiverType,
                                  String informationType) {
            mode = ViewMode.TCP_DISPLAY;
            this.ip = ip;
            this.receiverType = receiverType;
            this.informationType = informationType;
        }
        @Override
        public String getRecreationKey() {
            return Util.fixRecreationKey(ViewMode.TCP_DISPLAY + Util.RK_ARRAY_ATTRIBUTE_SEPARATOR +
                            ip + Util.RK_ARRAY_ATTRIBUTE_SEPARATOR +
                            receiverType + Util.RK_ARRAY_ATTRIBUTE_SEPARATOR +
                            informationType + Util.RK_ARRAY_ATTRIBUTE_SEPARATOR,
                            Util.RK_ARRAY_ATTRIBUTE_SEPARATOR);
        }
        public static TcpDisplaySettings recoverFromRecreationKey(String[] args) throws Exception {
            String ip = args[1];
            TcpConnectionManager.ReceiverType receiverType =
                    TcpConnectionManager.ReceiverType.valueOf(args[2]);
            String informationType = args[3];
            return new TcpDisplaySettings(ip, receiverType, informationType);
        }
        @Override
        public ModeSettings copy() {
            return new TcpDisplaySettings(ip, receiverType, informationType);
        }
    }
    class StaticTextSettings extends ModeSettings {
        public String text;
        public StaticTextSettings(String text) {
            mode = ViewMode.STATIC_TEXT;
            this.text = text;
        }
        @Override
        public String getRecreationKey() {
            return Util.fixRecreationKey(ViewMode.STATIC_TEXT + Util.RK_ARRAY_ATTRIBUTE_SEPARATOR +
                    text + Util.RK_ARRAY_ATTRIBUTE_SEPARATOR, Util.RK_ARRAY_ATTRIBUTE_SEPARATOR);
        }
        public static StaticTextSettings recoverFromRecreationKey(String[] args) throws Exception {
            String text = args[1];
            return new StaticTextSettings(text);
        }
        @Override
        public ModeSettings copy() {
            return new StaticTextSettings(text);
        }
    }
    class ClockSettings extends ModeSettings {
        public ClockSettings() {
            mode = ViewMode.CLOCK;
        }
        @Override
        public String getRecreationKey() {
            return Util.fixRecreationKey(ViewMode.CLOCK + Util.RK_ARRAY_ATTRIBUTE_SEPARATOR,
                    Util.RK_ARRAY_ATTRIBUTE_SEPARATOR);
        }
        public static ClockSettings recoverFromRecreationKey(String[] args) {
            return new ClockSettings();
        }
        @Override
        public ModeSettings copy() {
            return new ClockSettings();
        }
    }

    ModeSettings getModeSettings();
}
