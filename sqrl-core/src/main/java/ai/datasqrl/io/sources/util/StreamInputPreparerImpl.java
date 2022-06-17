package ai.datasqrl.io.sources.util;

import ai.datasqrl.config.error.ErrorCollector;
import ai.datasqrl.physical.stream.StreamEngine;
import ai.datasqrl.physical.stream.FunctionWithError;
import ai.datasqrl.physical.stream.StreamHolder;
import ai.datasqrl.io.formats.Format;
import ai.datasqrl.io.formats.TextLineFormat;
import ai.datasqrl.io.sources.SourceRecord;
import ai.datasqrl.io.sources.dataset.SourceTable;
import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;

public class StreamInputPreparerImpl implements StreamInputPreparer {

    public static final String PARSE_ERROR_TAG = "parse";

    public boolean isRawInput(SourceTable table) {
        //TODO: support other flexible formats
        return table.getConfiguration().getFormatParser() instanceof TextLineFormat.Parser;
    }

    public StreamHolder<SourceRecord.Raw> getRawInput(SourceTable table, StreamEngine.Builder builder) {
        Preconditions.checkArgument(isRawInput(table), "Not a valid raw input table: " + table);
        Format.Parser parser = table.getConfiguration().getFormatParser();
        if (parser instanceof TextLineFormat.Parser) {
            return text2Record(builder.fromTextSource(table),
                    (TextLineFormat.Parser)table.getConfiguration().getFormatParser());
        } else {
            throw new UnsupportedOperationException("Should never happen");
        }
    }

    public StreamHolder<SourceRecord.Raw> text2Record(StreamHolder<TimeAnnotatedRecord<String>> textSource,
                                                      TextLineFormat.Parser textparser) {
        return textSource.mapWithError(new MapText2Raw(textparser), PARSE_ERROR_TAG, SourceRecord.Raw.class);
    }

    @AllArgsConstructor
    private static class MapText2Raw implements FunctionWithError<TimeAnnotatedRecord<String>, SourceRecord.Raw> {

        private final TextLineFormat.Parser textparser;

        @Override
        public Optional<SourceRecord.Raw> apply(TimeAnnotatedRecord<String> t, Consumer<ErrorCollector> errorCollector) {
            Format.Parser.Result r = textparser.parse(t.getRecord());
            if (r.isSuccess()) {
                Instant sourceTime = r.getSourceTime();
                if (sourceTime==null) sourceTime = t.getSourceTime();
                return Optional.of(new SourceRecord.Raw(r.getRecord(),sourceTime));
            } else {
                return Optional.empty();
            }
        }
    }


}