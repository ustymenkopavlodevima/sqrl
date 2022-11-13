package ai.datasqrl.plan.calcite;

import ai.datasqrl.plan.calcite.rules.DAGExpansionRule;
import ai.datasqrl.plan.calcite.rules.SQRLPrograms;
import lombok.Value;
import org.apache.calcite.adapter.enumerable.EnumerableConvention;
import org.apache.calcite.plan.RelTrait;
import org.apache.calcite.rel.metadata.DefaultRelMetadataProvider;
import org.apache.calcite.rel.rules.CoreRules;
import org.apache.calcite.tools.Program;
import org.apache.calcite.tools.Programs;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * An {@link OptimizationStage}
 */
@Value
public class OptimizationStage {

    private static final List<OptimizationStage> ALL_STAGES = new ArrayList<>();

    private final int index;
    private final String name;
    private final Program program;
    private final Optional<RelTrait> trait;

    public OptimizationStage(String name, Program program, Optional<RelTrait> trait) {
        this.name = name;
        this.program = program;
        this.trait = trait;
        synchronized (ALL_STAGES) {
            this.index = ALL_STAGES.size();
            ALL_STAGES.add(index,this);
        }
    }

    public static List<Program> getAllPrograms() {
        return ALL_STAGES.stream().map(OptimizationStage::getProgram).collect(Collectors.toList());
    }

    /*
    ====== DEFINITION OF ACTUAL STAGES
     */

    public static final OptimizationStage PUSH_FILTER_INTO_JOIN = new OptimizationStage("PushDownProjections",
            Programs.hep(List.of(
                    CoreRules.FILTER_INTO_JOIN
                    ), false, DefaultRelMetadataProvider.INSTANCE),
            Optional.empty());

    public static final OptimizationStage READ_DAG_STITCHING = new OptimizationStage("ReadDAGExpansion",
            Programs.hep(List.of(new DAGExpansionRule.ReadOnly()),
                    false, DefaultRelMetadataProvider.INSTANCE), Optional.empty());

    public static final OptimizationStage WRITE_DAG_STITCHING = new OptimizationStage("WriteDAGExpansion",
            Programs.hep(List.of(new DAGExpansionRule.WriteOnly()), false, DefaultRelMetadataProvider.INSTANCE),
            Optional.empty());

    public static final OptimizationStage READ2WRITE_STITCHING = new OptimizationStage("Read2WriteAdjustment",
            Programs.hep(List.of(new DAGExpansionRule.Read2Write()), false, DefaultRelMetadataProvider.INSTANCE),
            Optional.empty());
    public static final OptimizationStage VOLCANO = new OptimizationStage("Volcano",
        SQRLPrograms.ENUMERABLE_VOLCANO, Optional.of(EnumerableConvention.INSTANCE)
        );

    //Enumerable
//    public static final OptimizationStage SQRL_ENUMERABLE_HEP = new OptimizationStage("SQRL2Enumerable",
//            Programs.hep(List.of(new SqrlExpansionRelRule()), false, DefaultRelMetadataProvider.INSTANCE),
//            Optional.empty());
//    public static final OptimizationStage STANDARD_ENUMERABLE_RULES = new OptimizationStage("standardEnumerable",
//            Programs.sequence(
//                    Programs.subQuery(DefaultRelMetadataProvider.INSTANCE),
//                    SQRLPrograms.ENUMERABLE_VOLCANO,
//                    Programs.calc(DefaultRelMetadataProvider.INSTANCE),
//                    Programs.hep(
//                            List.of(new SqrlDataSourceToEnumerableConverterRule()), false, DefaultRelMetadataProvider.INSTANCE
//                    )
//            ),
//            Optional.of(EnumerableConvention.INSTANCE));
//    public static final List<OptimizationStage> ENUMERABLE_STAGES = ImmutableList.of(SQRL_ENUMERABLE_HEP,STANDARD_ENUMERABLE_RULES);

}
