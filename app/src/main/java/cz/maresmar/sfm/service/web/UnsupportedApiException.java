/*
 * SmartFoodMenu - Android application for canteens extendable with plugins
 *
 * Copyright © 2016-2018  Martin Mareš <mmrmartin[at]gmail[dot]com>
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cz.maresmar.sfm.service.web;

/**
 * Exception is thrown when application receives response with unsupported API
 * <p>
 * Class also gives possibility to cary portal message specifying some info. </p>
 */

public class UnsupportedApiException extends Exception {

    private String mServerMessage;

    /**
     * Create new exception
     *
     * @param message       Code error message
     * @param serverMessage Server error message
     */
    public UnsupportedApiException(String message, String serverMessage) {
        super(message);
        this.mServerMessage = serverMessage;
    }

    /**
     * Returns server error message
     */
    public String getServerMessage() {
        return mServerMessage;
    }
}
