/*
 * Copyright (c) 2012-2013. Telecom ParisTech/TSI/MM/GPAC Jean-Claude Dufourd
 * This code was developed with the Coltram project, funded by the French ANR.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * This notice must stay in all subsequent versions of this code.
 */

package org.coltram.nsd.debug;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class OneLiner extends Formatter{
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append(max5(record.getLevel().getName()));
        sb.append(" ");
        String s = record.getSourceClassName();
        int len = s.length();
        if (len > 60) {
            sb.append("...");
            sb.append(s.substring(len-58, len));
        }
        else sb.append(s);
        sb.append(" ");
        sb.append(record.getSourceMethodName());
        sb.append(" ");
        while (sb.length() < 90) sb.append(".");
        sb.append(" ");
        sb.append(record.getMessage());
        sb.append("\n");
        return sb.toString();
    }

    private static String max5(String s) {
        return (s+"     ").substring(0, 5);
    }
}
