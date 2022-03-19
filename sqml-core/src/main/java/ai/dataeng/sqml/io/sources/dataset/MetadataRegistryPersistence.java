package ai.dataeng.sqml.io.sources.dataset;

import ai.dataeng.sqml.config.metadata.MetadataStore;
import ai.dataeng.sqml.config.provider.DatasetRegistryPersistenceProvider;
import ai.dataeng.sqml.io.sources.stats.SourceTableStatistics;
import ai.dataeng.sqml.io.sources.DataSourceConfiguration;
import ai.dataeng.sqml.io.sources.SourceTableConfiguration;
import ai.dataeng.sqml.tree.name.Name;
import ai.dataeng.sqml.type.basic.ProcessMessage;
import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
public class MetadataRegistryPersistence implements DatasetRegistryPersistence {

    public static final String STORE_TABLE_STATS_KEY = "stats";
    public static final String STORE_DATASET_KEY = "datasets";
    public static final String STORE_SOURCE_CONFIG_KEY = "source";
    public static final String STORE_TABLE_KEY = "tables";
    public static final String STORE_TABLE_CONFIG_KEY = "config";

    private final MetadataStore store;


    @Override
    public Set<DataSourceConfiguration> getDatasets() {
        return store.getSubKeys(STORE_DATASET_KEY).stream().map(dsName -> {
            DataSourceConfiguration config = store.get(DataSourceConfiguration.class,STORE_DATASET_KEY,dsName,STORE_SOURCE_CONFIG_KEY);
            Preconditions.checkArgument(config!=null,
                    "Persistence of configuration failed.");
            return config;
        }).collect(Collectors.toSet());
    }

    static String name2Key(Name name) {
        return name.getCanonical();
    }

    @Override
    public void putDataset(Name dataset, DataSourceConfiguration datasource) {
        store.put(datasource,STORE_DATASET_KEY,name2Key(dataset),STORE_SOURCE_CONFIG_KEY);
    }

    @Override
    public Set<SourceTableConfiguration> getTables(Name dataset) {
        return store.getSubKeys(STORE_DATASET_KEY,name2Key(dataset),STORE_TABLE_KEY).stream().map(tbName -> {
            SourceTableConfiguration config = store.get(SourceTableConfiguration.class,
                    STORE_DATASET_KEY,name2Key(dataset),STORE_TABLE_KEY,tbName,STORE_TABLE_CONFIG_KEY);
            Preconditions.checkArgument(config!=null,"Persistence of configuration failed.");
            Preconditions.checkArgument(name2Key(config.getTableName()).equals(tbName));
            return config;
        }).collect(Collectors.toSet());
    }

    @Override
    public void putTable(Name dataset, SourceTableConfiguration table) {
        store.put(table,STORE_DATASET_KEY,name2Key(dataset),STORE_TABLE_KEY,name2Key(table.getTableName()),STORE_TABLE_CONFIG_KEY);
    }

    @Override
    public SourceTableStatistics getTableStatistics(Name datasetName, Name tableName) {
        return store.get(SourceTableStatistics.class,
                STORE_DATASET_KEY,name2Key(datasetName),STORE_TABLE_KEY,name2Key(tableName),STORE_TABLE_STATS_KEY);
    }

    @Override
    public void putTableStatistics(Name datasetName, Name tableName, SourceTableStatistics stats) {
        store.put(stats,
                STORE_DATASET_KEY,name2Key(datasetName),STORE_TABLE_KEY,name2Key(tableName),STORE_TABLE_STATS_KEY);
    }

    public static class Provider implements DatasetRegistryPersistenceProvider {

        @Override
        public DatasetRegistryPersistence createRegistryPersistence(MetadataStore metaStore) {
            return new MetadataRegistryPersistence(metaStore);
        }
    }
}