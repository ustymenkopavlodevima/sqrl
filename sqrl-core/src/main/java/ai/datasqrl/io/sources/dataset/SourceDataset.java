package ai.datasqrl.io.sources.dataset;

import ai.datasqrl.config.error.ErrorPrefix;
import ai.datasqrl.io.sources.DataSource;
import ai.datasqrl.io.sources.SourceTableConfiguration;
import ai.datasqrl.parse.tree.name.Name;
import ai.datasqrl.parse.tree.name.NameCanonicalizer;
import ai.datasqrl.config.error.ErrorCollector;
import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import lombok.NonNull;
import lombok.Value;

/**
 * A {@link SourceDataset} defines a group of {@link SourceTable} that comprise one semantically coherent dataset.
 *
 * The role of a {@link SourceDataset} is to register all of its {@link SourceTable} within an execution environment.
 *
 */
public class SourceDataset {

    final DatasetRegistry registry;
    private DataSource source;
    private final Map<Name,SourceTable> tables = new HashMap<>();

    public SourceDataset(DatasetRegistry registry, DataSource source) {
        this.registry = registry;
        this.source = source;
        initializeTables();
    }

    void initializeTables() {
        //Read existing tables within dataset from store
        for (SourceTableConfiguration tblConfig : registry.persistence.getTables(getName())) {
            ErrorCollector errors = ErrorCollector.fromPrefix(ErrorPrefix.INITIALIZE);
            SourceTable table = initiateTable(tblConfig, errors);
            registry.tableMonitor.startTableMonitoring(table);
            errors.log();
        }
    }

    synchronized SourceTable initiateTable(SourceTableConfiguration tableConfig,
                                           ErrorCollector errors) {
        Name tblName = getCanonicalizer().name(tableConfig.getName());
        Preconditions.checkArgument(!tables.containsKey(tblName));
        SourceTable tbl = new SourceTable(this, tblName, tableConfig);
        tables.put(tblName,tbl);
        return tbl;
    }

    public synchronized SourceTable addTable(SourceTableConfiguration tableConfig,
                               ErrorCollector errors) {
        if (!tableConfig.validateAndInitialize(this.getSource(),errors)) {
            return null; //validation failed
        }
        Name tblName = getCanonicalizer().name(tableConfig.getName());
        SourceTable table = tables.get(tblName);
        if (table == null) {
            //New table
            table = initiateTable(tableConfig, errors);
        } else {
            errors.fatal("Table [%s] already exists. To update table, delete and re-add", tblName.getDisplay());
            return null;
        }
        registry.persistence.putTable(getName(), tblName, tableConfig);
        registry.tableMonitor.startTableMonitoring(table);
        return table;
    }

    public synchronized SourceTable removeTable(@NonNull Name tblName) {
        SourceTable table = tables.remove(tblName);
        if (table==null) return null;
        registry.tableMonitor.stopTableMonitoring(table);
        registry.persistence.removeTable(getName(), table.getName());
        registry.persistence.removeTableStatistics(getName(), table.getName());
        return table;
    }

    public SourceTable removeTable(@NonNull String name) {
        return Name.getIfValidName(name, getCanonicalizer(), this::removeTable);
    }

    public DataSource getSource() {
        return source;
    }

    /**
     * Returns all tables currently in the dataset
     * @return
     */
    public Collection<SourceTable> getTables() {
        return tables.values();
    }

    /**
     * Returns {@link SourceTable} of the given name in this dataset or NULL if such does not exist
     * @param name
     * @return
     */
    public SourceTable getTable(Name name) {
        return tables.get(name);
    }

    public SourceTable getTable(String name) {
        return Name.getIfValidName(name,getCanonicalizer(),this::getTable);
    }

    public boolean containsTable(String name) {
        return getTable(name)!=null;
    }

    public Name getName() {
        return source.getName();
    }

    public NameCanonicalizer getCanonicalizer() {
        return source.getCanonicalizer();
    }

    public Digest getDigest() {
        return new Digest(getName(),getCanonicalizer());
    }

    @Value
    public static class Digest implements Serializable {

        private final Name name;
        private final NameCanonicalizer canonicalizer;

    }

}