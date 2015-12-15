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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class TcpInformation {
    public enum InformationType {
        NONE,
        CONNECTIVITY_CHANGE,// boolean: connected
        RAW,
        // For TcpInformation instances of type CLASSIFIED_RESPONSE, still check type with
        // isClassifiedResponse() to make sure the right values are present
        CLASSIFIED_RESPONSE,
        UPDATE_MENU
    }

    private InformationType type;
    private String stringValue, responseClassifier, rawResponse;
    private int intValue;
    private double doubleValue;
    private boolean booleanValue;
    private boolean stringAvailable = false, intAvailable = false, doubleAvailable = false,
            booleanAvailable = false,classifiedResponse = false;
    public TcpInformation(InformationType type, String stringValue) {
        this.type = type;
        this.stringValue = stringValue;
        stringAvailable = true;
    }
    public TcpInformation(InformationType type, int intValue) {
        this.type = type;
        this.intValue = intValue;
        intAvailable = true;
    }
    public TcpInformation(InformationType type, double doubleValue) {
        this.type = type;
        this.doubleValue = doubleValue;
        doubleAvailable = true;
    }
    public TcpInformation(InformationType type, boolean booleanValue) {
        this.type = type;
        this.booleanValue = booleanValue;
        booleanAvailable = true;
    }
    public TcpInformation(@NonNull String responseClassifier, @Nullable String stringValue,
                          String rawResponse) {
        this.type = InformationType.CLASSIFIED_RESPONSE;
        this.responseClassifier = responseClassifier;
        this.stringValue = stringValue;
        this.rawResponse = rawResponse;
        stringAvailable = stringValue != null;
        classifiedResponse = true;
    }
    public TcpInformation(InformationType type) {
        this.type = type;
    }
    public InformationType getType() {
        return type;
    }
    public String getStringValue() {
        return stringValue;
    }
    public String getResponseClassifier() {
        return responseClassifier;
    }
    public String getRawResponse() {
        return rawResponse;
    }
    public int getIntValue() {
        return intValue;
    }
    public double getDoubleValue() {
        return doubleValue;
    }
    public boolean getBooleanValue() {
        return booleanValue;
    }
    public boolean isStringAvailable() {
        return stringAvailable;
    }
    public boolean isIntAvailable() {
        return intAvailable;
    }
    public boolean isDoubleAvailable() {
        return doubleAvailable;
    }
    public boolean isBooleanAvailable() {
        return booleanAvailable;
    }
    public boolean isClassifiedResponse() {
        return classifiedResponse;
    }
    public void overwriteStringValue(String stringValue) {
        if (stringAvailable) {
            this.stringValue = stringValue;
        }
    }

    @Override
    public String toString() {
        String s = getClass().getName() + " " + type;
        if (isClassifiedResponse()) {
            s += " responseClassifier: " + getResponseClassifier();
            s += " rawResponse: " + getRawResponse();
        }
        if (isStringAvailable()) {
            s += " string: " + getStringValue();
        }
        if (isIntAvailable()) {
            s += " int: " + getIntValue();
        }
        if (isDoubleAvailable()) {
            s += " double: " + getDoubleValue();
        }
        if (isBooleanAvailable()) {
            s += " boolean: " + getBooleanValue();
        }
        return s;
    }
}
