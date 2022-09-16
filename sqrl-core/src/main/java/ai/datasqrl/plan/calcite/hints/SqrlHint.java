package ai.datasqrl.plan.calcite.hints;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.hint.Hintable;
import org.apache.calcite.rel.hint.RelHint;
import org.apache.calcite.tools.RelBuilder;

import java.util.List;
import java.util.Optional;

public interface SqrlHint {

    RelHint getHint();

    default RelNode addHint(Hintable node) {
        return node.attachHints(List.of(getHint()));
    }

    default RelBuilder addTo(RelBuilder relBuilder) {
        return relBuilder.hints(getHint());
    }

    static<H extends SqrlHint> Optional<H> fromRel(RelNode node, SqrlHint.Constructor<H> hintConstructor) {
        if (node instanceof Hintable) {
            return ((Hintable)node).getHints().stream()
                    .filter(h -> h.hintName.equalsIgnoreCase(hintConstructor.getName()))
                    .filter(h -> h.inheritPath.isEmpty()) //we only want the hint on that particular join, not inherited ones
                    .findFirst().map(hintConstructor::fromHint);
        }
        return Optional.empty();
    }


    public interface Constructor<H extends SqrlHint> {

        public String getName();

        public H fromHint(RelHint hint);

    }

}
