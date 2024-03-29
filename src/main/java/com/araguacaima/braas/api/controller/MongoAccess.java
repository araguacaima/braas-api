package com.araguacaima.braas.api.controller;

import com.araguacaima.braas.api.model.BraasDrools;
import com.araguacaima.braas.core.google.model.Config;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.util.*;

import static com.araguacaima.braas.api.common.Commons.jsonUtils;
import static com.araguacaima.braas.core.Constants.environment;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoAccess {

    private static MongoDatabase database;
    private static CodecRegistry pojoCodecRegistry;

    static {
        String mongodb_uri = environment.get("MONGODB_URI");
        MongoClientURI mongoClientURI = new MongoClientURI(mongodb_uri);
        String databaseName = mongoClientURI.getDatabase();
        MongoClient mongoClient = MongoClients.create(mongodb_uri + "?retryWrites=false");

        // create codec registry for POJOs
        pojoCodecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));

        // get handle to "mydb" database
        database = mongoClient.getDatabase(databaseName).withCodecRegistry(pojoCodecRegistry);
    }

    public static <T> Collection<T> getAll(Class<T> clazz, String collectionName) throws IOException {
        MongoCollection<T> collection = database.getCollection(collectionName, clazz);
        Set<T> result = new LinkedHashSet<>();
        if (collection.countDocuments() == 0) {
            return null;
        } else {
            for (T document : collection.find()) {
                result.add(document);
            }
            return result;
        }
    }

    public static Map getAllAsMap(String collectionName) throws IOException {
        MongoCollection collection = database.getCollection(collectionName);
        Map<String, ?> result = new LinkedHashMap<>();
        if (collection.countDocuments() == 0) {
            return null;
        } else {
            for (Object document : collection.find()) {
                result.putAll(jsonUtils.fromJSON(document.toString(), Map.class));
            }
            return result;
        }
    }

    private static <T> T getById(Class<T> clazz, String collectionName, String id, String braasId) {
        MongoCollection<T> collection = database.getCollection(collectionName, clazz);
        return collection.find(eq(braasId, id)).first();
    }

    private static <T> T store(Class<T> clazz, String collectionName, T object) {
        MongoCollection<T> collection = database.getCollection(collectionName, clazz);
        collection.insertOne(object);
        return object;
    }

    private static <T> boolean update(Class<T> clazz, String collectionName, String id, Bson update, String fieldName) {
        MongoCollection<T> collection = database.getCollection(collectionName, clazz);
        UpdateResult result = collection.updateOne(eq(fieldName, id), update);
        return result.wasAcknowledged();
    }


    public static BraasDrools getBraasDroolsById(String collectionName, String id) {
        return getById(BraasDrools.class, collectionName, id, "braasId");
    }

    public static BraasDrools storeBraasDrools(String collectionName, BraasDrools object) {
        return store(BraasDrools.class, collectionName, object);
    }

    public static BraasDrools updateBraasDrools(String collectionName, BraasDrools braasDrools) {
        Bson update = combine(set("schemas", braasDrools.getSchemas()), set("spreadsheet", braasDrools.getSpreadsheet()));
        if (update(BraasDrools.class, collectionName, braasDrools.getBraasId(), update, "braasId")) {
            return braasDrools;
        }
        return null;
    }

    public static Config storeConfig(String collectionName, Config object) {
        return store(Config.class, collectionName, object);
    }

    public static Collection<Config> getConfigs() {
        return null;
    }
}
