package com.datasqrl.engine.stream.flink;

//import com.datasqrl.config.provider.JDBCConnectionProvider;
import com.datasqrl.engine.stream.StreamEngine;
import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.util.OutputTag;

//import org.apache.flink.connector.jdbc.JdbcConnectionOptions;

public interface FlinkStreamEngine extends StreamEngine {

  Builder createJob();

  interface Builder extends StreamEngine.Builder {

    StreamExecutionEnvironment getEnvironment();

    StreamTableEnvironment getTableEnvironment();

    OutputTag<ProcessError> getErrorTag(final String errorName);

    void setJobType(JobType jobType);

    @Override
    FlinkJob build();

  }

  FlinkJob createStreamJob(StreamExecutionEnvironment execEnv, JobType type);
//
//  static JdbcConnectionOptions getFlinkJDBC(JDBCConnectionProvider jdbc) {
//    return new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
//        .withUrl(jdbc.getDbURL())
//        .withDriverName(jdbc.getDriverName())
//        .withUsername(jdbc.getUser())
//        .withPassword(jdbc.getPassword())
//        .build();
//  }

  @AllArgsConstructor
  @Getter
  enum JobType {
    MONITOR("monitor"), SCRIPT("script");

    private final String name;

    public String toString() {
      return name;
    }
  }

  static String getFlinkName(String type, String identifier) {
    return type + "[" + identifier + "]";
  }

  @Slf4j
  abstract class FlinkJob implements StreamEngine.Job {

    private final StreamExecutionEnvironment execEnv;
    private final JobType type;
    protected Status status = Status.PREPARING;
    private String jobId = null;

    protected FlinkJob(StreamExecutionEnvironment execEnv, JobType type) {
      this.execEnv = execEnv;
      this.type = type;
    }

    @Override
    public String getId() {
      Preconditions.checkArgument(jobId != null,
          "Job id is only available once job has been submitted");
      //TODO: need to replace by jobid.toHex
      return jobId;
    }

    @Override
    public void execute(String name) {
      try {
        //TODO: move to async execution
        JobExecutionResult result = execEnv.execute(getFlinkName(type.getName(), name));
        jobId = result.getJobID().toHexString();
      } catch (Exception e) {
        status = Status.FAILED;
        throw new RuntimeException(e);
//        log.error("Failed to launch Flink job",e);
      }
      status = Status.RUNNING;
    }

    @Override
    public void cancel() {
      //TODO
      status = Status.COMPLETED;
      throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Status getStatus() {
      return status;
    }
  }

}