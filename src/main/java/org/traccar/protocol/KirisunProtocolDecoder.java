/*
 * Copyright 2012 - 2021 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import java.net.SocketAddress;
import java.util.regex.Pattern;

import org.traccar.BaseProtocolDecoder;
import org.traccar.Protocol;
import org.traccar.helper.DateBuilder;
import org.traccar.helper.Parser;
import org.traccar.helper.PatternBuilder;
import org.traccar.model.Position;
import org.traccar.session.DeviceSession;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;

public class KirisunProtocolDecoder extends BaseProtocolDecoder {

    private int photoPackets = 0;
    private ByteBuf photo;

    public KirisunProtocolDecoder(Protocol protocol) {
        super(protocol);
    }

private static final Pattern PATTERN = new PatternBuilder()
        .expression("([^|]+)")               // id (capture everything until |)
        .text("|")
        .expression("[^|]*")              // name/description
        .text("|")
        .number("(d+.d+)")                   // latitude
        .text("|")
        .number("(d+.d+)")                   // longitude
        .text("|")
        .number("(d+)")                      // LBS GPS:1, WIFI:2
        .text("|")
        .number("(d+)")                      // altitude
        .text("|")
        .number("(d+.d+)")                   // speed
        .text("|")
        .number("(d+)")                      // direction
        .text("|")
        .number("(dd)(dd)(dd)(dd)(dd)(dd)")  // datetime (Format:yyMMddHHmmss)
        .text("|")
        .number("(d+)")                      // SOS Flag
        .compile();

    private Position decodeRegular(Channel channel, SocketAddress remoteAddress, String sentence) {

        Parser parser = new Parser(PATTERN, sentence);
        if (!parser.matches()) {
            return null;
        }

        String imei = parser.next();
        DeviceSession deviceSession = getDeviceSession(channel, remoteAddress, imei);
        if (deviceSession == null) {
            return null;
        }

        Position position = new Position(getProtocolName());
        position.setDeviceId(deviceSession.getDeviceId());

        position.setLatitude(parser.nextDouble());
        position.setLongitude(parser.nextDouble());

        parser.next();

        position.setAltitude(parser.nextInt(0));
        position.setSpeed(parser.nextDouble(0));
        position.setCourse(parser.nextInt(0));

      // convert string with format yyMMddHHmmss to datebuilder
        DateBuilder dateBuilder = new DateBuilder()
                .setDate(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0))  // yy, MM, dd
                .setTime(parser.nextInt(0), parser.nextInt(0), parser.nextInt(0)); // HH, mm, ss

        position.setTime(dateBuilder.getDate());
        position.setValid(true);

        return position;
    }

    @Override
    protected Object decode(
            Channel channel, SocketAddress remoteAddress, Object msg) throws Exception {

        String sentence = (String) msg;

        return decodeRegular(channel, remoteAddress, sentence);
    }

}
