package ai.dataeng.sqml.ingest;

import ai.dataeng.sqml.db.keyvalue.HierarchyKeyValueStore;
import ai.dataeng.sqml.flink.EnvironmentFactory;
import ai.dataeng.sqml.schema2.name.Name;
import ai.dataeng.sqml.ingest.stats.SourceTableStatistics;
import ai.dataeng.sqml.ingest.source.SourceDataset;
import ai.dataeng.sqml.ingest.source.SourceTable;
import ai.dataeng.sqml.ingest.source.SourceTableQualifiedName;
import com.google.common.base.Preconditions;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

public class DataSourceRegistry implements DatasetLookup {

    private HierarchyKeyValueStore.Factory storeFactory;
    private HierarchyKeyValueStore store;

    private Map<Name, SourceDataset> sourceDatasets;
    private DataSourceMonitor monitor;

    public DataSourceRegistry(HierarchyKeyValueStore.Factory storeFactory) {
        this.storeFactory = storeFactory;
        this.store = storeFactory.open();
        this.sourceDatasets = new HashMap<>();
    }

    public void addDataset(@NonNull SourceDataset dataset) {
        Preconditions.checkArgument(!sourceDatasets.containsKey(dataset.getName()),"Dataset with given name already exists: %s", dataset.getName());
        sourceDatasets.put(dataset.getName(),dataset);
    }

    public synchronized void monitorDatasets(EnvironmentFactory envProvider) {
        Preconditions.checkArgument(monitor==null,"Monitor is already running");
        monitor = new DataSourceMonitor(envProvider, storeFactory);
        for (SourceDataset dataset : sourceDatasets.values()) dataset.addSourceTableListener(monitor);
    }

    @Override
    public SourceDataset getDataset(@NonNull Name name) {
        return sourceDatasets.get(name);
    }

    @Override
    public SourceTableStatistics getTableStatistics(@NonNull SourceTable table) {
        SourceTableStatistics acc = store.get(SourceTableStatistics.class,
                table.getQualifiedName().toString(), DataSourceMonitor.STATS_KEY);
        if (acc == null) acc = new SourceTableStatistics();
        return acc;
    }

}