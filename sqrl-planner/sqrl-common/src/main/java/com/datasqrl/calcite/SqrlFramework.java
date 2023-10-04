package com.datasqrl.calcite;

import com.datasqrl.calcite.type.TypeFactory;
import com.datasqrl.canonicalizer.Name;
import com.datasqrl.canonicalizer.NameCanonicalizer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import org.apache.calcite.config.CalciteConnectionConfigImpl;
import org.apache.calcite.jdbc.SqrlSchema;
import org.apache.calcite.rel.hint.HintStrategyTable;
import org.apache.calcite.rel.metadata.RelMetadataProvider;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;

import java.util.Properties;

@Getter
public class SqrlFramework {

  private final CatalogReader catalogReader;
  private final TypeFactory typeFactory;
  private final OperatorTable sqrlOperatorTable;
  private final SqrlSchema schema;
  private final HintStrategyTable hintStrategyTable;
  private final NameCanonicalizer nameCanonicalizer;
  private QueryPlanner queryPlanner;
  private RelMetadataProvider relMetadataProvider;
  private AtomicInteger uniqueMacroInt = new AtomicInteger(0);
  private AtomicInteger uniqueTableInt = new AtomicInteger(0);
  private AtomicInteger uniqueColumnInt = new AtomicInteger(0);
  private Map<Name,AtomicInteger> tableNameShadowing = new HashMap<Name,AtomicInteger>();

  public SqrlFramework() {
    this(null, HintStrategyTable.builder().build(), NameCanonicalizer.SYSTEM);
  }

  public SqrlFramework(RelMetadataProvider relMetadataProvider, HintStrategyTable hintStrategyTable,
      NameCanonicalizer nameCanonicalizer) {
    this.hintStrategyTable = hintStrategyTable;
    this.schema = new SqrlSchema(this);
    this.relMetadataProvider = relMetadataProvider;

    this.typeFactory = new TypeFactory();

    Properties info = new Properties();
    info.setProperty("caseSensitive", "false");
    CalciteConnectionConfigImpl config = new CalciteConnectionConfigImpl(info);

    this.nameCanonicalizer = nameCanonicalizer;
    SqrlNameMatcher nameMatcher = new SqrlNameMatcher(nameCanonicalizer);
    this.catalogReader = new CatalogReader(schema, typeFactory, config,
        nameMatcher);
    this.sqrlOperatorTable = new OperatorTable(nameCanonicalizer, catalogReader, SqlStdOperatorTable.instance());
    this.queryPlanner = resetPlanner();
  }

  public QueryPlanner resetPlanner() {
    this.queryPlanner = new QueryPlanner(this);
    return this.queryPlanner;
  }

  public int getUniqueTableId(Name tableName) {
//    AtomicInteger counter = tableNameShadowing.get(tableName);
//    if (counter==null) {
//      counter = new AtomicInteger(0);
//      tableNameShadowing.put(tableName,counter);
//    }
//    return counter.incrementAndGet();
    return uniqueTableInt.incrementAndGet();
  }
}