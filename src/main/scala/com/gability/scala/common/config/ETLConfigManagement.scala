package com.gability.scala.common.config

import com.gability.scala.common.utils.TypeParser._
import org.apache.spark.sql.{Dataset, SparkSession}
import Constants._
import com.gability.scala.common.metadata.Metadata._

object ETLConfigManagement {

  //TODO: ADD reference date for running and for unit, integration test
  //TODO: Analyze test/release/prod.Properties options

  //this
  def getSparkSession(jobName: String,
                      sessionConfig: Map[String, String] =
                        Map.empty[String, String],
                      enableHiveSupport: Boolean = false): SparkSession = {
    val sessionBuilder: SparkSession.Builder = SparkSession
      .builder()
      .appName(jobName)
      .master("local[*]")//TODO: remove this local test
    sessionConfig.foreach(conf => sessionBuilder.config(conf._1, conf._2))
    if (enableHiveSupport) sessionBuilder.enableHiveSupport().getOrCreate()
    else sessionBuilder.getOrCreate()
  }

  def getJobConfig(jobId: String, jobName: String,configTableName:String="job_config"): JobConfig = {
    val ss = getSparkSession(jobName)
    val jobParams = getPrimaryModelParams(ss, jobId,configTableName)
    JobConfig(jobParams, ss)
  }

  private def getPrimaryModelParams(ss: SparkSession,
                                    jobId: String,configTableName:String): Dataset[ConfigParam] = {
    val jobIdParsed = parseString(jobId, ZERO_LONG)
    import ss.implicits._
    ss.table(configTableName)
      .as[JobsParamConfig]
      .filter(jobs => jobs.job_id == jobId)
      .filter(conf => conf.config_seq == PRIMARY_PARAM_SEQ)
      .map(p => ConfigParam(jobIdParsed, Map(p.config_type -> p.config_value)))
      .as[ConfigParam]

  }

}
