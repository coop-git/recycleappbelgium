/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.recycleappbelgium.internal;

import static org.openhab.binding.recycleappbelgium.internal.RecycleappBelgiumBindingConstants.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.net.http.HttpRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The {@link RecycleappBelgiumHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Lennert Coopman - Initial contribution
 */
@NonNullByDefault
public class RecycleappBelgiumHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(RecycleappBelgiumHandler.class);
    private @NonNullByDefault({}) RecycleappBelgiumConfiguration config;
    private @Nullable ScheduledFuture<?> refreshTask;

    String httpResponse = "";
    String URL = "";
    JsonParser parser = new JsonParser();
    String Token = "";
    String ZipId = "";
    String StreetId = "";
    String Collection = "";
    String language = "";
    String delimiter = "***";

    public RecycleappBelgiumHandler(Thing thing) {
        super(thing);
    }

    String StartDate = "";
    String EndDate = "";

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // Nothing to do, read only
    }

    @Override
    public void initialize() {
        this.config = getConfigAs(RecycleappBelgiumConfiguration.class);
        updateStatus(ThingStatus.UNKNOWN);
        startAutomaticRefresh();
        updateStatus(ThingStatus.ONLINE);
    }

    private void startAutomaticRefresh() {
        refreshTask = scheduler.scheduleWithFixedDelay(this::refreshProcess, 0, config.refreshInterval, TimeUnit.HOURS);
    }

    private void refreshProcess() {
        logger.info("Start fetching next waste collection...");
        Token = parseToken();
        if (!Token.equals("")) {
            logger.debug("Token: {}", Token);
            ZipId = parsePostalCode();
            StreetId = parseStreet();
            Collection = parseCollection();    
            logger.debug("Zip id: {}", ZipId);
            logger.debug("Street id: {}", StreetId);
            // logger.info("Collections: {}", Collection);
            for (Channel channel : getThing().getChannels()) {
                ChannelUID channelUID = channel.getUID();
                String channelID = channelUID.getId();
                if (channelID.equals(NEXT_COLLECTION)) {
                    if (!Collection.equals("")) {
                        logger.debug("Next Collection: {}", Collection);
                        updateState(channelUID, StringType.valueOf(Collection));
                    }
                }
            }
        } else {
            logger.info("Empty token");
        }
    }

    private String parseCollection() {
        String CollectionDate = "";
        String Fraction = "";
        String PreviousCollectionDate = "";
        ArrayList<String> Collections = new ArrayList<String>();

        String ParsedCollection = getCollectionJSON();
        try {
            JsonElement jsonTree = parser.parse(ParsedCollection);
            JsonObject jsonObject = jsonTree.getAsJsonObject();
            JsonArray itemsArray = jsonObject.getAsJsonArray("items");
            for (JsonElement ia : itemsArray) {
                // Counter++;

                JsonObject itemsObject = ia.getAsJsonObject();
                CollectionDate = itemsObject.get("timestamp").getAsString();
                CollectionDate = CollectionDate.substring(0, 10);
                JsonObject fractionObject = itemsObject.get("fraction").getAsJsonObject();
                JsonObject nameObject = fractionObject.get("name").getAsJsonObject();
                language = config.Language.toLowerCase();
                Collections.add(CollectionDate + "xxx" + nameObject.get(language).getAsString());
            }
            ParsedCollection = "";
            PreviousCollectionDate = "";
            CollectionDate = "";
            for (Integer i = 1; i <= Collections.size(); i++) {
                String[] parts = Collections.get(i - 1).split("xxx");
                CollectionDate = parts[0];
                Fraction = parts[1];
                if (CollectionDate.equals(PreviousCollectionDate)) {
                    ParsedCollection = ParsedCollection + delimiter + Fraction;
                } else {
                    ParsedCollection = ParsedCollection + "(" + CollectionDate + ")" + Fraction;
                }
                PreviousCollectionDate = CollectionDate;
            }
            return ParsedCollection;
        } catch (Exception e) {
            logger.info("{}", e.toString());
            return "";
        }
    }

    private String getCollectionJSON() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        StartDate = sdf.format(cal.getTime());

        cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 6);
        EndDate = sdf.format(cal.getTime());

        try {
            return HttpRequestBuilder
                    .getFrom("https://recycleapp.be/api/app/v1/collections?zipcodeId=" + ZipId + "&streetId=" + StreetId
                            + "&houseNumber=" + config.HouseNumber + "&fromDate=" + StartDate + "&untilDate=" + EndDate
                            + "&size=100")
                    .withHeader("Authorization", Token).withHeader("x-consumer", "recycleapp.be").getContentAsString();
        } catch (IOException e) {
            return "";
        }
    }

    private String parseStreet() {
        String ParsedStreet = getStreetJSON();
        try {
            JsonElement jsonTree = parser.parse(ParsedStreet);
            JsonObject jsonObject = jsonTree.getAsJsonObject();
            JsonArray presetsArray = jsonObject.getAsJsonArray("items");
            for (JsonElement pr : presetsArray) {
                JsonObject presetObject = pr.getAsJsonObject();
                ParsedStreet = presetObject.get("id").getAsString();
            }
            return ParsedStreet;
        } catch (Exception e) {
            return "";
        }
    }

    private String getStreetJSON() {
        try {
            return HttpRequestBuilder
                    .getFrom("https://recycleapp.be/api/app/v1/streets?q=" + config.Street + "&zipcodes=" + ZipId)
                    .withHeader("Authorization", Token).withHeader("x-consumer", "recycleapp.be").getContentAsString();
        } catch (IOException e) {
            return "";
        }
    }

    private String parsePostalCode() {
        String ParsedZIP = getPostalCodeJSON();
        try {
            JsonElement jsonTree = parser.parse(ParsedZIP);
            JsonObject jsonObject = jsonTree.getAsJsonObject();
            JsonArray presetsArray = jsonObject.getAsJsonArray("items");
            for (JsonElement pr : presetsArray) {
                JsonObject presetObject = pr.getAsJsonObject();
                ParsedZIP = presetObject.get("id").getAsString(); // format: zip-unique id
            }
            return ParsedZIP;
        } catch (Exception e) {
            return "";
        }
    }

    private String getPostalCodeJSON() {
        URL = "https://recycleapp.be/api/app/v1/zipcodes?q=" + config.Zip;
        try {
            return HttpRequestBuilder.getFrom(URL).withHeader("Authorization", Token)
                    .withHeader("x-consumer", "recycleapp.be").getContentAsString();
        } catch (IOException e) {
            return "";
        }
    }

    private String parseToken() {
        String ParsedToken = getTokenJSON();
        try {
            JsonElement jsonTree = parser.parse(ParsedToken);
            JsonObject jsonObject = jsonTree.getAsJsonObject();
            ParsedToken = jsonObject.get("accessToken").getAsString();
            return ParsedToken;
        } catch (Exception e) {
            return "";
        }
    }

    private String getTokenJSON() {
        try {
            return HttpRequestBuilder.getFrom("https://www.recycleapp.be/api/app/v1/access-token").withHeader(
                    "x-secret",
                    "8a9pIQlfYpgmJZD15KdK70MCTR2xyD0EAvOmi9HCBfiBUY4n34ytxQmqo3AP2OET6tssYy6R4Be6N2M2GtiX3AcbiNxR8G7pOalN45dXPZ4emKE2c1nimx9B1YFciutJwFZHYHI2Qpzo0E0GCDHkg5")
                    // 2021:
                    // 8a9pIQlfYpgmJZD15KdK70MCTR2xyD0EAvOmi9HCBfiBUY4n34ytxQmqo3AP2OET6tssYy6R4Be6N2M2GtiX3AcbiNxR8G7pOalN45dXPZ4emKE2c1nimx9B1YFciutJwFZHYHI2Qpzo0E0GCDHkg5
                    // 2020:
                    // Qp4KmgmK2We1ydc9Hxso5D6K0frz3a9raj2tqLjWN5n53TnEijmmYz78pKlcma54sjKLKogt6f9WdnNUci6Gbujnz6b34hNbYo4DzyYRZL5yzdJyagFHS15PSi2kPUc4v2yMck81yFKhlk2aWCTe93
                    .withHeader("x-consumer", "recycleapp.be").getContentAsString();
        } catch (IOException e) {
            return "";
        }
    }

    @Override
    public void dispose() {
        refreshTask.cancel(true);
    }
}
