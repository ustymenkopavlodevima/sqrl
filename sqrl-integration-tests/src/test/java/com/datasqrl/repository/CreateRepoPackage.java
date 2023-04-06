package com.datasqrl.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.datasqrl.AbstractPhysicalSQRLIT;
import com.datasqrl.IntegrationTestSettings;
import com.datasqrl.packager.Publisher;
import com.datasqrl.packager.repository.ValidatePublication;
import com.datasqrl.util.ScriptBuilder;
import com.datasqrl.util.SnapshotTest;
import com.datasqrl.util.data.Sensors;
import com.datasqrl.util.data.UseCaseExample;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Used to create a new publication in the DataSQRL repository.
 * Set packagePath to the package to publish and output path to the repository data directory.
 *
 * Then commit the files to the repository and upload the zip file to the S3 bucket.
 */
public class CreateRepoPackage extends AbstractPhysicalSQRLIT {

  private final Path output = Path.of("../../sqrl-repository/repodata");

  @BeforeEach
  public void setup(TestInfo testInfo) throws IOException {
    this.snapshot = SnapshotTest.Snapshot.of(getClass(), testInfo);
    this.closeSnapshotOnValidate = false;
  }

  @Test
  @Disabled
  public void createPublication() {
    UseCaseExample example = Sensors.INSTANCE;
    createPublication(example.getRootPackageDirectory().resolve(UseCaseExample.DATA_PACKAGE+"-repo"),
        example.getTables());
  }

  public void createPublication(Path packagePath, Set<String> tables) {
    //First, we check that the package can actually be read
    testDataPackage(packagePath, tables);
    //Second, we publish
    ValidatePublication validate = new ValidatePublication("datasqrl", output, errors);
    Publisher publisher = new Publisher(errors);
    assertNotNull(publisher.publish(packagePath, validate));
    assertFalse(errors.isFatal());
  }

  public void testDataPackage(Path packagePath, Set<String> tables) {
    Path root = packagePath.getParent();
    initialize(IntegrationTestSettings.getFlinkWithDB(), root);

    ScriptBuilder script = new ScriptBuilder();
    script.add("IMPORT " + packagePath.getFileName().toString() + ".*;");
    List<String> resultTables = tables.stream().map(tbl -> {
      String resultTbl = tbl + "CountAll";
      script.add(resultTbl+" := SELECT count(1) AS num FROM "+tbl+";");
      return resultTbl;
    }).collect(Collectors.toList());


    validateTables(script.getScript(), resultTables, Set.of(), Set.of());
    String content = snapshot.getContent();
    resultTables.stream().forEach(tbl -> assertTrue(content.contains(tbl),tbl));
    System.out.println(content);
  }

}
