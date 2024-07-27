/*
 * Copyright 2006-2023 www.anyline.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package org.anyline.proxy;

import org.anyline.cache.CacheProvider;
import org.anyline.data.param.ConfigStore;
import org.anyline.data.runtime.DataRuntime;
import org.anyline.entity.OriginRow;
import org.anyline.metadata.*;
import org.anyline.metadata.graph.EdgeTable;
import org.anyline.metadata.graph.VertexTable;
import org.anyline.util.ConfigTable;
import org.anyline.util.encrypt.MD5Util;

import java.util.LinkedHashMap;
import java.util.List;

public class CacheProxy {

    private static final ThreadLocal<OriginRow> thread_caches = new ThreadLocal<>();
    private static final ThreadLocal<OriginRow> thread_names = new ThreadLocal<>();
    private static OriginRow application_caches = new OriginRow();
    private static OriginRow application_names = new OriginRow();
    public static CacheProvider provider;
    public CacheProxy() {}
    public static void init(CacheProvider provider) {
        CacheProxy.provider = provider;
    }
    private static OriginRow caches(){
        OriginRow result = new OriginRow();
        if(ConfigTable.METADATA_CACHE_SCOPE == 1){
            result = thread_caches.get();
            if(null == result){
                result = new OriginRow();
                thread_caches.set(result);
            }
        }else if(ConfigTable.METADATA_CACHE_SCOPE == 9){
            if(application_caches.isExpire(ConfigTable.METADATA_CACHE_SECOND*1000)){
                application_caches = new OriginRow();
            }
            result = application_caches;
        }
        return result;
    }
    private static OriginRow names(){
        OriginRow result = new OriginRow();
        if(ConfigTable.METADATA_CACHE_SCOPE == 1){
            result = thread_names.get();
            if(null == result){
                result = new OriginRow();
                thread_names.set(result);
            }
        }else if(ConfigTable.METADATA_CACHE_SCOPE == 9){
            if(application_names.isExpire(ConfigTable.METADATA_CACHE_SECOND*1000)){
                application_names = new OriginRow();
            }
            result = application_names;
        }
        return result;
    }
/*

    private static Map<String, DataRow> cache_columns = new HashMap<>();
    private static Map<String, Map<String, String>>  cache_names = new HashMap<>();
    private static Map<String, DataRow> cache_table_maps = new HashMap<>();
    private static Map<String, DataRow> cache_view_maps = new HashMap<>();
*/


    public static String key(DataRuntime runtime, String flag, boolean greedy, Catalog catalog, Schema schema, String pattern, int types, ConfigStore configs){
        StringBuilder key = new StringBuilder();
        key.append(runtime.datasource()).append("_").append(flag).append("_").append(greedy).append("_");
        if(null != catalog){
            key.append(catalog.getName());
        }
        key.append("_");
        if(null != schema){
            key.append(schema.getName());
        }
        key.append("_").append(pattern).append("_").append(types);
        if(null != configs){
            key.append(MD5Util.crypto(configs.json()));;
        }
        return key.toString().toUpperCase();
    }
    public static String key(DataRuntime runtime, String flag, boolean greedy, Table table){
        StringBuilder key = new StringBuilder();
        key.append(runtime.datasource()).append("_").append(flag).append("_").append(greedy).append("_");
        String name = table.getName();
        String catalog = table.getCatalogName();
        String schema = table.getSchemaName();
        if(null != catalog){
            key.append(catalog);
        }
        key.append("_");
        if(null != schema){
            key.append(schema);
        }
        key.append("_").append(name);
        return key.toString().toUpperCase();
    }
    public static String key(DataRuntime runtime, String flag, boolean greedy, Catalog catalog, Schema schema, String pattern){
        StringBuilder key = new StringBuilder();
        key.append(runtime.datasource()).append("_").append(flag).append("_").append(greedy).append("_");
        if(null != catalog){
            key.append(catalog.getName());
        }
        key.append("_");
        if(null != schema){
            key.append(schema.getName());
        }
        key.append("_").append(pattern);
        return key.toString().toUpperCase();
    }
    public static String name(String key) {
        return names().getString(key.toUpperCase());
    }
    public static void name(String key, String origin) {
        names().put(key.toUpperCase(), origin);
    }
    public static  <T extends Table> List<T> tables(String cache){
        List<T> tables = (List<T>)caches().get(cache);
        return tables;
    }
    public static  <T extends MasterTable> List<T> masterTables(String cache){
        List<T> tables = (List<T>)caches().get(cache);
        return tables;
    }
    public static  <T extends EdgeTable> List<T> edgeTables(String cache){
        List<T> tables = (List<T>)caches().get(cache);
        return tables;
    }
    public static  <T extends VertexTable> List<T> vertexTables(String cache){
        List<T> tables = (List<T>)caches().get(cache);
        return tables;
    }
    public static  <T extends Table> void tables(String cache, List<T> tables){
        caches().put(cache, tables);
    }

    public static  <T extends View> List<T> views(String cache){
        List<T> view = (List<T>)caches().get(cache);
        return view;
    }
    public static  <T extends View> void views(String cache, List<T> view){
        caches().put(cache, view);
    }

    public static  void cache(String cache, Object value){
        caches().put(cache, value);
    }

/*
    public static <T extends Table> void name(DriverAdapter adapter, List<T> tables) {
        if(null != tables) {
            for (Table table : tables) {
                name(adapter, table.getCatalog(), table.getSchema(), table.getName(), table.getName());
            }
        }
    }*/
/*
    private static String key(DriverAdapter adapter, Catalog catalog, Schema schema) {
        String key = null;
        String catalog_name = null;
        String schema_name = null;
        if(null != catalog && adapter.supportCatalog()) {
            catalog_name = catalog.getName();
        }
        if(null != schema && adapter.supportSchema()) {
            schema_name = schema.getName();
        }
        if(null != catalog_name) {
            key = catalog_name;
        }
        if(null != schema_name) {
            if(null != key) {
                key += "_" + schema_name;
            }else{
                key = schema_name;
            }
        }
        if(null != key) {
            key = key.toUpperCase();
        }else{
            key = "ALL";
        }
        return key;
    }
    private static String key(DriverAdapter adapter, Catalog catalog, Schema schema, Table table) {
        String table_name = null;
        if(null != table) {
            table_name = table.getName();
        }
        String key = key(adapter, catalog, schema);
        if(null != table_name) {
            if(null != key) {
                key += ":" + table_name;
            }else{
                key = table_name;
            }
        }
        if(null != key) {
            key = key.toUpperCase();
        }
        return key;
    }*//*
    public static void name(DriverAdapter adapter, Catalog catalog, Schema schema, String name, String origin) {
        String group_key = key(adapter, catalog, schema);
        Map<String, String> maps = cache_names.get(group_key);
        if(null == maps) {
            maps = new HashMap<>();
            cache_names.put(group_key, maps);
        }
        String name_key = (group_key + ":" + name).toUpperCase();
        maps.put(name_key, origin);
    }
    public static Map<String, String> names(DriverAdapter adapter, Catalog catalog, Schema schema) {
        return cache_names.get(key(adapter, catalog, schema));
    }
    public static String name(DriverAdapter adapter, boolean greedy, Catalog catalog, Schema schema, String name) {
        if(null == name) {
            return null;
        }
        String group_key = key(adapter, catalog, schema);
        Map<String, String> maps = cache_names.get(group_key);
        if(null != maps) {
            String name_key = (group_key + ":" + name).toUpperCase();
            String origin = maps.get(name_key);
            if(null != origin) {
                return origin;
            }
        }
        if(greedy) {
            for (Map<String, String> names : cache_names.values()) {
                for(String item:names.keySet()) {
                    if(item.endsWith((":"+name).toUpperCase())) {
                        return names.get(item);
                    }
                }
            }
        }
        return null;
    }*/
    /*public static String datasource(String datasource) {
        if(null == datasource || "common".equalsIgnoreCase(datasource)) {
            //datasource = DataSourceHolder.curDataSource();
        }
        if(null == datasource) {
            datasource = "default";
        }
        return datasource.toUpperCase();
    }
*//*
    public static String tableName(String datasource, String name) {
        DataRow row = cache_table_maps.get(datasource(datasource));
        if(null != row) {
            return row.getString(name);
        }
        return name;
    }
    public static String viewName(String datasource, String name) {
        DataRow row = cache_view_maps.get(datasource(datasource));
        if(null != row) {
            return row.getString(name);
        }
        return name;
    }
    public static void setTableMaps(String datasource, DataRow maps) {
        cache_table_maps.put(datasource(datasource), maps);
    }
    public static void setViewMaps(String datasource, DataRow maps) {
        cache_view_maps.put(datasource(datasource), maps);
    }*/


   /* public static DataRow getTableMaps(String datasource) {
        DataRow row = cache_table_maps.get(datasource(datasource));
        if(null == row) {
            row = new DataRow();
            cache_table_maps.put(datasource(datasource), row);
        }
        return row;
    }*/

  /*  public static DataRow getViewMaps(String datasource) {
        DataRow row = cache_view_maps.get(datasource(datasource));
        if(null == row) {
            row = new DataRow();
            cache_view_maps.put(datasource(datasource), row);
        }
        return row;
    }*/


    public static <T extends Column> LinkedHashMap<String, T> columns(String key) {
        return (LinkedHashMap<String, T>) caches().get(key);
    }
    public static <T extends Tag> LinkedHashMap<String, T> tags(String key) {
        return (LinkedHashMap<String, T>) caches().get(key);
    }


   /* public static <T extends Column> void columns(DriverAdapter adapter, String datasource, Table table, LinkedHashMap<String, T> columns) {
        if(null == table) {
            return;
        }
        String cache = ConfigTable.getString("METADATA_CACHE_KEY");
        String key = datasource(datasource) + "_COLUMNS_" + key(adapter, table.getCatalog(), table.getSchema(), table);
        key = key.toUpperCase();
        if(null != provider && BasicUtil.isNotEmpty(cache) && !ConfigTable.IS_CACHE_DISABLED) {
            provider.put(cache, key, columns);
        }else{
            DataRow static_cache = new DataRow();
            static_cache.put("keys", columns);
            cache_columns.put(key, static_cache);
        }
    } */

    public static void clear() {/*
        if(null != provider && !ConfigTable.IS_CACHE_DISABLED) {
            String cache = ConfigTable.METADATA_CACHE_KEY;
            if(BasicUtil.isNotEmpty(cache)) {
                provider.clear(cache);
            }
        }else{
            cache_columns.clear();
        }*/

        caches().clear();
        names().clear();
    }

}
